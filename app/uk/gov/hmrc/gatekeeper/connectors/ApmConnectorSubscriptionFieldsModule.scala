/*
 * Copyright 2025 HM Revenue & Customs
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

import scala.concurrent.Future

import play.api.http.Status._
import play.api.http.{ContentTypes, HeaderNames}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse, _}
import uk.gov.hmrc.http.HttpErrorFunctions._

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.subscriptionfields.domain.models.FieldName
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields._
import uk.gov.hmrc.gatekeeper.models._

object ApmConnectorSubscriptionFieldsModule {
  def urlSubscriptionFieldValues(baseUrl: String)(environment: Environment, clientId: ClientId, apiContext: ApiContext, apiVersion: ApiVersionNbr) =
    url"$baseUrl/field/application/${clientId}/context/${apiContext}/version/${apiVersion}?environment=${environment}"
}

trait ApmConnectorSubscriptionFieldsModule extends ApmConnectorModule {
  import play.api.libs.json._
  private[this] implicit val formatSubscriptionFieldDefinition: OFormat[SubscriptionFieldDefinition] = Json.format[SubscriptionFieldDefinition]

  private[this] val baseUrl = s"${config.serviceBaseUrl}/subscription-fields"

  def getAllFieldDefinitions(environment: Environment)(implicit hc: HeaderCarrier): Future[ApiDefinitionFields.Alias] = {
    http.get((url"${baseUrl}?environment=$environment"))
      .execute[Map[ApiContext, Map[ApiVersionNbr, Map[FieldName, SubscriptionFieldDefinition]]]]
  }

  def saveFieldValues(
    environment: Environment,
    clientId: ClientId,
    apiContext: ApiContext,
    apiVersion: ApiVersionNbr,
    fields: Fields.Alias
  )(implicit hc: HeaderCarrier
  ): Future[SaveSubscriptionFieldsResponse] = {
    val url = ApmConnectorSubscriptionFieldsModule.urlSubscriptionFieldValues(baseUrl)(environment, clientId, apiContext, apiVersion)

    http.put(url)
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

  def subsFieldsCsv(environment: Environment)(implicit hc: HeaderCarrier): Future[String] = {
    http.get(url"${config.serviceBaseUrl}/csv?environment=$environment")
      .setHeader(HeaderNames.ACCEPT -> ContentTypes.TEXT)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK         => response.body
          case statusCode => throw UpstreamErrorResponse("Failed to get CSV", statusCode)
        }
      }
  }
}
