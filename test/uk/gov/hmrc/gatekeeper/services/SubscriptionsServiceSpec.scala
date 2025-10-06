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

import scala.concurrent.ExecutionContext.Implicits.global

import mocks.connectors.CommandConnectorMockProvider
import org.mockito.scalatest.ResetMocksAfterEachTest
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.DispatchSuccessResult
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.{AsyncHmrcSpec, FixedClock}
import uk.gov.hmrc.apiplatform.modules.subscriptionfields.domain.models.FieldName
import uk.gov.hmrc.gatekeeper.builder.ApplicationBuilder
import uk.gov.hmrc.gatekeeper.connectors._
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields._
import uk.gov.hmrc.gatekeeper.models._

class SubscriptionsServiceSpec extends AsyncHmrcSpec with ResetMocksAfterEachTest {

  trait Setup
      extends MockitoSugar with ArgumentMatchersSugar
      with CommandConnectorMockProvider with ApplicationBuilder {

    val mockDeveloperConnector        = mock[DeveloperConnector]
    val mockSubscriptionFieldsService = mock[SubscriptionFieldsService]

    val subscriptionService = new SubscriptionsService(
      CommandConnectorMock.aMock,
      FixedClock.clock
    )
    val underTest           = spy(subscriptionService)

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val stdApp1 = standardApp

    val stdApp2 = standardApp2

    val privilegedAppGK = privilegedApp
    val ropcAppGK       = ropcApp

    val applicationWithHistory = ApplicationWithHistory(stdApp1, List.empty)
    val gatekeeperUserId       = "loggedin.gatekeeper"
    val gatekeeperUser         = Actors.GatekeeperUser("Bob Smith")

    val apiIdentifier = ApiIdentifier(ApiContext.random, ApiVersionNbr.random)

    val context = apiIdentifier.context
    val version = apiIdentifier.versionNbr

    val allProductionApplications   = List(stdApp1, stdApp2, privilegedApp)
    val allSandboxApplications      = allProductionApplications.map(_.withId(ApplicationId.random).inSandbox())
    val testContext                 = ApiContext("test-context")
    val unknownContext              = ApiContext("unknown-context")
    val superContext                = ApiContext("super-context")
    val sandboxTestContext          = ApiContext("sandbox-test-context")
    val sandboxUnknownContext       = ApiContext("sandbox-unknown-context")
    val sandboxSuperContext         = ApiContext("sandbox-super-context")
    val subscriptionFieldDefinition = SubscriptionFieldDefinition(FieldName.random, "description", "hint", "String", "shortDescription")
    val definitions                 = List(subscriptionFieldDefinition)
  }

  "subscribeToApi" should {
    "calls the service to subscribe to the API" in new Setup {
      CommandConnectorMock.IssueCommand.succeedsReturning(stdApp1)

      val result = await(underTest.subscribeToApi(stdApp1, apiIdentifier, gatekeeperUser))

      result.value shouldBe DispatchSuccessResult(stdApp1)
    }
  }

  "unsubscribeFromApi" should {
    "call the service to unsubscribe from the API" in new Setup {

      CommandConnectorMock.IssueCommand.succeedsReturning(stdApp1)

      val result = await(underTest.unsubscribeFromApi(stdApp1, apiIdentifier, gatekeeperUser))

      result.value shouldBe DispatchSuccessResult(stdApp1)
    }
  }
}
