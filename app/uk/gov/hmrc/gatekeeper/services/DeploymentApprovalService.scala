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

package uk.gov.hmrc.gatekeeper.services

import javax.inject.Inject
import uk.gov.hmrc.gatekeeper.connectors.{ProductionApiPublisherConnector, SandboxApiPublisherConnector}
import uk.gov.hmrc.gatekeeper.models.APIApprovalSummary
import uk.gov.hmrc.gatekeeper.models.Environment._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class DeploymentApprovalService @Inject() (
    sandboxApiPublisherConnector: SandboxApiPublisherConnector,
    productionApiPublisherConnector: ProductionApiPublisherConnector
  )(implicit ec: ExecutionContext
  ) {

  def fetchUnapprovedServices()(implicit hc: HeaderCarrier): Future[List[APIApprovalSummary]] = {
    val sandboxFuture    = sandboxApiPublisherConnector.fetchUnapproved()
    val productionFuture = productionApiPublisherConnector.fetchUnapproved()

    for {
      sandbox    <- sandboxFuture
      production <- productionFuture
    } yield (sandbox ++ production).distinct
  }

  def fetchApprovalSummary(serviceName: String, environment: Environment)(implicit hc: HeaderCarrier): Future[APIApprovalSummary] = {
    connectorFor(environment).fetchApprovalSummary(serviceName)
  }

  def approveService(serviceName: String, environment: Environment)(implicit hc: HeaderCarrier): Future[Unit] = {
    connectorFor(environment).approveService(serviceName)
  }

  def connectorFor(environment: Environment) = if (environment == PRODUCTION) productionApiPublisherConnector else sandboxApiPublisherConnector
}
