/*
 * Copyright 2016 HM Revenue & Customs
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

package unit.controllers

import java.util.UUID

import connectors.{ApiDefinitionConnector, ApplicationConnector, DeveloperConnector}
import model._
import org.joda.time.DateTime
import org.mockito.BDDMockito._
import org.mockito.Matchers._
import org.mockito.Matchers.{eq => mEq}
import org.scalatest.mock.MockitoSugar
import services.DeveloperService
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class DeveloperServiceSpec extends UnitSpec with MockitoSugar {
  def user(name: String, verified: Boolean = true) =
    User(s"$name@example.net", name, s"${name}son", Some(verified))

  def app(name: String, collaborators: Set[Collaborator]): ApplicationResponse = {
    ApplicationResponse(UUID.randomUUID(),
      name, None, collaborators, DateTime.now(), ApplicationState())
  }

  trait Setup {
    val testDeveloperService = new DeveloperService {
      val applicationConnector = mock[ApplicationConnector]
      val developerConnector = mock[DeveloperConnector]
      val apiDefinitionConnector = mock[ApiDefinitionConnector]
    }
    
    val collaborators = Set(
      Collaborator("sample@email.com", CollaboratorRole.ADMINISTRATOR),
      Collaborator("someone@email.com", CollaboratorRole.DEVELOPER))
  }

  "developerService" should {
    "list all developers when filtering not provided" in new Setup {
      val users = Seq(user("Bob", false), user("Brian"), user("Sheila"))

      val allApplications = Seq(
        ApplicationResponse(UUID.randomUUID(),
          "application1", None, collaborators, DateTime.now(), ApplicationState()),
        ApplicationResponse(UUID.randomUUID(),
          "application2", None, collaborators, DateTime.now(), ApplicationState()),
        ApplicationResponse(UUID.randomUUID(),
          "application3", None, collaborators, DateTime.now(), ApplicationState()))

      given(testDeveloperService.applicationConnector.fetchAllApplications()(any[HeaderCarrier])).willReturn(Future.successful(allApplications))
      given(testDeveloperService.developerConnector.fetchAll()(any[HeaderCarrier])).willReturn(Future.successful(users))
      given(testDeveloperService.apiDefinitionConnector.fetchAll()(any[HeaderCarrier])).willReturn(Seq.empty[APIDefinition])
      val result: Seq[ApplicationResponse] = await(testDeveloperService.fetchApplications(AllUsers)(new HeaderCarrier()))
      result shouldEqual allApplications
    }

    "list filtered developers when filtering is provided" in new Setup {
      val users = Seq(user("Bob", false), user("Brian"), user("Sheila"))

      val filteredApplications = Seq(
        ApplicationResponse(UUID.randomUUID(),
          "application1", None, collaborators, DateTime.now(), ApplicationState()),
        ApplicationResponse(UUID.randomUUID(),
          "application3", None, collaborators, DateTime.now(), ApplicationState()))

      given(testDeveloperService.applicationConnector.fetchAllApplicationsBySubscription(mEq("subscription"))(any[HeaderCarrier])).willReturn(Future.successful(filteredApplications))
      given(testDeveloperService.developerConnector.fetchAll()(any[HeaderCarrier])).willReturn(Future.successful(users))
      given(testDeveloperService.apiDefinitionConnector.fetchAll()(any[HeaderCarrier])).willReturn(Seq.empty[APIDefinition])
      val result = await(testDeveloperService.fetchApplications(Value("subscription"))(new HeaderCarrier()))
      result shouldBe filteredApplications
    }

    "Turn a list of users into an email list" in new Setup {
      val users = Seq(user("Bob", false), user("Brian"), user("Sheila"))
      val result = testDeveloperService.emailList(users)
      result shouldBe "Bob@example.net; Brian@example.net; Sheila@example.net"
    }

    "filter all users (no unregisted collaborators)" in new Setup {
      val users = Seq(user("Bob"), user("Jim"), user("Jacob"))
      val applications = Seq(
        app("application1", Set(
          Collaborator("Bob@example.net", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jacob@example.net", CollaboratorRole.DEVELOPER))))

      val result = testDeveloperService.filterUsersBy(AllUsers, applications)(users)
      result shouldBe Seq( user("Bob"), user("Jim"), user("Jacob"))
    }

    "filter all users (including unregistered collaborators)" in new Setup {
      val users = Seq( user("Bob"), user("Jim"), user("Jacob"))
      val applications = Seq(
        app("application1", Set(
          Collaborator("Bob@example.net", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jacob@example.net", CollaboratorRole.DEVELOPER))),
        app("application2", Set(
          Collaborator("Julia@example.net", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jim@example.net", CollaboratorRole.DEVELOPER))))

      val result = testDeveloperService.filterUsersBy(AllUsers, applications)(users)
      result shouldBe Seq( user("Bob"), user("Jim"), user("Jacob"), UnregisteredCollaborator("Julia@example.net"))
    }

    "filter users that have access to 1 or more applications" in new Setup {
      val users = Seq( user("Bob"), user("Jim"), user("Jacob"))
      val applications = Seq(
        app("application1", Set(
          Collaborator("Bob@example.net", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jacob@example.net", CollaboratorRole.DEVELOPER))),
        app("application2", Set(
          Collaborator("Julia@example.net", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jim@example.net", CollaboratorRole.DEVELOPER))))

      val result = testDeveloperService.filterUsersBy(OneOrMoreSubscriptions, applications)(users)
      result shouldBe Seq( user("Bob"), user("Jim"), user("Jacob"), UnregisteredCollaborator("Julia@example.net"))
    }

    "filter users that are not associated with any applications" in
      new Setup {
        val users = Seq( user("Shirley"), user("Gaia"), user("Jimbob"))
        val applications = Seq(
          app("application1", Set(
            Collaborator("Shirley@example.net", CollaboratorRole.ADMINISTRATOR),
            Collaborator("Jacob@example.net", CollaboratorRole.DEVELOPER))),
          app("application2", Set(
            Collaborator("Julia@example.net", CollaboratorRole.ADMINISTRATOR),
            Collaborator("Jim@example.net", CollaboratorRole.DEVELOPER))))

        val result = testDeveloperService.filterUsersBy(NoApplications, applications)(users)
        result shouldBe Seq(user("Gaia"), user("Jimbob"))
      }

    "filter users who have no subscriptions" in new Setup {
      val users = Seq( user("Shirley"), user("Gaia"), user("Jimbob"), user("Jim"))
      val _allApplications = Seq(
        app("application1", Set(
          Collaborator("Bob@example.net", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jacob@example.net", CollaboratorRole.DEVELOPER))),
        app("application2", Set(
          Collaborator("Julia@example.net", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jim@example.net", CollaboratorRole.DEVELOPER))))

      val result = testDeveloperService.filterUsersBy(NoSubscriptions, _allApplications)(users)
      result shouldBe Seq(user("Jim"), UnregisteredCollaborator("Bob@example.net"), UnregisteredCollaborator("Jacob@example.net"), UnregisteredCollaborator("Julia@example.net"))
    }

    "filter by status does no filtering when any status" in new Setup {
      val users = Seq(user("Bob", false), user("Brian"), user("Sheila"))
      val result = testDeveloperService.filterUsersBy(AnyStatus)(users)
      result shouldBe users
    }

    "filter by status only returns verified users when Verified status" in new Setup {
      val users = Seq(user("Bob", false), user("Brian"), user("Sheila"))
      val result = testDeveloperService.filterUsersBy(VerifiedStatus)(users)
      result shouldBe Seq(user("Brian"), user("Sheila"))
    }

    "filter by status only returns unverified users when Unverified status" in new Setup {
      val users = Seq(user("Bob", false), user("Brian"), user("Sheila"))
      val result = testDeveloperService.filterUsersBy(UnverifiedStatus)(users)
      result shouldBe Seq(user("Bob", verified = false))
    }
  }
}

