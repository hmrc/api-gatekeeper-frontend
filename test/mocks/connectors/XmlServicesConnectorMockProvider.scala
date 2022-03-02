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

import connectors._
import model.RegisteredUser
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import services.XmlService
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future.successful

trait XmlServicesConnectorMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>
  
  val mockXmlService = mock[XmlService]

  object XmlServicesConnectorMock {
    object GetAllApis {
      def returnsApis(user: RegisteredUser, xmlServiceNames: Set[String]) = when(mockXmlService.getXmlServicesForUserId(user)(*)).thenReturn(successful(xmlServiceNames))
      def returnsError(user: RegisteredUser) = when(mockXmlService.getXmlServicesForUserId(user)(*)).thenThrow(UpstreamErrorResponse("error", 500, 500, Map.empty))
    }
  }

}