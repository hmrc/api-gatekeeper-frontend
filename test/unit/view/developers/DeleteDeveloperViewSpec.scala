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

package unit.view.developers

import java.util.UUID

import config.AppConfig
import model._
import org.jsoup.Jsoup
import org.scalatestplus.play.OneServerPerSuite
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import unit.utils.ViewHelpers._
import utils.CSRFTokenHelper._
import views.html.developers.delete_developer

class DeleteDeveloperViewSpec extends UnitSpec with OneServerPerSuite {

  case class TestApplication(name: String,
                             collaborators: Set[Collaborator],
                             id: UUID = UUID.randomUUID(),
                             state: ApplicationState = ApplicationState(State.PRODUCTION)) extends Application

  def admin(email: String) = Collaborator(email, CollaboratorRole.ADMINISTRATOR)

  "delete developer view" should {
    implicit val request = FakeRequest().withCSRFToken
    implicit val userName = Some("gate.keeper")
    implicit val messages = applicationMessages
    implicit val appConfig = AppConfig

    "show the controls to delete the developer when the developer has no apps that they are the sole admin on" in {
      val app = TestApplication("appName1", Set(admin("email@example.com"), admin("other@example.com")))
      val developer = Developer("email@example.com", "firstname", "lastName", None, Seq(app))

      val document = Jsoup.parse(delete_developer(developer).body)
      elementExistsById(document, "submit") shouldBe true
      elementExistsById(document, "cancel") shouldBe true
      elementExistsById(document, "finish") shouldBe false
    }

    "not show the controls to delete the developer when the developer has no apps that they are the sole admin on" in {
      val app = TestApplication("appName1", Set(admin("email@example.com")))
      val developer = Developer("email@example.com", "firstname", "lastName", None, Seq(app))

      val document = Jsoup.parse(delete_developer(developer).body)
      elementExistsById(document, "submit") shouldBe false
      elementExistsById(document, "cancel") shouldBe false
      elementExistsById(document, "finish") shouldBe true
    }
  }
}
