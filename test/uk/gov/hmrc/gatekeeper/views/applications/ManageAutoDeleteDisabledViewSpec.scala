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

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Standard
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationResponse, ApplicationState, IpAllowlist, MoreApplication}
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{Collaborators, GrantLength, RateLimitTier}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId, Environment, LaxEmailAddress, UserId}
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInUser
import uk.gov.hmrc.gatekeeper.models.Forms.AutoDeletePreviouslyDisabledForm
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.utils.ViewHelpers._
import uk.gov.hmrc.gatekeeper.views.CommonViewSpec
import uk.gov.hmrc.gatekeeper.views.html.applications.ManageAutoDeleteDisabledView

class ManageAutoDeleteDisabledViewSpec extends CommonViewSpec {

  trait Setup {
    val request                                                    = FakeRequest().withCSRFToken
    val manageAutoDeleteDisabledView: ManageAutoDeleteDisabledView = app.injector.instanceOf[ManageAutoDeleteDisabledView]
    val grantLength                                                = GrantLength.EIGHTEEN_MONTHS.days

    val application: ApplicationResponse =
      ApplicationResponse(
        ApplicationId.random,
        ClientId("clientid"),
        "gatewayId",
        "application1",
        Environment.PRODUCTION,
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
        RateLimitTier.BRONZE,
        termsAndConditionsUrl = None,
        privacyPolicyUrl = None,
        checkInformation = None,
        blocked = false,
        ipAllowlist = IpAllowlist(),
        moreApplication = MoreApplication(allowAutoDelete = false)
      )

    val reason     = "Do not delete this application"
    val reasonDate = "26th Sept 2023"
  }

  "Auto Delete Disabled view" should {
    "show reason, date and radio buttons when auto deletion is disabled for application" in new Setup {
      val result: Appendable = manageAutoDeleteDisabledView(application, reason, reasonDate, AutoDeletePreviouslyDisabledForm.form)(request, LoggedInUser(None), messagesProvider)

      val document: Document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "h1", s"${application.name} has been set not to be deleted if it is inactive") shouldBe true
      elementIdentifiedByIdContainsText(document, "dd", "reason-text", "Reason")
      elementIdentifiedByIdContainsText(document, "dd", "reason-value", reason)
      elementIdentifiedByIdContainsText(document, "dd", "date-text", "Date")
      elementIdentifiedByIdContainsText(document, "dd", "date-value", reasonDate)
      elementExistsByText(document, "h2", s"Do you want to change the application to be deleted if it is inactive?") shouldBe true
      elementExistsByIdWithAttr(document, "auto-delete-no", "checked") shouldBe false
      elementExistsByIdWithAttr(document, "auto-delete-yes", "checked") shouldBe false
      labelIdentifiedByForAttrContainsText(document, "auto-delete-yes", "Yes") shouldBe true
      labelIdentifiedByForAttrContainsText(document, "auto-delete-no", "No, the application should not be deleted if inactive") shouldBe true
    }
  }
}
