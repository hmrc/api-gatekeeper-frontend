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

package uk.gov.hmrc.gatekeeper.controllers

import uk.gov.hmrc.gatekeeper.models.Environment._
import uk.gov.hmrc.gatekeeper.models.RateLimitTier.RateLimitTier
import uk.gov.hmrc.gatekeeper.models._
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.TokenProvider
import uk.gov.hmrc.gatekeeper.services.SubscriptionFieldsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.utils.{TitleChecker, WithCSRFAddToken}
import uk.gov.hmrc.gatekeeper.views.html.applications._
import uk.gov.hmrc.gatekeeper.views.html.approvedApplication.ApprovedView
import uk.gov.hmrc.gatekeeper.views.html.review.ReviewView
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}
  
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.successful

import uk.gov.hmrc.gatekeeper.models.applications.ApplicationWithSubscriptionData
import mocks.services.ApplicationServiceMockProvider
import uk.gov.hmrc.gatekeeper.builder.{ApiBuilder, ApplicationBuilder}
import uk.gov.hmrc.gatekeeper.utils.CollaboratorTracker
import org.mockito.captor.ArgCaptor
import mocks.connectors.ApplicationConnectorMockProvider
import org.joda.time.DateTime
import uk.gov.hmrc.gatekeeper.config.ErrorHandler
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationServiceMockModule
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.services.LdapAuthorisationServiceMockModule

class ApplicationControllerSpec 
    extends ControllerBaseSpec 
    with WithCSRFAddToken 
    with TitleChecker 
    with CollaboratorTracker {

  implicit val materializer = app.materializer

  private lazy val errorTemplateView = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView = app.injector.instanceOf[ForbiddenView]
  private lazy val applicationsView = app.injector.instanceOf[ApplicationsView]
  private lazy val applicationView = app.injector.instanceOf[ApplicationView]
  private lazy val manageAccessOverridesView = app.injector.instanceOf[ManageAccessOverridesView]
  private lazy val manageScopesView = app.injector.instanceOf[ManageScopesView]
  private lazy val ipAllowlistView = app.injector.instanceOf[IpAllowlistView]
  private lazy val manageIpAllowlistView = app.injector.instanceOf[ManageIpAllowlistView]
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
  private lazy val manageGrantLengthView = app.injector.instanceOf[ManageGrantLengthView]
  private lazy val manageGrantLengthSuccessView = app.injector.instanceOf[ManageGrantLengthSuccessView]
  private lazy val errorHandler = app.injector.instanceOf[ErrorHandler]
 
  running(app) {

    trait Setup extends ControllerSetupBase with ApplicationServiceMockProvider with ApplicationConnectorMockProvider with StrideAuthorisationServiceMockModule with LdapAuthorisationServiceMockModule { 

      val csrfToken = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken
      override val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken).withCSRFToken
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, superUserToken).withCSRFToken
      override val anAdminLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, adminToken).withCSRFToken

      val applicationWithOverrides = ApplicationWithHistory(
        basicApplication.copy(access = Standard(overrides = Set(PersistLogin))), List.empty)

      val privilegedApplication = ApplicationWithHistory(
        basicApplication.copy(access = Privileged(scopes = Set("openid", "email"))), List.empty)

      val ropcApplication = ApplicationWithHistory(
        basicApplication.copy(access = Ropc(scopes = Set("openid", "email"))), List.empty)
      val mockSubscriptionFieldsService = mock[SubscriptionFieldsService]

      val developers = List[RegisteredUser] {
        new RegisteredUser("joe.bloggs@example.co.uk", UserId.random, "joe", "bloggs", false)
      }

      LdapAuthorisationServiceMock.Auth.notAuthorised

      val underTest = new ApplicationController(
        StrideAuthorisationServiceMock.aMock,
        mockApplicationService,
        forbiddenView,
        mockApiDefinitionService,
        mockDeveloperService,
        mcc,
        applicationsView,
        applicationView,
        manageAccessOverridesView,
        manageScopesView,
        ipAllowlistView,
        manageIpAllowlistView,
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
        manageGrantLengthView,
        manageGrantLengthSuccessView,
        mockApmService,
        errorHandler,
        LdapAuthorisationServiceMock.aMock
      )

      def givenThePaginatedApplicationsWillBeReturned = {
        ApplicationServiceMock.SearchApplications.returns()
        FetchAllApiDefinitions.inAny.returns()
      }

    }

    "applicationsPage" should {
      "on request with no specified environment all sandbox applications supplied" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenThePaginatedApplicationsWillBeReturned

        val eventualResult: Future[Result] = underTest.applicationsPage()(aLoggedInRequest)

        status(eventualResult) shouldBe OK
        titleOf(eventualResult) shouldBe "Unit Test Title - Applications"
        val responseBody = Helpers.contentAsString(eventualResult)
        responseBody should include("<h1 class=\"govuk-heading-l\" id=\"applications-title\">Applications</h1>")

        verify(mockApplicationService).searchApplications(eqTo(Some(SANDBOX)), *)(*)
        verify(mockApiDefinitionService).fetchAllApiDefinitions(eqTo(Some(SANDBOX)))(*)
      }

      "on request for production all production applications supplied" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenThePaginatedApplicationsWillBeReturned

        val eventualResult: Future[Result] = underTest.applicationsPage(environment = Some("PRODUCTION"))(aLoggedInRequest)

        status(eventualResult) shouldBe OK

        verify(mockApplicationService).searchApplications(eqTo(Some(PRODUCTION)), *)(*)
        verify(mockApiDefinitionService).fetchAllApiDefinitions(eqTo(Some(PRODUCTION)))(*)
      }

      "on request for sandbox all sandbox applications supplied" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenThePaginatedApplicationsWillBeReturned

        val eventualResult: Future[Result] = underTest.applicationsPage(environment = Some("SANDBOX"))(aLoggedInRequest)

        status(eventualResult) shouldBe OK

        verify(mockApplicationService).searchApplications(eqTo(Some(SANDBOX)), *)(*)
        verify(mockApiDefinitionService).fetchAllApiDefinitions(eqTo(Some(SANDBOX)))(*)
      }

      "pass requested params with default params and default environment of SANDBOX to the service" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
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
        val result = underTest.applicationsPage()(aLoggedInRequestWithParams)

        status(result) shouldBe OK

        verify(mockApplicationService).searchApplications(eqTo(Some(SANDBOX)), eqTo(expectedParams))(*)
      }

      "redirect to the login page if the user is not logged in" in new Setup {
        StrideAuthorisationServiceMock.Auth.sessionRecordNotFound()

        val result = underTest.applicationsPage()(aLoggedInRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe
          Some("http://example.com")
      }

      "show button to add Privileged or ROPC app to superuser" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenThePaginatedApplicationsWillBeReturned

        val result = underTest.applicationsPage()(aSuperUserLoggedInRequest)
        status(result) shouldBe OK

        val body = contentAsString(result)

        body should include("Add privileged or ROPC application")

      }

      "not show button to add Privileged or ROPC app to non-superuser" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenThePaginatedApplicationsWillBeReturned
        
        val result = underTest.applicationsPage()(aLoggedInRequest)
        status(result) shouldBe OK

        val body = contentAsString(result)

        body shouldNot include("Add privileged or ROPC application")

      }
    }

    "applicationsPageExportCsv" should {
      "return csv data" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        
        val applicationResponse = ApplicationResponse(
            ApplicationId("c702a8f8-9b7c-4ddb-8228-e812f26a2f1e"),
            ClientId("9ee77d73-a65a-4e87-9cda-67863911e02f"),
            "the-gateway-id",
            "App Name",
            deployedTo = "SANDBOX",
            description = None,
            collaborators = Set.empty,
            createdOn = DateTime.parse("2001-02-03T12:01:02Z"),
            lastAccess = Some(DateTime.parse("2002-02-03T12:01:02Z")),
            Standard(),
            ApplicationState(),
            grantLength)

        ApplicationServiceMock.SearchApplications.returns(applicationResponse)

        val eventualResult: Future[Result] = underTest.applicationsPageCsv()(aLoggedInRequest)

        status(eventualResult) shouldBe OK
        
        val expectedCsvContent = """page: 1 of 1 from 1 results
Name,App ID,Client ID,Gateway ID,Environment,Status,Rate limit tier,Access type,Blocked,Has IP Allow List,Submitted/Created on,Last API call
App Name,c702a8f8-9b7c-4ddb-8228-e812f26a2f1e,9ee77d73-a65a-4e87-9cda-67863911e02f,the-gateway-id,SANDBOX,Created,BRONZE,STANDARD,false,false,2001-02-03T12:01:02.000Z,2002-02-03T12:01:02.000Z
"""

        val responseBody = Helpers.contentAsString(eventualResult)
        responseBody shouldBe expectedCsvContent
        
      }
    }

    "resendVerification" should {
      "call backend with correct application id and gatekeeper id when resend verification is invoked" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenTheAppWillBeReturned()

        val appCaptor = ArgCaptor[Application]
        val gatekeeperIdCaptor = ArgCaptor[String]
        when(mockApplicationService.resendVerification(*,*)(*)).thenReturn(successful(ResendVerificationSuccessful))

        await(underTest.resendVerification(applicationId)(aLoggedInRequest))

        verify(mockApplicationService).resendVerification(appCaptor, gatekeeperIdCaptor)(*)
        appCaptor hasCaptured basicApplication
        gatekeeperIdCaptor hasCaptured "Bobby Example"
      }
    }

    "manageScopes" should {
      "fetch an app with Privileged access for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        ApplicationServiceMock.FetchApplication.returns(privilegedApplication)

        val result = addToken(underTest.manageScopes(applicationId))(aSuperUserLoggedInRequest)

        status(result) shouldBe OK
      }

      "fetch an app with ROPC access for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        ApplicationServiceMock.FetchApplication.returns(ropcApplication)

        val result = addToken(underTest.manageScopes(applicationId))(aSuperUserLoggedInRequest)

        status(result) shouldBe OK
      }

      "return an error for a Standard app" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        ApplicationServiceMock.FetchApplication.returns(application)

        intercept[RuntimeException] {
          await(addToken(underTest.manageScopes(applicationId))(aSuperUserLoggedInRequest))
        }
      }

      "return forbidden for a non-super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments()

        ApplicationServiceMock.FetchApplication.returns(application)

        val result = addToken(underTest.manageScopes(applicationId))(aLoggedInRequest)

        status(result) shouldBe FORBIDDEN
      }
    }

    "updateScopes" should {
      "call the service to update scopes when a valid form is submitted for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()

        ApplicationServiceMock.UpdateScopes.succeeds()

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("scopes" -> "hello, individual-benefits")
        val result = addToken(underTest.updateScopes(applicationId))(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value}")

        verify(mockApplicationService)
          .updateScopes(eqTo(application.application), eqTo(Set("hello", "individual-benefits")))(*)

      }

      "return a bad request when an invalid form is submitted for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("scopes" -> "")
        val result = addToken(underTest.updateScopes(applicationId))(request)

        status(result) shouldBe BAD_REQUEST

        verify(mockApplicationService, never).updateScopes(*, *)(*)
      }

      "return a bad request when the service indicates that the scopes are invalid" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()

        ApplicationServiceMock.UpdateScopes.failsWithInvalidScopes()

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("scopes" -> "hello")
        val result = addToken(underTest.updateScopes(applicationId))(request)

        status(result) shouldBe BAD_REQUEST
      }

      "return forbidden when a form is submitted for a non-super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments()

        givenTheAppWillBeReturned()

        val request = aLoggedInRequest.withFormUrlEncodedBody()
        val result = addToken(underTest.updateScopes(applicationId))(request)

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).updateScopes(*, *)(*)
      }
    }

    "viewIpAllowlistPage" should {
      "return the view IP allowlist page for a normal user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenTheAppWillBeReturned()

        val result = underTest.viewIpAllowlistPage(applicationId)(aLoggedInRequest)

        status(result) shouldBe OK
        contentAsString(result) should include("View IP allow list")
      }
    }

    "manageGrantLengthPage" should {
      "return the manage grant length page for an admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        givenTheAppWillBeReturned()

        val result = underTest.manageGrantLength(applicationId)(anAdminLoggedInRequest)

        status(result) shouldBe OK
        contentAsString(result) should include("Manage application grant length")
      }

      "return the manage grant length page for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()

        val result = underTest.manageGrantLength(applicationId)(aSuperUserLoggedInRequest)

        status(result) shouldBe OK
        contentAsString(result) should include("Manage application grant length")
      }

      "return the forbidden page for a normal user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments()

        givenTheAppWillBeReturned()

        val result = underTest.manageGrantLength(applicationId)(aLoggedInRequest)

        status(result) shouldBe FORBIDDEN
        contentAsString(result) should include("You do not have permission")
      }
    }

    "updateGrantLength" should {
      "call the service to update the grant length when a valid form is submitted for an admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        givenTheAppWillBeReturned()

        ApplicationServiceMock.UpdateGrantLength.succeeds()

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("grantLength" -> "547")

        val result = addToken(underTest.updateGrantLength(applicationId))(request)

        status(result) shouldBe OK

        verify(mockApplicationService).updateGrantLength(eqTo(basicApplication), eqTo(GrantLength.EIGHTEEN_MONTHS))(*)
      }

      "return a bad request when an invalid form is submitted for an admin user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        givenTheAppWillBeReturned()

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody()

        val result = addToken(underTest.updateGrantLength(applicationId))(request)

        status(result) shouldBe BAD_REQUEST

        verify(mockApplicationService, never).updateGrantLength(*, *)(*)
      }

      "return forbidden when a form is submitted for a non-admin user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments()

        givenTheAppWillBeReturned()

        val request = aLoggedInRequest.withFormUrlEncodedBody("grantLength" -> "547")

        val result = addToken(underTest.updateGrantLength(applicationId))(request)

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).updateGrantLength(*, *)(*)
      }
    }

    "manageIpAllowlistPage" should {
      "return the manage IP allowlist page for an admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        givenTheAppWillBeReturned()

        val result = underTest.manageIpAllowlistPage(applicationId)(anAdminLoggedInRequest)

        status(result) shouldBe OK
        contentAsString(result) should include("Manage IP allow list")
      }

      "return the manage IP allowlist page for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()

        val result = underTest.manageIpAllowlistPage(applicationId)(aSuperUserLoggedInRequest)

        status(result) shouldBe OK
        contentAsString(result) should include("Manage IP allow list")
      }

      "return the forbidden page for a normal user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments()

        givenTheAppWillBeReturned()

        val result = underTest.manageIpAllowlistPage(applicationId)(aLoggedInRequest)

        status(result) shouldBe FORBIDDEN
        contentAsString(result) should include("You do not have permission")
      }
    }

    "manageIpAllowlistAction" should {
      val allowlistedIpToUpdate: String = "1.1.1.0/24"
      val required: Boolean = false

      "manage the IP allowlist using the app service for an admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        givenTheAppWillBeReturned()
        ApplicationServiceMock.ManageIpAllowlist.succeeds()
        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("required"-> required.toString, "allowlistedIps" -> allowlistedIpToUpdate)

        val result = underTest.manageIpAllowlistAction(applicationId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value}")
        verify(mockApplicationService).manageIpAllowlist(eqTo(application.application), eqTo(required), eqTo(Set(allowlistedIpToUpdate)))(*)
      }

      "manage the IP allowlist using the app service for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()
        ApplicationServiceMock.ManageIpAllowlist.succeeds()
        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("required"-> required.toString, "allowlistedIps" -> allowlistedIpToUpdate)

        val result = underTest.manageIpAllowlistAction(applicationId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value}")
        verify(mockApplicationService).manageIpAllowlist(eqTo(application.application), eqTo(required), eqTo(Set(allowlistedIpToUpdate)))(*)
      }

      "clear the IP allowlist when allowlistedIps is empty" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()
        ApplicationServiceMock.ManageIpAllowlist.succeeds()
        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("required"-> required.toString, "allowlistedIps" -> "")

        val result = underTest.manageIpAllowlistAction(applicationId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value}")
        verify(mockApplicationService).manageIpAllowlist(eqTo(application.application), eqTo(required), eqTo(Set()))(*)
      }

      "fail validation when clearing a required IP allowlist" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()
        ApplicationServiceMock.ManageIpAllowlist.succeeds()
        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("required"-> "true", "allowlistedIps" -> "")

        val result = underTest.manageIpAllowlistAction(applicationId)(request)

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("The IP allow list is mandatory for this application")
        verify(mockApplicationService, times(0)).manageIpAllowlist(*, *, *)(*)
      }

      "return bad request for invalid values" in new Setup {
        val invalidAllowlistedIps: Seq[String] = Seq(
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

        invalidAllowlistedIps.foreach { invalidAllowlistedIp =>
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
          givenTheAppWillBeReturned()
          val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("required"-> required.toString, "allowlistedIps" -> invalidAllowlistedIp)

          val result = underTest.manageIpAllowlistAction(applicationId)(request)

          status(result) shouldBe BAD_REQUEST
          verify(mockApplicationService, times(0)).manageIpAllowlist(*, *, *)(*)
        }
      }

      "return the forbidden page for a normal user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments()

        givenTheAppWillBeReturned()
        val request = aLoggedInRequest.withFormUrlEncodedBody("required"-> required.toString, "allowlistedIps" -> allowlistedIpToUpdate)

        val result = underTest.manageIpAllowlistAction(applicationId)(request)

        status(result) shouldBe FORBIDDEN
        contentAsString(result) should include("You do not have permission")
      }
    }

    "manageOverrides" should {
      "fetch an app with Standard access for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        ApplicationServiceMock.FetchApplication.returns(application)

        val result = addToken(underTest.manageAccessOverrides(applicationId))(aSuperUserLoggedInRequest)

        status(result) shouldBe OK
      }

      "return an error for a ROPC app" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        ApplicationServiceMock.FetchApplication.returns(ropcApplication)

        val result = addToken(underTest.manageAccessOverrides(applicationId))(aSuperUserLoggedInRequest)
        status(result) shouldBe BAD_REQUEST
      }

      "return an error for a Privileged app" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        ApplicationServiceMock.FetchApplication.returns(privilegedApplication)

        val result = addToken(underTest.manageAccessOverrides(applicationId))(aSuperUserLoggedInRequest)
        status(result) shouldBe BAD_REQUEST
      }

      "return forbidden for a non-super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments()

        ApplicationServiceMock.FetchApplication.returns(application)

        val result = addToken(underTest.manageAccessOverrides(applicationId))(aLoggedInRequest)

        status(result) shouldBe FORBIDDEN
      }
    }

    "updateOverrides" should {
      "call the service to update overrides when a valid form is submitted for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()

        ApplicationServiceMock.UpdateOverrides.succeeds()

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
          "persistLoginEnabled" -> "true",
          "grantWithoutConsentEnabled" -> "true", "grantWithoutConsentScopes" -> "hello, individual-benefits",
          "suppressIvForAgentsEnabled" -> "true", "suppressIvForAgentsScopes" -> "openid, email",
          "suppressIvForOrganisationsEnabled" -> "true", "suppressIvForOrganisationsScopes" -> "address, openid:mdtp",
          "suppressIvForIndividualsEnabled" -> "true", "suppressIvForIndividualsScopes" -> "email, openid:hmrc-enrolments")

        val result = addToken(underTest.updateAccessOverrides(applicationId))(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value}")

        verify(mockApplicationService).updateOverrides(
          eqTo(application.application),
          eqTo(Set(
            PersistLogin,
            GrantWithoutConsent(Set("hello", "individual-benefits")),
            SuppressIvForAgents(Set("openid", "email")),
            SuppressIvForOrganisations(Set("address", "openid:mdtp")),
            SuppressIvForIndividuals(Set("email", "openid:hmrc-enrolments"))
          )))(*)

      }

      "return a bad request when an invalid form is submitted for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
          "persistLoginEnabled" -> "true",
          "grantWithoutConsentEnabled" -> "true", "grantWithoutConsentScopes" -> "")

        val result = addToken(underTest.updateAccessOverrides(applicationId))(request)

        status(result) shouldBe BAD_REQUEST

        verify(mockApplicationService, never).updateOverrides(*, *)(*)

      }

      "return forbidden when a form is submitted for a non-super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments()

        givenTheAppWillBeReturned()

        val request = aLoggedInRequest.withFormUrlEncodedBody("persistLoginEnabled" -> "true")

        val result = addToken(underTest.updateAccessOverrides(applicationId))(request)

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).updateOverrides(*, *)(*)
      }
    }


    "manageRateLimitTier" should {
      "fetch the app and return the page for an admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        ApplicationServiceMock.FetchApplication.returns(application)

        val result = addToken(underTest.manageRateLimitTier(applicationId))(anAdminLoggedInRequest)

        status(result) shouldBe OK
        // verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), *)(*, *)

      }

      "return forbidden for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments()

        ApplicationServiceMock.FetchApplication.returns(application)

        val result = addToken(underTest.manageRateLimitTier(applicationId))(aSuperUserLoggedInRequest)

        status(result) shouldBe FORBIDDEN
      }

      "return forbidden for a user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments()

        ApplicationServiceMock.FetchApplication.returns(application)

        val result = addToken(underTest.manageRateLimitTier(applicationId))(aLoggedInRequest)

        status(result) shouldBe FORBIDDEN
      }
    }

    "updateRateLimitTier" should {
      "call the service to update the rate limit tier when a valid form is submitted for an admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        givenTheAppWillBeReturned()

        ApplicationServiceMock.UpdateRateLimitTier.succeeds()

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("tier" -> "GOLD")

        val result = addToken(underTest.updateRateLimitTier(applicationId))(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value}")

        verify(mockApplicationService).updateRateLimitTier(eqTo(basicApplication), eqTo(RateLimitTier.GOLD))(*)
      }

      "return a bad request when an invalid form is submitted for an admin user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        givenTheAppWillBeReturned()

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody()

        val result = addToken(underTest.updateRateLimitTier(applicationId))(request)

        status(result) shouldBe BAD_REQUEST

        verify(mockApplicationService, never).updateRateLimitTier(*, *)(*)
      }

      "return forbidden when a form is submitted for a non-admin user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments()

        givenTheAppWillBeReturned()

        val request = aLoggedInRequest.withFormUrlEncodedBody("tier" -> "GOLD")

        val result = addToken(underTest.updateRateLimitTier(applicationId))(request)

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).updateRateLimitTier(*, *)(*)
      }
    }

    "handleUplift" should {

      "call backend with correct application id and gatekeeper id when application is approved" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenTheAppWillBeReturned()

        val appCaptor = ArgumentCaptor.forClass(classOf[Application])
        val gatekeeperIdCaptor = ArgumentCaptor.forClass(classOf[String])
        when(mockApplicationService.approveUplift(appCaptor.capture(), gatekeeperIdCaptor.capture())(*))
          .thenReturn(successful(ApproveUpliftSuccessful))
        await(underTest.handleUplift(applicationId)(aLoggedInRequest.withFormUrlEncodedBody(("action", "APPROVE"))))
        appCaptor.getValue shouldBe basicApplication
        gatekeeperIdCaptor.getValue shouldBe "Bobby Example"

      }
    }

    "handleUpdateRateLimitTier" should {
      val tier = RateLimitTier.GOLD

      "change the rate limit for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        ApplicationServiceMock.FetchApplication.returns(application)

        val appCaptor = ArgumentCaptor.forClass(classOf[Application])
        val newTierCaptor = ArgumentCaptor.forClass(classOf[RateLimitTier])
        val hcCaptor = ArgumentCaptor.forClass(classOf[HeaderCarrier])

        when(mockApplicationService.updateRateLimitTier(appCaptor.capture(), newTierCaptor.capture())(hcCaptor.capture()))
          .thenReturn(successful(ApplicationUpdateSuccessResult))

        val result = underTest.handleUpdateRateLimitTier(applicationId)(aLoggedInRequest.withFormUrlEncodedBody(("tier", tier.toString)))
        status(result) shouldBe SEE_OTHER

        appCaptor.getValue shouldBe basicApplication
        newTierCaptor.getValue shouldBe tier

        verify(mockApplicationService, times(1)).updateRateLimitTier(basicApplication, tier)(hcCaptor.getValue)

      }

      "not call the application connector for a normal user " in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        ApplicationServiceMock.FetchApplication.returns(application)

        val result = underTest.handleUpdateRateLimitTier(applicationId)(aLoggedInRequest.withFormUrlEncodedBody(("tier", "GOLD")))
        status(result) shouldBe SEE_OTHER

        verify(mockApplicationService, never).updateRateLimitTier(*, *)(*)

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
      val privAccess = AppAccess(AccessType.PRIVILEGED, List.empty)
      val ropcAccess = AppAccess(AccessType.ROPC, List.empty)

      "with invalid form fields" can {
        "show the correct error message when no environment is chosen" in new Setup {
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)

          val result = addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", ""),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", appName),
              ("applicationDescription", description),
              ("adminEmail", adminEmail)))

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Tell us what environment")
        }

        "show the correct error message when no access type is chosen" in new Setup {
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)

          val result = addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("accessType", ""),
              ("applicationName", appName),
              ("applicationDescription", description),
              ("adminEmail", adminEmail)))

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Tell us what access type")
        }

        "show the correct error message when the app name is left empty" in new Setup {
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)

          val result = addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", ""),
              ("applicationDescription", description),
              ("adminEmail", adminEmail)))

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Provide an application name")

        }

        "show the correct error message when the new prod app name already exists in prod" in new Setup {
          val collaborators = Set("sample@example.com".asAdministratorCollaborator)
          val existingApp = ApplicationResponse(
            ApplicationId.random, ClientId.random, "gatewayId", "I Already Exist", "PRODUCTION", None, collaborators, DateTime.now(), Some(DateTime.now()), Standard(), ApplicationState(), grantLength)

          DeveloperServiceMock.SeekRegisteredUser.returnsFor(adminEmail)
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
          ApplicationServiceMock.FetchApplications.returns(existingApp)
          

          val result = addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", "I Already Exist"),
              ("applicationDescription", description),
              ("adminEmail", adminEmail)))

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Provide an application name that does not already exist")
        }

        "allow creation of a sandbox app even when the name already exists in production" in new Setup {
          val collaborators = Set("sample@example.com".asAdministratorCollaborator)
          val existingApp = ApplicationResponse(
            ApplicationId.random, ClientId.random, "gatewayId", "I Already Exist", "PRODUCTION", None, collaborators, DateTime.now(), Some(DateTime.now()), Standard(), ApplicationState(), grantLength)

          DeveloperServiceMock.SeekRegisteredUser.returnsFor(adminEmail)
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
          ApplicationServiceMock.FetchApplications.returns(existingApp)
          ApplicationServiceMock.CreatePrivOrROPCApp.returns(CreatePrivOrROPCAppSuccessResult(applicationId, "I Already Exist", "SANDBOX", clientId, totp, privAccess))

          val result = addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.SANDBOX.toString),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", "I Already Exist"),
              ("applicationDescription", description),
              ("adminEmail", adminEmail)))

          status(result) shouldBe OK

          contentAsString(result) should include("Application added")
          }

        "allow creation of a sandbox app if name already exists in sandbox" in new Setup {
          val collaborators = Set("sample@example.com".asAdministratorCollaborator)

          val existingApp = ApplicationResponse(
            ApplicationId.random, ClientId.random, "gatewayId", "I Already Exist", "SANDBOX", None, collaborators, DateTime.now(), Some(DateTime.now()), Standard(), ApplicationState(), grantLength)

          DeveloperServiceMock.SeekRegisteredUser.returnsFor(adminEmail)
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
          ApplicationServiceMock.FetchApplications.returns(existingApp)
          ApplicationServiceMock.CreatePrivOrROPCApp.returns(CreatePrivOrROPCAppSuccessResult(applicationId, "I Already Exist", "SANDBOX", clientId, totp, privAccess))

          val result = addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.SANDBOX.toString),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", "I Already Exist"),
              ("applicationDescription", description),
              ("adminEmail", adminEmail)))

          status(result) shouldBe OK

          contentAsString(result) should include("Application added")
          }

        "allow creation of a prod app if name already exists in sandbox" in new Setup {
          val collaborators = Set("sample@example.com".asAdministratorCollaborator)
          val existingApp = ApplicationResponse(
            ApplicationId.random, ClientId.random, "gatewayId", "I Already Exist", "SANDBOX", None, collaborators, DateTime.now(), Some(DateTime.now()), Standard(), ApplicationState(), grantLength)

          DeveloperServiceMock.SeekRegisteredUser.returnsFor(adminEmail)
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
          ApplicationServiceMock.FetchApplications.returns(existingApp)
          ApplicationServiceMock.CreatePrivOrROPCApp.returns(CreatePrivOrROPCAppSuccessResult(applicationId, "I Already Exist", "PRODUCTION", clientId, totp, privAccess))

          val result = addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", "I Already Exist"),
              ("applicationDescription", description),
              ("adminEmail", adminEmail)))

          status(result) shouldBe OK

          contentAsString(result) should include("Application added")
          }

        "show the correct error message when app description is left empty" in new Setup {
          DeveloperServiceMock.SeekRegisteredUser.returnsFor("a@example.com")
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)

          val result = addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", appName),
              ("applicationDescription", ""),
              ("adminEmail", adminEmail)))

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Provide an application description")
        }

        "show the correct error message when admin email is left empty" in new Setup {
          DeveloperServiceMock.SeekRegisteredUser.returnsFor("a@example.com")
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)

          val result = addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", appName),
              ("applicationDescription", description),
              ("adminEmail", "")))

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Provide an email address")
        }

        "show the correct error message when admin email is invalid" in new Setup {
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)

          val result = addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", appName),
              ("applicationDescription", description),
              ("adminEmail", "notAValidEmailAddress")))

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Provide a valid email address")
        }
      }

      "with valid form fields" can {
        "but the user is not a superuser" should {
          "show 403 forbidden" in new Setup {
            val email = "a@example.com"
            DeveloperServiceMock.SeekRegisteredUser.returnsFor(email)
            StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments()


            val result = addToken(underTest.createPrivOrROPCApplicationAction())(
              aLoggedInRequest.withFormUrlEncodedBody(
                ("environment", Environment.PRODUCTION.toString),
                ("accessType", privilegedAccessType.toString),
                ("applicationName", appName),
                ("applicationDescription", description),
                ("adminEmail", email)))

            status(result) shouldBe FORBIDDEN
          }
        }

        "and the user is a superuser" should {
          "show the success page for a priv app in production" in new Setup {
            DeveloperServiceMock.SeekRegisteredUser.returnsFor("a@example.com")
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
            ApplicationServiceMock.FetchApplications.returns()
            ApplicationServiceMock.CreatePrivOrROPCApp.returns(CreatePrivOrROPCAppSuccessResult(applicationId, appName, "PRODUCTION", clientId, totp, privAccess))

            val result = addToken(underTest.createPrivOrROPCApplicationAction())(
              aSuperUserLoggedInRequest.withFormUrlEncodedBody(
                ("environment", Environment.PRODUCTION.toString),
                ("accessType", privilegedAccessType.toString),
                ("applicationName", appName),
                ("applicationDescription", description),
                ("adminEmail", "a@example.com")))

            status(result) shouldBe OK

            contentAsString(result) should include(appName)
            contentAsString(result) should include("Application added")
            contentAsString(result) should include("This is your only chance to copy and save this application's TOTP.")
            contentAsString(result) should include(applicationId.value)
            contentAsString(result) should include("Production")
            contentAsString(result) should include("Privileged")
            contentAsString(result) should include(totpSecret)
            contentAsString(result) should include(clientId.value)
    
          }

          "show the success page for a priv app in sandbox" in new Setup {
            DeveloperServiceMock.SeekRegisteredUser.returnsFor("a@example.com")
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
            ApplicationServiceMock.FetchApplications.returns()
            ApplicationServiceMock.CreatePrivOrROPCApp.returns(CreatePrivOrROPCAppSuccessResult(applicationId, appName, "SANDBOX", clientId, totp, privAccess))

            val result = addToken(underTest.createPrivOrROPCApplicationAction())(
              aSuperUserLoggedInRequest.withFormUrlEncodedBody(
                ("environment", Environment.SANDBOX.toString),
                ("accessType", privilegedAccessType.toString),
                ("applicationName", appName),
                ("applicationDescription", description),
                ("adminEmail", "a@example.com")))

            status(result) shouldBe OK

            contentAsString(result) should include(appName)
            contentAsString(result) should include("Application added")
            contentAsString(result) should include("This is your only chance to copy and save this application's TOTP.")
            contentAsString(result) should include(applicationId.value)
            contentAsString(result) should include("Sandbox")
            contentAsString(result) should include("Privileged")
            contentAsString(result) should include(totpSecret)
            contentAsString(result) should include(clientId.value)
              }

          "show the success page for an ROPC app in production" in new Setup {
            DeveloperServiceMock.SeekRegisteredUser.returnsFor("a@example.com")
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
            ApplicationServiceMock.FetchApplications.returns()
            ApplicationServiceMock.CreatePrivOrROPCApp.returns(CreatePrivOrROPCAppSuccessResult(applicationId, appName, "PRODUCTION", clientId, None, ropcAccess))

            val result = addToken(underTest.createPrivOrROPCApplicationAction())(
              aSuperUserLoggedInRequest.withFormUrlEncodedBody(
                ("environment", Environment.PRODUCTION.toString),
                ("accessType", ropcAccessType.toString),
                ("applicationName", appName),
                ("applicationDescription", description),
                ("adminEmail", "a@example.com")))

            status(result) shouldBe OK

            contentAsString(result) should include(appName)
            contentAsString(result) should include("Application added")
            contentAsString(result) should include(applicationId.value)
            contentAsString(result) should include("Production")
            contentAsString(result) should include("ROPC")
            contentAsString(result) should include(clientId.value)
              }

          "show the success page for an ROPC app in sandbox" in new Setup {
            DeveloperServiceMock.SeekRegisteredUser.returnsFor("a@example.com")
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
            ApplicationServiceMock.FetchApplications.returns()
            ApplicationServiceMock.CreatePrivOrROPCApp.returns(CreatePrivOrROPCAppSuccessResult(applicationId, appName, "SANDBOX", clientId, None, ropcAccess))

            val result = addToken(underTest.createPrivOrROPCApplicationAction())(
              aSuperUserLoggedInRequest.withFormUrlEncodedBody(
                ("environment", Environment.SANDBOX.toString),
                ("accessType", ropcAccessType.toString),
                ("applicationName", appName),
                ("applicationDescription", description),
                ("adminEmail", "a@example.com")))

            status(result) shouldBe OK

            contentAsString(result) should include(appName)
            contentAsString(result) should include("Application added")
            contentAsString(result) should include(applicationId.value)
            contentAsString(result) should include("Sandbox")
            contentAsString(result) should include("ROPC")
            contentAsString(result) should include(clientId.value)
    
          }
        }
      }
    }

    "applicationPage" should {

      "return the application details without subscription fields" in new Setup with ApplicationBuilder with ApiBuilder {

        val application2 = buildApplication()
        val applicationWithSubscriptionData = ApplicationWithSubscriptionData(application2, Set.empty, Map.empty)
        val apiData = DefaultApiData.withName("API NAme").addVersion(VersionOne, DefaultVersionData)
        val apiContext = ApiContext("Api Context")
        val apiContextAndApiData = Map(apiContext -> apiData)

        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        ApmServiceMock.FetchApplicationById.returns(applicationWithSubscriptionData)

        ApmServiceMock.fetchAllPossibleSubscriptionsReturns(apiContextAndApiData)
        ApplicationServiceMock.FetchStateHistory.returns(buildStateHistory(application2.id, State.PRODUCTION))
        ApplicationServiceMock.DoesApplicationHaveSubmissions.succeedsFalse()
        
        DeveloperServiceMock.FetchDevelopersByEmails.returns(developers:_*)


        val result = addToken(underTest.applicationPage(applicationId))(aLoggedInRequest)

        status(result) shouldBe OK

      }
    }

    "blockApplicationPage" should {

      "return the page for block app if admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        ApplicationServiceMock.FetchApplication.returns(application)

        val result = addToken(underTest.blockApplicationPage(applicationId))(anAdminLoggedInRequest)

        status(result) shouldBe OK
      }


      "return forbidden for a non-admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments()

        ApplicationServiceMock.FetchApplication.returns(application)

        val result = addToken(underTest.blockApplicationPage(applicationId))(aSuperUserLoggedInRequest)

        status(result) shouldBe FORBIDDEN
      }
    }

    "blockApplicationAction" should {
      "call the service to block application when a valid form is submitted for an admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        givenTheAppWillBeReturned()

        ApplicationServiceMock.BlockApplication.succeeds()

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("applicationNameConfirmation" -> application.application.name)

        val result = addToken(underTest.blockApplicationAction(applicationId))(request)

        status(result) shouldBe OK

        verify(mockApplicationService).blockApplication(eqTo(basicApplication), *)(*)
      }

      "return a bad request when an invalid form is submitted for an admin user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        givenTheAppWillBeReturned()

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody()

        val result = addToken(underTest.blockApplicationAction(applicationId))(request)

        status(result) shouldBe BAD_REQUEST

        verify(mockApplicationService, never).blockApplication(*, *)(*)
      }

      "return forbidden when a form is submitted for a non-admin user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments()

        givenTheAppWillBeReturned()

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("applicationNameConfirmation" -> application.application.name)

        val result = addToken(underTest.blockApplicationAction(applicationId))(request)

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).blockApplication(*, *)(*)
      }

    }

    "unblockApplicationPage" should {

      "return the page for unblock app if admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        ApplicationServiceMock.FetchApplication.returns(application)

        val result = addToken(underTest.unblockApplicationPage(applicationId))(anAdminLoggedInRequest)

        status(result) shouldBe OK
      }


      "return forbidden for a non-admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments()

        ApplicationServiceMock.FetchApplication.returns(application)

        val result = addToken(underTest.unblockApplicationPage(applicationId))(aSuperUserLoggedInRequest)

        status(result) shouldBe FORBIDDEN
      }
    }

    "unblockApplicationAction" should {
      "call the service to unblock application when a valid form is submitted for an admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        givenTheAppWillBeReturned()

        ApplicationServiceMock.UnblockApplication.succeeds()

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("applicationNameConfirmation" -> application.application.name)

        val result = addToken(underTest.unblockApplicationAction(applicationId))(request)

        status(result) shouldBe OK

        verify(mockApplicationService).unblockApplication(eqTo(basicApplication), *)(*)
      }

      "return a bad request when an invalid form is submitted for an admin user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        givenTheAppWillBeReturned()

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody()

        val result = addToken(underTest.unblockApplicationAction(applicationId))(request)

        status(result) shouldBe BAD_REQUEST

        verify(mockApplicationService, never).unblockApplication(*, *)(*)
      }

      "return forbidden when a form is submitted for a non-admin user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments()

        givenTheAppWillBeReturned()

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("applicationNameConfirmation" -> application.application.name)

        val result = addToken(underTest.unblockApplicationAction(applicationId))(request)

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).unblockApplication(*, *)(*)
      }
    }

    def assertIncludesOneError(result: Future[Result], message: String) = {

      val body = contentAsString(result)

      body should include(message)
      assert(Jsoup.parse(body).getElementsByClass("govuk-form-group--error").size == 1)
    }
  }
}