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

import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HttpClient, _}

import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models.Environment.Environment
import uk.gov.hmrc.gatekeeper.models._

abstract class ApiPublisherConnector(implicit ec: ExecutionContext) {
  protected val httpClient: HttpClient
  val environment: Environment
  val serviceBaseUrl: String

  def http: HttpClient

  // implicit def readsList[T] = Json.readsList()

  def fetchUnapproved()(implicit hc: HeaderCarrier): Future[List[APIApprovalSummary]] = {
    http.GET[List[APIApprovalSummary]](s"$serviceBaseUrl/services/unapproved", Seq.empty, Seq.empty).map(_.map(_.copy(environment = Some(environment))))
  }

  def fetchApprovalSummary(serviceName: String)(implicit hc: HeaderCarrier): Future[APIApprovalSummary] = {
    http.GET[APIApprovalSummary](s"$serviceBaseUrl/service/$serviceName/summary").map(_.copy(environment = Some(environment)))
  }

  def approveService(serviceName: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    http.POST[ApproveServiceRequest, Either[UpstreamErrorResponse, Unit]](
      s"$serviceBaseUrl/service/$serviceName/approve",
      ApproveServiceRequest(serviceName),
      Seq("Content-Type" -> "application/json")
    )
      .map(_.fold(err => throw err, _ => ()))
  }

}

@Singleton
class SandboxApiPublisherConnector @Inject() (val appConfig: AppConfig, val httpClient: HttpClient, val proxiedHttpClient: ProxiedHttpClient)(implicit val ec: ExecutionContext)
    extends ApiPublisherConnector {

  val environment    = Environment.SANDBOX
  val serviceBaseUrl = appConfig.apiPublisherSandboxBaseUrl
  val useProxy       = appConfig.apiPublisherSandboxUseProxy
  val bearerToken    = appConfig.apiPublisherSandboxBearerToken
  val apiKey         = appConfig.apiPublisherSandboxApiKey

  val http = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient

}

@Singleton
class ProductionApiPublisherConnector @Inject() (val appConfig: AppConfig, val httpClient: HttpClient)(implicit val ec: ExecutionContext)
    extends ApiPublisherConnector {

  val environment    = Environment.PRODUCTION
  val serviceBaseUrl = appConfig.apiPublisherProductionBaseUrl

  val http = httpClient
}
