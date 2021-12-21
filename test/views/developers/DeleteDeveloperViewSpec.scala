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

import uk.gov.hmrc.modules.stride.domain.models.LoggedInUser
import org.jsoup.Jsoup
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import utils.FakeRequestCSRFSupport._
import utils.ViewHelpers._
import views.CommonViewSpec
import views.html.developers.DeleteDeveloperView
import model._

class DeleteDeveloperViewSpec extends CommonViewSpec {

  sealed case class TestApplication(name: String,
                                    collaborators: Set[Collaborator],
                                    id: ApplicationId = ApplicationId.random,
                                    state: ApplicationState = ApplicationState(State.PRODUCTION),
                                    clientId: ClientId = ClientId("a-client-id"),
                                    deployedTo: String = "PRODUCTION") extends Application

  def admin(email: String) = Collaborator(email, CollaboratorRole.ADMINISTRATOR, UserId.random)

  "delete developer view" should {
    implicit val request = FakeRequest().withCSRFToken
    implicit val userName = LoggedInUser(Some("gate.keeper"))
    implicit val messages = app.injector.instanceOf[MessagesControllerComponents].messagesApi.preferred(request)

    val deleteDeveloper = app.injector.instanceOf[DeleteDeveloperView]

    "show the controls to delete the developer when the developer has no apps that they are the sole admin on" in {
      val app = TestApplication("appName1", Set(admin("email@example.com"), admin("other@example.com")))
      val developer = Developer(RegisteredUser("email@example.com", UserId.random, "firstname", "lastName", false), List(app))

      val document = Jsoup.parse(deleteDeveloper(developer).body)
      elementExistsById(document, "submit") shouldBe true
      elementExistsById(document, "cancel") shouldBe true
      elementExistsById(document, "finish") shouldBe false
    }

    "not show the controls to delete the developer when the developer has no apps that they are the sole admin on" in {
      val app = TestApplication("appName1", Set(admin("email@example.com")))
      val developer = Developer(RegisteredUser("email@example.com", UserId.random, "firstname", "lastName", false), List(app))

      val document = Jsoup.parse(deleteDeveloper(developer).body)
      elementExistsById(document, "submit") shouldBe false
      elementExistsById(document, "cancel") shouldBe false
      elementExistsById(document, "finish") shouldBe true
    }
  }
}
