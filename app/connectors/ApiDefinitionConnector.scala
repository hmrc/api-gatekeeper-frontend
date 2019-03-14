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

import config.AppConfig
import javax.inject.{Inject, Singleton}
import model._
import model.Environment.Environment
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

abstract class ApiDefinitionConnector(implicit ec: ExecutionContext) {
  protected val httpClient: HttpClient
  protected val proxiedHttpClient: ProxiedHttpClient
  val environment: Environment
  val serviceBaseUrl: String
  val useProxy: Boolean
  val bearerToken: String

  def http: HttpClient = if (useProxy) proxiedHttpClient.withAuthorization(bearerToken) else httpClient

  def fetchPublic()(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] = {
    http.GET[Seq[APIDefinition]](s"$serviceBaseUrl/api-definition")
      .recover {
        case _: Upstream5xxResponse => throw new FetchApiDefinitionsFailed
      }
  }

  def fetchPrivate()(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] = {
    http.GET[Seq[APIDefinition]](s"$serviceBaseUrl/api-definition?type=private")
      .recover {
        case _: Upstream5xxResponse => throw new FetchApiDefinitionsFailed
      }
  }

}

@Singleton
class SandboxApiDefinitionConnector @Inject()(appConfig: AppConfig,
                                               val httpClient: HttpClient,
                                               val proxiedHttpClient: ProxiedHttpClient)(implicit ec: ExecutionContext)
  extends ApiDefinitionConnector {

  val environment = Environment.SANDBOX
  val serviceBaseUrl = appConfig.apiDefinitionSandboxBaseUrl
  val useProxy = appConfig.apiDefinitionSandboxUseProxy
  val bearerToken = appConfig.apiDefinitionSandboxBearerToken
}

@Singleton
class ProductionApiDefinitionConnector @Inject()(appConfig: AppConfig,
                                            val httpClient: HttpClient,
                                            val proxiedHttpClient: ProxiedHttpClient)(implicit ec: ExecutionContext)
  extends ApiDefinitionConnector {

  val environment = Environment.PRODUCTION
  val serviceBaseUrl = appConfig.apiDefinitionProductionBaseUrl
  val useProxy = appConfig.apiDefinitionProductionUseProxy
  val bearerToken = appConfig.apiDefinitionProductionBearerToken
}
