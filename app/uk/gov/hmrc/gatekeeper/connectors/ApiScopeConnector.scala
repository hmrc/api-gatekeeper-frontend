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

import uk.gov.hmrc.http.HttpErrorFunctions.is5xx
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse, _}

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models._

abstract class ApiScopeConnector(implicit ec: ExecutionContext) {
  val environment: Environment
  val serviceBaseUrl: String

  def http: HttpClientV2

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder

  private def for5xx(ex: Throwable): PartialFunction[Throwable, Nothing] = (err: Throwable) =>
    err match {
      case e: UpstreamErrorResponse if (is5xx(e.statusCode)) => throw ex
    }

  def fetchAll()(implicit hc: HeaderCarrier): Future[List[ApiScope]] = {
    configureEbridgeIfRequired(http.get(url"$serviceBaseUrl/scope")).execute[List[ApiScope]]
      .recover(for5xx(new FetchApiDefinitionsFailed)) // TODO - odd choice of exception
  }
}

@Singleton
class SandboxApiScopeConnector @Inject() (val appConfig: AppConfig, val http: HttpClientV2)(implicit val ec: ExecutionContext)
    extends ApiScopeConnector {

  val environment    = Environment.SANDBOX
  val serviceBaseUrl = appConfig.apiScopeSandboxBaseUrl
  val useProxy       = appConfig.apiScopeSandboxUseProxy
  val bearerToken    = appConfig.apiScopeSandboxBearerToken
  val apiKey         = appConfig.apiScopeSandboxApiKey

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder =
    EbridgeConfigurator.configure(useProxy, bearerToken, apiKey)(requestBuilder)
}

@Singleton
class ProductionApiScopeConnector @Inject() (val appConfig: AppConfig, val http: HttpClientV2)(implicit val ec: ExecutionContext)
    extends ApiScopeConnector {

  val environment    = Environment.PRODUCTION
  val serviceBaseUrl = appConfig.apiScopeProductionBaseUrl

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder = requestBuilder
}
