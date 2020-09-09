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

package controllers

import akka.stream.Materializer
import model.EmailOptionChoice.{API_SUBSCRIPTION, EMAIL_ALL_USERS, EMAIL_PREFERENCES, EmailOptionChoice}
import model.EmailPreferencesChoice.{EmailPreferencesChoice, TAX_REGIME, TOPIC}
import model.Environment.Environment
import model.TopicOptionChoice._
import model._
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.TokenProvider
import services.DeveloperService
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import utils.FakeRequestCSRFSupport._
import utils.{TitleChecker, WithCSRFAddToken}
import views.html.emails._
import views.html.{ErrorTemplate, ForbiddenView}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailsControllerSpec extends ControllerBaseSpec with WithCSRFAddToken with TitleChecker {

  implicit val materializer: Materializer = app.materializer

  private lazy val errorTemplateView = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView = app.injector.instanceOf[ForbiddenView]
  private lazy val sendEmailChoiceView = app.injector.instanceOf[SendEmailChoiceView]
  private lazy val emailInformationView = app.injector.instanceOf[EmailInformationView]
  private lazy val emailAllUsersView = app.injector.instanceOf[EmailAllUsersView]
  private lazy val emailApiSubscriptionsView = app.injector.instanceOf[EmailApiSubscriptionsView]
  private lazy val emailPreferencesChoiceView = app.injector.instanceOf[EmailPreferencesChoiceView]
  private lazy val emailPreferencesTopicView = app.injector.instanceOf[EmailPreferencesTopicView]
  private lazy val emailPreferencesAPICategoryView = app.injector.instanceOf[EmailPreferencesAPICategoryView]

  val apiVersion1 = ApiVersion("1")
  val apiVersion3 = ApiVersion("3")

  private lazy val emailPreferencesSpecificApiView = app.injector.instanceOf[EmailPreferencesSpecificApiView]
  private lazy val emailPreferencesSelectApiView = app.injector.instanceOf[EmailPreferencesSelectApiView]
  running(app) {

    trait Setup extends ControllerSetupBase {

      val csrfToken: (String, String) = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken
      override val aLoggedInRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(csrfToken, authToken, userToken).withCSRFToken
      override val aSuperUserLoggedInRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest().withSession(csrfToken, authToken, superUserToken).withCSRFToken
      override val anAdminLoggedInRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(csrfToken, authToken, adminToken).withCSRFToken

      def selectedEmailOptionRequest(selectedOption: EmailOptionChoice): FakeRequest[AnyContentAsFormUrlEncoded] =
        FakeRequest()
          .withSession(csrfToken, authToken, userToken)
          .withCSRFToken.withMethod("POST")
          .withFormUrlEncodedBody("sendEmailChoice" -> selectedOption.toString)

      def selectedEmailPreferencesRequest(selectedOption: EmailPreferencesChoice): FakeRequest[AnyContentAsFormUrlEncoded] =
        FakeRequest()
          .withSession(csrfToken, authToken, userToken)
          .withCSRFToken.withMethod("POST")
          .withFormUrlEncodedBody("sendEmailPreferences" -> selectedOption.toString)

      def createGetRequest(path: String) = {
        FakeRequest("GET", path)
          .withSession(csrfToken, authToken, userToken)
          .withCSRFToken
      }

      val mockDeveloperService: DeveloperService = mock[DeveloperService]
      val verifiedUser1: User = User("user1@hmrc.com", "verifiedUserA", "1", Some(true))
      val verifiedUser2: User = User("user2@hmrc.com", "verifiedUserB", "2", Some(true))
      val verifiedUser3: User = User("user3@hmrc.com", "verifiedUserC", "3", Some(true))
      val unVerifiedUser1: User = User("user1@somecompany.com", "unVerifiedUserA", "1", Some(false))
      val users = Seq(verifiedUser1, verifiedUser2, verifiedUser3)

      val category1 = APICategory("EXAMPLE", "Example")
      val category2 = APICategory("VAT", "Vat")
      val category3 = APICategory("AGENTS", "Agents")

      def givenVerifiedDeveloper(): Unit = {
        val users = Seq(verifiedUser1, verifiedUser2)
        when(mockDeveloperService.fetchUsers(*)).thenReturn(Future.successful(users))
      }

      def given3VerifiedDevelopers1Unverified(): Unit = {
        val users = Seq(verifiedUser1, verifiedUser2, verifiedUser3, unVerifiedUser1)
        when(mockDeveloperService.fetchUsers(*)).thenReturn(Future.successful(users))
      }

      def given3VerifiedDevelopers1UnverifiedSearchDevelopers(): Unit = {
        when(mockDeveloperService.searchDevelopers(any[Developers2Filter])(*)).thenReturn(Future.successful(users))
      }

      def givenfetchDevelopersByEmailPreferences(users: Seq[User]) = {
        when(mockDeveloperService.fetchDevelopersByEmailPreferences(any[TopicOptionChoice], any[Option[String]])(any[HeaderCarrier])).thenReturn(Future.successful(users))
      }

      def givenfetchDevelopersByAPICategoryEmailPreferences(users: Seq[User]) = {
        when(mockDeveloperService.fetchDevelopersByAPICategoryEmailPreferences(any[TopicOptionChoice], any[String])(any[HeaderCarrier])).thenReturn(Future.successful(users))
      }

      def givenNoVerifiedDevelopers(): Unit = {
        val users = Seq(unVerifiedUser1)
        when(mockDeveloperService.fetchUsers(*)).thenReturn(Future.successful(users))
      }

      def givenApiDefinition2Apis() = {
        val api1 = APIDefinition("service1", "/", "serviceName", "serviceDesc", ApiContext("service1"), Seq(ApiVersionDefinition(apiVersion1, APIStatus.BETA)), None)
        val api2 = APIDefinition("service2", "/", "service2Name", "service2Desc", ApiContext("service2"), Seq(ApiVersionDefinition(apiVersion3, APIStatus.STABLE)), None)
        when(mockApiDefinitionService.fetchAllApiDefinitions(any[Option[Environment]])(*))
          .thenReturn(Future.successful(Seq(api1, api2)))
      }

      def givenApiDefinition3Categories() = {

        when(mockApiDefinitionService.apiCategories()(any[HeaderCarrier]))
          .thenReturn(Future.successful(List(category1, category2, category3)))
      }

      val underTest = new EmailsController(
        mockDeveloperService,
        mockApiDefinitionService,
        sendEmailChoiceView,
        emailInformationView,
        emailAllUsersView,
        emailApiSubscriptionsView,
        emailPreferencesChoiceView,
        emailPreferencesTopicView,
        emailPreferencesAPICategoryView,
        emailPreferencesSpecificApiView,
        emailPreferencesSelectApiView,
        mockApplicationService,
        forbiddenView,
        mockAuthConnector,
        mcc,
        errorTemplateView
      )

    }

    "email landing page" should {
      "on initial request with logged in user should display disabled options and checked email all options" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        val eventualResult: Future[Result] = underTest.landing()(aLoggedInRequest)

        status(eventualResult) shouldBe OK
        titleOf(eventualResult) shouldBe "Unit Test Title - Send emails to users based on"
        val responseBody: String = Helpers.contentAsString(eventualResult)
        responseBody should include("<h1 id=\"pageTitle\">Send emails to users based on</h1>")
        responseBody should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/applications\">Applications</a>")
        responseBody should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/developers2\">Developers</a>")

        responseBody should include(raw"""<input id="EMAIL_PREFERENCES" name="sendEmailChoice" aria-label="Email users based on their preferences" type="radio" value="EMAIL_PREFERENCES" checked>""".stripMargin)

        responseBody should include(raw"""<input id="API_SUBSCRIPTION" name="sendEmailChoice" aria-label="Email users mandatory information about APIs they subscribe to" type="radio" value="API_SUBSCRIPTION">""".stripMargin)

        responseBody should include(raw"""<div class="float-left-block">Or</div>""")

        responseBody should include(raw"""<input id="EMAIL_ALL_USERS" name="sendEmailChoice" aria-label="Email all users with a Developer Hub account" type="radio" value="EMAIL_ALL_USERS">""".stripMargin)

        verifyAuthConnectorCalledForUser

      }
    }

    "choose email option" should {

      "redirect to the all users information page when EMAIL_ALL_USERS option chosen" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()

        val result: Result = await(underTest.chooseEmailOption()(selectedEmailOptionRequest(EMAIL_ALL_USERS)))

        status(result) shouldBe SEE_OTHER
        result.header.headers.get("Location") shouldBe Some("/api-gatekeeper/emails/all-users/information")
      }

      "redirect to the API Subscriptions information page when API_SUBSCRIPTION option chosen" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()

        val result: Result = await(underTest.chooseEmailOption()(selectedEmailOptionRequest(API_SUBSCRIPTION)))

        status(result) shouldBe SEE_OTHER
        result.header.headers.get("Location") shouldBe Some("/api-gatekeeper/emails/api-subscription/information")
      }

      "redirect to the Email Preferences page when EMAIL_PREFERENCES option chosen" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()

        val result: Result = await(underTest.chooseEmailOption()(selectedEmailOptionRequest(EMAIL_PREFERENCES)))

        status(result) shouldBe SEE_OTHER
        result.header.headers.get("Location") shouldBe Some("/api-gatekeeper/emails/email-preferences")
      }
    }

    "choose email preferences" should {
      "redirect to Topic page when TOPIC option chosen" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()

        val result: Result = await(underTest.chooseEmailPreferences()(selectedEmailPreferencesRequest(TOPIC)))

        status(result) shouldBe SEE_OTHER
        result.header.headers.get("Location") shouldBe Some("/api-gatekeeper/emails/email-preferences/by-topic")
      }

      "redirect to API page when SPECIFIC_API option chosen" in new Setup {

      }

      "redirect to Tax Regime page when TAX_REGIME option chosen" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()

        val result: Result = await(underTest.chooseEmailPreferences()(selectedEmailPreferencesRequest(TAX_REGIME)))

        status(result) shouldBe SEE_OTHER
        result.header.headers.get("Location") shouldBe Some("/api-gatekeeper/emails/email-preferences/by-api-category")
      }
    }

    "email information page" should {
      "on request with 'all-users' in uri path should render correctly" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        val eventualResult: Future[Result] = underTest.showEmailInformation("all-users")(aLoggedInRequest)

        status(eventualResult) shouldBe OK
        titleOf(eventualResult) shouldBe "Unit Test Title - Check you can send your email"
        val responseBody: String = Helpers.contentAsString(eventualResult)
        responseBody should include("<h1 id=\"pageTitle\" class=\"heading-large\">Check you can email all users</h1>")
        responseBody should include("<li>important notices and service updates</li>")
        responseBody should include("<li>changes to any application they have</li>")
        responseBody should include("<li>making their application accessible</li>")
        verifyAuthConnectorCalledForUser

      }

      "on request with 'api-subscription' in uri path should render correctly" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        val eventualResult: Future[Result] = underTest.showEmailInformation("api-subscription")(aLoggedInRequest)

        status(eventualResult) shouldBe OK
        titleOf(eventualResult) shouldBe "Unit Test Title - Check you can send your email"
        val responseBody: String = Helpers.contentAsString(eventualResult)
        responseBody should include("<h1 id=\"pageTitle\" class=\"heading-large\">Check you can send your email</h1>")
        responseBody should include("<li>important notices and service updates</li>")
        responseBody should include("<li>changes to any application they have</li>")
        responseBody should include("<li>making their application accessible</li>")
        verifyAuthConnectorCalledForUser

      }

      "on request with invalid or empty path will return NOT FOUND" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        val result = intercept[NotFoundException] {
          await(underTest.showEmailInformation("")(aLoggedInRequest))
        }

        result.message shouldBe "Page Not Found"
        verifyAuthConnectorCalledForUser

      }
    }

    "email all Users page" should {
      "on request should render correctly when 3 verified users are retrieved from developer service " in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        given3VerifiedDevelopers1Unverified()
        val eventualResult: Future[Result] = underTest.emailAllUsersPage()(aLoggedInRequest)

        status(eventualResult) shouldBe OK
        titleOf(eventualResult) shouldBe "Unit Test Title - Emails all users"
        val responseBody: String = Helpers.contentAsString(eventualResult)

        responseBody should include("<div><h1 id=\"pageTitle\">Email all users</h1></div>")
        verifyUserTable(responseBody, users)
        verifyAuthConnectorCalledForUser

      }

      "on request should render correctly when 2 users are retrieved from the developer service" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenVerifiedDeveloper()
        val eventualResult: Future[Result] = underTest.emailAllUsersPage()(aLoggedInRequest)

        status(eventualResult) shouldBe OK
        titleOf(eventualResult) shouldBe "Unit Test Title - Emails all users"
        val responseBody: String = Helpers.contentAsString(eventualResult)

        responseBody should include("<div><h1 id=\"pageTitle\">Email all users</h1></div>")
        responseBody should include("<div>2 results</div>")

        responseBody should include("<th tabindex=\"0\" class=\"sorting_left-aligned\">Email</th>")
        responseBody should include("<th tabindex=\"0\" class=\"sorting_left-aligned\">First name</th>")
        responseBody should include("<th tabindex=\"0\" class=\"sorting_left-aligned\">Last name</th>")

        responseBody should include(raw"""<td id="dev-email-0" width="45%">${verifiedUser1.email}</td>""")
        responseBody should include(raw"""<td id="dev-fn-0">${verifiedUser1.firstName}</td>""")
        responseBody should include(raw"""<td id="dev-sn-0">${verifiedUser1.lastName}</td>""")

        responseBody should include(raw"""<td id="dev-email-1" width="45%">${verifiedUser2.email}</td>""")
        responseBody should include(raw"""<td id="dev-fn-1">${verifiedUser2.firstName}</td>""")
        responseBody should include(raw"""<td id="dev-sn-1">${verifiedUser2.lastName}</td>""")
        verifyAuthConnectorCalledForUser

      }

      "on request should render correctly when no verified users are retrieved from the developer service" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenNoVerifiedDevelopers()
        val eventualResult: Future[Result] = underTest.emailAllUsersPage()(aLoggedInRequest)

        status(eventualResult) shouldBe OK
        titleOf(eventualResult) shouldBe "Unit Test Title - Emails all users"
        val responseBody: String = Helpers.contentAsString(eventualResult)

        responseBody should include("<div><h1 id=\"pageTitle\">Email all users</h1></div>")
        responseBody should include("<div>0 results</div>")

        responseBody should not include "<th tabindex=\"0\" class=\"sorting_left-aligned\">Email</th>"
        responseBody should not include "<th tabindex=\"0\" class=\"sorting_left-aligned\">First name</th>"
        responseBody should not include "<th tabindex=\"0\" class=\"sorting_left-aligned\">Last name</th>"

        verifyAuthConnectorCalledForUser

      }
    }

    "email subscribers page" should {

      "render correctly (not display user table) when no filter provided" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenApiDefinition2Apis
        val eventualResult: Future[Result] = underTest.emailApiSubscribersPage()(aLoggedInRequest)
        status(eventualResult) shouldBe OK
        titleOf(eventualResult) shouldBe "Unit Test Title - Email all users subscribed to an API"

        val responseBody: String = Helpers.contentAsString(eventualResult)
        responseBody should include(" <div><h1 id=\"pageTitle\">Email all users subscribed to an API</h1></div>")
        responseBody should include(raw"""<form name="developers-filters" action="/api-gatekeeper/emails/api-subscribers" method="get">""")
        responseBody should include(raw"""<option value="">Select API</option>""")
        responseBody should include(raw"""<option  value="service1__1">serviceName (1) (Beta) </option>""")
        responseBody should include(raw"""<option  value="service2__3">service2Name (3) (Stable) </option>""")
        responseBody should include(raw"""<input id="filter" type="submit" value="Filter" name="main-submit" class="button text--center float--left flush--left"/>""")
        responseBody should not include raw"""<table id="developer-table" class="no-footer developer-list" width="100%">"""
      }

      "render correctly and display users when api filter provided" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenApiDefinition2Apis
        given3VerifiedDevelopers1UnverifiedSearchDevelopers()
        val eventualResult: Future[Result] = underTest.emailApiSubscribersPage(Some("service2__3"))(createGetRequest("/emails/api-subscribers?apiVersionFilter=service2__3"))
        status(eventualResult) shouldBe OK
        titleOf(eventualResult) shouldBe "Unit Test Title - Email all users subscribed to an API"

        val responseBody: String = Helpers.contentAsString(eventualResult)
        responseBody should include(" <div><h1 id=\"pageTitle\">Email all users subscribed to an API</h1></div>")
        responseBody should include(raw"""<form name="developers-filters" action="/api-gatekeeper/emails/api-subscribers" method="get">""")
        responseBody should include(raw"""<option value="">Select API</option>""")
        responseBody should include(raw"""<option  value="service1__1">serviceName (1) (Beta) </option>""")
        responseBody should include(raw"""<option selected value="service2__3">service2Name (3) (Stable) </option>""")
        responseBody should include(raw"""<input id="filter" type="submit" value="Filter Again" name="main-submit" class="button--link text--center float--left flush--left"/>""")
        responseBody should include(raw"""<table id="developer-table" class="no-footer developer-list" width="100%">""")

        verifyUserTable(responseBody, users)
      }

    }

    "email preferences specific api page" should {
      "render the view correctly when no filter selected" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()

        val request = createGetRequest("/emails/api-subscribers/email-preferences/by-specific-api")
        val eventualResult: Future[Result] = underTest.emailPreferencesTopic()(request)
        status(eventualResult) shouldBe OK

        verifyZeroInteractions(mockDeveloperService)
      }
    }

    "email preferences topic page" should {
      "render the view correctly when no filter selected" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()

        val request = createGetRequest("/emails/api-subscribers/email-preferences/by-topic")
        val eventualResult: Future[Result] = underTest.emailPreferencesTopic()(request)
        status(eventualResult) shouldBe OK

        verifyZeroInteractions(mockDeveloperService)
      }

      "render the view correctly when filter selected and no users returned" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()

        givenfetchDevelopersByEmailPreferences(Seq.empty)
        val request = createGetRequest("/emails/api-subscribers/email-preferences/topic?topicOptionChoice=TECHNICAL")
        val eventualResult: Result = await(underTest.emailPreferencesTopic(Some("TECHNICAL"))(request))
        status(eventualResult) shouldBe OK

        val responseBody = Helpers.contentAsString(eventualResult)

        verifyUserTable(responseBody, Seq.empty)

      }

      "render the view correctly when filter selected and users returned" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()

        givenfetchDevelopersByEmailPreferences(users)
        val request = createGetRequest("/emails/api-subscribers/email-preferences/topic?topicOptionChoice=TECHNICAL")
        val eventualResult: Future[Result] = underTest.emailPreferencesTopic(Some("TECHNICAL"))(request)
        status(eventualResult) shouldBe OK

        val responseBody = Helpers.contentAsString(eventualResult)

        verifyUserTable(responseBody, users)

      }


    }

    "Email preferences API Category page" should {
      "render the view correctly when no filters selected" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenApiDefinition3Categories()
       // givenfetchDevelopersByAPICategoryEmailPreferences(Seq.empty)

        val request = createGetRequest("/emails/email-preferences/by-api-category")
        val eventualResult: Future[Result] = underTest.emailPreferencesAPICategory()(request)
        status(eventualResult) shouldBe OK

        verifyZeroInteractions(mockDeveloperService)
      }


      "render the view correctly when topic filter `TECHNICAL` selected and no users returned" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenApiDefinition3Categories()
        givenfetchDevelopersByAPICategoryEmailPreferences(Seq.empty)
        val request = createGetRequest(s"/emails/email-preferences/by-api-category?topicChosen=TECHNICAL&categoryChosen=${category1.category}")
        val eventualResult: Future[Result] = underTest.emailPreferencesAPICategory(Some("TECHNICAL"), Some(category1.category))(request)
        status(eventualResult) shouldBe OK

        val responseBody = Helpers.contentAsString(eventualResult)

        verifyUserTable(responseBody, Seq.empty)
      }

      "render the view correctly when Topic filter TECHNICAL selected and users returned" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenApiDefinition3Categories()
        givenfetchDevelopersByAPICategoryEmailPreferences(users)
        val request = createGetRequest(s"/emails/email-preferences/by-api-category?topicChosen=TECHNICAL&categoryChosen=${category1.category}")
        val eventualResult: Future[Result] = underTest.emailPreferencesAPICategory(Some("TECHNICAL"), Some(category1.category))(request)
        status(eventualResult) shouldBe OK

        val responseBody = Helpers.contentAsString(eventualResult)

        verifyUserTable(responseBody, users)


      }

    }

  }

  def verifyUserTable(responseBody: String, users: Seq[User], showZeroUsers: Boolean = false) {
    if (!users.isEmpty) {
      responseBody should include(s"<div>${users.size} results</div>")

      responseBody should include("<th tabindex=\"0\" class=\"sorting_left-aligned\">Email</th>")
      responseBody should include("<th tabindex=\"0\" class=\"sorting_left-aligned\">First name</th>")
      responseBody should include("<th tabindex=\"0\" class=\"sorting_left-aligned\">Last name</th>")

      for ((user, index) <- users.zipWithIndex) {
        responseBody should include(raw"""<td id="dev-email-${index}" width="45%">${user.email}</td>""")
        responseBody should include(raw"""<td id="dev-fn-${index}">${user.firstName}</td>""")
        responseBody should include(raw"""<td id="dev-sn-${index}">${user.lastName}</td>""")
      }
    } else {
      if (showZeroUsers) {
        responseBody should include("<div>0 results</div>")
      }
    }
  }

}
