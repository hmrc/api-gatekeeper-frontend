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

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.*
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, LaxEmailAddress}
import uk.gov.hmrc.gatekeeper.connectors.ApplicationCommandConnector
import uk.gov.hmrc.gatekeeper.utils.SubclassMockSupport

trait CommandConnectorMockProvider extends MockitoSugar with ArgumentMatchersSugar with SubclassMockSupport {

  object CommandConnectorMock {
    val aMock: ApplicationCommandConnector = mock[ApplicationCommandConnector]

    import CommandHandlerTypesForDispatchSuccessResult.types.*
    import CommandHandlerTypesForDispatchSuccessResult.types.Implicits.*

    object IssueCommand {
      import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.DispatchSuccessResult

      def succeeds()(using ec: ExecutionContext) = {
        val mockResult = mock[DispatchSuccessResult]
        when(aMock.dispatch(*[ApplicationId], *, *)(using *)).thenReturn(mockResult.asSuccess)
      }

      def succeedsReturning(app: ApplicationWithCollaborators)(using ec: ExecutionContext) = {
        when(aMock.dispatch(*[ApplicationId], *, *)(using *)).thenReturn(DispatchSuccessResult(app).asSuccess)
      }

      def failsWith(failure: CommandFailure)(using ec: ExecutionContext) = {
        when(aMock.dispatch(*[ApplicationId], *, *)(using *)).thenReturn(failure.asFailure)
      }

      def verifyCommand(id: ApplicationId) = {
        val cmdCaptor = ArgCaptor[ApplicationCommand]
        verify(aMock).dispatch(eqTo(id), cmdCaptor, *)(using *)
        cmdCaptor.value
      }

      def verifyNoCommandsIssued() = {
        verify(aMock, never).dispatch(*[ApplicationId], *, *)(using *)
      }

      object ToRemoveCollaborator {
        val mockResult = mock[DispatchSuccessResult]

        def succeeds()(using ec: ExecutionContext) = {
          when(aMock.dispatch(*[ApplicationId], *, *)(using *)).thenReturn(mockResult.asSuccess)
        }

        def succeedsFor(id: ApplicationId, adminsToEmail: Set[LaxEmailAddress])(using ec: ExecutionContext) = {
          when(aMock.dispatch(eqTo(id), *, eqTo(adminsToEmail))(using *)).thenReturn(mockResult.asSuccess)
        }

        def failsWithLastAdmin()(using ec: ExecutionContext) = {
          val mockResult = NonEmptyList.one(CommandFailures.CannotRemoveLastAdmin)
          when(aMock.dispatch(*[ApplicationId], *[ApplicationCommands.RemoveCollaborator], *)(using *)).thenReturn(mockResult.asFailure)
        }
      }
    }
  }
}
