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

import akka.actor.ActorSystem
import akka.pattern.FutureTimeoutSupport
import config.AppConfig
import javax.inject.{Inject, Singleton}
import model.Environment.Environment
import model._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.Retries

import scala.concurrent.{ExecutionContext, Future}

abstract class ApiPublisherConnector(implicit ec: ExecutionContext) extends Retries {
  protected val httpClient: HttpClient
  protected val proxiedHttpClient: ProxiedHttpClient
  val environment: Environment
  val serviceBaseUrl: String
  val useProxy: Boolean
  val bearerToken: String

  def http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken) else httpClient

  def fetchUnapproved()(implicit hc: HeaderCarrier): Future[Seq[APIApprovalSummary]] = {
    retry {
      http.GET[Seq[APIApprovalSummary]](s"$serviceBaseUrl/services/unapproved").map(_.map(_.copy(environment = Some(environment))))
    }
  }

  def fetchApprovalSummary(serviceName: String)(implicit hc: HeaderCarrier): Future[APIApprovalSummary] = {
    retry {
      http.GET[APIApprovalSummary](s"$serviceBaseUrl/service/$serviceName/summary").map(_.copy(environment = Some(environment)))
    }
  }

  def approveService(serviceName: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    http.POST[ApproveServiceRequest, HttpResponse](s"$serviceBaseUrl/service/$serviceName/approve",
      ApproveServiceRequest(serviceName), Seq("Content-Type" -> "application/json"))
      .map(_ => ())
      .recover {
        case _ => throw new UpdateApiDefinitionsFailed
      }
  }

}

@Singleton
class SandboxApiPublisherConnector @Inject()(val appConfig: AppConfig,
                                             val httpClient: HttpClient,
                                             val proxiedHttpClient: ProxiedHttpClient,
                                             val actorSystem: ActorSystem,
                                             val futureTimeout: FutureTimeoutSupport)(implicit val ec: ExecutionContext)
  extends ApiPublisherConnector {

  val environment = Environment.SANDBOX
  val serviceBaseUrl = appConfig.apiPublisherSandboxBaseUrl
  val useProxy = appConfig.apiPublisherSandboxUseProxy
  val bearerToken = appConfig.apiPublisherSandboxBearerToken
}

@Singleton
class ProductionApiPublisherConnector @Inject()(val appConfig: AppConfig,
                                                val httpClient: HttpClient,
                                                val proxiedHttpClient: ProxiedHttpClient,
                                                val actorSystem: ActorSystem,
                                                val futureTimeout: FutureTimeoutSupport)(implicit val ec: ExecutionContext)
  extends ApiPublisherConnector {

  val environment = Environment.PRODUCTION
  val serviceBaseUrl = appConfig.apiPublisherProductionBaseUrl
  val useProxy = appConfig.apiPublisherProductionUseProxy
  val bearerToken = appConfig.apiPublisherProductionBearerToken
}