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

import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models._

abstract class ApiPublisherConnector(implicit ec: ExecutionContext) {
  val environment: Environment
  val serviceBaseUrl: String

  def http: HttpClientV2

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder

  def fetchUnapproved()(implicit hc: HeaderCarrier): Future[List[APIApprovalSummary]] = {
    configureEbridgeIfRequired(http.get(url"$serviceBaseUrl/services/unapproved")).execute[List[APIApprovalSummary]]
      .map(_.map(_.copy(environment = Some(environment))))
  }

  def fetchAll()(implicit hc: HeaderCarrier): Future[List[APIApprovalSummary]] = {
    configureEbridgeIfRequired(http.get(url"$serviceBaseUrl/services")).execute[List[APIApprovalSummary]]
      .map(_.map(_.copy(environment = Some(environment))))
  }

  def fetchApprovalSummary(serviceName: String)(implicit hc: HeaderCarrier): Future[APIApprovalSummary] = {
    configureEbridgeIfRequired(http.get(url"$serviceBaseUrl/service/$serviceName/summary")).execute[APIApprovalSummary]
      .map(_.copy(environment = Some(environment)))
  }

  def approveService(serviceName: String, actor: Actors.GatekeeperUser)(implicit hc: HeaderCarrier): Future[Unit] = {
    configureEbridgeIfRequired(http.post(url"$serviceBaseUrl/service/$serviceName/approve"))
      .withBody(Json.toJson(ApproveServiceRequest(serviceName, actor)))
      .setHeader("Content-Type" -> "application/json")
      .execute[Either[UpstreamErrorResponse, Unit]]
      .map(_.fold(err => throw err, _ => ()))
  }
}

@Singleton
class SandboxApiPublisherConnector @Inject() (val appConfig: AppConfig, val http: HttpClientV2)(implicit val ec: ExecutionContext)
    extends ApiPublisherConnector {

  val environment    = Environment.SANDBOX
  val serviceBaseUrl = appConfig.apiPublisherSandboxBaseUrl
  val useProxy       = appConfig.apiPublisherSandboxUseProxy
  val bearerToken    = appConfig.apiPublisherSandboxBearerToken
  val apiKey         = appConfig.apiPublisherSandboxApiKey

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder =
    EbridgeConfigurator.configure(useProxy, bearerToken, apiKey)(requestBuilder)
}

@Singleton
class ProductionApiPublisherConnector @Inject() (val appConfig: AppConfig, val http: HttpClientV2)(implicit val ec: ExecutionContext)
    extends ApiPublisherConnector {

  val environment    = Environment.PRODUCTION
  val serviceBaseUrl = appConfig.apiPublisherProductionBaseUrl

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder = requestBuilder
}
