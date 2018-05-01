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

package unit.view.applications

import java.util.UUID

import config.AppConfig
import model._
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Flash
import play.api.test.FakeRequest
import unit.utils.ViewHelpers._

class ApplicationViewSpec extends PlaySpec with OneServerPerSuite {
  "application view" must {
    implicit val request = FakeRequest()

    val application =
      ApplicationResponse(
        UUID.randomUUID(),
        "application1",
        "PRODUCTION",
        None,
        Set(Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR), Collaborator("someone@example.com", CollaboratorRole.DEVELOPER)),
        DateTime.now(),
        Standard(),
        ApplicationState()
      )

    val applicationWithHistory = ApplicationWithHistory(application, Seq.empty)

    "show application information, including status information" in {

      val result = views.html.applications.application.render(applicationWithHistory, Seq.empty, false, request, None, Flash.emptyCookie, applicationMessages, AppConfig)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByAttr(document, "div", "data-status") mustBe true
      elementExistsByAttr(document, "div", "data-status-info") mustBe true
      elementIdentifiedByAttrContainsText(document, "div", "data-status", "Created") mustBe true
      elementIdentifiedByAttrContainsText(document, "div", "data-status-info", "A production application that its admin has created but not submitted for checking") mustBe true
      elementExistsById(document, "review") mustBe false
    }

    "show application information, including link to check application" in {

      val applicationPendingCheck = application.copy(state = ApplicationState(State.PENDING_GATEKEEPER_APPROVAL))

      val result = views.html.applications.application.render(applicationWithHistory.copy(application = applicationPendingCheck), Seq.empty, false, request, None, Flash.emptyCookie, applicationMessages, AppConfig)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByAttr(document, "div", "data-status") mustBe true
      elementExistsByAttr(document, "div", "data-status-info") mustBe true
      elementIdentifiedByAttrContainsText(document, "div", "data-status", "Pending gatekeeper check") mustBe true
      elementIdentifiedByAttrContainsText(document, "div", "data-status-info", "A production application that one of its admins has submitted for checking") mustBe true
      elementIdentifiedByIdContainsText(document, "a", "review", "Check application") mustBe true
    }

    "show application information, including superuser only actions, when logged in as superuser" in {

      val result = views.html.applications.application.render(applicationWithHistory, Seq.empty, true, request, None, Flash.emptyCookie, applicationMessages, AppConfig)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByText(document, "a", "Delete application") mustBe true
      elementExistsByText(document, "a", "Manage") mustBe true
    }

    "show application information, excluding superuser only actions, when logged in as non superuser" in {
      val result = views.html.applications.application.render(applicationWithHistory, Seq.empty, false, request, None, Flash.emptyCookie, applicationMessages, AppConfig)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByText(document, "a", "Delete application") mustBe false
      elementExistsByText(document, "a", "Manage") mustBe false
    }

    "show application information and click on associated developer" in {
      val result = views.html.applications.application.render(applicationWithHistory, Seq.empty, false, request, None, Flash.emptyCookie, applicationMessages, AppConfig)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByText(document, "a", "sample@example.com") mustBe true
    }

    "show application information, pending verification status should have link to resend email" in {
      val applicationPendingVerification = application.copy(state = ApplicationState(State.PENDING_REQUESTER_VERIFICATION))

      val result = views.html.applications.application.render(applicationWithHistory.copy(application = applicationPendingVerification),
        Seq.empty, isSuperUser = false, request, None, Flash.emptyCookie, applicationMessages, AppConfig)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByText(document, "a", "Resend verify email") mustBe true
    }
  }
}
