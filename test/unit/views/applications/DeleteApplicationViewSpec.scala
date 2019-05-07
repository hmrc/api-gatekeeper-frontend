/*
 * Copyright 2019 HM Revenue & Customs
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

package unit.views.applications

import java.util.UUID

import config.AppConfig
import model.Forms._
import model._
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Flash
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import unit.utils.ViewHelpers._
import utils.CSRFTokenHelper._
import utils.LoggedInUser

class DeleteApplicationViewSpec extends UnitSpec with OneServerPerSuite with MockitoSugar {
  trait Setup {
    val request = FakeRequest().withCSRFToken
    val mockAppConfig = mock[AppConfig]

    val application =
      ApplicationResponse(
        UUID.randomUUID(),
        "clientid",
        "application1",
        "PRODUCTION",
        None,
        Set(Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR), Collaborator("someone@example.com", CollaboratorRole.DEVELOPER)),
        DateTime.now(),
        Standard(),
        ApplicationState()
      )

    val applicationWithHistory = ApplicationWithHistory(application, Seq.empty)
  }

  "delete application view" should {

    "show application information, including superuser only actions, when logged in as superuser" in new Setup {

      val result = views.html.applications.delete_application.apply(applicationWithHistory, true, deleteApplicationForm.fill(DeleteApplicationForm("", None)))(request, LoggedInUser(None), Flash.emptyCookie, applicationMessages, mockAppConfig)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "button", "Delete application") shouldBe true
      elementExistsByText(document, "td", "PRODUCTION")
    }

    "show application information, excluding superuser only actions, when logged in as non superuser" in new Setup {
      val result = views.html.applications.delete_application.apply(applicationWithHistory, false, deleteApplicationForm.fill(DeleteApplicationForm("", None)))(request, LoggedInUser(None), Flash.emptyCookie, applicationMessages, mockAppConfig)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "a", "Delete application") shouldBe false
    }

    "show error message when no collaborator is chosen" in new Setup {
      val form = deleteApplicationForm.fill(DeleteApplicationForm("", None)).withError("collaboratorEmail", Messages("application.administrator.missing"))

      val result = views.html.applications.delete_application.apply(applicationWithHistory, true, form)(request, LoggedInUser(None), Flash.emptyCookie, applicationMessages, mockAppConfig)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "p", "Choose an administrator") shouldBe true
    }

    "show error message when the application name doesn't match" in new Setup {
      val form = deleteApplicationForm.fill(DeleteApplicationForm("", None)).withError("applicationNameConfirmation", Messages("application.confirmation.error"))

      val result = views.html.applications.delete_application.apply(applicationWithHistory, true, form)(request, LoggedInUser(None), Flash.emptyCookie, applicationMessages, mockAppConfig)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "p", "The application name doesn't match") shouldBe true
    }
  }
}
