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

import javax.inject.Inject

import config.AppConfig
import model._
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApiPublisherConnector @Inject()(appConfig: AppConfig, http: HttpClient) {

  def fetchUnapproved()(implicit hc: HeaderCarrier): Future[Seq[APIApprovalSummary]] = {
    http.GET[Seq[APIApprovalSummary]](s"${appConfig.apiPublisherBaseUrl}/services/unapproved")
      .recover { recovery }
  }

  def fetchApprovalSummary(serviceName: String)(implicit hc: HeaderCarrier) : Future[APIApprovalSummary] = {
    http.GET[APIApprovalSummary](s"${appConfig.apiPublisherBaseUrl}/service/$serviceName/summary")
      .recover { recovery }
  }

  def approveService(serviceName: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    http.POST(s"${appConfig.apiPublisherBaseUrl}/service/$serviceName/approve",
      ApproveServiceRequest(serviceName), Seq("Content-Type" -> "application/json"))
      .map(_ => ())
      .recover { recovery }
  }

  def recovery[U] : PartialFunction[Throwable, U]  = {
    case e: Upstream5xxResponse => throw new UpdateApiDefinitionsFailed
    case _ => throw new UpdateApiDefinitionsFailed
  }
}

