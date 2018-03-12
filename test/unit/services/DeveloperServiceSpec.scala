/*
 * Copyright 2018 HM Revenue & Customs
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

package unit.services

import java.util.UUID

import connectors.{ApplicationConnector, DeveloperConnector}
import model.Developer.createUnregisteredDeveloper
import model._
import org.joda.time.DateTime
import org.mockito.Matchers.{any, anyString, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import services.DeveloperService
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class DeveloperServiceSpec extends UnitSpec with MockitoSugar {

  def aUser(name: String) = User(s"$name@example.net", "Fred", "Example", Some(true))

  def aDeveloper(name: String, apps:Seq[Application]=Seq.empty, verified: Boolean = true) =
    Developer(s"$name@example.net", name, s"${name}son", Some(verified), apps)

  def anApp(name: String, collaborators: Set[Collaborator]): ApplicationResponse = {
    ApplicationResponse(UUID.randomUUID(), name, "PRODUCTION", None, collaborators, DateTime.now(), Standard(), ApplicationState())
  }

  trait Setup {
    val mockApplicationConnector = mock[ApplicationConnector]
    val mockDeveloperConnector = mock[DeveloperConnector]

    val underTest = new DeveloperService {
      val applicationConnector = mockApplicationConnector
      val developerConnector = mockDeveloperConnector
    }

    val collaborators = Set(
      Collaborator("sample@email.com", CollaboratorRole.ADMINISTRATOR),
      Collaborator("someone@email.com", CollaboratorRole.DEVELOPER))

    implicit val hc = HeaderCarrier()

    def fetchDeveloperWillReturn(developer: User, apps: Seq[ApplicationResponse]) = {
      when(mockDeveloperConnector.fetchByEmail(anyString)(any[HeaderCarrier]))
        .thenReturn(Future.successful(developer))
      when(mockApplicationConnector.fetchApplicationsByEmail(anyString)(any[HeaderCarrier]))
        .thenReturn(Future.successful(apps))
    }

    def deleteDeveloperWillSucceed = {
      when(mockDeveloperConnector.deleteDeveloper(any[DeleteDeveloperRequest])(any[HeaderCarrier])).thenReturn(Future.successful(DeveloperDeleteSuccessResult))
      when(mockApplicationConnector.removeCollaborator(anyString, anyString, anyString)(any[HeaderCarrier])).thenReturn(Future.successful(ApplicationUpdateSuccessResult))
    }
  }

  def bob(apps: Seq[Application]=Seq.empty) = aDeveloper("Bob", apps)
  def jim(apps: Seq[Application]=Seq.empty) = aDeveloper("Jim", apps)
  def jacob(apps: Seq[Application]=Seq.empty) = aDeveloper("Jacob", apps)
  def julia(apps: Set[Application]) = createUnregisteredDeveloper("Julia@example.net", apps)

  "developerService" should {

    "filter all users (no unregistered collaborators)" in new Setup {
      val applications = Seq(
        anApp("application1", Set(
          Collaborator("Bob@example.net", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jacob@example.net", CollaboratorRole.DEVELOPER))))
      val users = Seq(bob(applications), jim(applications), jacob(applications))


      val result = underTest.filterUsersBy(AllUsers, applications)(users)
      result shouldBe Seq(bob(applications), jim(applications), jacob(applications))
    }

    "filter all users (including unregistered collaborators)" in new Setup {
      val applications = Seq(
        anApp("application1", Set(
          Collaborator("Bob@example.net", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jacob@example.net", CollaboratorRole.DEVELOPER))),
        anApp("application2", Set(
          Collaborator("Julia@example.net", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jim@example.net", CollaboratorRole.DEVELOPER))))

      val users = Seq(bob(applications), jim(applications), jacob(applications))


      val result = underTest.filterUsersBy(AllUsers, applications)(users)
      result shouldBe Seq(bob(applications), jim(applications), jacob(applications), julia(applications.tail.toSet))
    }

    "filter users that have access to 1 or more applications" in new Setup {
      val applications = Seq(
        anApp("application1", Set(
          Collaborator("Bob@example.net", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jacob@example.net", CollaboratorRole.DEVELOPER))),
        anApp("application2", Set(
          Collaborator("Julia@example.net", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jim@example.net", CollaboratorRole.DEVELOPER))))

      val users = Seq(aDeveloper("Bob", applications), aDeveloper("Jim", applications), aDeveloper("Jacob", applications))


      val result = underTest.filterUsersBy(OneOrMoreSubscriptions, applications)(users)
      result shouldBe Seq(aDeveloper("Bob", applications), aDeveloper("Jim", applications), aDeveloper("Jacob", applications), julia(applications.tail.toSet))
    }

    "filter users that are not associated with any applications" in
      new Setup {
        val applications = Seq(
          anApp("application1", Set(
            Collaborator("Shirley@example.net", CollaboratorRole.ADMINISTRATOR),
            Collaborator("Jacob@example.net", CollaboratorRole.DEVELOPER))),
          anApp("application2", Set(
            Collaborator("Julia@example.net", CollaboratorRole.ADMINISTRATOR),
            Collaborator("Jim@example.net", CollaboratorRole.DEVELOPER))))
        val users = Seq(aDeveloper("Shirley", applications), aDeveloper("Gaia"), aDeveloper("Jimbob"))


        val result = underTest.filterUsersBy(NoApplications, applications)(users)
        result shouldBe Seq(aDeveloper("Gaia"), aDeveloper("Jimbob"))
      }

    "filter users who have no subscriptions" in new Setup {
      val _allApplications = Seq(
        anApp("application1", Set(
          Collaborator("Bob@example.net", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jacob@example.net", CollaboratorRole.DEVELOPER))),
        anApp("application2", Set(
          Collaborator("Julia@example.net", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jim@example.net", CollaboratorRole.DEVELOPER))))
      val users = Seq(aDeveloper("Shirley"), aDeveloper("Gaia"), aDeveloper("Jimbob"), aDeveloper("Jim", _allApplications))

      val result = underTest.filterUsersBy(NoSubscriptions, _allApplications)(users)


      result should have size 4

      result shouldBe Seq(aDeveloper("Jim", _allApplications),
        createUnregisteredDeveloper("Bob@example.net", Set(_allApplications.head)),
        createUnregisteredDeveloper("Jacob@example.net", Set(_allApplications.head)),
        createUnregisteredDeveloper("Julia@example.net", Set(_allApplications.tail.head)))
    }

    "filter by status does no filtering when any status" in new Setup {
      val users = Seq(aDeveloper("Bob", verified = false), aDeveloper("Brian"), aDeveloper("Sheila"))
      val result = underTest.filterUsersBy(AnyStatus)(users)
      result shouldBe users
    }

    "filter by status only returns verified users when Verified status" in new Setup {
      val users = Seq(aDeveloper("Bob", verified = false), aDeveloper("Brian"), aDeveloper("Sheila"))
      val result = underTest.filterUsersBy(VerifiedStatus)(users)
      result shouldBe Seq(aDeveloper("Brian"), aDeveloper("Sheila"))
    }

    "filter by status only returns unverified users when Unverified status" in new Setup {
      val users = Seq(aDeveloper("Bob", verified = false), aDeveloper("Brian"), aDeveloper("Sheila"))
      val result = underTest.filterUsersBy(UnverifiedStatus)(users)
      result shouldBe Seq(aDeveloper("Bob", verified = false))
    }

    "fetch the developer and the applications they are a team member on" in new Setup {
      val developer = aUser("Fred")
      val apps = Seq(anApp("application", Set(Collaborator(developer.email, CollaboratorRole.ADMINISTRATOR))))
      fetchDeveloperWillReturn(developer, apps)

      val result = await(underTest.fetchDeveloper(developer.email))
      result.toDeveloper shouldBe developer.toDeveloper(apps)
      verify(mockDeveloperConnector).fetchByEmail(eqTo(developer.email))(any[HeaderCarrier])
      verify(mockApplicationConnector).fetchApplicationsByEmail(eqTo(developer.email))(any[HeaderCarrier])
    }
  }

  "developerService.deleteDeveloper" should {

    "delete the developer if they have no associated apps" in new Setup {
      val gatekeeperUserId = "gate.keeper"
      val developer = aUser("Fred")
      val apps = Seq()
      fetchDeveloperWillReturn(developer, apps)
      deleteDeveloperWillSucceed

      val result = await(underTest.deleteDeveloper(developer.email, gatekeeperUserId))
      result shouldBe DeveloperDeleteSuccessResult

      verify(mockDeveloperConnector).deleteDeveloper(eqTo(DeleteDeveloperRequest(gatekeeperUserId, developer.email)))(any[HeaderCarrier])
    }

    "remove the developer from their associated apps before deleting them" in new Setup {
      val gatekeeperUserId = "gate.keeper"
      val developer = aUser("Fred")
      val apps = Seq(
        anApp("application1", collaborators + Collaborator(developer.email, CollaboratorRole.ADMINISTRATOR)),
        anApp("application2", collaborators + Collaborator(developer.email, CollaboratorRole.ADMINISTRATOR)),
        anApp("application3", collaborators + Collaborator(developer.email, CollaboratorRole.ADMINISTRATOR))
      )
      fetchDeveloperWillReturn(developer, apps)
      deleteDeveloperWillSucceed

      val result = await(underTest.deleteDeveloper(developer.email, gatekeeperUserId))
      result shouldBe DeveloperDeleteSuccessResult

      apps.map { app =>
        verify(mockApplicationConnector).removeCollaborator(eqTo(app.id.toString), eqTo(developer.email), eqTo(gatekeeperUserId))(any[HeaderCarrier])
      }
      verify(mockDeveloperConnector).deleteDeveloper(eqTo(DeleteDeveloperRequest(gatekeeperUserId, developer.email)))(any[HeaderCarrier])
    }

    "fail if the developer is the sole admin on any of their associated apps" in new Setup {
      val gatekeeperUserId = "gate.keeper"
      val developer = aUser("Fred")
      val apps = Seq(anApp("application", Set(Collaborator(developer.email, CollaboratorRole.ADMINISTRATOR))))
      fetchDeveloperWillReturn(developer, apps)

      val result = await(underTest.deleteDeveloper(developer.email, gatekeeperUserId))
      result shouldBe DeveloperDeleteFailureResult

      verify(mockDeveloperConnector, never).deleteDeveloper(any[DeleteDeveloperRequest])(any[HeaderCarrier])
      verify(mockApplicationConnector, never).removeCollaborator(anyString, anyString, anyString)(any[HeaderCarrier])
    }
  }
}
