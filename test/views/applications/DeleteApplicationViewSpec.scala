/*
 * Copyright 2020 HM Revenue & Customs
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

package views.applications

import java.util.UUID

import model.{LoggedInUser, _}
import model.Forms._
import org.joda.time.DateTime
import org.jsoup.Jsoup
import play.api.mvc.Flash
import play.api.test.FakeRequest
import utils.FakeRequestCSRFSupport._
import utils.ViewHelpers._
import views.html.applications.DeleteApplicationView
import views.CommonViewSpec

class DeleteApplicationViewSpec extends CommonViewSpec {

  trait Setup {
    val request = FakeRequest().withCSRFToken
    val deleteApplicationView = app.injector.instanceOf[DeleteApplicationView]
    val adminMissingMessages = messagesProvider.messages("application.administrator.missing")
    val confirmationErrorMessages = messagesProvider.messages("application.confirmation.error")

    val application =
      ApplicationResponse(
        ApplicationId(UUID.randomUUID().toString()),
        "clientid",
        "gatewayId",
        "application1",
        "PRODUCTION",
        None,
        Set(Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR), Collaborator("someone@example.com", CollaboratorRole.DEVELOPER)),
        DateTime.now(),
        DateTime.now(),
        Standard(),
        ApplicationState()
      )

    val applicationWithHistory = ApplicationWithHistory(application, Seq.empty)
  }

  "delete application view" should {
    "show application information, including superuser only actions, when logged in as superuser" in new Setup {
      val result = deleteApplicationView.apply(
        applicationWithHistory, isSuperUser = true, deleteApplicationForm.fill(DeleteApplicationForm("", None))
      )(request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByText(document, "button", "Delete application") mustBe true
      elementExistsByText(document, "td", "PRODUCTION")
    }

    "show application information, excluding superuser only actions, when logged in as non superuser" in new Setup {
      val result = deleteApplicationView.apply(
        applicationWithHistory, isSuperUser = false, deleteApplicationForm.fill(DeleteApplicationForm("", None))
      )(request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByText(document, "a", "Delete application") mustBe false
    }

    "show error message when no collaborator is chosen" in new Setup {
      val form = deleteApplicationForm.fill(DeleteApplicationForm("", None)).withError("collaboratorEmail", adminMissingMessages)

      val result = deleteApplicationView.apply(
        applicationWithHistory, isSuperUser = true, form)(request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByText(document, "p", "Choose an administrator") mustBe true
    }

    "show error message when the application name doesn't match" in new Setup {
      val form = deleteApplicationForm.fill(
        DeleteApplicationForm("", None)).withError("applicationNameConfirmation", confirmationErrorMessages)

      val result = deleteApplicationView.apply(
        applicationWithHistory, isSuperUser = true, form)(request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByText(document, "p", "The application name doesn't match") mustBe true
    }
  }
}
