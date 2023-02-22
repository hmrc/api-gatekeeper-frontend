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

package uk.gov.hmrc.gatekeeper.controllers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import akka.stream.Materializer

import play.api.libs.json.{JsArray, Json}
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.TokenProvider
import uk.gov.hmrc.http.NotFoundException

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.gatekeeper.models.APIAccessType.PUBLIC
import uk.gov.hmrc.gatekeeper.models.CombinedApiCategory.toAPICategory
import uk.gov.hmrc.gatekeeper.models.EmailOptionChoice.{API_SUBSCRIPTION, EMAIL_ALL_USERS, EMAIL_PREFERENCES, EmailOptionChoice}
import uk.gov.hmrc.gatekeeper.models.EmailPreferencesChoice.{EmailPreferencesChoice, SPECIFIC_API, TAX_REGIME, TOPIC}
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.utils.{TitleChecker, WithCSRFAddToken}
import uk.gov.hmrc.gatekeeper.views.html.emails._
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

class EmailsControllerSpec extends ControllerBaseSpec with WithCSRFAddToken with TitleChecker {

  implicit val materializer: Materializer = app.materializer

  private lazy val errorTemplateView                   = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView                       = app.injector.instanceOf[ForbiddenView]
  private lazy val mockSendEmailChoiceView             = mock[EmailLandingView]
  private lazy val mockEmailInformationView            = mock[EmailInformationView]
  private lazy val mockEmailAllUsersView               = mock[EmailAllUsersView]
  private lazy val mockEmailApiSubscriptionsView       = mock[EmailApiSubscriptionsView]
  private lazy val emailPreferencesChoiceView          = app.injector.instanceOf[EmailPreferencesChoiceView]
  private lazy val emailPreferencesTopicView           = app.injector.instanceOf[EmailPreferencesTopicView]
  private lazy val emailPreferencesAPICategoryView     = app.injector.instanceOf[EmailPreferencesAPICategoryView]
  private lazy val mockEmailPreferencesSpecificApiView = mock[EmailPreferencesSpecificApiView]
  private lazy val mockEmailPreferencesSelectApiView   = mock[EmailPreferencesSelectApiView]
  running(app) {

    trait Setup extends ControllerSetupBase {
      when(mockSendEmailChoiceView.apply()(*, *, *)).thenReturn(play.twirl.api.HtmlFormat.empty)
      when(mockEmailInformationView.apply(*)(*, *, *)).thenReturn(play.twirl.api.HtmlFormat.empty)
      when(mockEmailAllUsersView.apply(*, *, *)(*, *, *)).thenReturn(play.twirl.api.HtmlFormat.empty)
      when(mockEmailPreferencesSpecificApiView.apply(*, *, *, *, *)(*, *, *)).thenReturn(play.twirl.api.HtmlFormat.empty)
      when(mockEmailPreferencesSelectApiView.apply(*, *)(*, *, *)).thenReturn(play.twirl.api.HtmlFormat.empty)
      when(mockEmailApiSubscriptionsView.apply(*, *, *, *, *)(*, *, *)).thenReturn(play.twirl.api.HtmlFormat.empty)

      val csrfToken: (String, String)                                             = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken
      override val aLoggedInRequest: FakeRequest[AnyContentAsEmpty.type]          = FakeRequest().withSession(csrfToken, authToken, userToken).withCSRFToken
      override val aSuperUserLoggedInRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest().withSession(csrfToken, authToken, superUserToken).withCSRFToken
      override val anAdminLoggedInRequest: FakeRequest[AnyContentAsEmpty.type]    = FakeRequest().withSession(csrfToken, authToken, adminToken).withCSRFToken

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

      val verifiedUser1             = RegisteredUser("user1@hmrc.com", UserId.random, "verifiedUserA", "1", true)
      val verifiedUser2             = RegisteredUser("user2@hmrc.com", UserId.random, "verifiedUserB", "2", true)
      val verifiedUser3             = RegisteredUser("user3@hmrc.com", UserId.random, "verifiedUserC", "3", true)
      val unVerifiedUser1           = RegisteredUser("user1@somecompany.com", UserId.random, "unVerifiedUserA", "1", false)
      val users                     = List(verifiedUser1, verifiedUser2, verifiedUser3)
      val users3Verified1Unverified = List(verifiedUser1, verifiedUser2, verifiedUser3, unVerifiedUser1)
      val verified2Users            = List(verifiedUser1, verifiedUser2)
      val category1                 = APICategoryDetails("EXAMPLE", "Example")
      val category2                 = APICategoryDetails("VAT", "Vat")
      val category3                 = APICategoryDetails("AGENTS", "Agents")

      def givenVerifiedDeveloper() = DeveloperServiceMock.FetchUsers.returns(verified2Users: _*)

      def given3VerifiedDevelopers1Unverified() = DeveloperServiceMock.FetchUsers.returns(users3Verified1Unverified: _*)

      def given3VerifiedDevelopers1UnverifiedSearchDevelopers() = DeveloperServiceMock.SearchDevelopers.returns(users: _*)

      def givenNoVerifiedDevelopers() = DeveloperServiceMock.FetchUsers.returns(unVerifiedUser1)

      val api1                      = ApiDefinition(
        "service1",
        "/",
        "serviceName",
        "serviceDesc",
        ApiContext("service1"),
        List(ApiVersionDefinition(ApiVersion("1"), ApiVersionSource.UNKNOWN, ApiStatus.BETA)),
        None,
        categories = Some(List(category1.toAPICategory))
      )
      val api2                      = ApiDefinition(
        "service2",
        "/",
        "service2Name",
        "service2Desc",
        ApiContext("service2"),
        List(ApiVersionDefinition(ApiVersion("3"), ApiVersionSource.UNKNOWN, ApiStatus.STABLE)),
        None,
        categories = Some(List(category2.toAPICategory))
      )
      val twoApis                   = List(api1, api2)
      def givenApiDefinition2Apis() = {
        FetchAllDistinctApisIgnoreVersions.inAny.returns(twoApis: _*)
        FetchAllApiDefinitions.inAny.returns(twoApis: _*)
      }

      def givenApiDefinition3Categories() = {
        ApiCategories.returns(category1, category2, category3)
      }

      val combinedRestApi1 = CombinedApi("displayName1", "serviceName1", List(CombinedApiCategory("CUSTOMS")), ApiType.REST_API, Some(PUBLIC))
      val combinedXmlApi2  = CombinedApi("displayName2", "serviceName2", List(CombinedApiCategory("VAT")), ApiType.XML_API, Some(PUBLIC))
      val combinedList     = List(combinedRestApi1, combinedXmlApi2)

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
        mcc,
        errorTemplateView,
        mockApmService,
        StrideAuthorisationServiceMock.aMock
      )

    }

    "email landing page" should {
      "on initial request with logged in user should display disabled options and checked email all options" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        val result: Future[Result] = underTest.landing()(aLoggedInRequest)
        status(result) shouldBe OK

        verify(mockSendEmailChoiceView).apply()(*, *, *)
      }
    }

    "choose email option" should {
      "redirect to the all users information page when EMAIL_ALL_USERS option chosen" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        val result = underTest.chooseEmailOption()(selectedEmailOptionRequest(EMAIL_ALL_USERS))

        status(result) shouldBe SEE_OTHER
        headers(result).get("Location") shouldBe Some("/api-gatekeeper/emails/all-users/information")
      }

      "redirect to the API Subscriptions information page when API_SUBSCRIPTION option chosen" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

        val result = underTest.chooseEmailOption()(selectedEmailOptionRequest(API_SUBSCRIPTION))

        status(result) shouldBe SEE_OTHER
        headers(result).get("Location") shouldBe Some("/api-gatekeeper/emails/api-subscription/information")
      }

      "redirect to the Email Preferences page when EMAIL_PREFERENCES option chosen" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        val result = underTest.chooseEmailOption()(selectedEmailOptionRequest(EMAIL_PREFERENCES))

        status(result) shouldBe SEE_OTHER
        headers(result).get("Location") shouldBe Some("/api-gatekeeper/emails/email-preferences")
      }
    }

    "choose email preferences" should {
      "redirect to Topic page when TOPIC option chosen" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        val result = underTest.chooseEmailPreferences()(selectedEmailPreferencesRequest(TOPIC))

        status(result) shouldBe SEE_OTHER
        headers(result).get("Location") shouldBe Some("/api-gatekeeper/emails/email-preferences/by-topic")
      }

      "redirect to API page when SPECIFIC_API option chosen" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        val result = underTest.chooseEmailPreferences()(selectedEmailPreferencesRequest(SPECIFIC_API))

        status(result) shouldBe SEE_OTHER
        headers(result).get("Location") shouldBe Some("/api-gatekeeper/emails/email-preferences/select-api")
      }

      "redirect to Tax Regime page when TAX_REGIME option chosen" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        val result = underTest.chooseEmailPreferences()(selectedEmailPreferencesRequest(TAX_REGIME))

        status(result) shouldBe SEE_OTHER
        headers(result).get("Location") shouldBe Some("/api-gatekeeper/emails/email-preferences/by-api-category")
      }
    }

    "email information page" should {
      "on request with 'all-users' in uri path should render correctly" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        val result: Future[Result] = underTest.showEmailInformation("all-users")(aLoggedInRequest)

        status(result) shouldBe OK
        verify(mockEmailInformationView).apply(eqTo(EmailOptionChoice.EMAIL_ALL_USERS))(*, *, *)
      }

      "on request with 'api-subscription' in uri path should render correctly" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        val result: Future[Result] = underTest.showEmailInformation("api-subscription")(aLoggedInRequest)

        status(result) shouldBe OK
        verify(mockEmailInformationView).apply(eqTo(EmailOptionChoice.API_SUBSCRIPTION))(*, *, *)
      }

      "on request with invalid or empty path will return NOT FOUND" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        val result = intercept[NotFoundException] {
          await(underTest.showEmailInformation("")(aLoggedInRequest))
        }

        result.message shouldBe "Page Not Found"
      }
    }

    "email all Users page" should {
      "on request should render correctly when 3 verified users are retrieved from developer service " in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        given3VerifiedDevelopers1Unverified()
        val result: Future[Result] = underTest.emailAllUsersPage()(aLoggedInRequest)

        status(result) shouldBe OK
        val filteredUsers       = users3Verified1Unverified.filter(_.verified)
        val filteredUsersAsJson = Json.toJson(filteredUsers)
        val expectedEmailString = filteredUsers.map(_.email).mkString("; ")
        verify(mockEmailAllUsersView).apply(eqTo(filteredUsers), eqTo(filteredUsersAsJson), eqTo(expectedEmailString))(*, *, *)
      }

      "on request should render correctly when 2 users are retrieved from the developer service" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenVerifiedDeveloper()
        val result: Future[Result] = underTest.emailAllUsersPage()(aLoggedInRequest)

        status(result) shouldBe OK
        val expectedEmailString = verified2Users.map(_.email).mkString("; ")
        verify(mockEmailAllUsersView).apply(eqTo(verified2Users), *, eqTo(expectedEmailString))(*, *, *)
      }

      "on request should render correctly when no verified users are retrieved from the developer service" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoVerifiedDevelopers()
        val result: Future[Result] = underTest.emailAllUsersPage()(aLoggedInRequest)

        status(result) shouldBe OK
        verify(mockEmailAllUsersView).apply(eqTo(List.empty), eqTo(new JsArray()), eqTo(""))(*, *, *)
      }
    }

    "email subscribers page" should {
      "render correctly (not display user table) when no filter provided" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenApiDefinition2Apis
        val result: Future[Result] = underTest.emailApiSubscribersPage()(FakeRequest())
        status(result) shouldBe OK

        verify(mockEmailApiSubscriptionsView).apply(eqTo(underTest.getApiVersionsDropDownValues(twoApis)), eqTo(List.empty), eqTo(new JsArray()), eqTo(""), eqTo(Map.empty))(*, *, *)
      }

      "render correctly and display users when api filter provided" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenApiDefinition2Apis
        given3VerifiedDevelopers1UnverifiedSearchDevelopers()
        val result: Future[Result] = underTest.emailApiSubscribersPage(Some("service2__3"))(createGetRequest("/emails/api-subscribers?apiVersionFilter=service2__3"))
        status(result) shouldBe OK

        verify(mockEmailApiSubscriptionsView).apply(eqTo(underTest.getApiVersionsDropDownValues(twoApis)), eqTo(List.empty), eqTo(new JsArray()), eqTo(""), eqTo(Map.empty))(*, *, *)
      }
    }

    "email preferences select api page" should {
      "return ok on initial load" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        when(mockApmService.fetchAllCombinedApis()(*)).thenReturn(Future.successful(combinedList))

        val result: Future[Result] = underTest.selectSpecificApi(None)(FakeRequest())
        status(result) shouldBe OK

        verify(mockApmService).fetchAllCombinedApis()(*)
        verify(mockEmailPreferencesSelectApiView).apply(eqTo(combinedList.sortBy(_.displayName)), eqTo(List.empty))(*, *, *)
      }

      "return ok when filters provided" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        when(mockApmService.fetchAllCombinedApis()(*)).thenReturn(Future.successful(combinedList))

        val result: Future[Result] = underTest.selectSpecificApi(Some(List(combinedRestApi1.serviceName)))(FakeRequest())
        status(result) shouldBe OK

        verify(mockApmService).fetchAllCombinedApis()(*)
        verify(mockEmailPreferencesSelectApiView).apply(eqTo(combinedList.sortBy(_.displayName)), eqTo(List(combinedRestApi1)))(*, *, *)
      }
    }

    "email preferences specific api page" should {
      "redirect to select API page when no filter selected" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenApiDefinition2Apis()

        val result = underTest.emailPreferencesSpecificApis(List.empty, None)(FakeRequest())
        status(result) shouldBe SEE_OTHER
        headers(result).get("Location") shouldBe Some("/api-gatekeeper/emails/email-preferences/select-api")

        verifyZeroInteractions(mockDeveloperService)
        verifyZeroInteractions(mockEmailPreferencesSpecificApiView)
      }

      "render the view correctly when selected api filters are selected" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        when(mockApmService.fetchAllCombinedApis()(*)).thenReturn(Future.successful(combinedList))

        val selectedAPIs = List(combinedXmlApi2)

        val result: Future[Result] = underTest.emailPreferencesSpecificApis(selectedAPIs.map(_.serviceName), None)(FakeRequest())
        status(result) shouldBe OK

        verifyZeroInteractions(mockDeveloperService)
        verify(mockApmService).fetchAllCombinedApis()(*)
        verify(mockEmailPreferencesSpecificApiView).apply(eqTo(List.empty), eqTo(new JsArray()), eqTo(""), eqTo(selectedAPIs), eqTo(None))(*, *, *)

      }

      "render the view with results correctly when apis and topic filters have been selected" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

        when(mockApmService.fetchAllCombinedApis()(*)).thenReturn(Future.successful(combinedList))
        DeveloperServiceMock.FetchDevelopersBySpecificAPIEmailPreferences.returns(verified2Users: _*)

        val expectedEmailString = verified2Users.map(_.email).mkString("; ")

        val selectedAPIs  = List(combinedXmlApi2)
        val selectedTopic = TopicOptionChoice.BUSINESS_AND_POLICY

        val result: Future[Result] = underTest.emailPreferencesSpecificApis(selectedAPIs.map(_.serviceName), Some(selectedTopic.toString))(FakeRequest())
        status(result) shouldBe OK
        val apiNames               = selectedAPIs.map(_.serviceName)

        val categories = selectedAPIs.flatMap(_.categories.map(toAPICategory))

        verify(mockApmService).fetchAllCombinedApis()(*)
        verify(mockDeveloperService).fetchDevelopersBySpecificAPIEmailPreferences(eqTo(selectedTopic), eqTo(categories), eqTo(apiNames), *)(*)
        verify(mockEmailPreferencesSpecificApiView).apply(
          eqTo(verified2Users),
          eqTo(Json.toJson(verified2Users)),
          eqTo(expectedEmailString),
          eqTo(selectedAPIs),
          eqTo(Some(selectedTopic))
        )(*, *, *)
      }

    }

    "email preferences topic page" should {
      "render the view correctly when no filter selected" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

        val request                = createGetRequest("/emails/api-subscribers/email-preferences/by-topic")
        val result: Future[Result] = underTest.emailPreferencesTopic()(request)
        status(result) shouldBe OK

        verifyZeroInteractions(mockDeveloperService)
      }

      "render the view correctly when filter selected and no users returned" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        DeveloperServiceMock.FetchDevelopersByEmailPreferences.returns()
        val request = createGetRequest("/emails/api-subscribers/email-preferences/by-topic")

        val result = underTest.emailPreferencesTopic(Some("TECHNICAL"))(request)
        status(result) shouldBe OK

        val responseBody = contentAsString(result)

        verifyUserTable(responseBody, List.empty)
      }

      "render the view correctly when filter selected and users returned" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        DeveloperServiceMock.FetchDevelopersByEmailPreferences.returns(users: _*)

        val request                = createGetRequest("/emails/api-subscribers/email-preferences/topic?topicOptionChoice=TECHNICAL")
        val result: Future[Result] = underTest.emailPreferencesTopic(Some("TECHNICAL"))(request)
        status(result) shouldBe OK

        val responseBody = Helpers.contentAsString(result)

        verifyUserTable(responseBody, users)
      }
    }

    "Email preferences API Category page" should {
      "render the view correctly when no filters selected" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenApiDefinition3Categories()
        val request                = createGetRequest("/emails/email-preferences/by-api-category")
        val result: Future[Result] = underTest.emailPreferencesAPICategory()(request)
        status(result) shouldBe OK

        verifyZeroInteractions(mockDeveloperService)
      }

      "render the view correctly when topic filter `TECHNICAL` selected and no users returned" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenApiDefinition3Categories()
        DeveloperServiceMock.FetchDevelopersByAPICategoryEmailPreferences.returns()

        val request                = createGetRequest(s"/emails/email-preferences/by-api-category?topicChosen=TECHNICAL&categoryChosen=${category1.category}")
        val result: Future[Result] = underTest.emailPreferencesAPICategory(Some("TECHNICAL"), Some(category1.category))(request)
        status(result) shouldBe OK

        val responseBody = Helpers.contentAsString(result)

        verifyUserTable(responseBody, List.empty)
      }

      "render the view correctly when Topic filter TECHNICAL selected and users returned" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenApiDefinition3Categories()
        DeveloperServiceMock.FetchDevelopersByAPICategoryEmailPreferences.returns(users: _*)

        val request                = createGetRequest(s"/emails/email-preferences/by-api-category?topicChosen=TECHNICAL&categoryChosen=${category1.category}")
        val result: Future[Result] = underTest.emailPreferencesAPICategory(Some("TECHNICAL"), Some(category1.category))(request)
        status(result) shouldBe OK

        val responseBody = Helpers.contentAsString(result)

        verifyUserTable(responseBody, users)
      }
    }
  }

  def verifyUserTable(responseBody: String, users: List[User], showZeroUsers: Boolean = false) {
    if (users.nonEmpty) {
      responseBody should include(s"""<div class="govuk-body">${users.size} results</div>""")

      responseBody should include("""<th scope="col" class="govuk-table__header">Email</th>""")
      responseBody should include("""<th scope="col" class="govuk-table__header">First name</th>""")
      responseBody should include("""<th scope="col" class="govuk-table__header">Last name</th>""")

      for ((user, index) <- users.zipWithIndex) {
        responseBody should include(s"""<td id="dev-email-$index" class="govuk-table__cell">${user.email}</td>""")
        responseBody should include(s"""<td id="dev-fn-$index" class="govuk-table__cell">${user.firstName}</td>""")
        responseBody should include(s"""<td id="dev-sn-$index" class="govuk-table__cell">${user.lastName}</td>""")
      }
    } else {
      if (showZeroUsers) {
        responseBody should include("""<div class="govuk-body">0 results</div>""")
      }
    }
  }

}
