/*
 * Copyright 2017 HM Revenue & Customs
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
import org.mockito.Matchers.{eq => mEq}
import org.scalatest.mock.MockitoSugar
import services.DeveloperService
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier

class DeveloperServiceSpec extends UnitSpec with MockitoSugar {

  def user(name: String, apps:Seq[Application]=Seq.empty, verified: Boolean = true) =
    Developer(s"$name@example.net", name, s"${name}son", Some(verified), apps)

  def app(name: String, collaborators: Set[Collaborator]): ApplicationResponse = {
    ApplicationResponse(UUID.randomUUID(),
      name, None, collaborators, DateTime.now(), ApplicationState())
  }

  trait Setup {
    val testDeveloperService = new DeveloperService {
      val applicationConnector = mock[ApplicationConnector]
      val developerConnector = mock[DeveloperConnector]
    }
    val collaborators = Set(
      Collaborator("sample@email.com", CollaboratorRole.ADMINISTRATOR),
      Collaborator("someone@email.com", CollaboratorRole.DEVELOPER))

    implicit val hc = HeaderCarrier()
  }

  def bob(apps: Seq[Application]=Seq.empty) = user("Bob", apps)
  def jim(apps: Seq[Application]=Seq.empty) = user("Jim", apps)
  def jacob(apps: Seq[Application]=Seq.empty) = user("Jacob", apps)
  def julia(apps: Set[Application]) = createUnregisteredDeveloper("Julia@example.net", apps)

  "developerService" should {

    "filter all users (no unregistered collaborators)" in new Setup {
      val applications = Seq(
        app("application1", Set(
          Collaborator("Bob@example.net", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jacob@example.net", CollaboratorRole.DEVELOPER))))
      val users = Seq(bob(applications), jim(applications), jacob(applications))


      val result = testDeveloperService.filterUsersBy(AllUsers, applications)(users)
      result shouldBe Seq(bob(applications), jim(applications), jacob(applications))
    }

    "filter all users (including unregistered collaborators)" in new Setup {
      val applications = Seq(
        app("application1", Set(
          Collaborator("Bob@example.net", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jacob@example.net", CollaboratorRole.DEVELOPER))),
        app("application2", Set(
          Collaborator("Julia@example.net", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jim@example.net", CollaboratorRole.DEVELOPER))))

      val users = Seq(bob(applications), jim(applications), jacob(applications))


      val result = testDeveloperService.filterUsersBy(AllUsers, applications)(users)
      result shouldBe Seq(bob(applications), jim(applications), jacob(applications), julia(applications.tail.toSet))
    }

    "filter users that have access to 1 or more applications" in new Setup {
      val applications = Seq(
        app("application1", Set(
          Collaborator("Bob@example.net", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jacob@example.net", CollaboratorRole.DEVELOPER))),
        app("application2", Set(
          Collaborator("Julia@example.net", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jim@example.net", CollaboratorRole.DEVELOPER))))

      val users = Seq(user("Bob", applications), user("Jim", applications), user("Jacob", applications))


      val result = testDeveloperService.filterUsersBy(OneOrMoreSubscriptions, applications)(users)
      result shouldBe Seq(user("Bob", applications), user("Jim", applications), user("Jacob", applications), julia(applications.tail.toSet))
    }

    "filter users that are not associated with any applications" in
      new Setup {
        val applications = Seq(
          app("application1", Set(
            Collaborator("Shirley@example.net", CollaboratorRole.ADMINISTRATOR),
            Collaborator("Jacob@example.net", CollaboratorRole.DEVELOPER))),
          app("application2", Set(
            Collaborator("Julia@example.net", CollaboratorRole.ADMINISTRATOR),
            Collaborator("Jim@example.net", CollaboratorRole.DEVELOPER))))
        val users = Seq(user("Shirley", applications), user("Gaia"), user("Jimbob"))


        val result = testDeveloperService.filterUsersBy(NoApplications, applications)(users)
        result shouldBe Seq(user("Gaia"), user("Jimbob"))
      }

    "filter users who have no subscriptions" in new Setup {
      val _allApplications = Seq(
        app("application1", Set(
          Collaborator("Bob@example.net", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jacob@example.net", CollaboratorRole.DEVELOPER))),
        app("application2", Set(
          Collaborator("Julia@example.net", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jim@example.net", CollaboratorRole.DEVELOPER))))
      val users = Seq(user("Shirley"), user("Gaia"), user("Jimbob"), user("Jim", _allApplications))

      val result = testDeveloperService.filterUsersBy(NoSubscriptions, _allApplications)(users)


      result should have size 4

      result shouldBe Seq(user("Jim", _allApplications),
        createUnregisteredDeveloper("Bob@example.net", Set(_allApplications.head)),
        createUnregisteredDeveloper("Jacob@example.net", Set(_allApplications.head)),
        createUnregisteredDeveloper("Julia@example.net", Set(_allApplications.tail.head)))
    }

    "filter by status does no filtering when any status" in new Setup {
      val users = Seq(user("Bob", verified = false), user("Brian"), user("Sheila"))
      val result = testDeveloperService.filterUsersBy(AnyStatus)(users)
      result shouldBe users
    }

    "filter by status only returns verified users when Verified status" in new Setup {
      val users = Seq(user("Bob", verified = false), user("Brian"), user("Sheila"))
      val result = testDeveloperService.filterUsersBy(VerifiedStatus)(users)
      result shouldBe Seq(user("Brian"), user("Sheila"))
    }

    "filter by status only returns unverified users when Unverified status" in new Setup {
      val users = Seq(user("Bob", verified = false), user("Brian"), user("Sheila"))
      val result = testDeveloperService.filterUsersBy(UnverifiedStatus)(users)
      result shouldBe Seq(user("Bob", verified = false))
    }
  }
}
