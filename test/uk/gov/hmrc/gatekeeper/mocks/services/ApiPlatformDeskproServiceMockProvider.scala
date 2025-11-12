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

import scala.concurrent.Future.{failed, successful}

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.UpstreamErrorResponse

import uk.gov.hmrc.gatekeeper.connectors.ApiPlatformDeskproConnector.DeskproTicket
import uk.gov.hmrc.gatekeeper.services.ApiPlatformDeskproService

trait ApiPlatformDeskproServiceMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  val mockApiPlatformDeskproService = mock[ApiPlatformDeskproService]

  object ApiPlatformDeskproServiceMock {

    object GetDeskproTicket {

      def returns(ticketId: Int)(deskproTicket: DeskproTicket) =
        when(mockApiPlatformDeskproService.getDeskproTicket(eqTo(ticketId))(*)).thenReturn(successful(deskproTicket))

      def returnsNotFound() = when(mockApiPlatformDeskproService.getDeskproTicket(*)(*)).thenReturn(failed(UpstreamErrorResponse("Not found", 404)))

    }

  }
}
