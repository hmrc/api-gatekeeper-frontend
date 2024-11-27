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

package uk.gov.hmrc.gatekeeper.mocks.services

import scala.concurrent.Future.{failed, successful}

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.UpstreamErrorResponse

import uk.gov.hmrc.gatekeeper.models.organisations.{OrganisationId, OrganisationWithApps}
import uk.gov.hmrc.gatekeeper.services.OrganisationService

trait OrganisationServiceMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  val mockOrganisationService = mock[OrganisationService]

  object OrganisationServiceMock {

    object FetchApplicationsForOrganisation {

      def returns(organisationId: OrganisationId)(organisationWithApps: OrganisationWithApps) =
        when(mockOrganisationService.fetchOrganisationWithApplications(eqTo(organisationId), *[Map[String, String]])(*)).thenReturn(successful(organisationWithApps))

      def returnsNotFound() =
        when(mockOrganisationService.fetchOrganisationWithApplications(*[OrganisationId], *[Map[String, String]])(*)).thenReturn(failed(UpstreamErrorResponse("Not found", 404)))

    }

  }
}
