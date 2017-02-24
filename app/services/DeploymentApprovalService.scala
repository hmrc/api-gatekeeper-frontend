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

package services

import connectors.{ApiDefinitionConnector, ApiPublisherConnector}
import model.{APIApprovalSummary, ApproveServiceSuccessful}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

object DeploymentApprovalService extends DeploymentApprovalService{
  override val apiPublisherConnector = ApiPublisherConnector
}

trait DeploymentApprovalService {
  val apiPublisherConnector: ApiPublisherConnector

  def fetchUnapprovedServices(implicit hc: HeaderCarrier): Future[Seq[APIApprovalSummary]] = {
    apiPublisherConnector.fetchUnapproved()
  }

  def fetchApiDefinitionSummary(serviceName: String)(implicit hc: HeaderCarrier): Future[APIApprovalSummary] = {
    apiPublisherConnector.fetchApprovalSummary(serviceName)
  }

  def approveService(serviceName: String)(implicit hc: HeaderCarrier): Future[ApproveServiceSuccessful] = {
    apiPublisherConnector.approveService(serviceName)
  }
}
