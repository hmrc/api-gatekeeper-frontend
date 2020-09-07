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

import builder.SubscriptionsBuilder
import model.{LoggedInUser, _}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.jsoup.Jsoup
import play.api.mvc.Flash
import play.api.test.FakeRequest
import utils.ViewHelpers._
import views.CommonViewSpec
import views.html.applications.ApplicationView

class ApplicationViewSpec extends CommonViewSpec with SubscriptionsBuilder {
  trait Setup {
    implicit val request = FakeRequest()
    val applicationView = app.injector.instanceOf[ApplicationView]

    val developers = List[User] {
      new User("joe.bloggs@example.co.uk", "joe", "bloggs", None, None, false)
    }
  }

  "application view" must {
    val application =
      ApplicationResponse(
        ApplicationId.random,
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

    "show application with no check information" in new Setup {
      val result = applicationView.render(developers, applicationWithHistory, Seq.empty, Seq.empty,
        isAtLeastSuperUser = false, isAdmin = false, None, request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByAttr(document, "div", "data-terms") mustBe true
      elementIdentifiedByAttrContainsText(document, "div", "data-terms", "Not agreed") mustBe true
    }

    "show application with check information but no terms of use agreed" in new Setup {
      val checkInformation = CheckInformation()
      val applicationWithCheckInformationButNoTerms = ApplicationResponse(
        ApplicationId.random,
        "clientid",
        "gatewayId",
        "name",
        "PRODUCTION",
        None,
        Set.empty,
        DateTime.now(),
        DateTime.now(),
        Standard(),
        ApplicationState(),
        checkInformation = Option(checkInformation)
      )

      val result = applicationView.render(
        developers, applicationWithHistory.copy(application = applicationWithCheckInformationButNoTerms), Seq.empty, Seq.empty,
        isAtLeastSuperUser = false, isAdmin = false, None, request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByAttr(document, "div", "data-terms") mustBe true
      elementIdentifiedByAttrContainsText(document, "div", "data-terms", "Not agreed") mustBe true
    }

    "show application with check information and terms of use agreed" in new Setup {
      val termsOfUseVersion = "1.0"
      val termsOfUseAgreement = TermsOfUseAgreement("test", DateTime.now(), termsOfUseVersion)
      val checkInformation = CheckInformation(termsOfUseAgreements = Seq(termsOfUseAgreement))


      val applicationWithTermsOfUse = ApplicationResponse(
        ApplicationId.random,
        "clientid",
        "gatewayId",
        "name",
        "PRODUCTION",
        None,
        Set.empty,
        DateTime.now(),
        DateTime.now(),
        Standard(),
        ApplicationState(),
        checkInformation = Option(checkInformation)
      )

      val result = applicationView.render(
        developers, applicationWithHistory.copy(application = applicationWithTermsOfUse), Seq.empty, Seq.empty,
        isAtLeastSuperUser = false, isAdmin = false, Some(termsOfUseAgreement), request, LoggedInUser(None),
        Flash.emptyCookie, messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByAttr(document, "div", "data-terms") mustBe true
      elementIdentifiedByAttrContainsText(document, "div", "data-terms", "Not agreed") mustBe false
      val agreedText =
        s"v${termsOfUseAgreement.version} agreed by ${termsOfUseAgreement.emailAddress} on ${formatTermsOfUseAgreedDateTime(termsOfUseAgreement)}"
      elementIdentifiedByAttrContainsText(document, "div", "data-terms", agreedText) mustBe true
    }

    "show application with check information and multiple terms of use agreed" in new Setup {
      val oldVersion = "1.0"
      val oldTOUAgreement = TermsOfUseAgreement("test", DateTime.now().minusDays(1), oldVersion)
      val newVersion = "1.1"
      val newTOUAgreement = TermsOfUseAgreement("test", DateTime.now(), newVersion)
      val checkInformation = CheckInformation(termsOfUseAgreements = Seq(oldTOUAgreement, newTOUAgreement))

      val applicationWithTermsOfUse = ApplicationResponse(

        ApplicationId.random,
        "clientid",
        "gatewayId",
        "name",
        "PRODUCTION",
        None,
        Set.empty,
        DateTime.now(),
        DateTime.now(),
        Standard(),
        ApplicationState(),
        checkInformation = Option(checkInformation)
      )
      val result = applicationView.render(
        developers, applicationWithHistory.copy(application = applicationWithTermsOfUse), Seq.empty, Seq.empty,
        isAtLeastSuperUser = false, isAdmin = false, Some(newTOUAgreement), request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByAttr(document, "div", "data-terms") mustBe true
      elementIdentifiedByAttrContainsText(document, "div", "data-terms", "Not agreed") mustBe false
      val agreedText = s"v${newTOUAgreement.version} agreed by ${newTOUAgreement.emailAddress} on ${formatTermsOfUseAgreedDateTime(newTOUAgreement)}"
      elementIdentifiedByAttrContainsText(document, "div", "data-terms", agreedText) mustBe true
      result.body.contains(s"v$oldTOUAgreement.version") mustBe false
      result.body.contains(DateTimeFormat.longDate.print(oldTOUAgreement.timeStamp)) mustBe false
    }

    "show application information, including status information" in new Setup {

      val result = applicationView.render(developers, applicationWithHistory, Seq.empty, Seq.empty,
        isAtLeastSuperUser = false, isAdmin = false, None, request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByAttr(document, "div", "data-status") mustBe true
      elementExistsByAttr(document, "div", "data-status-info") mustBe true
      elementIdentifiedByAttrContainsText(document, "div", "data-status", "Created") mustBe true
      val checkingText = "A production application that its admin has created but not submitted for checking"
      elementIdentifiedByAttrContainsText(document, "div", "data-status-info", checkingText) mustBe true
      elementExistsById(document, "review") mustBe false
    }

    "show application information, including link to check application" in new Setup {

      val applicationPendingCheck = application.copy(state = ApplicationState(State.PENDING_GATEKEEPER_APPROVAL))

      val result = applicationView.render(developers, applicationWithHistory.copy(application = applicationPendingCheck), Seq.empty, Seq.empty,
        isAtLeastSuperUser = false, isAdmin = false, None, request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByAttr(document, "div", "data-status") mustBe true
      elementExistsByAttr(document, "div", "data-status-info") mustBe true
      elementIdentifiedByAttrContainsText(document, "div", "data-status", "Pending gatekeeper check") mustBe true
      val checkingText = "A production application that one of its admins has submitted for checking"
      elementIdentifiedByAttrContainsText(document, "div", "data-status-info", checkingText) mustBe true
      elementIdentifiedByIdContainsText(document, "a", "review", "Check application") mustBe true
    }

    "show application information, including superuser specific actions, when logged in as superuser" in new Setup {

      val result = applicationView.render(developers, applicationWithHistory, Seq.empty, Seq.empty,
        isAtLeastSuperUser = true, isAdmin = false, None, request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")

      elementExistsByText(document, "a", "Delete application") mustBe true
      elementExistsById(document, "manage-subscriptions") mustBe true

    }

    "show application information, excluding superuser specific actions, when logged in as non superuser" in new Setup {
      val result = applicationView.render(developers, applicationWithHistory, Seq.empty, Seq.empty,
        isAtLeastSuperUser = false, isAdmin = false, None, request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByText(document, "a", "Delete application") mustBe false
      elementExistsById(document, "manage-subscriptions") mustBe false

    }

    "show 'Manage' rate limit link when logged in as admin" in new Setup {

      val result = applicationView.render(developers, applicationWithHistory, Seq.empty, Seq.empty,
        isAtLeastSuperUser = true, isAdmin = true, None, request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")

      elementExistsById(document, "manage-rate-limit") mustBe true

    }

    "not show 'Manage' rate limit link when logged in as non admin" in new Setup {
      val result = applicationView.render(developers, applicationWithHistory, Seq.empty, Seq.empty,
        isAtLeastSuperUser = true, isAdmin = false, None, request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsById(document, "manage-rate-limit") mustBe false

    }

    "show 'Block Application' button when logged in as admin" in new Setup {

      val activeApplication = application.copy(state = ApplicationState(State.PRODUCTION))

      val result = applicationView.render(developers, applicationWithHistory.copy(application = activeApplication), Seq.empty, Seq.empty,
        isAtLeastSuperUser = true, isAdmin = true, None, request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsById(document, "block-application") mustBe true

    }

    "hide 'Block Application' button when logged in as non-admin" in new Setup {

      val activeApplication = application.copy(state = ApplicationState(State.PRODUCTION))

      val result = applicationView.render(developers, applicationWithHistory.copy(application = activeApplication), Seq.empty, Seq.empty,
        isAtLeastSuperUser = true, isAdmin = false, None, request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsById(document, "block-application") mustBe false

    }

    "show application information and click on associated developer" in new Setup {
      val result = applicationView.render(developers, applicationWithHistory, Seq.empty, Seq.empty,
        isAtLeastSuperUser = false, isAdmin = false, None, request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByText(document, "a", "sample@example.com") mustBe true
    }

    "show application information, pending verification status should have link to resend email" in new Setup {
      val applicationPendingVerification = application.copy(state = ApplicationState(State.PENDING_REQUESTER_VERIFICATION))

      val result = applicationView.render(developers, applicationWithHistory.copy(application = applicationPendingVerification),
        Seq.empty, Seq.empty, isAtLeastSuperUser = false, isAdmin = false, None, request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByText(document, "a", "Resend verify email") mustBe true
    }

    "show API subscriptions" in new Setup {
      val versionWithSubscriptionFields1 = buildVersionWithSubscriptionFields("1.0", true, application.id)
      val versionWithSubscriptionFields2 = buildVersionWithSubscriptionFields("2.0", true, application.id)

      val subscriptions = Seq(buildSubscription("My API Name", versions = Seq(versionWithSubscriptionFields1, versionWithSubscriptionFields2)))

      val result = applicationView.render(developers, applicationWithHistory,
        subscriptions, Seq.empty, isAtLeastSuperUser = false, isAdmin = false, None, request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      result.contentType must include("text/html")
      result.body.contains("API subscriptions") mustBe true
      result.body.contains("My API Name") mustBe true
      result.body.contains(s"${versionWithSubscriptionFields1.version.version} (Stable)") mustBe true
      result.body.contains(s"${versionWithSubscriptionFields2.version.version} (Stable)") mustBe true
    }

     "show subscriptions that have subscription fields configurartion" in new Setup {
      val versionWithSubscriptionFields1 = buildVersionWithSubscriptionFields("1.0", true, application.id)
      val versionWithSubscriptionFields2 = buildVersionWithSubscriptionFields("2.0", true, application.id)

      val subscriptions = Seq(buildSubscription("My API Name", versions = Seq(versionWithSubscriptionFields1, versionWithSubscriptionFields2)))

      val result = applicationView.render(developers, applicationWithHistory,
        Seq.empty, subscriptions, isAtLeastSuperUser = false, isAdmin = false, None, request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      result.contentType must include("text/html")
      result.body.contains("Subscription configuration") mustBe true
      result.body.contains("My API Name") mustBe true
      result.body.contains(s"${versionWithSubscriptionFields1.version.version} (Stable)") mustBe true
      result.body.contains(s"${versionWithSubscriptionFields2.version.version} (Stable)") mustBe true
    }

    "hide subscriptions configurartion" in new Setup {
      val subscriptions = Seq.empty

      val result = applicationView.render(developers, applicationWithHistory,
        Seq.empty, subscriptions, isAtLeastSuperUser = false, isAdmin = false, None, request, LoggedInUser(None), Flash.emptyCookie, messagesProvider)

      result.contentType must include("text/html")
      result.body.contains("Subscription configuration") mustBe false
    }
  }

  private def formatTermsOfUseAgreedDateTime(termsOfUseAgreement: TermsOfUseAgreement) = {
    DateTimeFormat.forPattern("dd MMMM yyyy").print(termsOfUseAgreement.timeStamp)
  }
}
