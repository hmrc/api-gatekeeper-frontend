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

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaboratorsFixtures
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.common.utils.{AsyncHmrcSpec, FixedClock}
import uk.gov.hmrc.gatekeeper.connectors.ApiPlatformDeskproConnector.DeskproTicket
import uk.gov.hmrc.gatekeeper.mocks.connectors.{ApiPlatformDeskproConnectorMockProvider, ThirdPartyOrchestratorConnectorMockProvider}

class ApiPlatformDeskproServiceSpec extends AsyncHmrcSpec with ApplicationWithCollaboratorsFixtures with FixedClock {

  trait Setup extends MockitoSugar with ArgumentMatchersSugar with ApiPlatformDeskproConnectorMockProvider with ThirdPartyOrchestratorConnectorMockProvider {
    implicit val hc: HeaderCarrier = new HeaderCarrier

    val apiPlatformDeskproService = new ApiPlatformDeskproService(apiPlatformDeskproConnector)

    val ticketId: Int = 123
    val personId: Int = 16
    val ticket        = DeskproTicket(ticketId, "ref1", personId, LaxEmailAddress("bob@example.com"), "awaiting_user", instant, instant, Some(instant), "subject 1")
  }

  "ApiPlatformDeskproService" when {
    "getDeskproTicket" should {

      "return a DeskproTicket" in new Setup {
        ApiPlatformDeskproConnectorMock.GetDeskproTicket.returns(ticket)

        val result = await(apiPlatformDeskproService.getDeskproTicket(ticketId))

        result shouldBe ticket
      }

      "return UpstreamErrorResponse when ticket not found" in new Setup {
        ApiPlatformDeskproConnectorMock.GetDeskproTicket.failsNotFound()

        val result = intercept[UpstreamErrorResponse] {
          await(apiPlatformDeskproService.getDeskproTicket(ticketId))
        }

        result shouldBe UpstreamErrorResponse("Not Found", NOT_FOUND)
      }
    }
  }
}
