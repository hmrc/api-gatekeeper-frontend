/*
 * Copyright 2019 HM Revenue & Customs
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

import config.AppConfig
import javax.inject.{Inject, Singleton}
import model.ApiSubscriptionFields._
import model._
import model.Environment.Environment
import play.api.http.Status.NO_CONTENT
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

abstract class SubscriptionFieldsConnector(implicit ec: ExecutionContext) {
  protected val httpClient: HttpClient
  protected val proxiedHttpClient: ProxiedHttpClient
  val environment: Environment
  val serviceBaseUrl: String
  val useProxy: Boolean
  val bearerToken: String
  val apiKey: String

  def http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient

  def fetchFieldValues(clientId: String, apiContext: String, apiVersion: String)(implicit hc: HeaderCarrier): Future[Option[SubscriptionFields]] = {
    val url = urlSubscriptionFieldValues(clientId, apiContext, apiVersion)
    http.GET[SubscriptionFields](url).map(Some(_)) recover recovery(None)
  }

  def fetchFieldDefinitions(apiContext: String, apiVersion: String)(implicit hc: HeaderCarrier): Future[Seq[SubscriptionField]] = {
    val url = urlSubscriptionFieldDefinition(apiContext, apiVersion)
    http.GET[FieldDefinitionsResponse](url).map(response => response.fieldDefinitions) recover recovery(Seq.empty[SubscriptionField])
  }

  def saveFieldValues(clientId: String, apiContext: String, apiVersion: String, fields: Fields)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val url = urlSubscriptionFieldValues(clientId, apiContext, apiVersion)
    http.PUT[SubscriptionFieldsPutRequest, HttpResponse](url, SubscriptionFieldsPutRequest(clientId, apiContext, apiVersion, fields))
  }

  def deleteFieldValues(clientId: String, apiContext: String, apiVersion: String)(implicit hc: HeaderCarrier): Future[FieldsDeleteResult] = {
    val url = urlSubscriptionFieldValues(clientId, apiContext, apiVersion)
    http.DELETE[HttpResponse](url).map { response => response.status match {
        case NO_CONTENT => FieldsDeleteSuccessResult
        case _ => FieldsDeleteFailureResult
      }
    } recover {
      case e: NotFoundException => FieldsDeleteSuccessResult
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

@Singleton
class SandboxSubscriptionFieldsConnector @Inject()(appConfig: AppConfig,
                                                   val httpClient: HttpClient,
                                                   val proxiedHttpClient: ProxiedHttpClient)(implicit ec: ExecutionContext)
  extends SubscriptionFieldsConnector {

  val environment = Environment.SANDBOX
  val serviceBaseUrl = appConfig.subscriptionFieldsSandboxBaseUrl
  val useProxy = appConfig.subscriptionFieldsSandboxUseProxy
  val bearerToken = appConfig.subscriptionFieldsSandboxBearerToken
  val apiKey = appConfig.subscriptionFieldsSandboxApiKey
}

@Singleton
class ProductionSubscriptionFieldsConnector @Inject()(appConfig: AppConfig,
                                                      val httpClient: HttpClient,
                                                      val proxiedHttpClient: ProxiedHttpClient)(implicit ec: ExecutionContext)
  extends SubscriptionFieldsConnector {

  val environment = Environment.PRODUCTION
  val serviceBaseUrl = appConfig.subscriptionFieldsProductionBaseUrl
  val useProxy = appConfig.subscriptionFieldsProductionUseProxy
  val bearerToken = appConfig.subscriptionFieldsProductionBearerToken
  val apiKey = appConfig.subscriptionFieldsProductionApiKey
}
