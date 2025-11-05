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

package uk.gov.hmrc.gatekeeper.mocks.connectors

import scala.concurrent.Future.{failed, successful}

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.UpstreamErrorResponse

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.gatekeeper.connectors.ApiPlatformDeskproConnector.{DeskproTicket, MarkPersonInactiveFailed, MarkPersonInactiveSuccess}
import uk.gov.hmrc.gatekeeper.connectors._
import uk.gov.hmrc.gatekeeper.models.organisations.{DeskproOrganisation, OrganisationId}

trait ApiPlatformDeskproConnectorMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  val apiPlatformDeskproConnector = mock[ApiPlatformDeskproConnector]

  object ApiPlatformDeskproConnectorMock {

    object GetOrganisation {
      def returns(org: DeskproOrganisation) = when(apiPlatformDeskproConnector.getOrganisation(*[OrganisationId], *)).thenReturn(successful(org))
    }

    object GetDeskproTicket {
      def returns(ticket: DeskproTicket) = when(apiPlatformDeskproConnector.getDeskproTicket(*, *)).thenReturn(successful(ticket))

      def failsNotFound() =
        when(apiPlatformDeskproConnector.getDeskproTicket(*, *)).thenReturn(failed(UpstreamErrorResponse("Not Found", NOT_FOUND)))

    }

    object GetOrganisationsForUser {
      def returns(orgs: Option[List[DeskproOrganisation]]) = when(apiPlatformDeskproConnector.getOrganisationsForUser(*[LaxEmailAddress], *)).thenReturn(successful(orgs))
    }

    object MarkPersonInactive {
      def suceeds() = when(apiPlatformDeskproConnector.markPersonInactive(*[LaxEmailAddress], *)).thenReturn(successful(MarkPersonInactiveSuccess))

      def fails() = when(apiPlatformDeskproConnector.markPersonInactive(*[LaxEmailAddress], *)).thenReturn(successful(MarkPersonInactiveFailed))
    }
  }
}
