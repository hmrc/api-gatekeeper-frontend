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

import java.net.URLEncoder

import mocks.TestRoles._
import model.Environment._
import model.RateLimitTier.RateLimitTier
import model._
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito._
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.TokenProvider
import services.{DeveloperService, SubscriptionFieldsService}
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.http.HeaderCarrier
import utils.FakeRequestCSRFSupport._
import utils.{TitleChecker, WithCSRFAddToken}
import views.html.applications._
import views.html.approvedApplication.ApprovedView
import views.html.review.ReviewView
import views.html.{ErrorTemplate, ForbiddenView}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

class ApplicationControllerSpec extends ControllerBaseSpec with WithCSRFAddToken with TitleChecker with MockitoSugar with ArgumentMatchersSugar {

  implicit val materializer = app.materializer

  private lazy val errorTemplateView = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView = app.injector.instanceOf[ForbiddenView]
  private lazy val applicationsView = app.injector.instanceOf[ApplicationsView]
  private lazy val applicationView = app.injector.instanceOf[ApplicationView]
  private lazy val manageSubscriptionsView = app.injector.instanceOf[ManageSubscriptionsView]
  private lazy val manageAccessOverridesView = app.injector.instanceOf[ManageAccessOverridesView]
  private lazy val manageScopesView = app.injector.instanceOf[ManageScopesView]
  private lazy val manageWhitelistedIpView = app.injector.instanceOf[ManageWhitelistedIpView]
  private lazy val manageRateLimitView = app.injector.instanceOf[ManageRateLimitView]
  private lazy val deleteApplicationView = app.injector.instanceOf[DeleteApplicationView]
  private lazy val deleteApplicationSuccessView = app.injector.instanceOf[DeleteApplicationSuccessView]
  private lazy val blockApplicationView = app.injector.instanceOf[BlockApplicationView]
  private lazy val blockApplicationSuccessView = app.injector.instanceOf[BlockApplicationSuccessView]
  private lazy val unblockApplicationView = app.injector.instanceOf[UnblockApplicationView]
  private lazy val unblockApplicationSuccessView = app.injector.instanceOf[UnblockApplicationSuccessView]
  private lazy val reviewView = app.injector.instanceOf[ReviewView]
  private lazy val approvedView = app.injector.instanceOf[ApprovedView]
  private lazy val createApplicationView = app.injector.instanceOf[CreateApplicationView]
  private lazy val createApplicationSuccessView = app.injector.instanceOf[CreateApplicationSuccessView]
  private lazy val manageTeamMembersView = app.injector.instanceOf[ManageTeamMembersView]
  private lazy val addTeamMemberView = app.injector.instanceOf[AddTeamMemberView]
  private lazy val removeTeamMemberView = app.injector.instanceOf[RemoveTeamMemberView]

  running(app) {

    trait Setup extends ControllerSetupBase {

      val csrfToken = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken
      override val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken).withCSRFToken
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, superUserToken).withCSRFToken
      override val anAdminLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, adminToken).withCSRFToken

      val applicationWithOverrides = ApplicationWithHistory(
        basicApplication.copy(access = Standard(overrides = Set(PersistLogin()))), Seq.empty)

      val privilegedApplication = ApplicationWithHistory(
        basicApplication.copy(access = Privileged(scopes = Set("openid", "email"))), Seq.empty)

      val ropcApplication = ApplicationWithHistory(
        basicApplication.copy(access = Ropc(scopes = Set("openid", "email"))), Seq.empty)

      val mockDeveloperService = mock[DeveloperService]
      val mockSubscriptionFieldsService = mock[SubscriptionFieldsService]

      val developers = List[User] {
        new User("joe.bloggs@example.co.uk", "joe", "bloggs", None, None, false)
      }

      val underTest = new ApplicationController(
        mockApplicationService,
        forbiddenView,
        mockApiDefinitionService,
        mockDeveloperService,
        mockAuthConnector,
        mcc,
        applicationsView,
        applicationView,
        manageSubscriptionsView,
        manageAccessOverridesView,
        manageScopesView,
        manageWhitelistedIpView,
        manageRateLimitView,
        deleteApplicationView,
        deleteApplicationSuccessView,
        errorTemplateView,
        blockApplicationView,
        blockApplicationSuccessView,
        unblockApplicationView,
        unblockApplicationSuccessView,
        reviewView,
        approvedView,
        createApplicationView,
        createApplicationSuccessView,
        manageTeamMembersView,
        addTeamMemberView,
        removeTeamMemberView
      )

      def givenThePaginatedApplicationsWillBeReturned = {
        val applications: PaginatedApplicationResponse = aPaginatedApplicationResponse(Seq.empty)
        given(mockApplicationService.searchApplications(*, *)(*)).willReturn(Future.successful(applications))
        given(mockApiDefinitionService.fetchAllApiDefinitions(*)(*)).willReturn(Seq.empty[APIDefinition])
      }
    }

    "applicationsPage" should {
      "on request with no specified environment all sandbox applications supplied" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenThePaginatedApplicationsWillBeReturned

        val eventualResult: Future[Result] = underTest.applicationsPage()(aLoggedInRequest)

        status(eventualResult) shouldBe OK
        titleOf(eventualResult) shouldBe "Unit Test Title - Applications"
        val responseBody = Helpers.contentAsString(eventualResult)
        responseBody should include("<h1>Applications</h1>")
        responseBody should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/applications\">Applications</a>")
        responseBody should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/developers2\">Developers</a>")

        verifyAuthConnectorCalledForUser

        verify(mockApplicationService).searchApplications(eqTo(Some(SANDBOX)), *)(*)
        verify(mockApiDefinitionService).fetchAllApiDefinitions(eqTo(Some(SANDBOX)))(*)
      }

      "on request for production all production applications supplied" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenThePaginatedApplicationsWillBeReturned

        val eventualResult: Future[Result] = underTest.applicationsPage(environment = Some("PRODUCTION"))(aLoggedInRequest)

        status(eventualResult) shouldBe OK

        verify(mockApplicationService).searchApplications(eqTo(Some(PRODUCTION)), *)(*)
        verify(mockApiDefinitionService).fetchAllApiDefinitions(eqTo(Some(PRODUCTION)))(*)
      }

      "on request for sandbox all sandbox applications supplied" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenThePaginatedApplicationsWillBeReturned

        val eventualResult: Future[Result] = underTest.applicationsPage(environment = Some("SANDBOX"))(aLoggedInRequest)

        status(eventualResult) shouldBe OK

        verify(mockApplicationService).searchApplications(eqTo(Some(SANDBOX)), *)(*)
        verify(mockApiDefinitionService).fetchAllApiDefinitions(eqTo(Some(SANDBOX)))(*)
      }

      "pass requested params with default params and default environment of SANDBOX to the service" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenThePaginatedApplicationsWillBeReturned

        val aLoggedInRequestWithParams = FakeRequest(GET, "/applications?search=abc&apiSubscription=ANY&status=CREATED&termsOfUse=ACCEPTED&accessType=STANDARD")
          .withSession(csrfToken, authToken, userToken).withCSRFToken
        val expectedParams = Map(
          "page" -> "1",
          "pageSize" -> "100",
          "sort" -> "NAME_ASC",
          "search" -> "abc",
          "apiSubscription" -> "ANY",
          "status" -> "CREATED",
          "termsOfUse" -> "ACCEPTED",
          "accessType" -> "STANDARD")
        val result = await(underTest.applicationsPage()(aLoggedInRequestWithParams))

        status(result) shouldBe OK

        verify(mockApplicationService).searchApplications(eqTo(Some(SANDBOX)), eqTo(expectedParams))(*)
      }

      "redirect to the login page if the user is not logged in" in new Setup {
        givenAUnsuccessfulLogin()

        val result = await(underTest.applicationsPage()(aLoggedInRequest))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe
          Some(
            s"https://loginUri?successURL=${URLEncoder.encode("http://mock-gatekeeper-frontend/api-gatekeeper/applications", "UTF-8")}" +
              s"&origin=${URLEncoder.encode("api-gatekeeper-frontend", "UTF-8")}")
      }

      "show button to add Privileged or ROPC app to superuser" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenThePaginatedApplicationsWillBeReturned

        val result = await(underTest.applicationsPage()(aSuperUserLoggedInRequest))
        status(result) shouldBe OK

        val body = bodyOf(result)

        body should include("Add privileged or ROPC application")

        verifyAuthConnectorCalledForUser
      }

      "not show button to add Privileged or ROPC app to non-superuser" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenThePaginatedApplicationsWillBeReturned
        
        val result = await(underTest.applicationsPage()(aLoggedInRequest))
        status(result) shouldBe OK

        val body = bodyOf(result)

        body shouldNot include("Add privileged or ROPC application")

        verifyAuthConnectorCalledForUser
      }
    }

    "resendVerification" should {
      "call backend with correct application id and gatekeeper id when resend verification is invoked" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenTheAppWillBeReturned()

        val appCaptor = ArgumentCaptor.forClass(classOf[Application])
        val gatekeeperIdCaptor = ArgumentCaptor.forClass(classOf[String])
        given(mockApplicationService.resendVerification(appCaptor.capture(), gatekeeperIdCaptor.capture())(*))
          .willReturn(Future.successful(ResendVerificationSuccessful))

        await(underTest.resendVerification(applicationId)(aLoggedInRequest))

        appCaptor.getValue shouldBe basicApplication
        gatekeeperIdCaptor.getValue shouldBe userName
        verifyAuthConnectorCalledForUser
      }
    }

    "manageScopes" should {
      "fetch an app with Privileged access for a super user" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned(privilegedApplication)

        val result = await(addToken(underTest.manageScopes(applicationId))(aSuperUserLoggedInRequest))

        status(result) shouldBe OK
        verifyAuthConnectorCalledForSuperUser
      }

      "fetch an app with ROPC access for a super user" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned(ropcApplication)

        val result = await(addToken(underTest.manageScopes(applicationId))(aSuperUserLoggedInRequest))

        status(result) shouldBe OK
        verifyAuthConnectorCalledForSuperUser
      }

      "return an error for a Standard app" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned(application)

        intercept[RuntimeException] {
          await(addToken(underTest.manageScopes(applicationId))(aSuperUserLoggedInRequest))
        }
        verifyAuthConnectorCalledForSuperUser
      }

      "return forbidden for a non-super user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.manageScopes(applicationId))(aLoggedInRequest))

        status(result) shouldBe FORBIDDEN
      }
    }

    "updateScopes" should {
      "call the service to update scopes when a valid form is submitted for a super user" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned()

        given(mockApplicationService.updateScopes(*, *)(*))
          .willReturn(Future.successful(UpdateScopesSuccessResult))

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("scopes" -> "hello, individual-benefits")
        val result = await(addToken(underTest.updateScopes(applicationId))(request))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value}")

        verify(mockApplicationService)
          .updateScopes(eqTo(application.application), eqTo(Set("hello", "individual-benefits")))(*)

        verifyAuthConnectorCalledForSuperUser
      }

      "return a bad request when an invalid form is submitted for a super user" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned()

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("scopes" -> "")
        val result = await(addToken(underTest.updateScopes(applicationId))(request))

        status(result) shouldBe BAD_REQUEST

        verify(mockApplicationService, never).updateScopes(*, *)(*)
        verifyAuthConnectorCalledForSuperUser
      }

      "return a bad request when the service indicates that the scopes are invalid" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned()

        given(mockApplicationService.updateScopes(*, *)(*))
          .willReturn(Future.successful(UpdateScopesInvalidScopesResult))

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("scopes" -> "hello")
        val result = await(addToken(underTest.updateScopes(applicationId))(request))

        status(result) shouldBe BAD_REQUEST
      }

      "return forbidden when a form is submitted for a non-super user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned()

        val request = aLoggedInRequest.withFormUrlEncodedBody()
        val result = await(addToken(underTest.updateScopes(applicationId))(request))

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).updateScopes(*, *)(*)
      }
    }

    "manageWhitelistedIp" should {
      "return the manage whitelisted IP page for an admin" in new Setup {
        givenTheUserIsAuthorisedAndIsAnAdmin()
        givenTheAppWillBeReturned()

        val result = await(underTest.manageWhitelistedIpPage(applicationId)(anAdminLoggedInRequest))

        status(result) shouldBe OK
        bodyOf(result) should include("Manage whitelisted IP")
      }

      "return the manage whitelisted IP page for a super user" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned()

        val result = await(underTest.manageWhitelistedIpPage(applicationId)(aSuperUserLoggedInRequest))

        status(result) shouldBe OK
        bodyOf(result) should include("Manage whitelisted IP")
      }

      "return the forbidden page for a normal user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned()

        val result = await(underTest.manageWhitelistedIpPage(applicationId)(aLoggedInRequest))

        status(result) shouldBe FORBIDDEN
        bodyOf(result) should include("You do not have permission")
      }
    }

    "manageWhitelistedIpAction" should {
      val whitelistedIpToUpdate: String = "1.1.1.0/24"

      "manage whitelisted IP using the app service for an admin" in new Setup {
        givenTheUserIsAuthorisedAndIsAnAdmin()
        givenTheAppWillBeReturned()
        given(mockApplicationService.manageWhitelistedIp(*, *)(*))
          .willReturn(Future.successful(UpdateIpWhitelistSuccessResult))
        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("whitelistedIps" -> whitelistedIpToUpdate)

        val result = await(underTest.manageWhitelistedIpAction(applicationId)(request))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value}")
        verify(mockApplicationService).manageWhitelistedIp(eqTo(application.application), eqTo(Set(whitelistedIpToUpdate)))(*)
      }

      "manage whitelisted IP using the app service for a super user" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned()
        given(mockApplicationService.manageWhitelistedIp(*, *)(*))
          .willReturn(Future.successful(UpdateIpWhitelistSuccessResult))
        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("whitelistedIps" -> whitelistedIpToUpdate)

        val result = await(underTest.manageWhitelistedIpAction(applicationId)(request))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value}")
        verify(mockApplicationService).manageWhitelistedIp(eqTo(application.application), eqTo(Set(whitelistedIpToUpdate)))(*)
      }

      "return bad request for invalid values" in new Setup {
        val invalidWhitelistedIps: Seq[String] = Seq(
          "1.1.1.0", // no mask
          "1.1.1.0/33", // mask greater than 32
          "1.1.1.0/23", // mask less than 24
          "1.1.1.0/", // incomplete mask
          "1.1.1/24", // IP address missing one octet
          "10.1.1.0/24", // within a private network range
          "172.20.1.0/24", // within a private network range
          "192.168.1.0/24", // within a private network range
          "10.0.0.0/24", // within a private network range, using the network address
          "10.255.255.255/24" // within a private network range, using the broadcast address
        )

        invalidWhitelistedIps.foreach { invalidWhitelistedIp =>
          givenTheUserIsAuthorisedAndIsASuperUser()
          givenTheAppWillBeReturned()
          val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("whitelistedIps" -> invalidWhitelistedIp)

          val result = await(underTest.manageWhitelistedIpAction(applicationId)(request))

          status(result) shouldBe BAD_REQUEST
          verify(mockApplicationService, times(0)).manageWhitelistedIp(eqTo(application.application), eqTo(Set(whitelistedIpToUpdate)))(*)
        }
      }

      "return the forbidden page for a normal user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned()
        val request = aLoggedInRequest.withFormUrlEncodedBody("whitelistedIps" -> whitelistedIpToUpdate)

        val result = await(underTest.manageWhitelistedIpAction(applicationId)(request))

        status(result) shouldBe FORBIDDEN
        bodyOf(result) should include("You do not have permission")
      }
    }

    "manageOverrides" should {
      "fetch an app with Standard access for a super user" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.manageAccessOverrides(applicationId))(aSuperUserLoggedInRequest))

        status(result) shouldBe OK
        verifyAuthConnectorCalledForSuperUser
      }

      "return an error for a ROPC app" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned(ropcApplication)

        intercept[RuntimeException] {
          await(addToken(underTest.manageAccessOverrides(applicationId))(aSuperUserLoggedInRequest))
        }
      }

      "return an error for a Privileged app" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned(privilegedApplication)

        intercept[RuntimeException] {
          await(addToken(underTest.manageAccessOverrides(applicationId))(aSuperUserLoggedInRequest))
        }
      }

      "return forbidden for a non-super user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.manageAccessOverrides(applicationId))(aLoggedInRequest))

        status(result) shouldBe FORBIDDEN
      }
    }

    "updateOverrides" should {
      "call the service to update overrides when a valid form is submitted for a super user" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned()

        given(mockApplicationService.updateOverrides(*, *)(*))
          .willReturn(Future.successful(UpdateOverridesSuccessResult))

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
          "persistLoginEnabled" -> "true",
          "grantWithoutConsentEnabled" -> "true", "grantWithoutConsentScopes" -> "hello, individual-benefits",
          "suppressIvForAgentsEnabled" -> "true", "suppressIvForAgentsScopes" -> "openid, email",
          "suppressIvForOrganisationsEnabled" -> "true", "suppressIvForOrganisationsScopes" -> "address, openid:mdtp",
          "suppressIvForIndividualsEnabled" -> "true", "suppressIvForIndividualsScopes" -> "email, openid:hmrc-enrolments")

        val result = await(addToken(underTest.updateAccessOverrides(applicationId))(request))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value}")

        verify(mockApplicationService).updateOverrides(
          eqTo(application.application),
          eqTo(Set(
            PersistLogin(),
            GrantWithoutConsent(Set("hello", "individual-benefits")),
            SuppressIvForAgents(Set("openid", "email")),
            SuppressIvForOrganisations(Set("address", "openid:mdtp")),
            SuppressIvForIndividuals(Set("email", "openid:hmrc-enrolments"))
          )))(*)

        verifyAuthConnectorCalledForSuperUser
      }

      "return a bad request when an invalid form is submitted for a super user" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned()

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
          "persistLoginEnabled" -> "true",
          "grantWithoutConsentEnabled" -> "true", "grantWithoutConsentScopes" -> "")

        val result = await(addToken(underTest.updateAccessOverrides(applicationId))(request))

        status(result) shouldBe BAD_REQUEST

        verify(mockApplicationService, never).updateOverrides(*, *)(*)

        verifyAuthConnectorCalledForSuperUser
      }

      "return forbidden when a form is submitted for a non-super user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned()

        val request = aLoggedInRequest.withFormUrlEncodedBody("persistLoginEnabled" -> "true")

        val result = await(addToken(underTest.updateAccessOverrides(applicationId))(request))

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).updateOverrides(*, *)(*)
      }
    }

    "subscribeToApi" should {
      val apiContext = ApiContext.random

      "call the service to subscribe to the API when submitted for a super user" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned()

        given(mockApplicationService.subscribeToApi(*, *[ApiContext], *)(*))
          .willReturn(Future.successful(ApplicationUpdateSuccessResult))

        val result = await(addToken(underTest.subscribeToApi(applicationId, apiContext, "1.0"))(aSuperUserLoggedInRequest))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value}/subscriptions")

        verify(mockApplicationService).subscribeToApi(eqTo(basicApplication), eqTo(apiContext), eqTo("1.0"))(*)
        verifyAuthConnectorCalledForSuperUser
      }

      "return forbidden when submitted for a non-super user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned()

        val result = await(addToken(underTest.subscribeToApi(applicationId, apiContext, "1.0"))(aLoggedInRequest))

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).subscribeToApi(eqTo(basicApplication), *[ApiContext], *)(*)
      }
    }

    "unsubscribeFromApi" should {
      val apiContext = ApiContext.random

      "call the service to unsubscribe from the API when submitted for a super user" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned()

        given(mockApplicationService.unsubscribeFromApi(*, *[ApiContext], *)(*))
          .willReturn(Future.successful(ApplicationUpdateSuccessResult))

        val result = await(addToken(underTest.unsubscribeFromApi(applicationId, apiContext, "1.0"))(aSuperUserLoggedInRequest))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value}/subscriptions")

        verify(mockApplicationService).unsubscribeFromApi(eqTo(basicApplication), eqTo(apiContext), eqTo("1.0"))(*)
        verifyAuthConnectorCalledForSuperUser
      }

      "return forbidden when submitted for a non-super user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned()

        val result = await(addToken(underTest.unsubscribeFromApi(applicationId, apiContext, "1.0"))(aLoggedInRequest))

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).unsubscribeFromApi(*, *[ApiContext], *)(*)
      }
    }

    "manageRateLimitTier" should {
      "fetch the app and return the page for an admin" in new Setup {
        givenTheUserIsAuthorisedAndIsAnAdmin()
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.manageRateLimitTier(applicationId))(anAdminLoggedInRequest))

        status(result) shouldBe OK
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), *)(*, *)

        verifyAuthConnectorCalledForAdmin
      }

      "return forbidden for a super user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.manageRateLimitTier(applicationId))(aSuperUserLoggedInRequest))

        status(result) shouldBe FORBIDDEN
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), *)(*, *)

      }

      "return forbidden for a user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.manageRateLimitTier(applicationId))(aLoggedInRequest))

        status(result) shouldBe FORBIDDEN
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), *)(*, *)
      }
    }

    "updateRateLimitTier" should {
      "call the service to update the rate limit tier when a valid form is submitted for an admin" in new Setup {
        givenTheUserIsAuthorisedAndIsAnAdmin()
        givenTheAppWillBeReturned()

        given(mockApplicationService.updateRateLimitTier(*, *)(*))
          .willReturn(Future.successful(ApplicationUpdateSuccessResult))

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("tier" -> "GOLD")

        val result = await(addToken(underTest.updateRateLimitTier(applicationId))(request))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value}")

        verify(mockApplicationService).updateRateLimitTier(eqTo(basicApplication), eqTo(RateLimitTier.GOLD))(*)
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), *)(*, *)
        verifyAuthConnectorCalledForAdmin
      }

      "return a bad request when an invalid form is submitted for an admin user" in new Setup {
        givenTheUserIsAuthorisedAndIsAnAdmin()
        givenTheAppWillBeReturned()

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody()

        val result = await(addToken(underTest.updateRateLimitTier(applicationId))(request))

        status(result) shouldBe BAD_REQUEST

        verify(mockApplicationService, never).updateRateLimitTier(*, *)(*)
        verifyAuthConnectorCalledForAdmin
      }

      "return forbidden when a form is submitted for a non-admin user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned()

        val request = aLoggedInRequest.withFormUrlEncodedBody("tier" -> "GOLD")

        val result = await(addToken(underTest.updateRateLimitTier(applicationId))(request))

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).updateRateLimitTier(*, *)(*)
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), *)(*, *)
      }
    }

    "handleUplift" should {

      "call backend with correct application id and gatekeeper id when application is approved" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenTheAppWillBeReturned()

        val appCaptor = ArgumentCaptor.forClass(classOf[Application])
        val gatekeeperIdCaptor = ArgumentCaptor.forClass(classOf[String])
        given(mockApplicationService.approveUplift(appCaptor.capture(), gatekeeperIdCaptor.capture())(*))
          .willReturn(Future.successful(ApproveUpliftSuccessful))
        await(underTest.handleUplift(applicationId)(aLoggedInRequest.withFormUrlEncodedBody(("action", "APPROVE"))))
        appCaptor.getValue shouldBe basicApplication
        gatekeeperIdCaptor.getValue shouldBe userName

        verifyAuthConnectorCalledForUser
      }
    }

    "handleUpdateRateLimitTier" should {
      val tier = RateLimitTier.GOLD

      "change the rate limit for a super user" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned(application)

        val appCaptor = ArgumentCaptor.forClass(classOf[Application])
        val newTierCaptor = ArgumentCaptor.forClass(classOf[RateLimitTier])
        val hcCaptor = ArgumentCaptor.forClass(classOf[HeaderCarrier])

        given(mockApplicationService.updateRateLimitTier(appCaptor.capture(), newTierCaptor.capture())(hcCaptor.capture()))
          .willReturn(Future.successful(ApplicationUpdateSuccessResult))

        val result = await(underTest.handleUpdateRateLimitTier(applicationId)(aLoggedInRequest.withFormUrlEncodedBody(("tier", tier.toString))))
        status(result) shouldBe SEE_OTHER

        appCaptor.getValue shouldBe basicApplication
        newTierCaptor.getValue shouldBe tier

        verify(mockApplicationService, times(1)).updateRateLimitTier(basicApplication, tier)(hcCaptor.getValue)

        verifyAuthConnectorCalledForUser
      }

      "not call the application connector for a normal user " in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenTheAppWillBeReturned(application)

        val result = await(underTest.handleUpdateRateLimitTier(applicationId)(aLoggedInRequest.withFormUrlEncodedBody(("tier", "GOLD"))))
        status(result) shouldBe SEE_OTHER

        verify(mockApplicationService, never).updateRateLimitTier(*, *)(*)

        verifyAuthConnectorCalledForUser
      }
    }

    "createPrivOrROPCApp" should {
      val appName = "My New App"
      val privilegedAccessType = AccessType.PRIVILEGED
      val ropcAccessType = AccessType.ROPC
      val description = "An application description"
      val adminEmail = "emailAddress@example.com"
      val clientId = ClientId.random
      val totpSecret = "THISISATOTPSECRETFORPRODUCTION"
      val totp = Some(TotpSecrets(totpSecret))
      val privAccess = AppAccess(AccessType.PRIVILEGED, Seq())
      val ropcAccess = AppAccess(AccessType.ROPC, Seq())

      "with invalid form fields" can {
        "show the correct error message when no environment is chosen" in new Setup {
          givenTheUserIsAuthorisedAndIsASuperUser()

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", ""),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", appName),
              ("applicationDescription", description),
              ("adminEmail", adminEmail))))

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Tell us what environment")
        }

        "show the correct error message when no access type is chosen" in new Setup {
          givenTheUserIsAuthorisedAndIsASuperUser()

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("accessType", ""),
              ("applicationName", appName),
              ("applicationDescription", description),
              ("adminEmail", adminEmail))))

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Tell us what access type")
        }

        "show the correct error message when the app name is left empty" in new Setup {
          givenTheUserIsAuthorisedAndIsASuperUser()

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", ""),
              ("applicationDescription", description),
              ("adminEmail", adminEmail))))

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Provide an application name")

        }

        "show the correct error message when the new prod app name already exists in prod" in new Setup {
          val collaborators = Set(Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR))
          val existingApp = ApplicationResponse(
            ApplicationId.random, ClientId.random, "gatewayId", "I Already Exist", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState())

          givenTheUserIsAuthorisedAndIsASuperUser()
          given(mockApplicationService.fetchApplications(*)).willReturn(Future.successful(Seq(existingApp)))

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", "I Already Exist"),
              ("applicationDescription", description),
              ("adminEmail", adminEmail))))

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Provide an application name that does not already exist")
        }

        "allow creation of a sandbox app if name already exists in production" in new Setup {

          val collaborators = Set(Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR))
          val existingApp = ApplicationResponse(
            ApplicationId.random, ClientId.random, "gatewayId", "I Already Exist", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState())

          givenTheUserIsAuthorisedAndIsASuperUser()
          given(mockApplicationService.fetchApplications(*)).willReturn(Future.successful(Seq(existingApp)))
          given(mockApplicationService
            .createPrivOrROPCApp(*, *, *, *, *)(*))
            .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(applicationId, "I Already Exist", "SANDBOX", clientId, totp, privAccess)))

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.SANDBOX.toString),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", "I Already Exist"),
              ("applicationDescription", description),
              ("adminEmail", adminEmail))))

          status(result) shouldBe OK

          bodyOf(result) should include("Application added")
          verifyAuthConnectorCalledForSuperUser
        }

        "allow creation of a sandbox app if name already exists in sandbox" in new Setup {
          val collaborators = Set(Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR))
          val existingApp = ApplicationResponse(
            ApplicationId.random, ClientId.random, "gatewayId", "I Already Exist", "SANDBOX", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState())

          givenTheUserIsAuthorisedAndIsASuperUser()
          given(mockApplicationService.fetchApplications(*)).willReturn(Future.successful(Seq(existingApp)))
          given(mockApplicationService
            .createPrivOrROPCApp(*, *, *, *, *)(*))
            .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(applicationId, "I Already Exist", "SANDBOX", clientId, totp, privAccess)))

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.SANDBOX.toString),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", "I Already Exist"),
              ("applicationDescription", description),
              ("adminEmail", adminEmail))))

          status(result) shouldBe OK

          bodyOf(result) should include("Application added")
          verifyAuthConnectorCalledForSuperUser
        }

        "allow creation of a prod app if name already exists in sandbox" in new Setup {
          val collaborators = Set(Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR))
          val existingApp = ApplicationResponse(
            ApplicationId.random, ClientId.random, "gatewayId", "I Already Exist", "SANDBOX", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState())

          givenTheUserIsAuthorisedAndIsASuperUser()
          given(mockApplicationService.fetchApplications(*)).willReturn(Future.successful(Seq(existingApp)))
          given(mockApplicationService
            .createPrivOrROPCApp(*, *, *, *, *)(*))
            .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(applicationId, "I Already Exist", "PRODUCTION", clientId, totp, privAccess)))

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", "I Already Exist"),
              ("applicationDescription", description),
              ("adminEmail", adminEmail))))

          status(result) shouldBe OK

          bodyOf(result) should include("Application added")
          verifyAuthConnectorCalledForSuperUser
        }

        "show the correct error message when app description is left empty" in new Setup {
          givenTheUserIsAuthorisedAndIsASuperUser()

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", appName),
              ("applicationDescription", ""),
              ("adminEmail", adminEmail))))

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Provide an application description")
        }

        "show the correct error message when admin email is left empty" in new Setup {
          givenTheUserIsAuthorisedAndIsASuperUser()

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", appName),
              ("applicationDescription", description),
              ("adminEmail", ""))))

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Provide an email address")
        }

        "show the correct error message when admin email is invalid" in new Setup {
          givenTheUserIsAuthorisedAndIsASuperUser()

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", appName),
              ("applicationDescription", description),
              ("adminEmail", "notAValidEmailAddress"))))

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Provide a valid email address")
        }
      }

      "with valid form fields" can {
        "but the user is not a superuser" should {
          "show 403 forbidden" in new Setup {
            givenTheUserHasInsufficientEnrolments()

            val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
              aLoggedInRequest.withFormUrlEncodedBody(
                ("environment", Environment.PRODUCTION.toString),
                ("accessType", privilegedAccessType.toString),
                ("applicationName", appName),
                ("applicationDescription", description),
                ("adminEmail", "a@example.com"))))

            status(result) shouldBe FORBIDDEN
          }
        }

        "and the user is a superuser" should {
          "show the success page for a priv app in production" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            given(mockApplicationService.fetchApplications(*)).willReturn(Future.successful(Seq()))
            given(mockApplicationService
              .createPrivOrROPCApp(*, *, *, *, *)(*))
              .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(applicationId, appName, "PRODUCTION", clientId, totp, privAccess)))

            val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
              aSuperUserLoggedInRequest.withFormUrlEncodedBody(
                ("environment", Environment.PRODUCTION.toString),
                ("accessType", privilegedAccessType.toString),
                ("applicationName", appName),
                ("applicationDescription", description),
                ("adminEmail", "a@example.com"))))

            status(result) shouldBe OK

            bodyOf(result) should include(appName)
            bodyOf(result) should include("Application added")
            bodyOf(result) should include("This is your only chance to copy and save this application's TOTP.")
            bodyOf(result) should include(applicationId.value)
            bodyOf(result) should include("Production")
            bodyOf(result) should include("Privileged")
            bodyOf(result) should include(totpSecret)
            bodyOf(result) should include(clientId.value)
            verifyAuthConnectorCalledForSuperUser

          }

          "show the success page for a priv app in sandbox" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            given(mockApplicationService.fetchApplications(*)).willReturn(Future.successful(Seq()))
            given(mockApplicationService
              .createPrivOrROPCApp(*, *, *, *, *)(*))
              .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(applicationId, appName, "SANDBOX", clientId, totp, privAccess)))

            val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
              aSuperUserLoggedInRequest.withFormUrlEncodedBody(
                ("environment", Environment.SANDBOX.toString),
                ("accessType", privilegedAccessType.toString),
                ("applicationName", appName),
                ("applicationDescription", description),
                ("adminEmail", "a@example.com"))))

            status(result) shouldBe OK

            bodyOf(result) should include(appName)
            bodyOf(result) should include("Application added")
            bodyOf(result) should include("This is your only chance to copy and save this application's TOTP.")
            bodyOf(result) should include(applicationId.value)
            bodyOf(result) should include("Sandbox")
            bodyOf(result) should include("Privileged")
            bodyOf(result) should include(totpSecret)
            bodyOf(result) should include(clientId.value)
            verifyAuthConnectorCalledForSuperUser
          }

          "show the success page for an ROPC app in production" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            given(mockApplicationService.fetchApplications(*)).willReturn(Future.successful(Seq()))
            given(mockApplicationService
              .createPrivOrROPCApp(*, *, *, *, *)(*))
              .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(applicationId, appName, "PRODUCTION", clientId, None, ropcAccess)))

            val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
              aSuperUserLoggedInRequest.withFormUrlEncodedBody(
                ("environment", Environment.PRODUCTION.toString),
                ("accessType", ropcAccessType.toString),
                ("applicationName", appName),
                ("applicationDescription", description),
                ("adminEmail", "a@example.com"))))

            status(result) shouldBe OK

            bodyOf(result) should include(appName)
            bodyOf(result) should include("Application added")
            bodyOf(result) should include(applicationId.value)
            bodyOf(result) should include("Production")
            bodyOf(result) should include("ROPC")
            bodyOf(result) should include(clientId.value)
            verifyAuthConnectorCalledForSuperUser
          }

          "show the success page for an ROPC app in sandbox" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            given(mockApplicationService.fetchApplications(*)).willReturn(Future.successful(Seq()))
            given(mockApplicationService
              .createPrivOrROPCApp(*, *, *, *, *)(*))
              .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(applicationId, appName, "SANDBOX", clientId, None, ropcAccess)))

            val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
              aSuperUserLoggedInRequest.withFormUrlEncodedBody(
                ("environment", Environment.SANDBOX.toString),
                ("accessType", ropcAccessType.toString),
                ("applicationName", appName),
                ("applicationDescription", description),
                ("adminEmail", "a@example.com"))))

            status(result) shouldBe OK

            bodyOf(result) should include(appName)
            bodyOf(result) should include("Application added")
            bodyOf(result) should include(applicationId.value)
            bodyOf(result) should include("Sandbox")
            bodyOf(result) should include("ROPC")
            bodyOf(result) should include(clientId.value)
            verifyAuthConnectorCalledForSuperUser

          }
        }
      }
    }

    "manageSubscription" when {
      val apiContext = ApiContext.random

      "the user is a superuser" should {
        "fetch the subscriptions with the fields" in new Setup {

          val subscription = Subscription("name", "serviceName", apiContext, Seq())
          givenTheUserIsAuthorisedAndIsASuperUser()
          givenTheAppWillBeReturned()
          given(mockApplicationService.fetchApplicationSubscriptions(*)(*)).willReturn(Seq(subscription))

          val result = await(addToken(underTest.manageSubscription(applicationId))(aSuperUserLoggedInRequest))

          status(result) shouldBe OK
          verify(mockApplicationService, times(1)).fetchApplicationSubscriptions(eqTo(application.application))(*)
          verifyAuthConnectorCalledForSuperUser
        }
      }

      "the user is not a superuser" should {
        "show 403 forbidden" in new Setup {
          val subscription = Subscription("name", "serviceName", apiContext, Seq())

          givenTheUserHasInsufficientEnrolments()

          given(mockApplicationService.fetchApplicationSubscriptions(*)(*)).willReturn(Seq(subscription))

          val result = await(addToken(underTest.manageSubscription(applicationId))(aLoggedInRequest))

          status(result) shouldBe FORBIDDEN
        }
      }
    }

    "applicationPage" should {
      val apiContext = ApiContext.random

      "return the application details without subscription fields" in new Setup {
        val subscriptions = Seq(Subscription("name", "serviceName", apiContext, Seq()))

        givenTheUserIsAuthorisedAndIsANormalUser()
        givenTheAppWillBeReturned()
        given(mockApplicationService.fetchApplicationSubscriptions(*)(*)).willReturn(subscriptions)
        given(mockDeveloperService.fetchDevelopersByEmails(eqTo(application.application.collaborators.map(colab => colab.emailAddress)))(*))
          .willReturn(developers)

        val result = await(addToken(underTest.applicationPage(applicationId))(aLoggedInRequest))

        status(result) shouldBe OK
        verify(mockApplicationService, times(1)).fetchApplicationSubscriptions(eqTo(application.application))(*)

        verify(mockSubscriptionFieldsService, never).fetchAllFieldDefinitions(*)(*)
        verify(mockSubscriptionFieldsService, never).fetchFieldsWithPrefetchedDefinitions(*, *, *)(*)
        verifyAuthConnectorCalledForUser
      }
    }

    "manageTeamMembers" when {
      "managing a privileged app" when {
        "the user is a superuser" should {
          "show 200 OK" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned(privilegedApplication)

            val result = await(addToken(underTest.manageTeamMembers(applicationId))(aSuperUserLoggedInRequest))

            status(result) shouldBe OK

            // The auth connector checks you are logged on. And the controller checks you are also a super user as it's a privileged app.
            verifyAuthConnectorCalledForUser
          }
        }

        "the user is not a superuser" should {
          "show 403 Forbidden" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(privilegedApplication)

            val result = await(addToken(underTest.manageTeamMembers(applicationId))(aLoggedInRequest))

            status(result) shouldBe FORBIDDEN
          }
        }
      }

      "managing an ROPC app" when {
        "the user is a superuser" should {
          "show 200 OK" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned(ropcApplication)

            val result = await(addToken(underTest.manageTeamMembers(applicationId))(aSuperUserLoggedInRequest))

            status(result) shouldBe OK

            verifyAuthConnectorCalledForUser
          }
        }

        "the user is not a superuser" should {
          "show 403 Forbidden" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(ropcApplication)

            val result = await(addToken(underTest.manageTeamMembers(applicationId))(aLoggedInRequest))

            status(result) shouldBe FORBIDDEN
          }
        }
      }

      "managing a standard app" when {
        "the user is a superuser" should {
          "show 200 OK" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            val result = await(addToken(underTest.manageTeamMembers(applicationId))(aSuperUserLoggedInRequest))

            status(result) shouldBe OK
            verifyAuthConnectorCalledForUser
          }
        }

        "the user is not a superuser" should {
          "show 200 OK" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned()

            val result = await(addToken(underTest.manageTeamMembers(applicationId))(aLoggedInRequest))

            status(result) shouldBe OK
            verifyAuthConnectorCalledForUser
          }
        }
      }
    }

    "addTeamMember" when {
      "managing a privileged app" when {
        "the user is a superuser" should {
          "show 200 OK" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned(privilegedApplication)

            val result = await(addToken(underTest.addTeamMember(applicationId))(aSuperUserLoggedInRequest))

            status(result) shouldBe OK
            verifyAuthConnectorCalledForUser
          }
        }

        "the user is not a superuser" should {
          "show 403 Forbidden" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(privilegedApplication)

            val result = await(addToken(underTest.addTeamMember(applicationId))(aLoggedInRequest))

            status(result) shouldBe FORBIDDEN
          }
        }
      }

      "managing an ROPC app" when {
        "the user is a superuser" should {
          "show 200 OK" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned(ropcApplication)

            val result = await(addToken(underTest.addTeamMember(applicationId))(aSuperUserLoggedInRequest))

            status(result) shouldBe OK
            verifyAuthConnectorCalledForUser
          }
        }

        "the user is not a superuser" should {
          "show 403 Forbidden" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(ropcApplication)

            val result = await(addToken(underTest.addTeamMember(applicationId))(aLoggedInRequest))

            status(result) shouldBe FORBIDDEN
          }
        }
      }

      "managing a standard app" when {
        "the user is a superuser" should {
          "show 200 OK" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            val result = await(addToken(underTest.addTeamMember(applicationId))(aSuperUserLoggedInRequest))

            status(result) shouldBe OK
            verifyAuthConnectorCalledForUser
          }
        }

        "the user is not a superuser" should {
          "show 200 OK" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned()

            val result = await(addToken(underTest.addTeamMember(applicationId))(aLoggedInRequest))

            status(result) shouldBe OK
            verifyAuthConnectorCalledForUser
          }
        }
      }
    }

    "addTeamMemberAction" when {
      val email = "email@example.com"

      "the user is a superuser" when {
        "the form is valid" should {
          val role = "DEVELOPER"

          "call the service to add the team member" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            given(mockApplicationService.addTeamMember(*, *, *)(*))
              .willReturn(Future.successful(ApplicationUpdateSuccessResult))

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", email), ("role", role))
            await(addToken(underTest.addTeamMemberAction(applicationId))(request))

            verify(mockApplicationService)
              .addTeamMember(eqTo(application.application), eqTo(Collaborator(email, CollaboratorRole.DEVELOPER)), eqTo("superUserName"))(*)
            verifyAuthConnectorCalledForUser
          }

          "redirect back to manageTeamMembers when the service call is successful" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            given(mockApplicationService.addTeamMember(*, *, *)(*))
              .willReturn(Future.successful(ApplicationUpdateSuccessResult))

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", email), ("role", role))
            val result = await(addToken(underTest.addTeamMemberAction(applicationId))(request))

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value}/team-members")
            verifyAuthConnectorCalledForUser
          }

          "show 400 BadRequest when the service call fails with TeamMemberAlreadyExists" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            given(mockApplicationService.addTeamMember(*, *, *)(*))
              .willReturn(Future.failed(new TeamMemberAlreadyExists))

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", email), ("role", role))
            val result = await(addToken(underTest.addTeamMemberAction(applicationId))(request))

            status(result) shouldBe BAD_REQUEST
          }
        }

        "the form is invalid" should {
          "show 400 BadRequest when the email is invalid" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("email", "NOT AN EMAIL ADDRESS"),
              ("role", "DEVELOPER"))

            val result = await(addToken(underTest.addTeamMemberAction(applicationId))(request))

            status(result) shouldBe BAD_REQUEST
          }

          "show 400 BadRequest when the role is invalid" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("email", email),
              ("role", ""))

            val result = await(addToken(underTest.addTeamMemberAction(applicationId))(request))

            status(result) shouldBe BAD_REQUEST
          }
        }
      }

      "the user is not a superuser" when {
        "manging a privileged app" should {
          "show 403 Forbidden" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(privilegedApplication)

            val request = aLoggedInRequest.withFormUrlEncodedBody(
              ("email", email),
              ("role", "DEVELOPER"))

            val result = await(addToken(underTest.addTeamMemberAction(applicationId))(request))

            status(result) shouldBe FORBIDDEN
          }
        }

        "managing an ROPC app" should {
          "show 403 Forbidden" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(ropcApplication)

            val request = aLoggedInRequest.withFormUrlEncodedBody(
              ("email", email),
              ("role", "DEVELOPER"))

            val result = await(addToken(underTest.addTeamMemberAction(applicationId))(request))

            status(result) shouldBe FORBIDDEN
          }
        }

        "managing a standard app" should {
          "show 303 See Other when valid" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned()

            given(mockApplicationService.addTeamMember(*, *, *)(*))
              .willReturn(Future.successful(ApplicationUpdateSuccessResult))

            val request = aLoggedInRequest.withFormUrlEncodedBody(
              ("email", email),
              ("role", "DEVELOPER"))

            val result = await(addToken(underTest.addTeamMemberAction(applicationId))(request))

            status(result) shouldBe SEE_OTHER
            verifyAuthConnectorCalledForUser
          }
        }
      }
    }

    "removeTeamMember" when {
      val email = "email@example.com"

      "the user is a superuser" when {
        "the form is valid" should {
          "show the remove team member page successfully with the provided email address" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", email))
            val result = await(addToken(underTest.removeTeamMember(applicationId))(request))

            status(result) shouldBe OK
            bodyOf(result) should include(email)
            verifyAuthConnectorCalledForUser
          }
        }

        "the form is invalid" should {
          "show a 400 Bad Request" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", "NOT AN EMAIL ADDRESS"))
            val result = await(addToken(underTest.removeTeamMember(applicationId))(request))

            status(result) shouldBe BAD_REQUEST
          }
        }
      }

      "the user is not a superuser" when {
        "managing a privileged app" should {
          "show 403 Forbidden" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(privilegedApplication)

            val request = aLoggedInRequest.withFormUrlEncodedBody(("email", email))
            val result = await(addToken(underTest.removeTeamMember(applicationId))(request))

            status(result) shouldBe FORBIDDEN
          }
        }

        "managing an ROPC app" should {
          "show 403 Forbidden" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(ropcApplication)

            val request = aLoggedInRequest.withFormUrlEncodedBody(("email", email))
            val result = await(addToken(underTest.removeTeamMember(applicationId))(request))

            status(result) shouldBe FORBIDDEN
          }
        }

        "managing a standard app" should {
          "show 200 OK" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned()

            val request = aLoggedInRequest.withFormUrlEncodedBody(("email", email))
            val result = await(addToken(underTest.removeTeamMember(applicationId))(request))

            status(result) shouldBe OK
            verifyAuthConnectorCalledForUser
          }
        }
      }
    }

    "removeTeamMemberAction" when {
      val email = "email@example.com"

      "the user is a superuser" when {
        "the form is valid" when {
          "the action is not confirmed" should {
            val confirm = "No"

            "redirect back to the manageTeamMembers page" in new Setup {
              givenTheUserIsAuthorisedAndIsASuperUser()
              givenTheAppWillBeReturned()

              val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", email), ("confirm", confirm))
              val result = await(addToken(underTest.removeTeamMemberAction(applicationId))(request))

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value}/team-members")
              verifyAuthConnectorCalledForUser
            }
          }

          "the action is confirmed" should {
            val confirm = "Yes"

            "call the service with the correct params" in new Setup {
              givenTheUserIsAuthorisedAndIsASuperUser()
              givenTheAppWillBeReturned()

              given(mockApplicationService.removeTeamMember(*, *, *)(*))
                .willReturn(Future.successful(ApplicationUpdateSuccessResult))

              val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", email), ("confirm", confirm))
              val result = await(addToken(underTest.removeTeamMemberAction(applicationId))(request))

              status(result) shouldBe SEE_OTHER

              verify(mockApplicationService).removeTeamMember(eqTo(application.application), eqTo(email), eqTo("superUserName"))(*)
              verifyAuthConnectorCalledForUser
            }

            "show a 400 Bad Request when the service fails with TeamMemberLastAdmin" in new Setup {
              givenTheUserIsAuthorisedAndIsASuperUser()
              givenTheAppWillBeReturned()

              given(mockApplicationService.removeTeamMember(*, *, *)(*))
                .willReturn(Future.failed(new TeamMemberLastAdmin))

              val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", email), ("confirm", confirm))
              val result = await(addToken(underTest.removeTeamMemberAction(applicationId))(request))

              status(result) shouldBe BAD_REQUEST
            }

            "redirect to the manageTeamMembers page when the service call is successful" in new Setup {
              givenTheUserIsAuthorisedAndIsASuperUser()
              givenTheAppWillBeReturned()

              given(mockApplicationService.removeTeamMember(*, *, *)(*))
                .willReturn(Future.successful(ApplicationUpdateSuccessResult))

              val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", email), ("confirm", confirm))
              val result = await(addToken(underTest.removeTeamMemberAction(applicationId))(request))

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value}/team-members")
              verifyAuthConnectorCalledForUser
            }
          }
        }

        "the form is invalid" should {
          "show 400 Bad Request" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", "NOT AN EMAIL ADDRESS"))
            val result = await(addToken(underTest.removeTeamMemberAction(applicationId))(request))

            status(result) shouldBe BAD_REQUEST
          }
        }
      }

      "the user is not a superuser" when {
        "when managing a privileged app" should {
          "show 403 forbidden" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(privilegedApplication)

            val request = aLoggedInRequest.withFormUrlEncodedBody(("email", email), ("confirm", "Yes"))
            val result = await(addToken(underTest.removeTeamMemberAction(applicationId))(request))

            status(result) shouldBe FORBIDDEN
          }
        }

        "when managing an ROPC app" should {
          "show 403 Forbidden" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(privilegedApplication)

            val request = aLoggedInRequest.withFormUrlEncodedBody(("email", email), ("confirm", "Yes"))
            val result = await(addToken(underTest.removeTeamMemberAction(applicationId))(request))

            status(result) shouldBe FORBIDDEN
          }
        }

        "when managing a standard app" should {
          "show 303 OK" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned()
            given(mockApplicationService.removeTeamMember(*, *, *)(*))
              .willReturn(Future.successful(ApplicationUpdateSuccessResult))

            val request = aLoggedInRequest.withFormUrlEncodedBody(("email", email), ("confirm", "Yes"))
            val result = await(addToken(underTest.removeTeamMemberAction(applicationId))(request))

            status(result) shouldBe SEE_OTHER
          }
        }
      }
    }

    "blockApplicationPage" should {

      "return the page for block app if admin" in new Setup {
        givenTheUserIsAuthorisedAndIsAnAdmin()
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.blockApplicationPage(applicationId))(anAdminLoggedInRequest))

        status(result) shouldBe OK
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), *)(*, *)
        verifyAuthConnectorCalledForAdmin
      }


      "return forbidden for a non-admin" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.blockApplicationPage(applicationId))(aSuperUserLoggedInRequest))

        status(result) shouldBe FORBIDDEN
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), *)(*, *)

      }
    }

    "blockApplicationAction" should {
      "call the service to block application when a valid form is submitted for an admin" in new Setup {
        givenTheUserIsAuthorisedAndIsAnAdmin()
        givenTheAppWillBeReturned()

        given(mockApplicationService.blockApplication(*, *)(*))
          .willReturn(Future.successful(ApplicationBlockSuccessResult))

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("applicationNameConfirmation" -> application.application.name)

        val result = await(addToken(underTest.blockApplicationAction(applicationId))(request))

        status(result) shouldBe OK

        verify(mockApplicationService).blockApplication(eqTo(basicApplication), *)(*)
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), *)(*, *)
        verifyAuthConnectorCalledForAdmin
      }

      "return a bad request when an invalid form is submitted for an admin user" in new Setup {
        givenTheUserIsAuthorisedAndIsAnAdmin()
        givenTheAppWillBeReturned()

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody()

        val result = await(addToken(underTest.blockApplicationAction(applicationId))(request))

        status(result) shouldBe BAD_REQUEST

        verify(mockApplicationService, never).blockApplication(*, *)(*)
        verifyAuthConnectorCalledForAdmin
      }

      "return forbidden when a form is submitted for a non-admin user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned()

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("applicationNameConfirmation" -> application.application.name)

        val result = await(addToken(underTest.blockApplicationAction(applicationId))(request))

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).blockApplication(*, *)(*)
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), *)(*, *)
      }

    }

    "unblockApplicationPage" should {

      "return the page for unblock app if admin" in new Setup {
        givenTheUserIsAuthorisedAndIsAnAdmin()
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.unblockApplicationPage(applicationId))(anAdminLoggedInRequest))

        status(result) shouldBe OK
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), *)(*, *)
        verifyAuthConnectorCalledForAdmin
      }


      "return forbidden for a non-admin" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.unblockApplicationPage(applicationId))(aSuperUserLoggedInRequest))

        status(result) shouldBe FORBIDDEN
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), *)(*, *)

      }
    }

    "unblockApplicationAction" should {
      "call the service to unblock application when a valid form is submitted for an admin" in new Setup {
        givenTheUserIsAuthorisedAndIsAnAdmin()
        givenTheAppWillBeReturned()

        given(mockApplicationService.unblockApplication(*, *)(*))
          .willReturn(Future.successful(ApplicationUnblockSuccessResult))

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("applicationNameConfirmation" -> application.application.name)

        val result = await(addToken(underTest.unblockApplicationAction(applicationId))(request))

        status(result) shouldBe OK

        verify(mockApplicationService).unblockApplication(eqTo(basicApplication), *)(*)
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), *)(*, *)
        verifyAuthConnectorCalledForAdmin
      }

      "return a bad request when an invalid form is submitted for an admin user" in new Setup {
        givenTheUserIsAuthorisedAndIsAnAdmin()
        givenTheAppWillBeReturned()

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody()

        val result = await(addToken(underTest.unblockApplicationAction(applicationId))(request))

        status(result) shouldBe BAD_REQUEST

        verify(mockApplicationService, never).unblockApplication(*, *)(*)
        verifyAuthConnectorCalledForAdmin
      }

      "return forbidden when a form is submitted for a non-admin user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned()

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("applicationNameConfirmation" -> application.application.name)

        val result = await(addToken(underTest.unblockApplicationAction(applicationId))(request))

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).unblockApplication(*, *)(*)
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), *)(*, *)
      }

    }

    def assertIncludesOneError(result: Result, message: String) = {

      val body = bodyOf(result)

      body should include(message)
      assert(Jsoup.parse(body).getElementsByClass("form-field--error").size == 1)
    }

    def aPaginatedApplicationResponse(applications: Seq[ApplicationResponse]): PaginatedApplicationResponse = {
      val page = 1
      val pageSize = 10
      PaginatedApplicationResponse(applications, page, pageSize, total = applications.size, matching = applications.size)
    }
  }
}
