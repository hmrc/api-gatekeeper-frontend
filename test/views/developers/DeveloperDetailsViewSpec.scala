/*
 * Copyright 2021 HM Revenue & Customs
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

package views.developers

import java.util.UUID

import model.{LoggedInUser, _}
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import utils.ViewHelpers._
import views.html.developers.DeveloperDetailsView
import views.CommonViewSpec

class DeveloperDetailsViewSpec extends CommonViewSpec {

  sealed case class TestApplication(id: ApplicationId,
                                    name: String,
                                    state: ApplicationState,
                                    collaborators: Set[Collaborator],
                                    clientId: ClientId = ClientId("a-client-id"),
                                    deployedTo: String = "PRODUCTION") extends Application

  trait Setup {
    implicit val request = FakeRequest()

    val developer = Developer(RegisteredUser("email@example.com", UserId.random, "firstname", "lastName", true), Seq())

    val developerDetails = app.injector.instanceOf[DeveloperDetailsView]

    def testDeveloperDetails(developer: Developer) = {
      val result = developerDetails.render(developer, true, request, LoggedInUser(None), messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")

      elementExistsByText(document, "h1", developer.email) mustBe true
      document.getElementById("first-name").text mustBe developer.firstName
      document.getElementById("last-name").text mustBe developer.lastName
      document.getElementById("organisation").text mustBe (developer.organisation match {
        case Some(text) => text
        case None => ""
      })
      document.getElementById("status").text mustBe (developer.status match {
        case UnverifiedStatus => "not yet verified"
        case VerifiedStatus => "verified"
        case _ => "unregistered"
      })
    }
  }

  "developer details view" must {
    "show unregistered developer details when logged in as superuser" in new Setup {
      val unregisteredDeveloper = Developer(UnregisteredUser("email@example.com", UserId.random), Seq())
      testDeveloperDetails(unregisteredDeveloper)
    }

    "show unverified developer details when logged in as superuser" in new Setup {
      val unverifiedDeveloper = Developer(RegisteredUser("email@example.com", UserId.random, "firstname", "lastName", false), Seq())
      testDeveloperDetails(unverifiedDeveloper)
    }

    "show verified developer details when logged in as superuser" in new Setup {
      val verifiedDeveloper = Developer(RegisteredUser("email@example.com", UserId.random, "firstname", "lastName", true), Seq())
      testDeveloperDetails(verifiedDeveloper)
    }

    "show developer with organisation when logged in as superuser" in new Setup {
      val verifiedDeveloper = Developer(RegisteredUser("email@example.com", UserId.random, "firstname", "lastName", true, Some("test organisation")), Seq())
      testDeveloperDetails(verifiedDeveloper)
    }

    "show developer with no applications when logged in as superuser" in new Setup {
      val result = developerDetails.render(developer, true, request, LoggedInUser(None), messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")

      elementExistsByText(document, "h2", "Associated applications") mustBe true
      document.getElementById("applications").text mustBe "None"
    }

    "show developer with applications when logged in as superuser" in new Setup {
      val testApplication1: TestApplication = TestApplication(ApplicationId(UUID.randomUUID().toString), "appName1", ApplicationState(State.TESTING), Set(Collaborator("email@example.com", CollaboratorRole.ADMINISTRATOR, UserId.random)))
      val testApplication2: TestApplication = TestApplication(ApplicationId(UUID.randomUUID().toString), "appName2", ApplicationState(State.PRODUCTION), Set(Collaborator("email@example.com", CollaboratorRole.DEVELOPER, UserId.random)))

      val developerWithApps: Developer = developer.copy(applications = Seq(testApplication1, testApplication2))

      val result = developerDetails.render(developerWithApps, true, request, LoggedInUser(None), messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")

      elementExistsByText(document, "h2", "Associated applications") mustBe true
      elementExistsByText(document, "a", "appName1") mustBe true
      elementExistsByText(document, "td", "Admin") mustBe true
      elementExistsByText(document, "a", "appName2") mustBe true
      elementExistsByText(document, "td", "Developer") mustBe true
    }

    "show developer details with delete button when logged in as superuser" in new Setup {
      val result = developerDetails.render(developer, true, request, LoggedInUser(None), messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")

      elementExistsByText(document, "a", "Delete developer") mustBe true
    }

    "show developer details WITH delete button when logged in as non-superuser" in new Setup {
      val result = developerDetails.render(developer, false, request, LoggedInUser(None), messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")

      elementExistsByText(document, "a", "Delete developer") mustBe true
    }
  }
}

