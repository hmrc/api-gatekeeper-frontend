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

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.gatekeeper.connectors.{ProductionApiPublisherConnector, SandboxApiPublisherConnector}
import uk.gov.hmrc.gatekeeper.models.APIApprovalSummary

@Singleton
class DeploymentApprovalService @Inject() (
    sandboxApiPublisherConnector: SandboxApiPublisherConnector,
    productionApiPublisherConnector: ProductionApiPublisherConnector
  )(implicit ec: ExecutionContext
  ) {

  def fetchAllServices()(implicit hc: HeaderCarrier): Future[List[APIApprovalSummary]] = {
    val sandboxFuture    = sandboxApiPublisherConnector.fetchAll()
    val productionFuture = productionApiPublisherConnector.fetchAll()

    for {
      sandbox    <- sandboxFuture
      production <- productionFuture
    } yield (sandbox ++ production).distinct
  }

  def searchServices(params: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[List[APIApprovalSummary]] = {
    val sandboxFuture    = sandboxApiPublisherConnector.searchServices(params)
    val productionFuture = productionApiPublisherConnector.searchServices(params)

    for {
      sandbox    <- sandboxFuture
      production <- productionFuture
    } yield (sandbox ++ production).distinct.sortBy(_.createdOn)(Ordering[Option[Instant]].reverse)
  }

  def fetchApprovalSummary(serviceName: String, environment: Environment)(implicit hc: HeaderCarrier): Future[APIApprovalSummary] = {
    connectorFor(environment).fetchApprovalSummary(serviceName)
  }

  def approveService(serviceName: String, environment: Environment, actor: Actors.GatekeeperUser, notes: Option[String] = None)(implicit hc: HeaderCarrier): Future[Unit] = {
    connectorFor(environment).approveService(serviceName, actor, notes)
  }

  def declineService(serviceName: String, environment: Environment, actor: Actors.GatekeeperUser, notes: Option[String] = None)(implicit hc: HeaderCarrier): Future[Unit] = {
    connectorFor(environment).declineService(serviceName, actor, notes)
  }

  def addComment(serviceName: String, environment: Environment, actor: Actors.GatekeeperUser, notes: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    connectorFor(environment).addComment(serviceName, actor, notes)
  }

  def connectorFor(environment: Environment) = if (environment == Environment.PRODUCTION) productionApiPublisherConnector else sandboxApiPublisherConnector
}
