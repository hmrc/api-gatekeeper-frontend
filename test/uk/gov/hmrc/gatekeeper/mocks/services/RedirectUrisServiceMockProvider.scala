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

import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.services.RedirectUrisService

trait RedirectUrisServiceMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  val mockRedirectUrisService = mock[RedirectUrisService]

  object RedirectUrisServiceMock {

    object ManageRedirectUris {
      def succeeds() = when(mockRedirectUrisService.manageLoginRedirectUris(*, *, *)(*)).thenReturn(successful(ApplicationUpdateSuccessResult))
    }
  }
}
