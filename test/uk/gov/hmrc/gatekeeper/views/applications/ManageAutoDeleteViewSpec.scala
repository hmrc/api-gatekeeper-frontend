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

import java.time.{LocalDateTime, Period}

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat.Appendable

import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{ApplicationId, Collaborators}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInUser
import uk.gov.hmrc.gatekeeper.models.Forms.AutoDeleteConfirmationForm
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.applications.MoreApplication
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.utils.ViewHelpers._
import uk.gov.hmrc.gatekeeper.views.CommonViewSpec
import uk.gov.hmrc.gatekeeper.views.html.applications.ManageAutoDeleteView

class ManageAutoDeleteViewSpec extends CommonViewSpec {

  trait Setup {
    val request                                    = FakeRequest().withCSRFToken
    val manageAutoDeleteView: ManageAutoDeleteView = app.injector.instanceOf[ManageAutoDeleteView]
    val grantLength: Period                        = Period.ofDays(547)

    val application: ApplicationResponse =
      ApplicationResponse(
        ApplicationId.random,
        ClientId("clientid"),
        "gatewayId",
        "application1",
        "PRODUCTION",
        None,
        Set(
          Collaborators.Administrator(UserId.random, LaxEmailAddress("sample@example.com")),
          Collaborators.Developer(UserId.random, LaxEmailAddress("someone@example.com"))
        ),
        LocalDateTime.now(),
        Some(LocalDateTime.now()),
        Standard(),
        ApplicationState(),
        grantLength,
        ipAllowlist = IpAllowlist()
      )
  }

  "Auto Delete view" should {
    "show Auto Delete information and radio button 'Yes' selected when allowAutoDelete is true for application" in new Setup {
      val result: Appendable = manageAutoDeleteView(application, AutoDeleteConfirmationForm.form)(request, LoggedInUser(None), messagesProvider)

      val document: Document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "h1", s"Do you want ${application.name} to be deleted if it is inactive?") shouldBe true
      elementExistsByText(document, "p", "Applications that don't make any API calls for a long time are deleted by the system, unless they are excluded.") shouldBe true
      elementExistsByIdWithAttr(document, "auto-delete-no", "checked") shouldBe false
      elementExistsByIdWithAttr(document, "auto-delete-yes", "checked") shouldBe true
      labelIdentifiedByForAttrContainsText(document, "yes", "Yes") shouldBe true
    }

    "show Auto Delete information and radio button 'No' selected when allowAutoDelete is false for application" in new Setup {
      val result: Appendable =
        manageAutoDeleteView(application.copy(moreApplication = MoreApplication(false)), AutoDeleteConfirmationForm.form)(request, LoggedInUser(None), messagesProvider)

      val document: Document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "h1", s"Do you want ${application.name} to be deleted if it is inactive?") shouldBe true
      elementExistsByText(document, "p", "Applications that don't make any API calls for a long time are deleted by the system, unless they are excluded.") shouldBe true
      elementExistsByIdWithAttr(document, "auto-delete-yes", "checked") shouldBe false
      elementExistsByIdWithAttr(document, "auto-delete-no", "checked") shouldBe true
      labelIdentifiedByForAttrContainsText(document, "no", "No") shouldBe true
      elementExistsByIdWithClass(document, "conditional-reason", "govuk-radios__conditional") shouldBe true
      elementExistsById(document, "reason") shouldBe true
      labelIdentifiedByForAttrContainsText(document, "reason", "Give the reasons for excluding this application from being deleted if it is inactive") shouldBe true
    }
  }
}
