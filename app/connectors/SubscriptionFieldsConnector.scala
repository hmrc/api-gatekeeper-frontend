/*
 * Copyright 2018 HM Revenue & Customs
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

import config.WSHttp
import connectors.AuthConnector.baseUrl
import model.ApiSubscriptionFields.{FieldDefinitionsResponse, SubscriptionField, SubscriptionFields}
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object SubscriptionFieldsConnector extends SubscriptionFieldsConnector {
  override val subscriptionFieldsBaseUrl: String = s"${baseUrl("api-subscription-fields")}"
  override val http = WSHttp
}

trait SubscriptionFieldsConnector {
  val subscriptionFieldsBaseUrl: String

  val http: HttpGet


  def fetchFieldValues(clientId: String, apiContext: String, apiVersion: String)(implicit hc: HeaderCarrier): Future[Option[SubscriptionFields]] = {
    val url = urlSubscriptionFieldValues(clientId, apiContext, apiVersion)
    http.GET[SubscriptionFields](url).map(Some(_)) recover recovery(None)
  }

  def fetchFieldDefinitions(apiContext: String, apiVersion: String)(implicit hc: HeaderCarrier): Future[Seq[SubscriptionField]] = {
    val url = urlSubscriptionFieldDefinition(apiContext, apiVersion)
    http.GET[FieldDefinitionsResponse](url).map(response => response.fieldDefinitions) recover recovery(Seq.empty[SubscriptionField])
  }
  private def urlEncode(str: String, encoding: String = "UTF-8") = {
    encode(str, encoding)
  }
  private def urlSubscriptionFieldValues(clientId: String, apiContext: String, apiVersion: String) =
    s"$subscriptionFieldsBaseUrl/field/application/${urlEncode(clientId)}/context/${urlEncode(apiContext)}/version/${urlEncode(apiVersion)}"

  private def urlSubscriptionFieldDefinition(apiContext: String, apiVersion: String) =
    s"$subscriptionFieldsBaseUrl/definition/context/${urlEncode(apiContext)}/version/${urlEncode(apiVersion)}"

  private def recovery[T](value: T): PartialFunction[Throwable, T] = {
    case _: NotFoundException => value
  }
}
