/*
 * Copyright 2021 HM Revenue & Customs
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

import akka.actor.ActorSystem
import akka.pattern.FutureTimeoutSupport
import config.AppConfig
import javax.inject.{Inject, Singleton}
import model._
import model.Environment.Environment
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

abstract class ApiScopeConnector(implicit ec: ExecutionContext) {
  protected val httpClient: HttpClient
  protected val proxiedHttpClient: ProxiedHttpClient
  val environment: Environment
  val serviceBaseUrl: String
  val useProxy: Boolean
  val bearerToken: String
  val apiKey: String

  def http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient

  def fetchAll()(implicit hc: HeaderCarrier): Future[Seq[ApiScope]] = {
    http.GET[Seq[ApiScope]](s"$serviceBaseUrl/scope")
      .recover {
        case _: Upstream5xxResponse => throw new FetchApiDefinitionsFailed
      }
  }
}

@Singleton
class SandboxApiScopeConnector @Inject()(val appConfig: AppConfig,
                                         val httpClient: HttpClient,
                                         val proxiedHttpClient: ProxiedHttpClient)(implicit val ec: ExecutionContext)
  extends ApiScopeConnector {

  val environment = Environment.SANDBOX
  val serviceBaseUrl = appConfig.apiScopeSandboxBaseUrl
  val useProxy = appConfig.apiScopeSandboxUseProxy
  val bearerToken = appConfig.apiScopeSandboxBearerToken
  val apiKey = appConfig.apiScopeSandboxApiKey
}

@Singleton
class ProductionApiScopeConnector @Inject()(val appConfig: AppConfig,
                                            val httpClient: HttpClient,
                                            val proxiedHttpClient: ProxiedHttpClient)(implicit val ec: ExecutionContext)
  extends ApiScopeConnector {

  val environment = Environment.PRODUCTION
  val serviceBaseUrl = appConfig.apiScopeProductionBaseUrl
  val useProxy = appConfig.apiScopeProductionUseProxy
  val bearerToken = appConfig.apiScopeProductionBearerToken
  val apiKey = appConfig.apiScopeProductionApiKey
}