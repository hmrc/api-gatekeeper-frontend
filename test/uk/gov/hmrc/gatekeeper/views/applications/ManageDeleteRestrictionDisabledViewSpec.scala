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
import org.jsoup.nodes.Document

import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat.Appendable

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationState, ApplicationWithCollaborators, Collaborators, DeleteRestriction, MoreApplication}
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInUser
import uk.gov.hmrc.gatekeeper.builder.ApplicationBuilder
import uk.gov.hmrc.gatekeeper.models.Forms.DeleteRestrictionPreviouslyDisabledForm
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.utils.ViewHelpers._
import uk.gov.hmrc.gatekeeper.views.CommonViewSpec
import uk.gov.hmrc.gatekeeper.views.html.applications.ManageDeleteRestrictionDisabledView

class ManageDeleteRestrictionDisabledViewSpec extends CommonViewSpec {

  trait Setup extends ApplicationBuilder {
    val request                                                                  = FakeRequest().withCSRFToken
    val manageDeleteRestrictionDisabledView: ManageDeleteRestrictionDisabledView = app.injector.instanceOf[ManageDeleteRestrictionDisabledView]

    val application: ApplicationWithCollaborators =
      buildApplication(
        ApplicationId.random,
        ClientId("clientid"),
        "gatewayId",
        Some("application1"),
        Environment.PRODUCTION,
        None,
        Set(
          Collaborators.Administrator(UserId.random, LaxEmailAddress("sample@example.com")),
          Collaborators.Developer(UserId.random, LaxEmailAddress("someone@example.com"))
        ),
        Instant.now(),
        Some(Instant.now()),
        access = Access.Standard(),
        state = ApplicationState(updatedOn = Instant.now())
      )
  }

  "Manage Delete Restriction Disabled view" should {
    "show radio button 'No' checked when no delete restriction for application (allow delete is enabled)" in new Setup {
      val result: Appendable = manageDeleteRestrictionDisabledView(application, DeleteRestrictionPreviouslyDisabledForm.form)(request, LoggedInUser(None), messagesProvider)

      val document: Document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "h1", s"Do you want to protect ${application.name} from being deleted?") shouldBe true
      elementExistsByText(
        document,
        "p",
        "This will protect the application from being deleted by users and by the system if it does not make any API calls for a long time."
      ) shouldBe true
      elementExistsByIdWithAttr(document, "yes-protect-allow-delete-no", "checked") shouldBe false
      elementExistsByIdWithAttr(document, "no-protect-allow-delete-yes", "checked") shouldBe true
      labelIdentifiedByForAttrContainsText(document, "yes-protect-allow-delete-no", "Yes") shouldBe true
    }

    "show reason text area when radio button 'Yes' selected" in new Setup {
      val result: Appendable =
        manageDeleteRestrictionDisabledView(
          application.modify(_.copy(deleteRestriction = DeleteRestriction.DoNotDelete("", Actors.GatekeeperUser("User"), instant))),
          DeleteRestrictionPreviouslyDisabledForm.form
        )(request, LoggedInUser(None), messagesProvider)

      val document: Document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "h1", s"Do you want to protect ${application.name} from being deleted?") shouldBe true
      elementExistsByText(
        document,
        "p",
        "This will protect the application from being deleted by users and by the system if it does not make any API calls for a long time."
      ) shouldBe true
      elementExistsByIdWithAttr(document, "no-protect-allow-delete-yes", "checked") shouldBe false
      elementExistsByIdWithAttr(document, "yes-protect-allow-delete-no", "checked") shouldBe true
      labelIdentifiedByForAttrContainsText(document, "yes-protect-allow-delete-no", "Yes") shouldBe true
      elementExistsByIdWithClass(document, "conditional-reason", "govuk-radios__conditional") shouldBe true
      elementExistsById(document, "reason") shouldBe true
      labelIdentifiedByForAttrContainsText(document, "reason", "Give the reasons for protecting this application from being deleted. Please note that the reasons are externally visible.") shouldBe true
    }
  }
}
