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

import java.time.Clock
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import cats.data.OptionT

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpReads.Implicits._

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaborators, ApplicationWithSubscriptionFields}
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.QueriedApplication
import uk.gov.hmrc.apiplatform.modules.applications.query.domain.models.ApplicationQuery
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, Environment}
import uk.gov.hmrc.apiplatform.modules.common.services.{ApplicationLogger, ClockNow}
import uk.gov.hmrc.gatekeeper.connectors.ThirdPartyOrchestratorConnector
import uk.gov.hmrc.gatekeeper.models.ApplicationWithSubscriptionFieldsAndStateHistory

@Singleton
class ApplicationQueryService @Inject() (
    tpoConnector: ThirdPartyOrchestratorConnector,
    val clock: Clock
  )(implicit ec: ExecutionContext
  ) extends ApplicationLogger with ClockNow {

  def fetchApplication(appId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithCollaborators]] = {
    val qry = ApplicationQuery.ById(appId, Nil)

    OptionT(tpoConnector.query[Option[ApplicationWithCollaborators]](Environment.PRODUCTION)(qry)).orElse(
      OptionT(tpoConnector.query[Option[ApplicationWithCollaborators]](Environment.SANDBOX)(qry))
    )
      .value
  }

  def fetchApplicationWithSubscriptionFields(appId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithSubscriptionFields]] = {
    val qry = ApplicationQuery.ById(appId, Nil, wantSubscriptions = true, wantSubscriptionFields = true)

    OptionT(tpoConnector.query[Option[ApplicationWithSubscriptionFields]](Environment.PRODUCTION)(qry)).orElse(
      OptionT(tpoConnector.query[Option[ApplicationWithSubscriptionFields]](Environment.SANDBOX)(qry))
    )
      .value
  }

  def fetchApplicationWithSubscriptionFieldsAndHistory(appId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithSubscriptionFieldsAndStateHistory]] = {
    val qry = ApplicationQuery.ById(appId, Nil, wantSubscriptions = true, wantSubscriptionFields = true, wantStateHistory = true)

    OptionT(tpoConnector.query[Option[QueriedApplication]](Environment.PRODUCTION)(qry)).orElse(
      OptionT(tpoConnector.query[Option[QueriedApplication]](Environment.SANDBOX)(qry))
    )
      .map(qa => ApplicationWithSubscriptionFieldsAndStateHistory(qa.asAppSubsFields.get, qa.stateHistory.get))
      .value
  }
}
