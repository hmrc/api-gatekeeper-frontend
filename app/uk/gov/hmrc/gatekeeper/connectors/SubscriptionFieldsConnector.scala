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
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse, _}

import uk.gov.hmrc.apiplatform.modules.applications.subscriptions.domain.models.FieldName
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields.{SubscriptionFieldDefinition, _}
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.services.SubscriptionFieldsService.{DefinitionsByApiVersion, SubscriptionFieldsConnector}

abstract class AbstractSubscriptionFieldsConnector(implicit ec: ExecutionContext) extends SubscriptionFieldsConnector {
  val environment: Environment
  val serviceBaseUrl: String

  import SubscriptionFieldsConnector.JsonFormatters._
  import SubscriptionFieldsConnector._

  def http: HttpClientV2

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder

  def fetchAllFieldValues()(implicit hc: HeaderCarrier): Future[List[ApplicationApiFieldValues]] = {
    val url = url"$serviceBaseUrl/field"
    configureEbridgeIfRequired(http.get(url)).execute[AllApiFieldValues].map(_.subscriptions)
  }

  def saveFieldValues(
      clientId: ClientId,
      apiContext: ApiContext,
      apiVersion: ApiVersionNbr,
      fields: Fields.Alias
    )(implicit hc: HeaderCarrier
    ): Future[SaveSubscriptionFieldsResponse] = {
    val url = urlSubscriptionFieldValues(clientId, apiContext, apiVersion)

    configureEbridgeIfRequired(http.put(url"$url"))
      .withBody(Json.toJson(SubscriptionFieldsPutRequest(clientId, apiContext, apiVersion, fields)))
      .execute[HttpResponse]
      .map(_ match {
        case resp: HttpResponse if (is2xx(resp.status)) => SaveSubscriptionFieldsSuccessResponse

        case HttpResponse(BAD_REQUEST, body, _) =>
          Json.parse(body).validate[Map[String, String]] match {
            case s: JsSuccess[Map[String, String]] => SaveSubscriptionFieldsFailureResponse(s.get)
            case _                                 => SaveSubscriptionFieldsFailureResponse(Map.empty)
          }
        case HttpResponse(status, body, _)      => throw UpstreamErrorResponse(body, status)
      })
  }

  private def urlSubscriptionFieldValues(clientId: ClientId, apiContext: ApiContext, apiVersion: ApiVersionNbr) =
    SubscriptionFieldsConnector.urlSubscriptionFieldValues(serviceBaseUrl)(clientId, apiContext, apiVersion)
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
    val http: HttpClientV2
  )(implicit val ec: ExecutionContext
  ) extends AbstractSubscriptionFieldsConnector {

  val environment: Environment = Environment.SANDBOX
  val serviceBaseUrl: String   = appConfig.subscriptionFieldsSandboxBaseUrl
  val useProxy: Boolean        = appConfig.subscriptionFieldsSandboxUseProxy
  val bearerToken: String      = appConfig.subscriptionFieldsSandboxBearerToken
  val apiKey: String           = appConfig.subscriptionFieldsSandboxApiKey

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder =
    EbridgeConfigurator.configure(useProxy, bearerToken, apiKey)(requestBuilder)
}

@Singleton
class ProductionSubscriptionFieldsConnector @Inject() (val appConfig: AppConfig, val http: HttpClientV2)(implicit val ec: ExecutionContext)
    extends AbstractSubscriptionFieldsConnector {

  val environment: Environment = Environment.PRODUCTION
  val serviceBaseUrl: String   = appConfig.subscriptionFieldsProductionBaseUrl

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder = requestBuilder
}
