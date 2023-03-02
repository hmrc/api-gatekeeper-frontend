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

import cats.data.NonEmptyList
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.applications.domain.models.Collaborator
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.{CommandFailure, CommandFailures}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.gatekeeper.models.Application
import uk.gov.hmrc.gatekeeper.services.TeamMemberService

trait TeamMemberServiceMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  val mockTeamMemberService = mock[TeamMemberService]

  object TeamMemberServiceMock {

    object AddTeamMember {
      import cats.syntax.either._

      def succeeds() = when(mockTeamMemberService.addTeamMember(*, *, *)(*)).thenReturn(successful(().asRight[NonEmptyList[CommandFailure]]))

      def failsDueToExistingAlready() =
        when(mockTeamMemberService.addTeamMember(*, *, *)(*))
          .thenReturn(successful(NonEmptyList.one(CommandFailures.CollaboratorAlreadyExistsOnApp).asLeft[Unit]))

      def verifyCalledWith(application: Application, collaborator: Collaborator) =
        verify(mockTeamMemberService).addTeamMember(eqTo(application), eqTo(collaborator), *)(*)
    }

    object RemoveTeamMember {
      import cats.syntax.either._

      def succeeds() =
        when(mockTeamMemberService.removeTeamMember(*, *[LaxEmailAddress], *)(*))
          .thenReturn(successful(().asRight[NonEmptyList[CommandFailure]]))

      def failsDueToLastAdmin() =
        when(mockTeamMemberService.removeTeamMember(*, *[LaxEmailAddress], *)(*))
          .thenReturn(successful(NonEmptyList.one(CommandFailures.CannotRemoveLastAdmin).asLeft[Unit]))

      def verifyCalledWith(application: Application, emailToRemove: LaxEmailAddress, gatekeeperUserName: String) =
        verify(mockTeamMemberService).removeTeamMember(eqTo(application), eqTo(emailToRemove), eqTo(gatekeeperUserName))(*)
    }
  }
}
