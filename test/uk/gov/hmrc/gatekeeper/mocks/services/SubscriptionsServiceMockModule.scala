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

import scala.concurrent.ExecutionContext.Implicits.global

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiIdentifier
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.Actors
import uk.gov.hmrc.gatekeeper.models.ApplicationResponse
import uk.gov.hmrc.gatekeeper.services.SubscriptionsService

trait SubscriptionsServiceMockModule extends MockitoSugar with ArgumentMatchersSugar {

  trait AbstractSubscriptionsServiceMock {
    val CHT = new CommandHandlerTypes[DispatchSuccessResult] {}

    import CHT.Implicits._

    def aMock: SubscriptionsService

    object SubscribeToApi {

      def succeeds() = {
        val mockApp = mock[ApplicationResponse]
        when(aMock.subscribeToApi(*, *, *)(*)).thenReturn(DispatchSuccessResult(mockApp).asSuccess)
      }

      def verifyCalledWith(app: ApplicationResponse, apiIdentifier: ApiIdentifier, user: Actors.GatekeeperUser) =
        verify(aMock).subscribeToApi(eqTo(app), eqTo(apiIdentifier), eqTo(user))(*)

      def verifyNotCalled() =
        verify(aMock, never).subscribeToApi(*, *, *)(*)
    }

    object UnsubscribeFromApi {

      def succeeds() = {
        val mockApp = mock[ApplicationResponse]
        when(aMock.unsubscribeFromApi(*, *, *)(*)).thenReturn(DispatchSuccessResult(mockApp).asSuccess)
      }

      def verifyCalledWith(app: ApplicationResponse, apiIdentifier: ApiIdentifier, user: Actors.GatekeeperUser) =
        verify(aMock).unsubscribeFromApi(eqTo(app), eqTo(apiIdentifier), eqTo(user))(*)

      def verifyNotCalled() =
        verify(aMock, never).unsubscribeFromApi(*, *, *)(*)
    }
  }

  object SubscriptionsServiceMock extends AbstractSubscriptionsServiceMock {
    val aMock = mock[SubscriptionsService]
  }
}
