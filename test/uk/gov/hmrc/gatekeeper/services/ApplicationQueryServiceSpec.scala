/*
 * Copyright 2025 HM Revenue & Customs
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

import scala.concurrent.ExecutionContext.Implicits.global

import org.mockito.scalatest.ResetMocksAfterEachTest
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.QueriedApplication
import uk.gov.hmrc.apiplatform.modules.applications.query.domain.models.ApplicationQuery
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.{AsyncHmrcSpec, FixedClock}
import uk.gov.hmrc.apiplatform.modules.subscriptionfields.domain.models.ApiFieldMap
import uk.gov.hmrc.gatekeeper.builder.ApplicationBuilder
import uk.gov.hmrc.gatekeeper.mocks.connectors.ThirdPartyOrchestratorConnectorMockProvider
import uk.gov.hmrc.gatekeeper.models.ApplicationWithSubscriptionFieldsAndStateHistory

class ApplicationQueryServiceSpec
    extends AsyncHmrcSpec
    with ResetMocksAfterEachTest
    with ApplicationWithCollaboratorsFixtures
    with ApplicationWithSubscriptionsFixtures
    with ApiIdentifierFixtures
    with StateHistoryFixtures {

  trait Setup
      extends MockitoSugar
      with ArgumentMatchersSugar
      with ThirdPartyOrchestratorConnectorMockProvider
      with ApplicationBuilder {

    val qryService                 = new ApplicationQueryService(
      TPOConnectorMock.aMock,
      FixedClock.clock
    )
    implicit val hc: HeaderCarrier = HeaderCarrier()
  }

  "fetchApplication" should {
    val qry = ApplicationQuery.ById(standardApp.id, Nil)
    val app = standardApp

    "get from production" in new Setup {
      TPOConnectorMock.Query.returnsForQry[Option[ApplicationWithCollaborators]](Environment.PRODUCTION)(qry, Some(app))

      await(qryService.fetchApplication(app.id)) shouldBe Some(app)
    }

    "get from sandbox" in new Setup {
      TPOConnectorMock.Query.returnsForQry[Option[ApplicationWithCollaborators]](Environment.PRODUCTION)(qry, None)
      TPOConnectorMock.Query.returnsForQry[Option[ApplicationWithCollaborators]](Environment.SANDBOX)(qry, Some(app))

      await(qryService.fetchApplication(app.id)) shouldBe Some(app)
    }
  }

  "fetchApplicationWithSubscriptionFields" should {
    val qry = ApplicationQuery.ById(standardApp.id, Nil, wantSubscriptions = true, wantSubscriptionFields = true)
    val app = standardApp.withSubscriptions(someSubscriptions).withFieldValues(ApiFieldMap.empty)

    "get from production" in new Setup {
      TPOConnectorMock.Query.returnsForQry[Option[ApplicationWithSubscriptionFields]](Environment.PRODUCTION)(qry, Some(app))

      await(qryService.fetchApplicationWithSubscriptionFields(app.id)) shouldBe Some(app)
    }

    "get from sandbox" in new Setup {
      TPOConnectorMock.Query.returnsForQry[Option[ApplicationWithSubscriptionFields]](Environment.PRODUCTION)(qry, None)
      TPOConnectorMock.Query.returnsForQry[Option[ApplicationWithSubscriptionFields]](Environment.SANDBOX)(qry, Some(app))

      await(qryService.fetchApplicationWithSubscriptionFields(app.id)) shouldBe Some(app)
    }
  }

  "fetchApplicationWithSubscriptionFieldsAndHistory" should {
    val qry          = ApplicationQuery.ById(standardApp.id, Nil, wantSubscriptions = true, wantSubscriptionFields = true, wantStateHistory = true)
    val stateHistory = List(aStateHistoryTesting.copy(applicationId = standardApp.id))
    val app          = QueriedApplication(standardApp.details, standardApp.collaborators, Some(someSubscriptions), Some(ApiFieldMap.empty), Some(stateHistory))

    "get from production" in new Setup {
      TPOConnectorMock.Query.returnsForQry[Option[QueriedApplication]](Environment.PRODUCTION)(qry, Some(app))

      await(qryService.fetchApplicationWithSubscriptionFieldsAndHistory(app.details.id)) shouldBe Some(ApplicationWithSubscriptionFieldsAndStateHistory(
        app.asAppSubsFields.get,
        stateHistory
      ))
    }

    "get from sandbox" in new Setup {
      TPOConnectorMock.Query.returnsForQry[Option[QueriedApplication]](Environment.PRODUCTION)(qry, None)
      TPOConnectorMock.Query.returnsForQry[Option[QueriedApplication]](Environment.SANDBOX)(qry, Some(app))

      await(qryService.fetchApplicationWithSubscriptionFieldsAndHistory(app.details.id)) shouldBe Some(ApplicationWithSubscriptionFieldsAndStateHistory(
        app.asAppSubsFields.get,
        stateHistory
      ))
    }
  }
}
