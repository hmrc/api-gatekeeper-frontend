/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.http.Status._
import play.api.libs.json.{Format, JsSuccess, Json}
import uk.gov.hmrc.http.HttpErrorFunctions._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields.{SubscriptionFieldDefinition, SubscriptionFieldValue, _}
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.services.SubscriptionFieldsService.{DefinitionsByApiVersion, SubscriptionFieldsConnector}

abstract class AbstractSubscriptionFieldsConnector(implicit ec: ExecutionContext) extends SubscriptionFieldsConnector {
  protected val httpClient: HttpClient
  val environment: Environment
  val serviceBaseUrl: String

  import SubscriptionFieldsConnector.JsonFormatters._
  import SubscriptionFieldsConnector._

  def http: HttpClient

  def fetchFieldsValuesWithPrefetchedDefinitions(
      clientId: ClientId,
      apiIdentifier: ApiIdentifier,
      definitionsCache: DefinitionsByApiVersion
    )(implicit hc: HeaderCarrier
    ): Future[List[SubscriptionFieldValue]] = {

    def getDefinitions(): Future[List[SubscriptionFieldDefinition]] = Future.successful(definitionsCache.getOrElse(apiIdentifier, List.empty))

    internalFetchFieldValues(() => getDefinitions())(clientId, apiIdentifier)
  }

  def fetchFieldValues(clientId: ClientId, apiContext: ApiContext, versionNbr: ApiVersionNbr)(implicit hc: HeaderCarrier): Future[List[SubscriptionFieldValue]] = {

    def getDefinitions() = fetchFieldDefinitions(apiContext, versionNbr)

    internalFetchFieldValues(() => getDefinitions())(clientId, ApiIdentifier(apiContext, versionNbr))
  }

  def fetchAllFieldValues()(implicit hc: HeaderCarrier): Future[List[ApplicationApiFieldValues]] = {
    val url = s"$serviceBaseUrl/field"
    http.GET[AllApiFieldValues](url).map(_.subscriptions)
  }

  private def internalFetchFieldValues(
      getDefinitions: () => Future[List[SubscriptionFieldDefinition]]
    )(
      clientId: ClientId,
      apiIdentifier: ApiIdentifier
    )(implicit hc: HeaderCarrier
    ): Future[List[SubscriptionFieldValue]] = {

    def joinFieldValuesToDefinitions(defs: List[SubscriptionFieldDefinition], fieldValues: Fields.Alias): List[SubscriptionFieldValue] = {
      defs.map(field => SubscriptionFieldValue(field, fieldValues.getOrElse(field.name, FieldValue.empty)))
    }

    def ifDefinitionsGetValues(definitions: List[SubscriptionFieldDefinition]): Future[Option[ApplicationApiFieldValues]] = {
      if (definitions.isEmpty) {
        Future.successful(None)
      } else {
        fetchApplicationApiValues(clientId, apiIdentifier.context, apiIdentifier.versionNbr)
      }
    }

    for {
      definitions: List[SubscriptionFieldDefinition] <- getDefinitions()
      subscriptionFields                             <- ifDefinitionsGetValues(definitions)
      fieldValues                                     = subscriptionFields.fold(Fields.empty)(_.fields)
    } yield joinFieldValuesToDefinitions(definitions, fieldValues)
  }

  def fetchFieldDefinitions(apiContext: ApiContext, apiVersion: ApiVersionNbr)(implicit hc: HeaderCarrier): Future[List[SubscriptionFieldDefinition]] = {
    val url = urlSubscriptionFieldDefinition(apiContext, apiVersion)
    http.GET[Option[ApiFieldDefinitions]](url).map(_.fold(List.empty[SubscriptionFieldDefinition])(_.fieldDefinitions.map(toDomain)))
  }

  def fetchAllFieldDefinitions()(implicit hc: HeaderCarrier): Future[DefinitionsByApiVersion] = {
    val url = s"$serviceBaseUrl/definition"
    http.GET[Option[AllApiFieldDefinitions]](url)
      .map(_.fold(DefinitionsByApiVersion.empty)(toDomain))
  }

  def saveFieldValues(
      clientId: ClientId,
      apiContext: ApiContext,
      apiVersion: ApiVersionNbr,
      fields: Fields.Alias
    )(implicit hc: HeaderCarrier
    ): Future[SaveSubscriptionFieldsResponse] = {
    val url = urlSubscriptionFieldValues(clientId, apiContext, apiVersion)

    http.PUT[SubscriptionFieldsPutRequest, HttpResponse](url, SubscriptionFieldsPutRequest(clientId, apiContext, apiVersion, fields)).map(_ match {
      case resp: HttpResponse if (is2xx(resp.status)) => SaveSubscriptionFieldsSuccessResponse

      case HttpResponse(BAD_REQUEST, body, _) =>
        Json.parse(body).validate[Map[String, String]] match {
          case s: JsSuccess[Map[String, String]] => SaveSubscriptionFieldsFailureResponse(s.get)
          case _                                 => SaveSubscriptionFieldsFailureResponse(Map.empty)
        }
      case HttpResponse(status, body, _)      => throw UpstreamErrorResponse(body, status)
    })
  }

  def deleteFieldValues(clientId: ClientId, apiContext: ApiContext, apiVersion: ApiVersionNbr)(implicit hc: HeaderCarrier): Future[FieldsDeleteResult] = {
    val url = urlSubscriptionFieldValues(clientId, apiContext, apiVersion)
    http.DELETE[HttpResponse](url).map(_.status match {
      case NO_CONTENT | NOT_FOUND => FieldsDeleteSuccessResult
      case _                      => FieldsDeleteFailureResult
    }) recover {
      case _ => FieldsDeleteFailureResult
    }
  }

  private def fetchApplicationApiValues(clientId: ClientId, apiContext: ApiContext, apiVersion: ApiVersionNbr)(implicit hc: HeaderCarrier): Future[Option[ApplicationApiFieldValues]] = {
    val url = urlSubscriptionFieldValues(clientId, apiContext, apiVersion)
    http.GET[Option[ApplicationApiFieldValues]](url)
  }

  private def urlSubscriptionFieldValues(clientId: ClientId, apiContext: ApiContext, apiVersion: ApiVersionNbr) =
    SubscriptionFieldsConnector.urlSubscriptionFieldValues(serviceBaseUrl)(clientId, apiContext, apiVersion)

  private def urlSubscriptionFieldDefinition(apiContext: ApiContext, apiVersion: ApiVersionNbr) =
    SubscriptionFieldsConnector.urlSubscriptionFieldDefinition(serviceBaseUrl)(apiContext, apiVersion)
}

object SubscriptionFieldsConnector extends UrlEncoders {

  def urlSubscriptionFieldValues(baseUrl: String)(clientId: ClientId, apiContext: ApiContext, apiVersion: ApiVersionNbr) =
    s"$baseUrl/field/application/${clientId.urlEncode}/context/${apiContext.urlEncode}/version/${apiVersion.urlEncode}"

  def urlSubscriptionFieldDefinition(baseUrl: String)(apiContext: ApiContext, apiVersion: ApiVersionNbr) =
    s"$baseUrl/definition/context/${apiContext.urlEncode}/version/${apiVersion.urlEncode}"

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
    fs.apis.map(fd =>
      ApiIdentifier(fd.apiContext, fd.apiVersion) -> fd.fieldDefinitions.map(toDomain)
    )
      .toMap
  }

  private[connectors] case class FieldDefinition(name: FieldName, description: String, hint: String, `type`: String, shortDescription: String)

  private[connectors] case class ApiFieldDefinitions(apiContext: ApiContext, apiVersion: ApiVersionNbr, fieldDefinitions: List[FieldDefinition])

  private[connectors] case class AllApiFieldDefinitions(apis: List[ApiFieldDefinitions])

  private[connectors] case class AllApiFieldValues(subscriptions: List[ApplicationApiFieldValues])

  object JsonFormatters extends APIDefinitionFormatters {
    implicit val format: Format[ApplicationApiFieldValues]                            = Json.format[ApplicationApiFieldValues]
    implicit val formatFieldDefinition: Format[FieldDefinition]                       = Json.format[FieldDefinition]
    implicit val formatApiFieldDefinitionsResponse: Format[ApiFieldDefinitions]       = Json.format[ApiFieldDefinitions]
    implicit val formatAllApiFieldDefinitionsResponse: Format[AllApiFieldDefinitions] = Json.format[AllApiFieldDefinitions]
    implicit val formatAllApplicationApiFieldValues: Format[AllApiFieldValues]        = Json.format[AllApiFieldValues]
  }
}

@Singleton
class SandboxSubscriptionFieldsConnector @Inject() (
    val appConfig: AppConfig,
    val httpClient: HttpClient,
    val proxiedHttpClient: ProxiedHttpClient
  )(implicit val ec: ExecutionContext
  ) extends AbstractSubscriptionFieldsConnector {

  val environment: Environment = Environment.SANDBOX
  val serviceBaseUrl: String   = appConfig.subscriptionFieldsSandboxBaseUrl
  val useProxy: Boolean        = appConfig.subscriptionFieldsSandboxUseProxy
  val bearerToken: String      = appConfig.subscriptionFieldsSandboxBearerToken
  val apiKey: String           = appConfig.subscriptionFieldsSandboxApiKey
  val http: HttpClient         = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient
}

@Singleton
class ProductionSubscriptionFieldsConnector @Inject() (val appConfig: AppConfig, val httpClient: HttpClient)(implicit val ec: ExecutionContext)
    extends AbstractSubscriptionFieldsConnector {

  val environment: Environment = Environment.PRODUCTION
  val serviceBaseUrl: String   = appConfig.subscriptionFieldsProductionBaseUrl
  val http                     = httpClient
}
