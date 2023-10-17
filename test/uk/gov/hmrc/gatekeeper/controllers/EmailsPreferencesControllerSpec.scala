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
import org.scalatest.Assertion

import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF.TokenProvider
import uk.gov.hmrc.http.NotFoundException

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.gatekeeper.models.EmailOptionChoice.EmailOptionChoice
import uk.gov.hmrc.gatekeeper.models.EmailPreferencesChoice.{EmailPreferencesChoice, SPECIFIC_API, TAX_REGIME, TOPIC}
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.utils.{TitleChecker, WithCSRFAddToken}
import uk.gov.hmrc.gatekeeper.views.html.emails._
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

class EmailsPreferencesControllerSpec extends ControllerBaseSpec with WithCSRFAddToken with TitleChecker {

  implicit val materializer: Materializer = app.materializer

  private lazy val errorTemplateView                             = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView                                 = app.injector.instanceOf[ForbiddenView]
  private lazy val mockEmailPreferencesSpecificApiViewNew        = mock[EmailPreferencesSpecificApiNewView]
  private lazy val mockEmailPreferencesSelectTopicView           = mock[EmailPreferencesSelectTopicView]
  private lazy val mockEmailPreferencesSelectedTopicView         = mock[EmailPreferencesSelectedTopicView]
  private lazy val mockEmailPreferencesChoiceNewView             = mock[EmailPreferencesChoiceNewView]
  private lazy val mockEmailPreferencesSelectApiNewView          = mock[EmailPreferencesSelectApiNewView]
  private lazy val mockEmailPreferencesSelectTaxRegimeView       = mock[EmailPreferencesSelectTaxRegimeView]
  private lazy val mockEmailPreferencesSelectedTaxRegimeView     = mock[EmailPreferencesSelectedTaxRegimeView]
  private lazy val mockEmailPreferencesSelectedUserTaxRegimeView = mock[EmailPreferencesSelectedUserTaxRegimeView]
  private lazy val mockEmailPreferencesSelectUserTopicView       = mock[EmailPreferencesSelectUserTopicView]
  private lazy val mockEmailPreferencesSelectedUserTopicView     = mock[EmailPreferencesSelectedUserTopicView]
  private lazy val mockEmailAllUsersNewView                      = mock[EmailAllUsersNewView]
  private lazy val mockEmailInformationNewView                   = mock[EmailInformationNewView]
  private lazy val mockEmailPreferencesSelectSubscribedApiView   = mock[EmailPreferencesSelectSubscribedApiView]
  private lazy val mockEmailPreferencesSubscribedApiView         = mock[EmailPreferencesSubscribedApiView]
  private lazy val mockEmailPreferencesSelectedSubscribedApiView = mock[EmailPreferencesSelectedSubscribedApiView]

  running(app) {

    trait Setup extends ControllerSetupBase {
      when(mockEmailInformationNewView.apply(*)(*, *, *)).thenReturn(play.twirl.api.HtmlFormat.empty)
      when(mockEmailAllUsersNewView.apply(*, *, *, *, *)(*, *, *)).thenReturn(play.twirl.api.HtmlFormat.empty)
      when(mockEmailPreferencesSelectApiNewView.apply(*, *, *)(*, *, *)).thenReturn(play.twirl.api.HtmlFormat.empty)
      when(mockEmailPreferencesSelectedTopicView.apply(*, *, *, *, *, *, *, *, *)(*, *, *)).thenReturn(play.twirl.api.HtmlFormat.empty)
      when(mockEmailPreferencesSelectTopicView.apply(*, *)(*, *, *)).thenReturn(play.twirl.api.HtmlFormat.empty)
      when(mockEmailPreferencesChoiceNewView.apply()(*, *, *)).thenReturn(play.twirl.api.HtmlFormat.empty)
      when(mockEmailPreferencesSelectedTaxRegimeView.apply(*, *)(*, *, *)).thenReturn(play.twirl.api.HtmlFormat.empty)
      when(mockEmailPreferencesSelectedUserTaxRegimeView.apply(*, *, *, *, *, *)(*, *, *)).thenReturn(play.twirl.api.HtmlFormat.empty)
      when(mockEmailPreferencesSelectUserTopicView.apply(*)(*, *, *)).thenReturn(play.twirl.api.HtmlFormat.empty)
      when(mockEmailPreferencesSelectedUserTopicView.apply(*, *, *, *, *, *)(*, *, *)).thenReturn(play.twirl.api.HtmlFormat.empty)
      when(mockEmailPreferencesSelectedSubscribedApiView.apply(*, *, *, *, *, *)(*, *, *)).thenReturn(play.twirl.api.HtmlFormat.empty)

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

      val verifiedUser1             = RegisteredUser("user1@hmrc.com".toLaxEmail, UserId.random, "verifiedUserA", "1", true)
      val verifiedUser2             = RegisteredUser("user2@hmrc.com".toLaxEmail, UserId.random, "verifiedUserB", "2", true)
      val verifiedUser3             = RegisteredUser("user3@hmrc.com".toLaxEmail, UserId.random, "verifiedUserC", "3", true)
      val unVerifiedUser1           = RegisteredUser("user1@somecompany.com".toLaxEmail, UserId.random, "unVerifiedUserA", "1", false)
      val users                     = List(verifiedUser1, verifiedUser2, verifiedUser3)
      val users3Verified1Unverified = List(verifiedUser1, verifiedUser2, verifiedUser3, unVerifiedUser1)
      val verified2Users            = List(verifiedUser1, verifiedUser2)
      val category1                 = ApiCategory.EXAMPLE
      val category2                 = ApiCategory.VAT
      val category3                 = ApiCategory.AGENTS
      val categorySet               = Set(category1, category2, category3)
      val offset                    = 0
      val limit                     = 4

      def givenVerifiedDeveloper() = DeveloperServiceMock.FetchUsers.returns(verified2Users: _*)

      def given3VerifiedDevelopers1Unverified() = DeveloperServiceMock.FetchUsers.returns(users3Verified1Unverified: _*)

      def given3PaginatedUsersAreFetched(totalCount: Int) = DeveloperServiceMock.FetchUsersPaginated.returns(totalCount, users3Verified1Unverified: _*)

      def given2PaginatedUsersAreFetched(totalCount: Int) = DeveloperServiceMock.FetchUsersPaginated.returns(totalCount, verified2Users: _*)

      def given3VerifiedDevelopers1UnverifiedSearchDevelopers() = DeveloperServiceMock.SearchDevelopers.returns(users: _*)

      def givenNoPaginatedDevelopersAreFetched() = DeveloperServiceMock.FetchUsersPaginated.returns(0, List(): _*)

      def givenNoVerifiedDevelopers() = DeveloperServiceMock.FetchUsers.returns(unVerifiedUser1)

      val api1    = ApiDefinition(
        ServiceName("service1"),
        "/",
        "serviceName",
        "serviceDesc",
        ApiContext("service1"),
        List(ApiVersion(ApiVersionNbr("1"), ApiStatus.BETA, ApiAccess.PUBLIC, List.empty, false, None, ApiVersionSource.UNKNOWN)),
        false,
        false,
        None,
        List(category1)
      )
      val api2    = ApiDefinition(
        ServiceName("service2"),
        "/",
        "service2Name",
        "service2Desc",
        ApiContext("service2"),
        List(ApiVersion(ApiVersionNbr("3"), ApiStatus.STABLE, ApiAccess.PUBLIC, List.empty, false, None, ApiVersionSource.UNKNOWN)),
        false,
        false,
        None,
        List(category2)
      )
      val twoApis = List(api1, api2)

      def givenApiDefinition2Apis() = {
        FetchAllDistinctApisIgnoreVersions.inAny.returns(twoApis: _*)
        FetchAllApiDefinitions.inAny.returns(twoApis: _*)
      }

      val serviceNameOne = "serviceNameOne"
      val serviceNameTwo = "serviceNameTwo"

      val combinedRestApi  = CombinedApi("displayName1", serviceNameOne, Set(ApiCategory.CUSTOMS), ApiType.REST_API, Some(ApiAccessType.PUBLIC))
      val combinedXmlApi   = CombinedApi("displayName2", serviceNameTwo, Set(ApiCategory.VAT), ApiType.XML_API, Some(ApiAccessType.PUBLIC))
      val combinedApisList = List(combinedRestApi, combinedXmlApi)

      val underTest = new EmailsPreferencesController(
        mockDeveloperService,
        mockApiDefinitionService,
        mockEmailAllUsersNewView,
        mockEmailInformationNewView,
        mockEmailPreferencesChoiceNewView,
        mockEmailPreferencesSpecificApiViewNew,
        mockEmailPreferencesSelectApiNewView,
        mockEmailPreferencesSelectTopicView,
        mockEmailPreferencesSelectedTopicView,
        mockEmailPreferencesSelectTaxRegimeView,
        mockEmailPreferencesSelectedTaxRegimeView,
        mockEmailPreferencesSelectedUserTaxRegimeView,
        mockEmailPreferencesSelectUserTopicView,
        mockEmailPreferencesSelectedUserTopicView,
        mockEmailPreferencesSelectSubscribedApiView,
        mockEmailPreferencesSubscribedApiView,
        mockEmailPreferencesSelectedSubscribedApiView,
        mockApplicationService,
        forbiddenView,
        mcc,
        errorTemplateView,
        mockApmService,
        StrideAuthorisationServiceMock.aMock
      )
    }

    "Choose email preferences" should {
      "redirect to Topic page when TOPIC option chosen" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        val result: Future[Result] = underTest.chooseEmailPreferences()(selectedEmailPreferencesRequest(TOPIC))

        status(result) shouldBe SEE_OTHER
        verifyLocation(result, "/api-gatekeeper/emails/email-preferences/select-user-topic")
      }

      "redirect to API page when SPECIFIC_API option chosen" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        val result = underTest.chooseEmailPreferences()(selectedEmailPreferencesRequest(SPECIFIC_API))

        status(result) shouldBe SEE_OTHER
        verifyLocation(result, "/api-gatekeeper/emails/email-preferences/select-api-new")
      }

      "redirect to Tax Regime page when TAX_REGIME option chosen" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        val result = underTest.chooseEmailPreferences()(selectedEmailPreferencesRequest(TAX_REGIME))

        status(result) shouldBe SEE_OTHER
        verifyLocation(result, "/api-gatekeeper/emails/email-preferences/select-tax-regime")
      }
    }

    "Email information page" should {
      "render as expected on request with 'all-users' in uri path" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        val result: Future[Result] = underTest.showEmailInformation("all-users")(aLoggedInRequest)

        status(result) shouldBe OK
        verify(mockEmailInformationNewView).apply(eqTo(EmailOptionChoice.EMAIL_ALL_USERS))(*, *, *)
      }

      "render as expected on request with 'api-subscription' in uri path" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        val result: Future[Result] = underTest.showEmailInformation("api-subscription")(aLoggedInRequest)

        status(result) shouldBe OK
        verify(mockEmailInformationNewView).apply(eqTo(EmailOptionChoice.API_SUBSCRIPTION))(*, *, *)
      }

      "return NOT FOUND on request with invalid or empty path" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        val result: NotFoundException = intercept[NotFoundException] {
          await(underTest.showEmailInformation("")(aLoggedInRequest))
        }

        result.message shouldBe "Page Not Found"
      }
    }

    "Email all Users page" should {
      "render as expected when 3 verified users are retrieved from developer service" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        given3PaginatedUsersAreFetched(4)
        val result: Future[Result] = underTest.emailAllUsersPage(1, 3)(aLoggedInRequest)

        status(result) shouldBe OK
        val filteredUsers       = users3Verified1Unverified.filter(_.verified)
        val expectedEmailString = filteredUsers.map(_.email.text).mkString("; ")

        verify(mockEmailAllUsersNewView).apply(eqTo(filteredUsers), eqTo(expectedEmailString), eqTo(1), eqTo(3), eqTo(4))(*, *, *)
      }

      "render as expected when 2 users are retrieved from the developer service" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        given2PaginatedUsersAreFetched(4)
        val result: Future[Result] = underTest.emailAllUsersPage(1, 2)(aLoggedInRequest)

        status(result) shouldBe OK
        val expectedEmailString = verified2Users.map(_.email.text).mkString("; ")
        verify(mockEmailAllUsersNewView).apply(eqTo(verified2Users), eqTo(expectedEmailString), eqTo(1), eqTo(2), eqTo(4))(*, *, *)
      }

      "render as expected when no verified users are retrieved from the developer service" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoPaginatedDevelopersAreFetched()
        val result: Future[Result] = underTest.emailAllUsersPage(0, 0)(aLoggedInRequest)

        status(result) shouldBe OK
        verify(mockEmailAllUsersNewView).apply(eqTo(List.empty), eqTo(""), eqTo(0), eqTo(0), eqTo(0))(*, *, *)
      }

    }

    "Email preferences select api page" should {
      "render page as expected" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        when(mockApmService.fetchAllCombinedApis()(*)).thenReturn(Future.successful(combinedApisList))

        val result: Future[Result] = underTest.selectSpecificApi(None)(FakeRequest())
        status(result) shouldBe OK

        verify(mockApmService).fetchAllCombinedApis()(*)
        verify(mockEmailPreferencesSelectApiNewView).apply(eqTo(combinedApisList.sortBy(_.displayName)), eqTo(List.empty), eqTo(None))(*, *, *)
      }
    }

    "Email preferences choice new page" should {
      "render the view as expected" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        val request                = createGetRequest("/emails/email-preferences-new")
        val result: Future[Result] = underTest.emailPreferencesChoice()(request)
        status(result) shouldBe OK

        verifyZeroInteractions(mockDeveloperService)
      }
    }

    "Select topic page" should {
      "render the view correctly with topics to choose from" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        when(mockApmService.fetchAllCombinedApis()(*)).thenReturn(Future.successful(combinedApisList))
        DeveloperServiceMock.FetchDevelopersByAPICategoryEmailPreferences.returns(users: _*)

        val request                = createGetRequest("/emails/email-preferences/select-topic")
        val result: Future[Result] = underTest.selectTopicPage(
          Some(combinedApisList.map(_.serviceName)),
          Some(TopicOptionChoice.TECHNICAL)
        )(request)

        status(result) shouldBe OK
      }
    }

    "Email Preferences Selected API(s) Topic page" should {

      "render the view correctly with selected APIs and topic" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        when(mockApmService.fetchAllCombinedApis()(*)).thenReturn(Future.successful(combinedApisList))
        DeveloperServiceMock.FetchDevelopersByEmailPreferencesPaginated.returns(users: _*)

        val request                = createGetRequest("/emails/email-preferences/selected-api-topic")
        val result: Future[Result] = underTest.selectedApiTopic(
          Some(TopicOptionChoice.BUSINESS_AND_POLICY),
          Some(category1),
          combinedApisList.map(_.serviceName),
          0,
          4
        )(request)
        status(result) shouldBe OK

        verify(mockApmService).fetchAllCombinedApis()(*)
      }

      "render the view correctly when topic filter `TECHNICAL` selected and no users returned" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        when(mockApmService.fetchAllCombinedApis()(*)).thenReturn(Future.successful(combinedApisList))
        DeveloperServiceMock.FetchDevelopersByEmailPreferencesPaginated.returns()

        val request                = createGetRequest("/emails/email-preferences/selected-api-topic")
        val result: Future[Result] = underTest.selectedApiTopic(
          Some(TopicOptionChoice.TECHNICAL),
          Some(category1),
          combinedApisList.map(_.serviceName),
          0,
          4
        )(request)

        status(result) shouldBe OK
      }
    }

    "Email preferences select another api" should {

      "return select api page when selected option yes" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

        val result: Future[Result] = underTest.addAnotherApiOption("Yes", Some(combinedApisList.map(_.serviceName)), None)(FakeRequest())

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/emails/email-preferences/select-api-new?selectedAPIs=$serviceNameOne&selectedAPIs=$serviceNameTwo")
      }

      "return select api page when selected option no" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

        val result: Future[Result] = underTest.addAnotherApiOption("No", Some(combinedApisList.map(_.serviceName)), None)(FakeRequest())

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/emails/email-preferences/select-topic?selectedAPIs=$serviceNameOne&selectedAPIs=$serviceNameTwo")
      }
    }

    "Email preferences select another tax regime" should {

      "return select tax regime page when selected option yes" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

        val result: Future[Result] = underTest.addAnotherTaxRegimeOption("Yes", Some(Set(category1)), offset, limit)(FakeRequest())

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/emails/email-preferences/select-tax-regime?selectedCategories=${category1}")
      }

      "return select topic page when selected option no" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        DeveloperServiceMock.FetchDevelopersBySpecificTaxRegimesEmailPreferences.returns(users: _*)

        val result: Future[Result] = underTest.addAnotherTaxRegimeOption("No", Some(Set(category1)), offset, limit)(FakeRequest())
        status(result) shouldBe SEE_OTHER
      }

      "return selected user tax regime" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        DeveloperServiceMock.FetchDevelopersBySpecificTaxRegimesEmailPreferences.returns(users: _*)

        val result: Future[Result] = underTest.selectedUserTaxRegime(Some(Set(category1)), 0, 4)(FakeRequest())
        status(result) shouldBe OK
      }

    }

    "Select user topic page" should {
      "render the view correctly with topics to choose from" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

        val request                = createGetRequest("/emails/email-preferences/select-user-topic")
        val result: Future[Result] = underTest.selectUserTopicPage(
          Some(TopicOptionChoice.TECHNICAL)
        )(request)

        status(result) shouldBe OK
      }
    }

    "Email Preferences selected user topic page" should {

      "render the view correctly with selected topic" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        DeveloperServiceMock.FetchDevelopersByEmailPreferencesPaginated.returns(users: _*)

        val request = createGetRequest("/emails/email-preferences/selected-user-topic")

        val result: Future[Result] = underTest.selectedUserTopic(Some(TopicOptionChoice.BUSINESS_AND_POLICY), offset, limit)(request)

        status(result) shouldBe OK
      }

      "render the view correctly with no topic" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        DeveloperServiceMock.FetchDevelopersByEmailPreferencesPaginated.returns(users: _*)

        val request = createGetRequest("/emails/email-preferences/selected-user-topic")

        val result: Future[Result] = underTest.selectedUserTopic(None, offset, limit)(request)

        status(result) shouldBe OK
      }
    }

    "Email Preferences selected subscribed api page" should {

      "render the view correctly with selected subscribed api" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        when(mockApmService.fetchAllCombinedApis()(*)).thenReturn(Future.successful(combinedApisList))
        DeveloperServiceMock.FetchDevelopersBySpecificApisEmailPreferences.returns(users: _*)

        val request = createGetRequest("/emails/email-preferences/selected-subscribed-api")

        val page1Result: Future[Result] = underTest.selectedSubscribedApi(combinedApisList.map(api => api.serviceName), 0, 1)(request)
        status(page1Result) shouldBe OK
        val page2Result: Future[Result] = underTest.selectedSubscribedApi(combinedApisList.map(api => api.serviceName), 1, 2)(request)
        status(page2Result) shouldBe OK
        val page3Result: Future[Result] = underTest.selectedSubscribedApi(combinedApisList.map(api => api.serviceName), 2, 3)(request)
        status(page3Result) shouldBe OK
      }
    }

    "Email preferences add another subscribed api" should {

      "return select api page when selected option yes" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

        val result: Future[Result] = underTest.addAnotherSubscribedApiOption("Yes", Some(combinedApisList.map(_.serviceName)), offset, limit)(FakeRequest())

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/emails/email-preferences/select-subscribed-api?selectedAPIs=$serviceNameOne&selectedAPIs=$serviceNameTwo")
      }

      "return select api page when selected option no" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

        val result: Future[Result] = underTest.addAnotherSubscribedApiOption("no", Some(combinedApisList.map(_.serviceName)), offset, limit)(FakeRequest())

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(
          s"/api-gatekeeper/emails/email-preferences/selected-subscribed-api?selectedAPIs=$serviceNameOne&selectedAPIs=$serviceNameTwo&limit=$limit"
        )
      }
    }
  }

  private def verifyLocation(result: Future[Result], expectedLocation: String): Assertion = {
    headers(result).get("Location") shouldBe Some(expectedLocation)
  }

  def verifyUserTable(responseBody: String, users: List[User], showZeroUsers: Boolean = false): Unit = {
    if (users.nonEmpty) {
      responseBody should include(s"""<div class="govuk-body">${users.size} results</div>""")

      responseBody should include("""<th scope="col" class="govuk-table__header">Email</th>""")
      responseBody should include("""<th scope="col" class="govuk-table__header">First name</th>""")
      responseBody should include("""<th scope="col" class="govuk-table__header">Last name</th>""")

      for ((user, index) <- users.zipWithIndex) {
        responseBody should include(s"""<td id="dev-email-$index" class="govuk-table__cell">${user.email.text}</td>""")
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
