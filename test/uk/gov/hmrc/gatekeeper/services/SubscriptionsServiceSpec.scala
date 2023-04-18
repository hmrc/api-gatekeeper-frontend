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

import java.time.{LocalDateTime, Period}
import scala.concurrent.ExecutionContext.Implicits.global

import mocks.connectors.CommandConnectorMockProvider
import org.mockito.scalatest.ResetMocksAfterEachTest
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{ApplicationId, Collaborator, Collaborators}
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.DispatchSuccessResult
import uk.gov.hmrc.apiplatform.modules.common.domain.models.Actors
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.utils.{AsyncHmrcSpec, FixedClock}
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.gatekeeper.connectors._
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields._
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.services.SubscriptionFieldsService.DefinitionsByApiVersion

class SubscriptionsServiceSpec extends AsyncHmrcSpec with ResetMocksAfterEachTest {

  trait Setup
      extends MockitoSugar with ArgumentMatchersSugar
      with CommandConnectorMockProvider {

    val mockDeveloperConnector        = mock[DeveloperConnector]
    val mockSubscriptionFieldsService = mock[SubscriptionFieldsService]

    val subscriptionService = new SubscriptionsService(
      CommandConnectorMock.aMock,
      FixedClock.clock
    )
    val underTest           = spy(subscriptionService)

    implicit val hc = HeaderCarrier()

    val collaborators = Set[Collaborator](
      Collaborators.Administrator(UserId.random, "sample@example.com".toLaxEmail),
      Collaborators.Developer(UserId.random, "someone@example.com".toLaxEmail)
    )

    val grantLength: Period = Period.ofDays(547)

    val stdApp1 = ApplicationResponse(
      ApplicationId.random,
      ClientId("clientid1"),
      "gatewayId1",
      "application1",
      "PRODUCTION",
      None,
      collaborators,
      LocalDateTime.now(),
      Some(LocalDateTime.now()),
      Standard(),
      ApplicationState(),
      grantLength
    )

    val stdApp2 = ApplicationResponse(
      ApplicationId.random,
      ClientId("clientid2"),
      "gatewayId2",
      "application2",
      "PRODUCTION",
      None,
      collaborators,
      LocalDateTime.now(),
      Some(LocalDateTime.now()),
      Standard(),
      ApplicationState(),
      grantLength
    )

    val privilegedApp = ApplicationResponse(
      ApplicationId.random,
      ClientId("clientid3"),
      "gatewayId3",
      "application3",
      "PRODUCTION",
      None,
      collaborators,
      LocalDateTime.now(),
      Some(LocalDateTime.now()),
      Privileged(),
      ApplicationState(),
      grantLength
    )

    val ropcApp                = ApplicationResponse(
      ApplicationId.random,
      ClientId("clientid4"),
      "gatewayId4",
      "application4",
      "PRODUCTION",
      None,
      collaborators,
      LocalDateTime.now(),
      Some(LocalDateTime.now()),
      Ropc(),
      ApplicationState(),
      grantLength
    )
    val applicationWithHistory = ApplicationWithHistory(stdApp1, List.empty)
    val gatekeeperUserId       = "loggedin.gatekeeper"
    val gatekeeperUser         = Actors.GatekeeperUser("Bob Smith")

    val apiIdentifier = ApiIdentifier(ApiContext.random, ApiVersion.random)

    val context = apiIdentifier.context
    val version = apiIdentifier.version

    val allProductionApplications                      = List(stdApp1, stdApp2, privilegedApp)
    val allSandboxApplications                         = allProductionApplications.map(_.copy(id = ApplicationId.random, deployedTo = "SANDBOX"))
    val testContext                                    = ApiContext("test-context")
    val unknownContext                                 = ApiContext("unknown-context")
    val superContext                                   = ApiContext("super-context")
    val sandboxTestContext                             = ApiContext("sandbox-test-context")
    val sandboxUnknownContext                          = ApiContext("sandbox-unknown-context")
    val sandboxSuperContext                            = ApiContext("sandbox-super-context")
    val subscriptionFieldDefinition                    = SubscriptionFieldDefinition(FieldName.random, "description", "hint", "String", "shortDescription")
    val prefetchedDefinitions: DefinitionsByApiVersion = Map(apiIdentifier -> List(subscriptionFieldDefinition))
    val definitions                                    = List(subscriptionFieldDefinition)
  }

  "subscribeToApi" should {
    "calls the service to subscribe to the API" in new Setup {
      CommandConnectorMock.IssueCommand.succeedsReturning(stdApp1)

      val result = await(underTest.subscribeToApi(stdApp1, apiIdentifier, gatekeeperUser))

      result.right.value shouldBe DispatchSuccessResult(stdApp1)
    }
  }

  "unsubscribeFromApi" should {
    "call the service to unsubscribe from the API" in new Setup {

      CommandConnectorMock.IssueCommand.succeedsReturning(stdApp1)

      val result = await(underTest.unsubscribeFromApi(stdApp1, apiIdentifier, gatekeeperUser))

      result.right.value shouldBe DispatchSuccessResult(stdApp1)
    }
  }
}
