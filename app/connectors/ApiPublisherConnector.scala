/*
 * Copyright 2017 HM Revenue & Customs
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

import config.WSHttp
import model._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ApiPublisherConnector  extends ApiPublisherConnector with ServicesConfig {
  override val serviceBaseUrl = baseUrl("api-publisher")
  override val http = WSHttp
}

trait ApiPublisherConnector  {
  val serviceBaseUrl: String
  val http: HttpGet with HttpPost

  def fetchUnapproved()(implicit hc: HeaderCarrier): Future[Seq[APIApprovalSummary]] = {
    http.GET[Seq[APIApprovalSummary]](s"$serviceBaseUrl/unapproved")
      .recover {
        case e: Upstream5xxResponse => throw new FetchApiDefinitionsFailed
      }
  }

  def fetchApprovalSummary(serviceName: String)(implicit hc: HeaderCarrier) : Future[APIApprovalSummary] = {
    http.GET[APIApprovalSummary](s"$serviceBaseUrl/$serviceName/summary")
      .recover {
        case e: Upstream5xxResponse => throw new FetchApiDefinitionsFailed
      }
  }

  def approveService(serviceName: String)(implicit hc: HeaderCarrier): Future[ApproveServiceSuccessful] = {
    http.POST(s"$serviceBaseUrl/$serviceName/approve",
      ApproveServiceRequest(serviceName), Seq("Content-Type" -> "application/json"))
      .map(_ => ApproveServiceSuccessful)
      .recover {
        case e: Upstream5xxResponse => throw new UpdateApiDefinitionsFailed
        case _ => throw new UpdateApiDefinitionsFailed
      }
  }
}
