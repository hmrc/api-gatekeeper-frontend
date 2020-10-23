/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors

import java.util.UUID

import akka.actor.ActorSystem
import akka.pattern.FutureTimeoutSupport
import config.AppConfig
import javax.inject.{Inject, Singleton}
import model.Environment.Environment
import model.SubscriptionFields.{SubscriptionFieldDefinition, SubscriptionFieldValue, _}
import model._
import play.api.http.Status.{NO_CONTENT, OK, BAD_REQUEST, CREATED}
import play.api.libs.json.{Format, Json, JsSuccess}
import services.SubscriptionFieldsService.{DefinitionsByApiVersion, SubscriptionFieldsConnector}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.Retries

import scala.concurrent.{ExecutionContext, Future}

abstract class AbstractSubscriptionFieldsConnector(implicit ec: ExecutionContext) extends SubscriptionFieldsConnector with Retries {
  protected val httpClient: HttpClient
  protected val proxiedHttpClient: ProxiedHttpClient
  val environment: Environment
  val serviceBaseUrl: String
  val useProxy: Boolean
  val bearerToken: String
  val apiKey: String

  import SubscriptionFieldsConnector.JsonFormatters._
  import SubscriptionFieldsConnector._

  def http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient

  def fetchFieldsValuesWithPrefetchedDefinitions(clientId: ClientId, apiIdentifier: ApiIdentifier, definitionsCache: DefinitionsByApiVersion)
                                                (implicit hc: HeaderCarrier): Future[Seq[SubscriptionFieldValue]] = {

    def getDefinitions() =
      Future.successful(definitionsCache.getOrElse(apiIdentifier, Seq.empty))

    internalFetchFieldValues(getDefinitions)(clientId, apiIdentifier)
  }

  def fetchFieldValues(clientId: ClientId, apiContext: ApiContext, version: ApiVersion)(implicit hc: HeaderCarrier): Future[Seq[SubscriptionFieldValue]] = {

    def getDefinitions() =
      fetchFieldDefinitions(apiContext, version)

    internalFetchFieldValues(getDefinitions)(clientId, ApiIdentifier(apiContext, version))
  }

  private def internalFetchFieldValues(getDefinitions: () => Future[Seq[SubscriptionFieldDefinition]])
                                      (clientId: ClientId,
                                       apiIdentifier: ApiIdentifier)
                                      (implicit hc: HeaderCarrier): Future[Seq[SubscriptionFieldValue]] = {

    def joinFieldValuesToDefinitions(defs: Seq[SubscriptionFieldDefinition], fieldValues: Fields.Alias): Seq[SubscriptionFieldValue] = {
      defs.map(field => SubscriptionFieldValue(field, fieldValues.getOrElse(field.name, FieldValue.empty)))
    }

    def ifDefinitionsGetValues(definitions: Seq[SubscriptionFieldDefinition]): Future[Option[ApplicationApiFieldValues]] = {
      if (definitions.isEmpty) {
        Future.successful(None)
      }
      else {
        fetchApplicationApiValues(clientId, apiIdentifier.context, apiIdentifier.version)
      }
    }

    for {
      definitions: Seq[SubscriptionFieldDefinition] <- getDefinitions()
      subscriptionFields <- ifDefinitionsGetValues(definitions)
      fieldValues = subscriptionFields.fold(Fields.empty)(_.fields)
    }  yield joinFieldValuesToDefinitions(definitions, fieldValues)
  }

  def fetchFieldDefinitions(apiContext: ApiContext, apiVersion: ApiVersion)(implicit hc: HeaderCarrier): Future[Seq[SubscriptionFieldDefinition]] = {
    val url = urlSubscriptionFieldDefinition(apiContext, apiVersion)
    retry {
      http.GET[ApiFieldDefinitions](url).map(response => response.fieldDefinitions.map(toDomain))
    } recover recovery(Seq.empty[SubscriptionFieldDefinition])
  }

  def fetchAllFieldDefinitions()(implicit hc: HeaderCarrier): Future[DefinitionsByApiVersion] = {
    val url = s"$serviceBaseUrl/definition"
    retry {
      for {
        response <- http.GET[AllApiFieldDefinitions](url)
      } yield toDomain(response)

    } recover recovery(DefinitionsByApiVersion.empty)
  }

  def saveFieldValues(clientId: ClientId, apiContext: ApiContext, apiVersion: ApiVersion, fields: Fields.Alias)
                     (implicit hc: HeaderCarrier): Future[SaveSubscriptionFieldsResponse] = {
    val url = urlSubscriptionFieldValues(clientId, apiContext, apiVersion)

    import CustomResponseHandlers.permissiveBadRequestResponseHandler

    http.PUT[SubscriptionFieldsPutRequest, HttpResponse](url, SubscriptionFieldsPutRequest(clientId, apiContext, apiVersion, fields)).map { response =>
      response.status match {
        case BAD_REQUEST =>
          Json.parse(response.body).validate[Map[String, String]] match {
            case s: JsSuccess[Map[String, String]] => SaveSubscriptionFieldsFailureResponse(s.get)
            case _ => SaveSubscriptionFieldsFailureResponse(Map.empty)
          }
        case OK | CREATED => SaveSubscriptionFieldsSuccessResponse
      }
    }
  }

  def deleteFieldValues(clientId: ClientId, apiContext: ApiContext, apiVersion: ApiVersion)(implicit hc: HeaderCarrier): Future[FieldsDeleteResult] = {
    val url = urlSubscriptionFieldValues(clientId, apiContext, apiVersion)
    http.DELETE[HttpResponse](url).map { response =>
      response.status match {
        case NO_CONTENT => FieldsDeleteSuccessResult
        case _ => FieldsDeleteFailureResult
      }
    } recover {
      case _: NotFoundException => FieldsDeleteSuccessResult
      case _ => FieldsDeleteFailureResult
    }
  }

  private def fetchApplicationApiValues(clientId: ClientId, apiContext: ApiContext, apiVersion: ApiVersion)
                                       (implicit hc: HeaderCarrier): Future[Option[ApplicationApiFieldValues]] = {
    val url = urlSubscriptionFieldValues(clientId, apiContext, apiVersion)
    retry {
      http.GET[ApplicationApiFieldValues](url).map(Some(_))
    } recover recovery(None)
  }

  private def urlSubscriptionFieldValues(clientId: ClientId, apiContext: ApiContext, apiVersion: ApiVersion) =
    s"$serviceBaseUrl/field/application/${clientId.urlEncode()}/context/${apiContext.urlEncode()}/version/${apiVersion.urlEncode()}"

  private def urlSubscriptionFieldDefinition(apiContext: ApiContext, apiVersion: ApiVersion) =
    s"$serviceBaseUrl/definition/context/${apiContext.urlEncode()}/version/${apiVersion.urlEncode()}"

  private def recovery[T](value: T): PartialFunction[Throwable, T] = {
    case _: NotFoundException => value
  }
}

object SubscriptionFieldsConnector {

  def toDomain(f: FieldDefinition): SubscriptionFieldDefinition = {
    SubscriptionFieldDefinition(
      name = f.name,
      description = f.description,
      `type` = f.`type`,
      hint = f.hint,
      shortDescription = f.shortDescription
    )
  }

  def toDomain(fs: AllApiFieldDefinitions): DefinitionsByApiVersion = {
    fs.apis.map( fd =>
      ApiIdentifier(fd.apiContext, fd.apiVersion) -> fd.fieldDefinitions.map(toDomain)
    )
    .toMap
  }

  private[connectors] case class ApplicationApiFieldValues(clientId: ClientId, apiContext: ApiContext, apiVersion: ApiVersion, fieldsId: UUID, fields: Map[FieldName, FieldValue])

  private[connectors] case class FieldDefinition(name: FieldName, description: String, hint: String, `type`: String, shortDescription: String)

  private[connectors] case class ApiFieldDefinitions(apiContext: ApiContext, apiVersion: ApiVersion, fieldDefinitions: List[FieldDefinition])

  private[connectors] case class AllApiFieldDefinitions(apis: Seq[ApiFieldDefinitions])

  object JsonFormatters extends APIDefinitionFormatters {
    implicit val format: Format[ApplicationApiFieldValues] = Json.format[ApplicationApiFieldValues]
    implicit val formatFieldDefinition: Format[FieldDefinition] = Json.format[FieldDefinition]
    implicit val formatApiFieldDefinitionsResponse: Format[ApiFieldDefinitions] = Json.format[ApiFieldDefinitions]
    implicit val formatAllApiFieldDefinitionsResponse: Format[AllApiFieldDefinitions] = Json.format[AllApiFieldDefinitions]
  }
}

@Singleton
class SandboxSubscriptionFieldsConnector @Inject()(val appConfig: AppConfig,
                                                   val httpClient: HttpClient,
                                                   val proxiedHttpClient: ProxiedHttpClient,
                                                   val actorSystem: ActorSystem,
                                                   val futureTimeout: FutureTimeoutSupport)(implicit val ec: ExecutionContext)
  extends AbstractSubscriptionFieldsConnector {

  val environment: Environment = Environment.SANDBOX
  val serviceBaseUrl: String = appConfig.subscriptionFieldsSandboxBaseUrl
  val useProxy: Boolean = appConfig.subscriptionFieldsSandboxUseProxy
  val bearerToken: String = appConfig.subscriptionFieldsSandboxBearerToken
  val apiKey: String = appConfig.subscriptionFieldsSandboxApiKey
}

@Singleton
class ProductionSubscriptionFieldsConnector @Inject()(val appConfig: AppConfig,
                                                      val httpClient: HttpClient,
                                                      val proxiedHttpClient: ProxiedHttpClient,
                                                      val actorSystem: ActorSystem,
                                                      val futureTimeout: FutureTimeoutSupport)(implicit val ec: ExecutionContext)
  extends AbstractSubscriptionFieldsConnector {

  val environment: Environment = Environment.PRODUCTION
  val serviceBaseUrl: String = appConfig.subscriptionFieldsProductionBaseUrl
  val useProxy: Boolean = appConfig.subscriptionFieldsProductionUseProxy
  val bearerToken: String = appConfig.subscriptionFieldsProductionBearerToken
  val apiKey: String = appConfig.subscriptionFieldsProductionApiKey
}
