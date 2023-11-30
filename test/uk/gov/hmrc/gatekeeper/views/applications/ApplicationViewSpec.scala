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

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, Period}

import org.jsoup.Jsoup

import play.api.mvc.Flash
import play.api.test.FakeRequest

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Standard
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationState, CheckInformation, IpAllowlist, MoreApplication, TermsOfUseAgreement}
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{GrantLength, RateLimitTier}
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.gatekeeper.builder.{ApiBuilder, ApplicationBuilder, SubscriptionsBuilder}
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.applications.NewApplication
import uk.gov.hmrc.gatekeeper.models.view.{ApplicationViewModel, ResponsibleIndividualHistoryItem}
import uk.gov.hmrc.gatekeeper.services.TermsOfUseService.TermsOfUseAgreementDisplayDetails
import uk.gov.hmrc.gatekeeper.utils.ViewHelpers._
import uk.gov.hmrc.gatekeeper.views.CommonViewSpec
import uk.gov.hmrc.gatekeeper.views.html.applications.ApplicationView

class ApplicationViewSpec extends CommonViewSpec with SubscriptionsBuilder with ApiBuilder with ApplicationBuilder {

  trait Setup {
    implicit val request = FakeRequest()
    val applicationView  = app.injector.instanceOf[ApplicationView]

    val developers = List[RegisteredUser] {
      new RegisteredUser(LaxEmailAddress("joe.bloggs@example.co.uk"), UserId.random, "joe", "bloggs", false)
    }

    val clientId = ClientId("clientid")

    val application = buildApplication(
      id = ApplicationId.random,
      clientId = clientId,
      gatewayId = "gateway",
      name = Some("AnApplicationName"),
      createdOn = LocalDateTime.now(),
      lastAccess = Some(LocalDateTime.now()),
      lastAccessTokenUsage = None,
      deployedTo = Environment.PRODUCTION,
      description = None,
      collaborators = Set.empty,
      access = Standard(),
      state = ApplicationState(),
      rateLimitTier = RateLimitTier.BRONZE,
      blocked = false,
      checkInformation = None,
      ipAllowlist = IpAllowlist()
    )

    val DefaultApplicationViewModel = ApplicationViewModel(
      developers = developers,
      application = application,
      subscriptions = List.empty,
      subscriptionsThatHaveFieldDefns = List.empty,
      stateHistory = List.empty,
      hasSubmissions = false,
      gatekeeperApprovalsUrl = s"http://localhost:1234/api-gatekeeper-approvals-frontend/applications/${application.id}",
      List(ResponsibleIndividualHistoryItem("Mr Responsible Individual", "ri@example.com", "22 June 2022", "Present")),
      None, // Some(TermsOfUseAgreementDisplayDetails("ri@example.com", "12 March 2023", "2"))
      false,
      termsOfUseInvitationUrl = s"http://localhost:1234/api-gatekeeper-approvals-frontend/applications/${application.id}/send-new-terms-of-use"
    )
  }

  trait SubscriptionsSetup extends Setup {

    val subscriptionsViewData: List[(String, List[(ApiVersionNbr, ApiStatus)])] = List(
      (
        "My API Name",
        List(
          (VersionOne, ApiStatus.STABLE),
          (VersionTwo, ApiStatus.BETA)
        )
      ),
      (
        "My Other API Name",
        List(
          (VersionOne, ApiStatus.STABLE)
        )
      )
    )
  }

  "application view" should {
    "show application with no check information" in new Setup {
      val result = applicationView.render(DefaultApplicationViewModel, strideUserRequest, Flash.emptyCookie)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByAttr(document, "dd", "data-terms") shouldBe true
      elementIdentifiedByAttrContainsText(document, "dd", "data-terms", "Not agreed") shouldBe true
    }

    "show application with check information but no terms of use agreed" in new Setup {

      val checkInformation                          = CheckInformation()
      val applicationWithCheckInformationButNoTerms = application.withCheckInformation(checkInformation)

      val result = applicationView.render(
        DefaultApplicationViewModel.withApplication(applicationWithCheckInformationButNoTerms),
        strideUserRequest,
        Flash.emptyCookie
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByAttr(document, "dd", "data-terms") shouldBe true
      elementIdentifiedByAttrContainsText(document, "dd", "data-terms", "Not agreed") shouldBe true
    }

    "show application with check information and terms of use agreed" in new Setup {
      val termsOfUseAgreement = TermsOfUseAgreementDisplayDetails("ri@example.com", "12 March 2023", "2")

      val result = applicationView.render(
        DefaultApplicationViewModel.withMaybeLatestTOUAgreement(Some(termsOfUseAgreement)),
        strideUserRequest,
        Flash.emptyCookie
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByAttr(document, "dd", "data-terms") shouldBe true
      elementIdentifiedByAttrContainsText(document, "dd", "data-terms", "Not agreed") shouldBe false
      val agreedText =
        s"v${termsOfUseAgreement.version} agreed by ${termsOfUseAgreement.emailAddress} on ${termsOfUseAgreement.date}"
      elementIdentifiedByAttrContainsText(document, "dd", "data-terms", agreedText) shouldBe true
    }

    "show application information, including responsible individual table" in new Setup {
      val result = applicationView.render(
        DefaultApplicationViewModel,
        strideUserRequest,
        Flash.emptyCookie
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsById(document, "responsible-individual-table") shouldBe true
      elementExistsByAttr(document, "th", "data-responsible-individual-name") shouldBe true
      elementIdentifiedByAttrContainsText(document, "th", "data-responsible-individual-name", "Mr Responsible Individual") shouldBe true
      elementExistsByAttr(document, "td", "data-responsible-individual-email") shouldBe true
      elementIdentifiedByAttrContainsText(document, "td", "data-responsible-individual-email", "ri@example.com") shouldBe true
      elementExistsByAttr(document, "td", "data-responsible-individual-from-date") shouldBe true
      elementIdentifiedByAttrContainsText(document, "td", "data-responsible-individual-from-date", "22 June 2022") shouldBe true
      elementExistsByAttr(document, "td", "data-responsible-individual-to-date") shouldBe true
      elementIdentifiedByAttrContainsText(document, "td", "data-responsible-individual-to-date", "Present") shouldBe true
    }

    "show application information, including status information" in new Setup {
      val result = applicationView.render(
        DefaultApplicationViewModel,
        strideUserRequest,
        Flash.emptyCookie
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByAttr(document, "span", "data-status") shouldBe true
      elementExistsByAttr(document, "span", "data-status-info") shouldBe true
      elementIdentifiedByAttrContainsText(document, "span", "data-status", "Created") shouldBe true
      val checkingText = "A production application that its admin has created but not submitted for checking"
      elementIdentifiedByAttrContainsText(document, "span", "data-status-info", checkingText) shouldBe true
      elementExistsById(document, "review") shouldBe false
    }

    "show application information, including link to check application" in new Setup {
      val applicationPendingCheck = application.pendingGKApproval

      val result = applicationView.render(
        DefaultApplicationViewModel.withApplication(applicationPendingCheck),
        strideUserRequest,
        Flash.emptyCookie
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByAttr(document, "span", "data-status") shouldBe true
      elementExistsByAttr(document, "span", "data-status-info") shouldBe true
      elementIdentifiedByAttrContainsText(document, "span", "data-status", "Pending gatekeeper check") shouldBe true
      val checkingText = "A production application that one of its admins has submitted for checking"
      elementIdentifiedByAttrContainsText(document, "span", "data-status-info", checkingText) shouldBe true
      elementIdentifiedByIdContainsText(document, "a", "review", "Check application") shouldBe true
    }

    "show application information, including superuser specific actions, when logged in as superuser" in new Setup {
      val result = applicationView.render(
        DefaultApplicationViewModel,
        superUserRequest,
        Flash.emptyCookie
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")

      elementExistsByText(document, "a", "Delete application") shouldBe true
      elementExistsById(document, "manage-subscriptions") shouldBe true

    }

    "show application information, excluding superuser specific actions, when logged in as non superuser" in new Setup {
      val result = applicationView.render(
        DefaultApplicationViewModel,
        strideUserRequest,
        Flash.emptyCookie
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "a", "Delete application") shouldBe false
      elementExistsById(document, "manage-subscriptions") shouldBe false

    }

    "show 'Manage' rate limit link when logged in as admin" in new Setup {
      val result = applicationView.render(
        DefaultApplicationViewModel,
        adminRequest,
        Flash.emptyCookie
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")

      elementExistsById(document, "manage-rate-limit") shouldBe true

    }

    "not show 'Manage' rate limit link when logged in as non admin" in new Setup {
      val result = applicationView.render(
        DefaultApplicationViewModel,
        superUserRequest,
        Flash.emptyCookie
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsById(document, "manage-rate-limit") shouldBe false

    }

    "show 'Manage' Application deleted if inactive link when logged in as admin" in new Setup {
      val result = applicationView.render(
        DefaultApplicationViewModel,
        adminRequest,
        Flash.emptyCookie
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")

      elementExistsById(document, "manage-application-deleted-if-active") shouldBe true
      elementExistsByAttr(document, "dd", "data-application-deleted-if-active") shouldBe true
      elementIdentifiedByAttrContainsText(document, "dd", "data-application-deleted-if-active", "Yes") shouldBe true

    }

    "show 'Manage' Application deleted if inactive link when logged in as super user" in new Setup {
      val result = applicationView.render(
        DefaultApplicationViewModel,
        superUserRequest,
        Flash.emptyCookie
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")

      elementExistsById(document, "manage-application-deleted-if-active") shouldBe true
      elementExistsByAttr(document, "dd", "data-application-deleted-if-active") shouldBe true
      elementIdentifiedByAttrContainsText(document, "dd", "data-application-deleted-if-active", "Yes") shouldBe true

    }

    "not show 'Manage' Application deleted if inactive link when logged in as user" in new Setup {
      val result = applicationView.render(
        DefaultApplicationViewModel,
        strideUserRequest,
        Flash.emptyCookie
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsById(document, "manage-application-deleted-if-active") shouldBe false

    }

    "show 'Block Application' button when logged in as admin" in new Setup {
      val activeApplication = application.inProduction

      val result = applicationView.render(
        DefaultApplicationViewModel.withApplication(activeApplication),
        adminRequest,
        Flash.emptyCookie
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsById(document, "block-application") shouldBe true

    }

    "hide 'Block Application' button when logged in as non-admin" in new Setup {
      val activeApplication = application.inProduction

      val result = applicationView.render(
        DefaultApplicationViewModel.withApplication(activeApplication),
        superUserRequest,
        Flash.emptyCookie
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsById(document, "block-application") shouldBe false

    }

    "show application information and click on associated developer" in new Setup {
      val user   = RegisteredUser(LaxEmailAddress("sample@example.com"), UserId.random, "joe", "bloggs", true)
      val result = applicationView.render(
        DefaultApplicationViewModel.withDeveloper(user),
        strideUserRequest,
        Flash.emptyCookie
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "a", "sample@example.com") shouldBe true
    }

    "show application information, pending verification status should have link to resend email" in new Setup {
      val applicationPendingVerification = application.pendingVerification

      val result = applicationView.render(
        DefaultApplicationViewModel.withApplication(applicationPendingVerification),
        strideUserRequest,
        Flash.emptyCookie
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "a", "Resend verify email") shouldBe true
    }

    "show API subscriptions" in new SubscriptionsSetup {
      val result = applicationView.render(
        DefaultApplicationViewModel.withSubscriptions(subscriptionsViewData),
        strideUserRequest,
        Flash.emptyCookie
      )

      result.contentType should include("text/html")
      result.body.contains("API subscriptions") shouldBe true
      result.body.contains("My API Name") shouldBe true
      result.body.contains(s"${VersionOne.value} (Stable)") shouldBe true
      result.body.contains(s"${VersionTwo.value} (Beta)") shouldBe true
      result.body.contains("My Other API Name") shouldBe true
      result.body.contains(s"${VersionOne.value} (Stable)") shouldBe true
    }

    "show subscriptions that have subscription fields configurartion" in new SubscriptionsSetup {
      val result = applicationView.render(
        DefaultApplicationViewModel.withSubscriptionsThatHaveFieldDefns(subscriptionsViewData),
        strideUserRequest,
        Flash.emptyCookie
      )
      result.contentType should include("text/html")
      result.body.contains("Subscription configuration") shouldBe true
      result.body.contains("My API Name") shouldBe true
      result.body.contains(s"${VersionOne.value} (Stable)") shouldBe true
      result.body.contains(s"${VersionTwo.value} (Beta)") shouldBe true
      result.body.contains("My Other API Name") shouldBe true
      result.body.contains(s"${VersionOne.value} (Stable)") shouldBe true
    }

    "hide subscriptions configuration" in new Setup {
      val subscriptions = List.empty

      val result = applicationView.render(
        DefaultApplicationViewModel.withSubscriptions(subscriptions),
        strideUserRequest,
        Flash.emptyCookie
      )

      result.contentType should include("text/html")
      result.body.contains("Subscription configuration") shouldBe false
    }

    "show button for application events" in new Setup {
      val subscriptions = List.empty

      val result = applicationView.render(
        DefaultApplicationViewModel.withSubscriptions(subscriptions),
        strideUserRequest,
        Flash.emptyCookie
      )

      result.contentType should include("text/html")
      result.body.contains("Check application changes") shouldBe true
    }

    "show manage IP allowlist link when user is at least a superuser" in new Setup {
      val result = applicationView.render(
        DefaultApplicationViewModel,
        superUserRequest,
        Flash.emptyCookie
      )

      val document = Jsoup.parse(result.body)

      elementExistsById(document, "manage-ip-allowlist") shouldBe true
      elementExistsById(document, "view-ip-allowlist") shouldBe false
    }

    "not show IP allowlist links for normal users when the IP allowlist is not active" in new Setup {
      val result = applicationView.render(
        DefaultApplicationViewModel,
        strideUserRequest,
        Flash.emptyCookie
      )

      val document = Jsoup.parse(result.body)

      elementExistsById(document, "view-ip-allowlist") shouldBe false
      elementExistsById(document, "manage-ip-allowlist") shouldBe false
    }

    "show view IP allowlist link for normal users when the IP allowlist is active" in new Setup {
      val x      = DefaultApplicationViewModel.withApplication(application.withIpAllowlist(IpAllowlist(allowlist = Set("1.1.1.1/24"))))
      val result = applicationView.render(
        x,
        strideUserRequest,
        Flash.emptyCookie
      )

      val document = Jsoup.parse(result.body)

      elementExistsById(document, "view-ip-allowlist") shouldBe true
      elementExistsById(document, "manage-ip-allowlist") shouldBe false
    }
  }

  private def formatTermsOfUseAgreedDateTime(termsOfUseAgreement: TermsOfUseAgreement) = {
    DateTimeFormatter.ofPattern("dd MMMM yyyy").format(termsOfUseAgreement.timeStamp)
  }
}
