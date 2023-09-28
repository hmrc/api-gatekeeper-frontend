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

package mocks.services

import scala.concurrent.Future.successful

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.UpstreamErrorResponse

import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.gatekeeper.models.RegisteredUser
import uk.gov.hmrc.gatekeeper.models.xml.XmlOrganisation
import uk.gov.hmrc.gatekeeper.services.XmlService

trait XmlServiceMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  val mockXmlService = mock[XmlService]

  object XmlServiceMock {

    object GetXmlServicesForUser {

      def returnsApis(user: RegisteredUser, xmlServiceNames: Set[String]) =
        when(mockXmlService.getXmlServicesForUser(eqTo(user))(*)).thenReturn(successful(xmlServiceNames))

      def returnsError(user: RegisteredUser) =
        when(mockXmlService.getXmlServicesForUser(eqTo(user))(*)).thenThrow(UpstreamErrorResponse("error", 500, 500, Map.empty))
    }

    object GetXmlOrganisationsForUser {

      def returnsOrganisations(userId: UserId, xmlOrganisations: List[XmlOrganisation]) =
        when(mockXmlService.findOrganisationsByUserId(eqTo(userId))(*)).thenReturn(successful(xmlOrganisations))

      def returnsError() =
        when(mockXmlService.findOrganisationsByUserId(*[UserId])(*)).thenThrow(UpstreamErrorResponse("error", 500, 500, Map.empty))
    }
  }

}
