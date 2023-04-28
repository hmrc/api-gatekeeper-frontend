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

package mocks.connectors

import scala.concurrent.ExecutionContext

import cats.data.NonEmptyList
import org.mockito.captor.ArgCaptor
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.gatekeeper.connectors.ApplicationCommandConnector
import uk.gov.hmrc.gatekeeper.models.ApplicationResponse

trait CommandConnectorMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  object CommandConnectorMock {
    val CHT = new CommandHandlerTypes[DispatchSuccessResult] {}

    import CHT.Implicits._

    val aMock: ApplicationCommandConnector = mock[ApplicationCommandConnector]

    object IssueCommand {
      import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.DispatchSuccessResult

      def succeeds()(implicit ec: ExecutionContext) = {
        val mockResult = mock[DispatchSuccessResult]
        when(aMock.dispatch(*[ApplicationId], *, *)(*)).thenReturn(mockResult.asSuccess)
      }

      def succeedsReturning(app: ApplicationResponse)(implicit ec: ExecutionContext) = {
        when(aMock.dispatch(*[ApplicationId], *, *)(*)).thenReturn(DispatchSuccessResult(app).asSuccess)
      }

      def failsWith(failure: CommandFailure)(implicit ec: ExecutionContext) = {
        when(aMock.dispatch(*[ApplicationId], *, *)(*)).thenReturn(failure.asFailure)
      }

      def verifyCommand(id: ApplicationId) = {
        val cmdCaptor = ArgCaptor[ApplicationCommand]
        verify(aMock).dispatch(eqTo(id), cmdCaptor.capture, *)(*)
        cmdCaptor.value
      }

      def verifyNoCommandsIssued() = {
        verify(aMock, never).dispatch(*[ApplicationId], *, *)(*)
      }

      object ToRemoveCollaborator {
        val mockResult = mock[DispatchSuccessResult]

        def succeeds()(implicit ec: ExecutionContext) = {
          when(aMock.dispatch(*[ApplicationId], *, *)(*)).thenReturn(mockResult.asSuccess)
        }

        def succeedsFor(id: ApplicationId, adminsToEmail: Set[LaxEmailAddress])(implicit ec: ExecutionContext) = {
          when(aMock.dispatch(eqTo(id), *, eqTo(adminsToEmail))(*)).thenReturn(mockResult.asSuccess)
        }

        def failsWithLastAdmin()(implicit ec: ExecutionContext) = {
          val mockResult = NonEmptyList.one(CommandFailures.CannotRemoveLastAdmin)
          when(aMock.dispatch(*[ApplicationId], *[ApplicationCommands.RemoveCollaborator], *)(*)).thenReturn(mockResult.asFailure)
        }
      }
    }
  }
}
