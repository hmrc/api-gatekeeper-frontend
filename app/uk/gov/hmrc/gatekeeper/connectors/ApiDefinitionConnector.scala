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
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiDefinition
import uk.gov.hmrc.apiplatform.modules.common.domain.models.Environment
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models._

abstract class ApiDefinitionConnector(implicit ec: ExecutionContext) {
  protected val httpClient: HttpClient
  val environment: Environment
  val serviceBaseUrl: String

  def http: HttpClient

  private def for5xx(ex: Throwable): PartialFunction[Throwable, Nothing] = (err: Throwable) =>
    err match {
      case e: UpstreamErrorResponse if (is5xx(e.statusCode)) => throw ex
    }

  def fetchPublic()(implicit hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    http.GET[List[ApiDefinition]](s"$serviceBaseUrl/api-definition")
      .recover(for5xx(new FetchApiDefinitionsFailed))
  }

  def fetchPrivate()(implicit hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    http.GET[List[ApiDefinition]](s"$serviceBaseUrl/api-definition?type=private")
      .recover(for5xx(new FetchApiDefinitionsFailed))
  }
}

@Singleton
class SandboxApiDefinitionConnector @Inject() (val appConfig: AppConfig, val httpClient: HttpClient, val proxiedHttpClient: ProxiedHttpClient)(implicit val ec: ExecutionContext)
    extends ApiDefinitionConnector {

  val environment    = Environment.SANDBOX
  val serviceBaseUrl = appConfig.apiDefinitionSandboxBaseUrl
  val useProxy       = appConfig.apiDefinitionSandboxUseProxy
  val bearerToken    = appConfig.apiDefinitionSandboxBearerToken
  val apiKey         = appConfig.apiDefinitionSandboxApiKey

  val http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient
}

@Singleton
class ProductionApiDefinitionConnector @Inject() (val appConfig: AppConfig, val httpClient: HttpClient)(implicit val ec: ExecutionContext)
    extends ApiDefinitionConnector {

  val http: HttpClient = httpClient

  val environment    = Environment.PRODUCTION
  val serviceBaseUrl = appConfig.apiDefinitionProductionBaseUrl
}
