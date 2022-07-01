/*
 * Copyright 2022 HM Revenue & Customs
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

package mocks.connectors

import uk.gov.hmrc.gatekeeper.connectors._
import uk.gov.hmrc.gatekeeper.models.UserId
import uk.gov.hmrc.gatekeeper.models.xml._
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future.successful

trait XmlServicesConnectorMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  val mockXmlServicesConnector = mock[XmlServicesConnector]

  object XmlServicesConnectorMock {
    object GetAllApis {
      def returnsApis(xmlApis: Seq[XmlApi]) = when(mockXmlServicesConnector.getAllApis()(*)).thenReturn(successful(xmlApis))

      def returnsError() =
        when(mockXmlServicesConnector.getAllApis()(*)).thenThrow(UpstreamErrorResponse("error", 500, 500, Map.empty))
    }

    object GetOrganisations {
      def returnsOrganisations(userId: UserId, xmlOrganisations: List[XmlOrganisation]) =
        when(mockXmlServicesConnector.findOrganisationsByUserId(eqTo(userId))(*)).thenReturn(successful(xmlOrganisations))

      def returnsError() =
        when(mockXmlServicesConnector.findOrganisationsByUserId(*[UserId])(*)).thenThrow(UpstreamErrorResponse("error", 500, 500, Map.empty))
    }
  }

}