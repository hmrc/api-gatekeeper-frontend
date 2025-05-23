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

package uk.gov.hmrc.gatekeeper.views.applications

import java.time.{Instant, LocalDateTime}

import org.jsoup.Jsoup

import play.api.mvc.Flash
import play.api.test.FakeRequest

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationState, Collaborators}
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInUser
import uk.gov.hmrc.gatekeeper.builder.ApplicationBuilder
import uk.gov.hmrc.gatekeeper.models.Forms._
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.utils.ViewHelpers._
import uk.gov.hmrc.gatekeeper.views.CommonViewSpec
import uk.gov.hmrc.gatekeeper.views.html.applications.DeleteApplicationView

class DeleteApplicationViewSpec extends CommonViewSpec {

  trait Setup extends ApplicationBuilder {
    val request                   = FakeRequest().withCSRFToken
    val deleteApplicationView     = app.injector.instanceOf[DeleteApplicationView]
    val adminMissingMessages      = messagesProvider.messages("application.administrator.missing")
    val confirmationErrorMessages = messagesProvider.messages("application.confirmation.error")

    val adminEmail = "sample@example.com"

    val application =
      buildApplication(
        ApplicationId.random,
        ClientId("clientid"),
        "gatewayId",
        Some("application1"),
        Environment.PRODUCTION,
        None,
        Set(
          Collaborators.Administrator(UserId.random, LaxEmailAddress(adminEmail)),
          Collaborators.Developer(UserId.random, LaxEmailAddress("someone@example.com"))
        ),
        Instant.now(),
        Some(Instant.now()),
        access = Access.Standard(),
        state = ApplicationState(updatedOn = Instant.now())
      )

    val applicationWithHistory = ApplicationWithHistory(application, List.empty)
  }

  "delete application view" should {
    "show application information, including superuser only actions, when logged in as superuser but not team admin" in new Setup {
      val result = deleteApplicationView.apply(
        applicationWithHistory,
        isSuperUser = true,
        deleteApplicationForm.fill(DeleteApplicationForm("", None))
      )(request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "button", "Delete application") shouldBe true
      elementExistsByText(document, "dd", "Production") shouldBe true
      elementExistsByIdWithAttr(document, "emailAddress-0", "checked") shouldBe false
      labelIdentifiedByForAttrContainsText(document, "emailAddress-0", adminEmail) shouldBe true
    }

    "show application information, including superuser only actions, when logged in as both superuser and team admin" in new Setup {
      val result = deleteApplicationView.apply(
        applicationWithHistory,
        isSuperUser = true,
        deleteApplicationForm.fill(DeleteApplicationForm("", Some(adminEmail)))
      )(request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      val document = Jsoup.parse(result.body)

      elementExistsByIdWithAttr(document, "emailAddress-0", "checked") shouldBe true
      labelIdentifiedByForAttrContainsText(document, "emailAddress-0", adminEmail) shouldBe true
    }

    "show application information, excluding superuser only actions, when logged in as non superuser" in new Setup {
      val result = deleteApplicationView.apply(
        applicationWithHistory,
        isSuperUser = false,
        deleteApplicationForm.fill(DeleteApplicationForm("", None))
      )(request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "a", "Delete application") shouldBe false
    }

    "show error message when no collaborator is chosen" in new Setup {
      val form = deleteApplicationForm.fill(DeleteApplicationForm("", None)).withError("collaboratorEmail", adminMissingMessages)

      val result = deleteApplicationView.apply(
        applicationWithHistory,
        isSuperUser = true,
        form
      )(request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "p", "Error: Choose an administrator") shouldBe true
    }

    "show error message when the application name doesn't match" in new Setup {
      val form = deleteApplicationForm.fill(
        DeleteApplicationForm("", None)
      ).withError("applicationNameConfirmation", confirmationErrorMessages)

      val result = deleteApplicationView.apply(
        applicationWithHistory,
        isSuperUser = true,
        form
      )(request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "p", "Error: The application name doesn't match") shouldBe true
    }
  }
}
