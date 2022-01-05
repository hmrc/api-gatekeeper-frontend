/*
 * Copyright 2022 HM Revenue & Customs
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

import builder.SubscriptionsBuilder
import uk.gov.hmrc.modules.stride.domain.models.LoggedInUser
import org.joda.time.format.DateTimeFormat
import org.jsoup.Jsoup
import play.api.mvc.Flash
import play.api.test.FakeRequest
import utils.ViewHelpers._
import views.CommonViewSpec
import views.html.applications.ApplicationView
import model.view.ApplicationViewModel
import model.applications.NewApplication
import builder.ApplicationBuilder
import builder.ApiBuilder
import model.ApiStatus._
import org.joda.time.DateTime
import java.time.Period
import model._

class ApplicationViewSpec extends CommonViewSpec with SubscriptionsBuilder with ApiBuilder with ApplicationBuilder {
  trait Setup {
    implicit val request = FakeRequest()
    val applicationView = app.injector.instanceOf[ApplicationView]

    val developers = List[RegisteredUser] {
      new RegisteredUser("joe.bloggs@example.co.uk", UserId.random, "joe", "bloggs", false)
    }

    val clientId = ClientId("clientid")
    val grantLength: Period = Period.ofDays(547)

    val application = NewApplication(
      id = ApplicationId.random,
      clientId = clientId,
      gatewayId = "gateway",
      name = "AnApplicationName",
      createdOn = DateTime.now(),
      lastAccess = DateTime.now(),
      lastAccessTokenUsage = None,
      deployedTo = Environment.PRODUCTION,
      description = None,
      collaborators = Set.empty,
      access = Standard(),
      state = ApplicationState(),
      rateLimitTier = RateLimitTier.BRONZE,
      blocked = false,
      checkInformation = None,
      ipAllowlist = IpAllowlist(),
      grantLength
    )

    val DefaultApplicationViewModel = ApplicationViewModel(
      developers = developers,
      application = application,
      subscriptions = List.empty,
      subscriptionsThatHaveFieldDefns = List.empty,
      stateHistory = List.empty,
      isAtLeastSuperUser = false,
      isAdmin = false,
      hasSubmissions = false,
      gatekeeperApprovalsUrl = "http://localhost:1234/api-gatekeeper-approvals-frontend/applications"
    )
  }

  trait SubscriptionsSetup extends Setup {
      val subscriptionsViewData: List[(String, List[(ApiVersion, ApiStatus)])] = List(
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
      val result = applicationView.render(DefaultApplicationViewModel, request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByAttr(document, "div", "data-terms") shouldBe true
      elementIdentifiedByAttrContainsText(document, "div", "data-terms", "Not agreed") shouldBe true
    }

    "show application with check information but no terms of use agreed" in new Setup {

      val checkInformation = CheckInformation()
      val applicationWithCheckInformationButNoTerms = application.withCheckInformation(checkInformation)

      val result = applicationView.render(
        DefaultApplicationViewModel.withApplication(applicationWithCheckInformationButNoTerms),
        request,
        LoggedInUser(None),
        Flash.emptyCookie,
        messagesProvider
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByAttr(document, "div", "data-terms") shouldBe true
      elementIdentifiedByAttrContainsText(document, "div", "data-terms", "Not agreed") shouldBe true
    }

    "show application with check information and terms of use agreed" in new Setup {
      val termsOfUseVersion = "1.0"
      val termsOfUseAgreement = TermsOfUseAgreement("test", DateTime.now(), termsOfUseVersion)
      val checkInformation = CheckInformation(termsOfUseAgreements = List(termsOfUseAgreement))
      val applicationWithTermsOfUse = application.withCheckInformation(checkInformation)

      val result = applicationView.render(
        DefaultApplicationViewModel.withApplication(applicationWithTermsOfUse),
        request,
        LoggedInUser(None),
        Flash.emptyCookie,
        messagesProvider
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByAttr(document, "div", "data-terms") shouldBe true
      elementIdentifiedByAttrContainsText(document, "div", "data-terms", "Not agreed") shouldBe false
      val agreedText =
        s"v${termsOfUseAgreement.version} agreed by ${termsOfUseAgreement.emailAddress} on ${formatTermsOfUseAgreedDateTime(termsOfUseAgreement)}"
      elementIdentifiedByAttrContainsText(document, "div", "data-terms", agreedText) shouldBe true
    }

    "show application with check information and multiple terms of use agreed" in new Setup {
      val oldVersion = "1.0"
      val oldTOUAgreement = TermsOfUseAgreement("test", DateTime.now().minusDays(1), oldVersion)
      val newVersion = "1.1"
      val newTOUAgreement = TermsOfUseAgreement("test", DateTime.now(), newVersion)
      val checkInformation = CheckInformation(termsOfUseAgreements = List(oldTOUAgreement, newTOUAgreement))
      val applicationWithTermsOfUse = application.copy(checkInformation = Some(checkInformation))

      val result = applicationView.render(
        DefaultApplicationViewModel.withApplication(applicationWithTermsOfUse),
        request,
        LoggedInUser(None),
        Flash.emptyCookie,
        messagesProvider
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByAttr(document, "div", "data-terms") shouldBe true
      elementIdentifiedByAttrContainsText(document, "div", "data-terms", "Not agreed") shouldBe false
      val agreedText = s"v${newTOUAgreement.version} agreed by ${newTOUAgreement.emailAddress} on ${formatTermsOfUseAgreedDateTime(newTOUAgreement)}"
      elementIdentifiedByAttrContainsText(document, "div", "data-terms", agreedText) shouldBe true
      result.body.contains(s"v$oldTOUAgreement.version") shouldBe false
      result.body.contains(DateTimeFormat.longDate.print(oldTOUAgreement.timeStamp)) shouldBe false
    }

    "show application information, including status information" in new Setup {
      val result = applicationView.render(
        DefaultApplicationViewModel,
        request,
        LoggedInUser(None),
        Flash.emptyCookie,
        messagesProvider
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByAttr(document, "div", "data-status") shouldBe true
      elementExistsByAttr(document, "div", "data-status-info") shouldBe true
      elementIdentifiedByAttrContainsText(document, "div", "data-status", "Created") shouldBe true
      val checkingText = "A production application that its admin has created but not submitted for checking"
      elementIdentifiedByAttrContainsText(document, "div", "data-status-info", checkingText) shouldBe true
      elementExistsById(document, "review") shouldBe false
    }

    "show application information, including link to check application" in new Setup {
      val applicationPendingCheck = application.pendingGKApproval

      val result = applicationView.render(
        DefaultApplicationViewModel.withApplication(applicationPendingCheck),
        request,
        LoggedInUser(None),
        Flash.emptyCookie,
        messagesProvider
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByAttr(document, "div", "data-status") shouldBe true
      elementExistsByAttr(document, "div", "data-status-info") shouldBe true
      elementIdentifiedByAttrContainsText(document, "div", "data-status", "Pending gatekeeper check") shouldBe true
      val checkingText = "A production application that one of its admins has submitted for checking"
      elementIdentifiedByAttrContainsText(document, "div", "data-status-info", checkingText) shouldBe true
      elementIdentifiedByIdContainsText(document, "a", "review", "Check application") shouldBe true
    }

    "show application information, including superuser specific actions, when logged in as superuser" in new Setup {
      val result = applicationView.render(
        DefaultApplicationViewModel.asSuperUser,
        request,
        LoggedInUser(None),
        Flash.emptyCookie,
        messagesProvider
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")

      elementExistsByText(document, "a", "Delete application") shouldBe true
      elementExistsById(document, "manage-subscriptions") shouldBe true

    }

    "show application information, excluding superuser specific actions, when logged in as non superuser" in new Setup {
      val result = applicationView.render(
        DefaultApplicationViewModel,
        request,
        LoggedInUser(None),
        Flash.emptyCookie,
        messagesProvider
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "a", "Delete application") shouldBe false
      elementExistsById(document, "manage-subscriptions") shouldBe false

    }

    "show 'Manage' rate limit link when logged in as admin" in new Setup {
      val result = applicationView.render(
        DefaultApplicationViewModel.asAdmin,
        request,
        LoggedInUser(None),
        Flash.emptyCookie,
        messagesProvider
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")

      elementExistsById(document, "manage-rate-limit") shouldBe true

    }

    "not show 'Manage' rate limit link when logged in as non admin" in new Setup {
      val result = applicationView.render(
        DefaultApplicationViewModel.asSuperUser,
        request,
        LoggedInUser(None),
        Flash.emptyCookie,
        messagesProvider
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsById(document, "manage-rate-limit") shouldBe false

    }

    "show 'Block Application' button when logged in as admin" in new Setup {
      val activeApplication = application.inProduction

      val result = applicationView.render(
        DefaultApplicationViewModel.withApplication(activeApplication).asSuperUser.asAdmin,
        request,
        LoggedInUser(None),
        Flash.emptyCookie,
        messagesProvider
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsById(document, "block-application") shouldBe true

    }

    "hide 'Block Application' button when logged in as non-admin" in new Setup {
      val activeApplication = application.inProduction

      val result = applicationView.render(
        DefaultApplicationViewModel.withApplication(activeApplication).asSuperUser,
        request,
        LoggedInUser(None),
        Flash.emptyCookie,
        messagesProvider
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsById(document, "block-application") shouldBe false

    }

    "show application information and click on associated developer" in new Setup {
      val user = RegisteredUser("sample@example.com", UserId.random, "joe", "bloggs", true)
      val result = applicationView.render(
        DefaultApplicationViewModel.withDeveloper(user),
        request,
        LoggedInUser(None),
        Flash.emptyCookie,
        messagesProvider
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "a", "sample@example.com") shouldBe true
    }

    "show application information, pending verification status should have link to resend email" in new Setup {
      val applicationPendingVerification = application.pendingVerification

      val result = applicationView.render(
        DefaultApplicationViewModel.withApplication(applicationPendingVerification),
        request,
        LoggedInUser(None),
        Flash.emptyCookie,
        messagesProvider
      )

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "a", "Resend verify email") shouldBe true
    }

    "show API subscriptions" in new SubscriptionsSetup {
      val result = applicationView.render(
        DefaultApplicationViewModel.withSubscriptions(subscriptionsViewData),
        request,
        LoggedInUser(None),
        Flash.emptyCookie,
        messagesProvider
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
        request,
        LoggedInUser(None),
        Flash.emptyCookie,
        messagesProvider
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
        request,
        LoggedInUser(None),
        Flash.emptyCookie,
        messagesProvider
      )

      result.contentType should include("text/html")
      result.body.contains("Subscription configuration") shouldBe false
    }

    "show manage IP allowlist link when user is at least a superuser" in new Setup {
      val result = applicationView.render(
        DefaultApplicationViewModel.asSuperUser,
        request,
        LoggedInUser(None),
        Flash.emptyCookie,
        messagesProvider
      )

      val document = Jsoup.parse(result.body)

      elementExistsById(document, "manage-ip-allowlist") shouldBe true
      elementExistsById(document, "view-ip-allowlist") shouldBe false
    }

    "not show IP allowlist links for normal users when the IP allowlist is not active" in new Setup {
      val result = applicationView.render(
        DefaultApplicationViewModel,
        request,
        LoggedInUser(None),
        Flash.emptyCookie,
        messagesProvider
      )

      val document = Jsoup.parse(result.body)

      elementExistsById(document, "view-ip-allowlist") shouldBe false
      elementExistsById(document, "manage-ip-allowlist") shouldBe false
    }

    "show view IP allowlist link for normal users when the IP allowlist is active" in new Setup {
      val x = DefaultApplicationViewModel.withApplication(application.withIpAllowlist(IpAllowlist(allowlist = Set("1.1.1.1/24"))))
      val result = applicationView.render(
        x,
        request,
        LoggedInUser(None),
        Flash.emptyCookie,
        messagesProvider
      )

      val document = Jsoup.parse(result.body)

      elementExistsById(document, "view-ip-allowlist") shouldBe true
      elementExistsById(document, "manage-ip-allowlist") shouldBe false
    }
  }

  private def formatTermsOfUseAgreedDateTime(termsOfUseAgreement: TermsOfUseAgreement) = {
    DateTimeFormat.forPattern("dd MMMM yyyy").print(termsOfUseAgreement.timeStamp)
  }
}
