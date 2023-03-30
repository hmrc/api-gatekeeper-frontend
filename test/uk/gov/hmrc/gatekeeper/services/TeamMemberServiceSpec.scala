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

package uk.gov.hmrc.gatekeeper.services

import java.time.{LocalDateTime, Period}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful

import cats.data.NonEmptyList
import mocks.connectors._
import org.mockito.scalatest.ResetMocksAfterEachTest
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{ApplicationId, Collaborator, Collaborators}
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.gatekeeper.connectors._
import uk.gov.hmrc.gatekeeper.models._

class TeamMemberServiceSpec extends AsyncHmrcSpec with ResetMocksAfterEachTest {

  trait Setup
      extends MockitoSugar
      with ArgumentMatchersSugar
      with CommandConnectorMockProvider {

    val mockDeveloperConnector        = mock[DeveloperConnector]
    val mockSubscriptionFieldsService = mock[SubscriptionFieldsService]

    val teamMemberService = new TeamMemberService(
      CommandConnectorMock.aMock,
      mockDeveloperConnector
    )
    val underTest         = spy(teamMemberService)

    implicit val hc = HeaderCarrier()

    val collaborators = Set[Collaborator](
      Collaborators.Administrator(UserId.random, "sample@example.com".toLaxEmail),
      Collaborators.Developer(UserId.random, "someone@example.com".toLaxEmail)
    )

    val grantLength: Period = Period.ofDays(547)

    val stdApp1 = ApplicationResponse(
      ApplicationId.random,
      ClientId("clientid1"),
      "gatewayId1",
      "application1",
      "PRODUCTION",
      None,
      collaborators,
      LocalDateTime.now(),
      Some(LocalDateTime.now()),
      Standard(),
      ApplicationState(),
      grantLength
    )

    val gatekeeperUserId = "loggedin.gatekeeper"
  }

  "removeTeamMember" should {
    val requestingUser = "bob in SDST"

    "issue command to remove a member from an app successfully" in new Setup {
      val application          = stdApp1
      val collaboratorToRemove = application.collaborators.filter(_.isDeveloper).head

      when(mockDeveloperConnector.fetchByEmails(*)(*)).thenReturn(successful(List.empty))
      CommandConnectorMock.IssueCommand.ToRemoveCollaborator.succeeds()

      await(underTest.removeTeamMember(application, collaboratorToRemove.emailAddress, requestingUser)) shouldBe Right(())
    }

    "propagate CannotRemoveLastAdmin error from application connector" in new Setup {
      val collaboratorToRemove = stdApp1.collaborators.filter(_.isDeveloper).head

      when(mockDeveloperConnector.fetchByEmails(*)(*)).thenReturn(successful(List.empty))
      CommandConnectorMock.IssueCommand.ToRemoveCollaborator.failsWithLastAdmin()

      await(underTest.removeTeamMember(stdApp1, collaboratorToRemove.emailAddress, requestingUser)) shouldBe Left(NonEmptyList.one(CommandFailures.CannotRemoveLastAdmin))
    }

    "include correct set of admins to email" in new Setup {

      val memberToRemove     = "removeMe@example.com".toLaxEmail
      val gatekeeperUserName = "bob in SDST"

      val verifiedAdmin      = Collaborators.Administrator(UserId.random, "verified@example.com".toLaxEmail)
      val verifiedOtherAdmin = Collaborators.Administrator(UserId.random, "verifiedother@example.com".toLaxEmail)
      val unverifiedAdmin    = Collaborators.Administrator(UserId.random, "unverified@example.com".toLaxEmail)
      val adminToRemove      = Collaborators.Administrator(UserId.random, memberToRemove)
      val verifiedDeveloper  = Collaborators.Developer(UserId.random, "developer@example.com".toLaxEmail)

      val application    = stdApp1.copy(collaborators = Set(verifiedAdmin, unverifiedAdmin, adminToRemove, verifiedDeveloper))
      val nonAdderAdmins = List(
        RegisteredUser(verifiedAdmin.emailAddress, UserId.random, "verified", "user", true),
        RegisteredUser(unverifiedAdmin.emailAddress, UserId.random, "unverified", "user", false),
        RegisteredUser(verifiedOtherAdmin.emailAddress, UserId.random, "verified", "user", true)
      )
      when(mockDeveloperConnector.fetchByEmails(eqTo(Set(verifiedAdmin.emailAddress, unverifiedAdmin.emailAddress)))(*))
        .thenReturn(successful(nonAdderAdmins))

      val expectedAdminsToEmail = Set(verifiedAdmin.emailAddress, verifiedOtherAdmin.emailAddress)

      CommandConnectorMock.IssueCommand.ToRemoveCollaborator.succeedsFor(application.id, expectedAdminsToEmail)

      await(underTest.removeTeamMember(application, memberToRemove, gatekeeperUserName)) shouldBe Right(())

    }
  }
}
