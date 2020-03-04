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

import java.net.URLEncoder.encode
import java.util.UUID

import akka.actor.ActorSystem
import akka.pattern.FutureTimeoutSupport
import config.AppConfig
import javax.inject.{Inject, Singleton}
import model.apiSubscriptionFields._
import model._
import model.Environment.Environment
import play.api.Logger
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.{Format, Json}
import services.SubscriptionFieldsService.DefinitionsByApiVersion
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.Retries

import scala.concurrent.{ExecutionContext, Future}

abstract class SubscriptionFieldsConnector(implicit ec: ExecutionContext) extends Retries {

  import SubscriptionFieldsConnector._

  protected val httpClient: HttpClient
  protected val proxiedHttpClient: ProxiedHttpClient
  val environment: Environment
  val serviceBaseUrl: String
  val useProxy: Boolean
  val bearerToken: String
  val apiKey: String

  def http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient

  private def fetchApplicationApiValues(clientId: String, apiContext: String, apiVersion: String)
                                       (implicit hc: HeaderCarrier): Future[Option[SubscriptionFields]] = {
    val url = urlSubscriptionFieldValues(clientId, apiContext, apiVersion)
    retry {
      // TODO: Remove me
      Logger.info(s"fetchFieldValues() - About to call $url in ${environment.toString}")
      http.GET[SubscriptionFields](url).map(Some(_))
    } recover recovery(None)
  }

  def fetchFieldsValuesWithDefinitionCache(clientId: String, apiContextVersion: ApiContextVersion, definitionsCache: DefinitionsByApiVersion)
                       (implicit hc: HeaderCarrier): Future[Seq[SubscriptionFieldValue]] = {
    def joinFieldValuesToDefinitions(defs: Seq[SubscriptionFieldDefinition], fieldValues: Fields): Seq[SubscriptionFieldValue] = {
      defs.map(field => SubscriptionFieldValue(field, fieldValues.get(field.name)))
    }

    def ifDefinitionsGetValues(definitions: Seq[SubscriptionFieldDefinition]): Future[Option[SubscriptionFields]] = {
      if (definitions.isEmpty) {
        Future.successful(None)
      }
      else {
        fetchApplicationApiValues(clientId, apiContextVersion.context, apiContextVersion.version)
      }
    }

    val definitions: Seq[SubscriptionFieldDefinition] = definitionsCache.getOrElse(apiContextVersion,Seq.empty)

    for {
      values: Option[SubscriptionFields] <- ifDefinitionsGetValues(definitions)
      fields: Fields = values.fold(Map.empty[String, String])(s => s.fields)
    } yield joinFieldValuesToDefinitions(definitions, fields)
  }

  // TODO: Test me
  def fetchAllFieldDefinitions()(implicit hc: HeaderCarrier): Future[DefinitionsByApiVersion] = {

    def toMapEntry(definitions: FieldDefinitionsResponse) =
      (ApiContextVersion(definitions.apiContext, definitions.apiVersion), definitions.fieldDefinitions)

    val url = s"$serviceBaseUrl/definition"
    // TODO: Remove me
    Logger.info(s"fetchAllFieldDefinitions() - About to call $url in ${environment.toString}")
    retry {
      for {
        response <- http.GET[AllFieldDefinitionsResponse](url)
        mapEntries = response.apis.map(toMapEntry)
      } yield mapEntries.toMap

    } recover recovery(DefinitionsByApiVersion.empty)
  }

  def saveFieldValues(clientId: String, apiContext: String, apiVersion: String, fields: Fields)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val url = urlSubscriptionFieldValues(clientId, apiContext, apiVersion)
    http.PUT[SubscriptionFieldsPutRequest, HttpResponse](url, SubscriptionFieldsPutRequest(clientId, apiContext, apiVersion, fields))
  }

  def deleteFieldValues(clientId: String, apiContext: String, apiVersion: String)(implicit hc: HeaderCarrier): Future[FieldsDeleteResult] = {
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

  private def urlEncode(str: String, encoding: String = "UTF-8") = encode(str, encoding)

  private def urlSubscriptionFieldValues(clientId: String, apiContext: String, apiVersion: String) =
    s"$serviceBaseUrl/field/application/${urlEncode(clientId)}/context/${urlEncode(apiContext)}/version/${urlEncode(apiVersion)}"

  private def urlSubscriptionFieldDefinition(apiContext: String, apiVersion: String) =
    s"$serviceBaseUrl/definition/context/${urlEncode(apiContext)}/version/${urlEncode(apiVersion)}"

  private def recovery[T](value: T): PartialFunction[Throwable, T] = {
    case _: NotFoundException => value
  }
}

object SubscriptionFieldsConnector {

  // TODO: Move all the formatters here

  private[connectors] case class DefinitionDto(name: String, description: String, hint: String, `type`: String)

  private[connectors] case class ValueDto(name: String, description: String, hint: String, `type`: String, value: Option[String])

  private[connectors] case class SubscriptionFields(clientId: String, apiContext: String, apiVersion: String, fieldsId: UUID, fields: Map[String, String])

  object SubscriptionFields {
    implicit val format: Format[SubscriptionFields] = Json.format[SubscriptionFields]
  }
}

@Singleton
class SandboxSubscriptionFieldsConnector @Inject()(val appConfig: AppConfig,
                                                   val httpClient: HttpClient,
                                                   val proxiedHttpClient: ProxiedHttpClient,
                                                   val actorSystem: ActorSystem,
                                                   val futureTimeout: FutureTimeoutSupport)(implicit val ec: ExecutionContext)
  extends SubscriptionFieldsConnector {

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
  extends SubscriptionFieldsConnector {

  val environment: Environment = Environment.PRODUCTION
  val serviceBaseUrl: String = appConfig.subscriptionFieldsProductionBaseUrl
  val useProxy: Boolean = appConfig.subscriptionFieldsProductionUseProxy
  val bearerToken: String = appConfig.subscriptionFieldsProductionBearerToken
  val apiKey: String = appConfig.subscriptionFieldsProductionApiKey
}
