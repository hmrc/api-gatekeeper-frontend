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
import model.EmailPreferencesChoice.{EmailPreferencesChoice, SPECIFIC_API, TAX_REGIME, TOPIC}
import model.Environment.Environment
import model.TopicOptionChoice.TopicOptionChoice
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
  private lazy val mockSendEmailChoiceView = mock[EmailLandingView]
  private lazy val mockEmailInformationView = mock[EmailInformationView]
  private lazy val mockEmailAllUsersView = mock[EmailAllUsersView]
  private lazy val mockEmailApiSubscriptionsView = mock[EmailApiSubscriptionsView]
  private lazy val emailPreferencesChoiceView = app.injector.instanceOf[EmailPreferencesChoiceView]
  private lazy val emailPreferencesTopicView = app.injector.instanceOf[EmailPreferencesTopicView]
  private lazy val emailPreferencesAPICategoryView = app.injector.instanceOf[EmailPreferencesAPICategoryView]
  private lazy val mockEmailPreferencesSpecificApiView = mock[EmailPreferencesSpecificApiView]
  private lazy val mockEmailPreferencesSelectApiView = mock[EmailPreferencesSelectApiView]
  running(app) {

    trait Setup extends ControllerSetupBase {
      when(mockSendEmailChoiceView.apply()(*,*,*)).thenReturn(play.twirl.api.HtmlFormat.empty)
      when(mockEmailInformationView.apply(*)(*,*,*)).thenReturn(play.twirl.api.HtmlFormat.empty)
      when(mockEmailAllUsersView.apply(*, *)(*, *, *)).thenReturn(play.twirl.api.HtmlFormat.empty)
      when(mockEmailPreferencesSpecificApiView.apply(*, *, *, *)(*, *, *)).thenReturn(play.twirl.api.HtmlFormat.empty)
      when(mockEmailPreferencesSelectApiView.apply(*, *)(*, *, *)).thenReturn(play.twirl.api.HtmlFormat.empty)
      when(mockEmailApiSubscriptionsView.apply(*, *, *, *)(*, *, *)).thenReturn(play.twirl.api.HtmlFormat.empty)

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
      val verifiedUser1: User = User(UserId.random, "user1@hmrc.com", "verifiedUserA", "1", Some(true))
      val verifiedUser2: User = User(UserId.random, "user2@hmrc.com", "verifiedUserB", "2", Some(true))
      val verifiedUser3: User = User(UserId.random, "user3@hmrc.com", "verifiedUserC", "3", Some(true))
      val unVerifiedUser1: User = User(UserId.random, "user1@somecompany.com", "unVerifiedUserA", "1", Some(false))
      val users = Seq(verifiedUser1, verifiedUser2, verifiedUser3)
      val users3Verified1Unverified = Seq(verifiedUser1, verifiedUser2, verifiedUser3, unVerifiedUser1)
      val verified2Users = Seq(verifiedUser1, verifiedUser2)
      val category1 = APICategoryDetails("EXAMPLE", "Example")
      val category2 = APICategoryDetails("VAT", "Vat")
      val category3 = APICategoryDetails("AGENTS", "Agents")
 

      def givenVerifiedDeveloper(): Unit = {
    
        when(mockDeveloperService.fetchUsers(*)).thenReturn(Future.successful(verified2Users))
      }

      def given3VerifiedDevelopers1Unverified(): Unit = {
        when(mockDeveloperService.fetchUsers(*)).thenReturn(Future.successful(users3Verified1Unverified))
      }

      def given3VerifiedDevelopers1UnverifiedSearchDevelopers(): Unit = {
        when(mockDeveloperService.searchDevelopers(any[Developers2Filter])(*)).thenReturn(Future.successful(users))
      }

      def givenfetchDevelopersByEmailPreferences(users: Seq[User]) = {
        when(mockDeveloperService.fetchDevelopersByEmailPreferences(*, *)(*)).thenReturn(Future.successful(users))
      }

      def givenfetchDevelopersByAPICategoryEmailPreferences(users: Seq[User]) = {
        when(mockDeveloperService.fetchDevelopersByAPICategoryEmailPreferences(any[TopicOptionChoice], any[APICategory])(*)).thenReturn(Future.successful(users))
      }

      def givenfetchDevelopersBySpecificAPIEmailPreferences(users: Seq[User]) = {
        when(mockDeveloperService.fetchDevelopersBySpecificAPIEmailPreferences(*,*, *)(*)).thenReturn(Future.successful(users))
      }

      def givenNoVerifiedDevelopers(): Unit = {
        val users = Seq(unVerifiedUser1)
        when(mockDeveloperService.fetchUsers(*)).thenReturn(Future.successful(users))
      }

        val api1 = ApiDefinition("service1", "/", "serviceName", "serviceDesc", ApiContext("service1"), Seq(ApiVersionDefinition(ApiVersion("1"), ApiStatus.BETA)), None, categories = Some(Seq(category1.toAPICategory)))
        val api2 = ApiDefinition("service2", "/", "service2Name", "service2Desc", ApiContext("service2"), Seq(ApiVersionDefinition(ApiVersion("3"), ApiStatus.STABLE)), None, categories = Some(Seq(category2.toAPICategory)))
        val twoApis = Seq(api1, api2)
        def givenApiDefinition2Apis() = {
          when(mockApiDefinitionService.fetchAllDistinctApisIgnoreVersions(any[Option[Environment]])(*))
          .thenReturn(Future.successful(twoApis))
          when(mockApiDefinitionService.fetchAllApiDefinitions(any[Option[Environment]])(*))
            .thenReturn(Future.successful(twoApis))
      }

      def givenApiDefinition3Categories() = {

        when(mockApiDefinitionService.apiCategories()(any[HeaderCarrier]))
          .thenReturn(Future.successful(List(category1, category2, category3)))
      }

      val underTest = new EmailsController(
        mockDeveloperService,
        mockApiDefinitionService,
        mockSendEmailChoiceView,
        mockEmailInformationView,
        mockEmailAllUsersView,
        mockEmailApiSubscriptionsView,
        emailPreferencesChoiceView,
        emailPreferencesTopicView,
        emailPreferencesAPICategoryView,
        mockEmailPreferencesSpecificApiView,
        mockEmailPreferencesSelectApiView,
        mockApplicationService,
        forbiddenView,
        mockAuthConnector,
        mcc,
        errorTemplateView,
        mockApmService
      )

    }

    "email landing page" should {
      "on initial request with logged in user should display disabled options and checked email all options" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        val eventualResult: Future[Result] = underTest.landing()(aLoggedInRequest)
        status(eventualResult) shouldBe OK
       
        verifyAuthConnectorCalledForUser
        verify(mockSendEmailChoiceView).apply()(*,*,*)
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
        givenTheUserIsAuthorisedAndIsANormalUser()
        val result: Result = await(underTest.chooseEmailPreferences()(selectedEmailPreferencesRequest(SPECIFIC_API)))

        status(result) shouldBe SEE_OTHER
        result.header.headers.get("Location") shouldBe Some("/api-gatekeeper/emails/email-preferences/select-api")
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
        verify(mockEmailInformationView).apply(eqTo(EmailOptionChoice.EMAIL_ALL_USERS))(*,*,*)
        verifyAuthConnectorCalledForUser
      }

      "on request with 'api-subscription' in uri path should render correctly" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        val eventualResult: Future[Result] = underTest.showEmailInformation("api-subscription")(aLoggedInRequest)

        status(eventualResult) shouldBe OK
        verify(mockEmailInformationView).apply(eqTo(EmailOptionChoice.API_SUBSCRIPTION))(*,*,*)
        verifyAuthConnectorCalledForUser
      }

      "on request with invalid or empty path will return NOT FOUND" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        val result = intercept[NotFoundException] {
          await(underTest.showEmailInformation("")(aLoggedInRequest))
        }

        verifyZeroInteractions(mockEmailInformationView)
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
        val filteredUsers = users3Verified1Unverified.filter((u:User) => u.verified.contains(true))
        val expectedEmailString = filteredUsers.map(_.email).mkString("; ")
        verify(mockEmailAllUsersView).apply(eqTo(filteredUsers), eqTo(expectedEmailString))(*, *, *)
        verifyAuthConnectorCalledForUser
      }

      "on request should render correctly when 2 users are retrieved from the developer service" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenVerifiedDeveloper()
        val eventualResult: Future[Result] = underTest.emailAllUsersPage()(aLoggedInRequest)

        status(eventualResult) shouldBe OK
        val expectedEmailString = verified2Users.map(_.email).mkString("; ")
        verify(mockEmailAllUsersView).apply(eqTo(verified2Users), eqTo(expectedEmailString))(*, *, *)

        verifyAuthConnectorCalledForUser
      }

      "on request should render correctly when no verified users are retrieved from the developer service" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenNoVerifiedDevelopers()
        val eventualResult: Future[Result] = underTest.emailAllUsersPage()(aLoggedInRequest)

        status(eventualResult) shouldBe OK
        verify(mockEmailAllUsersView).apply(eqTo(Seq.empty), eqTo(""))(*, *, *)
        verifyAuthConnectorCalledForUser
      }
    }

    "email subscribers page" should {

      "render correctly (not display user table) when no filter provided" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenApiDefinition2Apis
        val eventualResult: Future[Result] = underTest.emailApiSubscribersPage()(FakeRequest())
        status(eventualResult) shouldBe OK

        verify(mockEmailApiSubscriptionsView).apply(eqTo(underTest.getApiVersionsDropDownValues(twoApis)), eqTo(Seq.empty), eqTo(""), eqTo(Map.empty))(*, *, *)
        verifyAuthConnectorCalledForUser
      }

      "render correctly and display users when api filter provided" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenApiDefinition2Apis
        given3VerifiedDevelopers1UnverifiedSearchDevelopers()
        val eventualResult: Future[Result] = underTest.emailApiSubscribersPage(Some("service2__3"))(createGetRequest("/emails/api-subscribers?apiVersionFilter=service2__3"))
        status(eventualResult) shouldBe OK
        
        verify(mockEmailApiSubscriptionsView).apply(eqTo(underTest.getApiVersionsDropDownValues(twoApis)), eqTo(Seq.empty), eqTo(""), eqTo(Map.empty))(*, *, *)
        verifyAuthConnectorCalledForUser
      }

    }


    "email preferences select api page" should {
       "return ok on initial load" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenApiDefinition2Apis()

        val eventualResult: Future[Result] = underTest.selectSpecficApi(None)(FakeRequest())
        status(eventualResult) shouldBe OK

        verify(mockEmailPreferencesSelectApiView).apply(eqTo(twoApis.sortBy(_.name)), eqTo(Seq.empty))(*, *, *)
      }

     "return ok when filters provided" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenApiDefinition2Apis()

        val eventualResult: Future[Result] = underTest.selectSpecficApi(Some(Seq(api1.serviceName)))(FakeRequest())
        status(eventualResult) shouldBe OK

        verify(mockEmailPreferencesSelectApiView).apply(eqTo(twoApis.sortBy(_.name)), eqTo(Seq(api1)))(*, *, *)
      }
    }

    "email preferences specific api page" should {
      "redirect to select API page when no filter selected" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenApiDefinition2Apis()

        val eventualResult: Result = await(underTest.emailPreferencesSpecificApis(Seq.empty, None)(FakeRequest()))
        status(eventualResult) shouldBe SEE_OTHER
        eventualResult.header.headers.get("Location") shouldBe Some("/api-gatekeeper/emails/email-preferences/select-api")

        verifyZeroInteractions(mockDeveloperService)
        verifyZeroInteractions(mockEmailPreferencesSpecificApiView)
      }
      
      "render the view correctly when selected api filters are selected" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenApiDefinition2Apis()

        val selectedAPIs = Seq(api1)

        val eventualResult: Future[Result] = underTest.emailPreferencesSpecificApis(selectedAPIs.map(_.serviceName), None)(FakeRequest())
        status(eventualResult) shouldBe OK

        verifyZeroInteractions(mockDeveloperService)
        verify(mockEmailPreferencesSpecificApiView).apply(eqTo(Seq.empty), eqTo(""), eqTo(selectedAPIs), eqTo(None))(*, *, *)
      }

      "render the view with results correctly when apis and topic filters have been selected" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenApiDefinition2Apis()
        givenfetchDevelopersBySpecificAPIEmailPreferences(verified2Users)

        val expectedEmailString = verified2Users.map(_.email).mkString("; ")

        val selectedAPIs = Seq(api1)
        val selectedTopic = TopicOptionChoice.BUSINESS_AND_POLICY

        val eventualResult: Future[Result] = underTest.emailPreferencesSpecificApis(selectedAPIs.map(_.serviceName), Some(selectedTopic.toString))(FakeRequest())
        status(eventualResult) shouldBe OK
        val apiNames = selectedAPIs.map(_.serviceName)
         val  categories = selectedAPIs.flatMap(_.categories.getOrElse(Seq.empty))

        verify(mockDeveloperService).fetchDevelopersBySpecificAPIEmailPreferences(eqTo(selectedTopic), eqTo(categories), eqTo(apiNames))(*)
        verify(mockEmailPreferencesSpecificApiView).apply(eqTo(verified2Users), eqTo(expectedEmailString), eqTo(selectedAPIs), eqTo(Some(selectedTopic)))(*, *, *)
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
        val eventualResult: Result = await(underTest.emailPreferencesTopic(Some("TECHNICAL"))(FakeRequest()))
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
    if (users.nonEmpty) {
      responseBody should include(s"<div>${users.size} results</div>")

      responseBody should include("<th tabindex=\"0\" class=\"sorting_left-aligned\">Email</th>")
      responseBody should include("<th tabindex=\"0\" class=\"sorting_left-aligned\">First name</th>")
      responseBody should include("<th tabindex=\"0\" class=\"sorting_left-aligned\">Last name</th>")

      for ((user, index) <- users.zipWithIndex) {
        responseBody should include(raw"""<td id="dev-email-$index" width="45%">${user.email}</td>""")
        responseBody should include(raw"""<td id="dev-fn-$index">${user.firstName}</td>""")
        responseBody should include(raw"""<td id="dev-sn-$index">${user.lastName}</td>""")
      }
    } else {
      if (showZeroUsers) {
        responseBody should include("<div>0 results</div>")
      }
    }
  }

}
