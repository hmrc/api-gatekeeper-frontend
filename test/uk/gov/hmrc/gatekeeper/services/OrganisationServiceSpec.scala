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

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId, UserId}
import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.gatekeeper.mocks.ApplicationResponseBuilder
import uk.gov.hmrc.gatekeeper.mocks.connectors.{ApiPlatformDeskproConnectorMockProvider, ThirdPartyOrchestratorConnectorMockProvider}
import uk.gov.hmrc.gatekeeper.models.organisations.{DeskproOrganisation, DeskproPerson, OrganisationId, OrganisationWithApps}

class OrganisationServiceSpec extends AsyncHmrcSpec {

  trait Setup extends MockitoSugar with ArgumentMatchersSugar with ApiPlatformDeskproConnectorMockProvider with ThirdPartyOrchestratorConnectorMockProvider {
    implicit val hc: HeaderCarrier = new HeaderCarrier

    val organisationService = new OrganisationService(apiPlatformDeskproConnector, thirdPartyOrchestratorConnectorMock)

    val organisationId      = OrganisationId("1")
    val organisationName    = "Org Name"
    val email               = "bob@example.com".toLaxEmail
    val organisation        = DeskproOrganisation(organisationId, organisationName, List(DeskproPerson("name", email)))
    val applicationResponse = ApplicationResponseBuilder.buildApplication(ApplicationId.random, ClientId.random, UserId.random)
  }

  "OrganisationService" when {
    "fetchApplicationsForOrganisation" should {
      "return a list of OrganisationWithApps" in new Setup {
        ApiPlatformDeskproConnectorMock.GetOrganisation.returns(organisation)
        ThirdPartyOrchestratorConnectorMock.GetApplicationsByEmails.returns(List(email))(applicationResponse)

        val result = await(organisationService.fetchOrganisationWithApplications(organisationId))

        result shouldBe OrganisationWithApps(organisationName, List(applicationResponse))
      }
    }
  }
}
