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

import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.successful

import mocks.connectors._
import mocks.services._
import org.apache.pekko.stream.Materializer
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.captor.ArgCaptor

import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.TokenProvider
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.DeleteRestriction.DoNotDelete
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.Environment._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationServiceMockModule, StrideAuthorisationServiceMockModule}
import uk.gov.hmrc.gatekeeper.builder.{ApiBuilder, ApplicationBuilder}
import uk.gov.hmrc.gatekeeper.config.ErrorHandler
import uk.gov.hmrc.gatekeeper.connectors.ApplicationConnector.AppWithSubscriptionsForCsvResponse
import uk.gov.hmrc.gatekeeper.mocks.connectors.ThirdPartyOrchestratorConnectorMockProvider
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.applications.ApplicationsByAnswer
import uk.gov.hmrc.gatekeeper.services.TermsOfUseService.TermsOfUseAgreementDisplayDetails
import uk.gov.hmrc.gatekeeper.services.{SubscriptionFieldsService, TermsOfUseService}
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.utils.{CollaboratorTracker, TitleChecker, WithCSRFAddToken}
import uk.gov.hmrc.gatekeeper.views.html.applications._
import uk.gov.hmrc.gatekeeper.views.html.approvedApplication.ApprovedView
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

class ApplicationControllerSpec
    extends ControllerBaseSpec
    with WithCSRFAddToken
    with TitleChecker
    with CollaboratorTracker
    with ApplicationWithCollaboratorsFixtures {

  implicit val materializer: Materializer = app.materializer

  private lazy val errorTemplateView                    = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView                        = app.injector.instanceOf[ForbiddenView]
  private lazy val applicationsView                     = app.injector.instanceOf[ApplicationsView]
  private lazy val applicationView                      = app.injector.instanceOf[ApplicationView]
  private lazy val manageAccessOverridesView            = app.injector.instanceOf[ManageAccessOverridesView]
  private lazy val manageScopesView                     = app.injector.instanceOf[ManageScopesView]
  private lazy val ipAllowlistView                      = app.injector.instanceOf[IpAllowlistView]
  private lazy val manageIpAllowlistView                = app.injector.instanceOf[ManageIpAllowlistView]
  private lazy val manageRateLimitView                  = app.injector.instanceOf[ManageRateLimitView]
  private lazy val deleteApplicationView                = app.injector.instanceOf[DeleteApplicationView]
  private lazy val deleteApplicationSuccessView         = app.injector.instanceOf[DeleteApplicationSuccessView]
  private lazy val blockApplicationView                 = app.injector.instanceOf[BlockApplicationView]
  private lazy val blockApplicationSuccessView          = app.injector.instanceOf[BlockApplicationSuccessView]
  private lazy val unblockApplicationView               = app.injector.instanceOf[UnblockApplicationView]
  private lazy val unblockApplicationSuccessView        = app.injector.instanceOf[UnblockApplicationSuccessView]
  private lazy val approvedView                         = app.injector.instanceOf[ApprovedView]
  private lazy val createApplicationView                = app.injector.instanceOf[CreateApplicationView]
  private lazy val createApplicationSuccessView         = app.injector.instanceOf[CreateApplicationSuccessView]
  private lazy val manageGrantLengthView                = app.injector.instanceOf[ManageGrantLengthView]
  private lazy val manageGrantLengthSuccessView         = app.injector.instanceOf[ManageGrantLengthSuccessView]
  private lazy val manageDeleteRestrictionDisabledView  = app.injector.instanceOf[ManageDeleteRestrictionDisabledView]
  private lazy val manageDeleteRestrictionEnabledView   = app.injector.instanceOf[ManageDeleteRestrictionEnabledView]
  private lazy val applicationProtectedFromDeletionView = app.injector.instanceOf[ApplicationProtectedFromDeletionView]
  private lazy val manageDeleteRestrictionSuccessView   = app.injector.instanceOf[ManageDeleteRestrictionSuccessView]
  private lazy val errorHandler                         = app.injector.instanceOf[ErrorHandler]

  running(app) {

    trait Setup extends ControllerSetupBase
        with ApplicationServiceMockProvider
        with ApplicationQueryServiceMockProvider
        with ApplicationConnectorMockProvider
        with ApmServiceMockProvider
        with ThirdPartyOrchestratorConnectorMockProvider
        with StrideAuthorisationServiceMockModule
        with LdapAuthorisationServiceMockModule {

      val csrfToken                          = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken
      override val aLoggedInRequest          = FakeRequest().withSession(csrfToken, authToken, userToken).withCSRFToken
      val aLoggedInRequestForDeletedApps     = FakeRequest(GET, "?status=ALL").withSession(csrfToken, authToken, userToken).withCSRFToken
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, superUserToken).withCSRFToken
      override val anAdminLoggedInRequest    = FakeRequest().withSession(csrfToken, authToken, adminToken).withCSRFToken

      lazy val applicationWithOverrides = basicApplication.withAccess(Access.Standard(overrides = Set(OverrideFlag.PersistLogin)))

      lazy val privilegedApplication = basicApplication.withAccess(Access.Privileged(scopes = Set("openid", "email")))

      lazy val ropcApplication = basicApplication.withAccess(Access.Ropc(scopes = Set("openid", "email")))

      val mockSubscriptionFieldsService = mock[SubscriptionFieldsService]
      val mockTermsOfUseService         = mock[TermsOfUseService]

      val developers = List[RegisteredUser] {
        new RegisteredUser("joe.bloggs@example.co.uk".toLaxEmail, UserId.random, "joe", "bloggs", false)
      }

      val basicAppWithDeleteRestrictionEnabled   = basicApplication.modify(_.copy(deleteRestriction = aDeleteRestriction))
      val basicAppWithDeleteRestrictionEnabledId = basicAppWithDeleteRestrictionEnabled.id
      val appWithDeleteRestrictionEnabled        = basicAppWithDeleteRestrictionEnabled

      LdapAuthorisationServiceMock.Auth.notAuthorised

      val underTest = new ApplicationController(
        StrideAuthorisationServiceMock.aMock,
        mockApplicationService,
        ApplicationQueryServiceMock.aMock,
        forbiddenView,
        mockApiDefinitionService,
        mockDeveloperService,
        mockTermsOfUseService,
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
        approvedView,
        createApplicationView,
        createApplicationSuccessView,
        manageGrantLengthView,
        manageGrantLengthSuccessView,
        manageDeleteRestrictionDisabledView,
        manageDeleteRestrictionEnabledView,
        manageDeleteRestrictionSuccessView,
        applicationProtectedFromDeletionView,
        mockApmService,
        errorHandler,
        LdapAuthorisationServiceMock.aMock
      )

      def givenThePaginatedApplicationsWillBeReturned = {
        ApplicationServiceMock.SearchApplications.returns()
        FetchAllApiDefinitions.inAny.returns()
        ApmServiceMock.FetchNonOpenApiDefinitions.returns()
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

        verify(mockApplicationService).searchApplications(eqTo(Environment.SANDBOX), *)(*)
        verify(mockApmService).fetchNonOpenApis(eqTo(SANDBOX))(*)
      }

      "on request for production all production applications supplied" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenThePaginatedApplicationsWillBeReturned

        val eventualResult: Future[Result] = underTest.applicationsPage(environment = Some(Environment.PRODUCTION))(aLoggedInRequest)

        status(eventualResult) shouldBe OK

        verify(mockApplicationService).searchApplications(eqTo(Environment.PRODUCTION), *)(*)
        verify(mockApmService).fetchNonOpenApis(eqTo(PRODUCTION))(*)
      }

      "on request for sandbox all sandbox applications supplied" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenThePaginatedApplicationsWillBeReturned

        val eventualResult: Future[Result] = underTest.applicationsPage(environment = Some(Environment.SANDBOX))(aLoggedInRequest)

        status(eventualResult) shouldBe OK

        verify(mockApplicationService).searchApplications(eqTo(Environment.SANDBOX), *)(*)
        verify(mockApmService).fetchNonOpenApis(eqTo(SANDBOX))(*)
      }

      "pass requested params with default params and default environment of SANDBOX to the service" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenThePaginatedApplicationsWillBeReturned

        val aLoggedInRequestWithParams = FakeRequest(GET, "/applications?search=abc&apiSubscription=ANY&status=CREATED&termsOfUse=ACCEPTED&accessType=STANDARD")
          .withSession(csrfToken, authToken, userToken).withCSRFToken
        val expectedParams             = Map(
          "page"            -> "1",
          "pageSize"        -> "100",
          "sort"            -> "NAME_ASC",
          "search"          -> "abc",
          "apiSubscription" -> "ANY",
          "status"          -> "CREATED",
          "termsOfUse"      -> "ACCEPTED",
          "accessType"      -> "STANDARD",
          "includeDeleted"  -> "false"
        )
        val result                     = underTest.applicationsPage()(aLoggedInRequestWithParams)

        status(result) shouldBe OK

        verify(mockApplicationService).searchApplications(eqTo(Environment.SANDBOX), eqTo(expectedParams))(*)
      }

      "redirect to the login page if the user is not logged in" in new Setup {
        StrideAuthorisationServiceMock.Auth.sessionRecordNotFound

        val result = underTest.applicationsPage()(aLoggedInRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe
          Some("http://example.com")
      }

      "show button to add Privileged app to superuser" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenThePaginatedApplicationsWillBeReturned

        val result = underTest.applicationsPage()(aSuperUserLoggedInRequest)
        status(result) shouldBe OK

        val body = contentAsString(result)

        body should include("Add privileged application")

      }

      "not show button to add Privileged app to non-superuser" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenThePaginatedApplicationsWillBeReturned

        val result = underTest.applicationsPage()(aLoggedInRequest)
        status(result) shouldBe OK

        val body = contentAsString(result)

        body shouldNot include("Add privileged application")

      }
    }

    "appsByAnswer" should {
      "return csv data" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        ApplicationServiceMock.FetchApplicationsByAnswer.returns(List(ApplicationsByAnswer("12345", List(applicationId))))
        private val question = "vat-registration-number"
        val result           = underTest.fetchApplicationByAnswer(question)(aLoggedInRequest)
        status(result) shouldBe OK
        Helpers.contentAsString(result) shouldBe s"${question},count,applications\n12345,1,$applicationId\n"
      }
    }

    "applicationsPageExportCsv" should {
      "return csv data including Collaborator column for Stride user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

        val applicationResponse = buildApplication(
          ApplicationId(UUID.fromString("c702a8f8-9b7c-4ddb-8228-e812f26a2f1e")),
          ClientId("9ee77d73-a65a-4e87-9cda-67863911e02f"),
          "the-gateway-id",
          Some("App Name"),
          deployedTo = Environment.SANDBOX,
          description = None,
          collaborators = Set(
            Collaborator(emailAddress = LaxEmailAddress("some@something.com"), role = Collaborator.Roles.ADMINISTRATOR, userId = UserId(UUID.randomUUID())),
            Collaborator(emailAddress = LaxEmailAddress("another@somethingelse.com"), role = Collaborator.Roles.DEVELOPER, userId = UserId(UUID.randomUUID()))
          ),
          createdOn = Instant.parse("2001-02-03T12:01:02Z"),
          lastAccess = Some(Instant.parse("2002-02-03T12:01:02Z")),
          access = Access.Standard(importantSubmissionData = Some(defaultImportantSubmissionData)),
          state = ApplicationState(updatedOn = instant),
          loginRedirectUris = List(LoginRedirectUri.unsafeApply("http://localhost:8080/callback")),
          postLogoutRedirectUris = List(PostLogoutRedirectUri.unsafeApply("http://localhost:8080/logout"), PostLogoutRedirectUri.unsafeApply("http://localhost:8080/feedback")),
          deleteRestriction = aDeleteRestriction
        )

        ApplicationServiceMock.SearchApplications.returns(applicationResponse)

        val eventualResult: Future[Result] = underTest.applicationsPageCsv()(aLoggedInRequest)

        status(eventualResult) shouldBe OK

        val expectedCsvContent = """page: 1 of 1 from 1 results
Name,App ID,Client ID,Gateway ID,Environment,Status,Rate limit tier,Access type,Overrides,Blocked,Has IP Allow List,Submitted/Created on,Last API call,Restricted from deletion,Number of Redirect URIs,Number of Post Logout Redirect URIs,Collaborator,Responsible Individual
App Name,c702a8f8-9b7c-4ddb-8228-e812f26a2f1e,9ee77d73-a65a-4e87-9cda-67863911e02f,the-gateway-id,SANDBOX,Created,BRONZE,STANDARD,,false,false,2001-02-03T12:01:02Z,2002-02-03T12:01:02Z,true,1,2,Administrator:some@something.com|Developer:another@somethingelse.com,a@example.com
"""

        val responseBody = Helpers.contentAsString(eventualResult)
        responseBody shouldBe expectedCsvContent

      }

      "return csv data excluding Collaborator column for LDAP user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments
        LdapAuthorisationServiceMock.Auth.succeeds

        val applicationResponse = buildApplication(
          ApplicationId(UUID.fromString("c702a8f8-9b7c-4ddb-8228-e812f26a2f1e")),
          ClientId("9ee77d73-a65a-4e87-9cda-67863911e02f"),
          "the-gateway-id",
          Some("App Name"),
          deployedTo = Environment.SANDBOX,
          description = None,
          collaborators = Set(
            Collaborator(emailAddress = LaxEmailAddress("some@something.com"), role = Collaborator.Roles.ADMINISTRATOR, userId = UserId(UUID.randomUUID())),
            Collaborator(emailAddress = LaxEmailAddress("another@somethingelse.com"), role = Collaborator.Roles.DEVELOPER, userId = UserId(UUID.randomUUID()))
          ),
          createdOn = Instant.parse("2001-02-03T12:01:02Z"),
          lastAccess = Some(Instant.parse("2002-02-03T12:01:02Z")),
          access = Access.Standard(),
          state = ApplicationState(updatedOn = instant),
          loginRedirectUris = List(LoginRedirectUri.unsafeApply("http://localhost:8080/callback")),
          postLogoutRedirectUris = List(PostLogoutRedirectUri.unsafeApply("http://localhost:8080/logout"), PostLogoutRedirectUri.unsafeApply("http://localhost:8080/feedback")),
          deleteRestriction = aDeleteRestriction
        )

        ApplicationServiceMock.SearchApplications.returns(applicationResponse)

        val eventualResult: Future[Result] = underTest.applicationsPageCsv()(aLoggedInRequest)

        status(eventualResult) shouldBe OK

        val expectedCsvContent = """page: 1 of 1 from 1 results
Name,App ID,Client ID,Gateway ID,Environment,Status,Rate limit tier,Access type,Overrides,Blocked,Has IP Allow List,Submitted/Created on,Last API call,Restricted from deletion,Number of Redirect URIs,Number of Post Logout Redirect URIs
App Name,c702a8f8-9b7c-4ddb-8228-e812f26a2f1e,9ee77d73-a65a-4e87-9cda-67863911e02f,the-gateway-id,SANDBOX,Created,BRONZE,STANDARD,,false,false,2001-02-03T12:01:02Z,2002-02-03T12:01:02Z,true,1,2
"""

        val responseBody = Helpers.contentAsString(eventualResult)
        responseBody shouldBe expectedCsvContent

      }

      "return csv data including deleted data where Deleted app(s) in results" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        LdapAuthorisationServiceMock.Auth.succeeds

        val applicationResponse       = buildApplication(
          ApplicationId(UUID.fromString("c702a8f8-9b7c-4ddb-8228-e812f26a2f1e")),
          ClientId("9ee77d73-a65a-4e87-9cda-67863911e02f"),
          "the-gateway-id",
          Some("App Name"),
          deployedTo = Environment.SANDBOX,
          description = None,
          collaborators = Set(
            Collaborator(emailAddress = LaxEmailAddress("some@something.com"), role = Collaborator.Roles.ADMINISTRATOR, userId = UserId(UUID.randomUUID())),
            Collaborator(emailAddress = LaxEmailAddress("another@somethingelse.com"), role = Collaborator.Roles.DEVELOPER, userId = UserId(UUID.randomUUID()))
          ),
          createdOn = Instant.parse("2001-02-03T12:01:02Z"),
          lastAccess = Some(Instant.parse("2002-02-03T12:01:02Z")),
          access = Access.Standard(importantSubmissionData = Some(defaultImportantSubmissionData)),
          state = ApplicationState(updatedOn = instant),
          deleteRestriction = aDeleteRestriction
        )
        val secondApplicationResponse = buildApplication(
          ApplicationId(UUID.fromString("c702a8f8-9b7c-4ddb-8228-e812f26a2f1e")),
          ClientId("9ee77d73-a65a-4e87-9cda-67863911e02f"),
          "the-gateway-id",
          Some("App Name"),
          deployedTo = Environment.SANDBOX,
          description = None,
          collaborators = Set(
            Collaborator(emailAddress = LaxEmailAddress("some@something.com"), role = Collaborator.Roles.ADMINISTRATOR, userId = UserId(UUID.randomUUID())),
            Collaborator(emailAddress = LaxEmailAddress("another@somethingelse.com"), role = Collaborator.Roles.DEVELOPER, userId = UserId(UUID.randomUUID()))
          ),
          createdOn = Instant.parse("2001-02-03T12:01:02Z"),
          lastAccess = Some(Instant.parse("2002-02-03T13:02:01Z")),
          access = Access.Standard(),
          state = ApplicationState(name = State.DELETED, updatedOn = instant),
          lastActionActor = ActorType.GATEKEEPER
        )
        ApplicationServiceMock.SearchApplications.returns(List(applicationResponse, secondApplicationResponse): _*)

        val eventualResult: Future[Result] = underTest.applicationsPageCsv()(aLoggedInRequestForDeletedApps)

        status(eventualResult) shouldBe OK

        val expectedCsvContent = """page: 1 of 1 from 2 results
Name,App ID,Client ID,Gateway ID,Environment,Status,Rate limit tier,Access type,Overrides,Blocked,Has IP Allow List,Submitted/Created on,Last API call,Restricted from deletion,Number of Redirect URIs,Number of Post Logout Redirect URIs,Collaborator,Responsible Individual,Deleted by,When deleted
App Name,c702a8f8-9b7c-4ddb-8228-e812f26a2f1e,9ee77d73-a65a-4e87-9cda-67863911e02f,the-gateway-id,SANDBOX,Created,BRONZE,STANDARD,,false,false,2001-02-03T12:01:02Z,2002-02-03T12:01:02Z,true,0,0,Administrator:some@something.com|Developer:another@somethingelse.com,a@example.com,UNKNOWN,N/A
App Name,c702a8f8-9b7c-4ddb-8228-e812f26a2f1e,9ee77d73-a65a-4e87-9cda-67863911e02f,the-gateway-id,SANDBOX,Deleted,BRONZE,STANDARD,,false,false,2001-02-03T12:01:02Z,2002-02-03T13:02:01Z,false,0,0,Administrator:some@something.com|Developer:another@somethingelse.com,,GATEKEEPER,2020-01-02T03:04:05.006Z
"""

        val responseBody = Helpers.contentAsString(eventualResult)
        responseBody shouldBe expectedCsvContent

      }
    }

    "applicationsWithSubscriptionsCsv" should {
      "return csv data" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

        val response1 = AppWithSubscriptionsForCsvResponse(
          applicationIdOne,
          appNameOne,
          Some(instant),
          Set(
            ApiIdentifier(ApiContext("hello"), ApiVersionNbr("1.0")),
            ApiIdentifier(ApiContext("hello"), ApiVersionNbr("2.0")),
            ApiIdentifier(ApiContext("api-documentation-test-service"), ApiVersionNbr("1.5"))
          )
        )
        val response2 = AppWithSubscriptionsForCsvResponse(
          applicationIdTwo,
          appNameTwo,
          None,
          Set(
            ApiIdentifier(ApiContext("hello"), ApiVersionNbr("1.0")),
            ApiIdentifier(ApiContext("individual-tax"), ApiVersionNbr("1.0"))
          )
        )

        ApplicationServiceMock.FetchApplicationsWithSubscriptions.returns(response1, response2)

        val eventualResult: Future[Result] = underTest.applicationWithSubscriptionsCsv()(aLoggedInRequest)

        status(eventualResult) shouldBe OK

        val expectedCsvContent =
          s"""Name,App ID,Environment,Last API call,api-documentation-test-service.1.5,hello.1.0,hello.2.0,individual-tax.1.0
$appNameOne,$applicationIdOne,SANDBOX,${nowAsText},true,true,true,false
$appNameTwo,$applicationIdTwo,SANDBOX,,false,true,false,true
"""

        val responseBody = Helpers.contentAsString(eventualResult)
        responseBody shouldBe expectedCsvContent

      }
    }

    "resendVerification" should {
      "call backend with correct application id and gatekeeper id when resend verification is invoked" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenTheAppWillBeReturned()

        val appCaptor          = ArgCaptor[ApplicationWithCollaborators]
        val gatekeeperIdCaptor = ArgCaptor[String]
        when(mockApplicationService.resendVerification(*, *)(*)).thenReturn(successful(ApplicationUpdateSuccessResult))

        await(underTest.resendVerification(applicationId)(aLoggedInRequest))

        verify(mockApplicationService).resendVerification(appCaptor, gatekeeperIdCaptor)(*)
        appCaptor hasCaptured basicApplication
        gatekeeperIdCaptor hasCaptured "Bobby Example"
      }
    }

    "manageScopes" should {
      "fetch an app with Privileged access for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        ApplicationQueryServiceMock.FetchApplication.returns(privilegedApplication)

        val result = addToken(underTest.manageScopes(applicationId))(aSuperUserLoggedInRequest)

        status(result) shouldBe OK
      }

      "fetch an app with ROPC access for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        ApplicationQueryServiceMock.FetchApplication.returns(ropcApplication)

        val result = addToken(underTest.manageScopes(applicationId))(aSuperUserLoggedInRequest)

        status(result) shouldBe OK
      }

      "return an error for a Standard app" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        ApplicationQueryServiceMock.FetchApplication.returns(application)

        intercept[RuntimeException] {
          await(addToken(underTest.manageScopes(applicationId))(aSuperUserLoggedInRequest))
        }
      }

      "return forbidden for a non-super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        ApplicationQueryServiceMock.FetchApplication.returns(application)

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
        val result  = addToken(underTest.updateScopes(applicationId))(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString()}")

        verify(mockApplicationService)
          .updateScopes(eqTo(application), eqTo(Set("hello", "individual-benefits")), eqTo("Bobby Example"))(*)

      }

      "return a bad request when an invalid form is submitted for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("scopes" -> "")
        val result  = addToken(underTest.updateScopes(applicationId))(request)

        status(result) shouldBe BAD_REQUEST

        verify(mockApplicationService, never).updateScopes(*, *, *)(*)
      }

      "return a bad request when the service indicates that the scopes are invalid" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()

        ApplicationServiceMock.UpdateScopes.failsWithInvalidScopes()

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("scopes" -> "hello")
        val result  = addToken(underTest.updateScopes(applicationId))(request)

        status(result) shouldBe BAD_REQUEST
      }

      "return forbidden when a form is submitted for a non-super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        givenTheAppWillBeReturned()

        val request = aLoggedInRequest.withFormUrlEncodedBody()
        val result  = addToken(underTest.updateScopes(applicationId))(request)

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).updateScopes(*, *, *)(*)
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
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

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

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("grantLength" -> GrantLength.EIGHTEEN_MONTHS.period.getDays.toString)

        val result = addToken(underTest.updateGrantLength(applicationId))(request)

        status(result) shouldBe OK

        verify(mockApplicationService).updateGrantLength(eqTo(basicApplication), *, *)(*)
      }

      "return a bad request when an invalid form is submitted for an admin user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        givenTheAppWillBeReturned()

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody()

        val result = addToken(underTest.updateGrantLength(applicationId))(request)

        status(result) shouldBe BAD_REQUEST

        verify(mockApplicationService, never).updateGrantLength(*, *, *)(*)
      }

      "return forbidden when a form is submitted for a non-admin user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        givenTheAppWillBeReturned()

        val request = aLoggedInRequest.withFormUrlEncodedBody("grantLength" -> GrantLength.EIGHTEEN_MONTHS.period.getDays.toString)

        val result = addToken(underTest.updateGrantLength(applicationId))(request)

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).updateGrantLength(*, *, *)(*)
      }
    }

    "manageDeleteRestriction" should {
      "return the manage delete restriction disabled page for an admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)
        ApplicationQueryServiceMock.FetchApplication.returns(basicApplication)

        val result = underTest.manageDeleteRestriction(applicationId)(anAdminLoggedInRequest)

        status(result) shouldBe OK
        contentAsString(result) should include("This means it cannot be deleted even if it's not used to make API calls for a while.")
      }

      "return the manage delete restriction disabled page for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        ApplicationQueryServiceMock.FetchApplication.returns(basicApplication)

        val result = underTest.manageDeleteRestriction(applicationId)(aSuperUserLoggedInRequest)

        status(result) shouldBe OK
        contentAsString(result) should include("This means it cannot be deleted even if it's not used to make API calls for a while.")
      }

      "return the manage delete restriction enabled page when an event exists and GK user is admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)
        ApplicationQueryServiceMock.FetchApplication.returns(appWithDeleteRestrictionEnabled)

        val result = underTest.manageDeleteRestriction(basicAppWithDeleteRestrictionEnabledId)(anAdminLoggedInRequest)

        status(result) shouldBe OK
        contentAsString(result) should include(s"${appWithDeleteRestrictionEnabled.name} has been set not to be deleted if it is inactive")
      }

      "return the manage delete restriction enabled page when an event exists and GK user is super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        ApplicationQueryServiceMock.FetchApplication.returns(appWithDeleteRestrictionEnabled)

        val result = underTest.manageDeleteRestriction(basicAppWithDeleteRestrictionEnabledId)(anAdminLoggedInRequest)

        status(result) shouldBe OK
        contentAsString(result) should include(s"${appWithDeleteRestrictionEnabled.name} has been set not to be deleted if it is inactive")
      }

      "return the forbidden page for a normal user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        givenTheAppWillBeReturned()

        val result = underTest.manageDeleteRestriction(applicationId)(aLoggedInRequest)

        status(result) shouldBe FORBIDDEN
        contentAsString(result) should include("You do not have permission")
      }
    }

    "updateDeleteRestrictionPreviouslyDisabled" should {
      val noReason = "No reasons given"
      val reason   = "Some reason"

      "call the service to disable deletion restriction when a valid form is submitted for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)

        givenTheAppWillBeReturned()

        ApplicationServiceMock.UpdateAllowDelete.succeeds()

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("confirm" -> "yes", "reason" -> "")

        val result = addToken(underTest.updateDeleteRestrictionPreviouslyDisabled(applicationId))(request)

        status(result) shouldBe OK

        verify(mockApplicationService).updateDeleteRestriction(eqTo(applicationId), eqTo(true), *, eqTo(noReason))(*)
      }

      "call the service to enable deletion restriction when a valid form is submitted for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)

        givenTheAppWillBeReturned()

        ApplicationServiceMock.UpdateAllowDelete.succeeds()

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("confirm" -> "no", "reason" -> reason)

        val result = addToken(underTest.updateDeleteRestrictionPreviouslyDisabled(applicationId))(request)

        status(result) shouldBe OK

        verify(mockApplicationService).updateDeleteRestriction(eqTo(applicationId), eqTo(false), *, eqTo(reason))(*)
      }

      "return a bad request when an invalid form is submitted for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)

        givenTheAppWillBeReturned()

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody()

        val result = addToken(underTest.updateDeleteRestrictionPreviouslyDisabled(applicationId))(request)

        status(result) shouldBe BAD_REQUEST

        verify(mockApplicationService, never).updateDeleteRestriction(*[ApplicationId], *, *, *)(*)
      }

      "return forbidden when a form is submitted for a normal user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        givenTheAppWillBeReturned()

        val request = aLoggedInRequest.withFormUrlEncodedBody("confirm" -> "yes")

        val result = addToken(underTest.updateDeleteRestrictionPreviouslyDisabled(applicationId))(request)

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).updateDeleteRestriction(*[ApplicationId], *, *, eqTo(noReason))(*)
      }
    }

    "updateDeleteRestrictionPreviouslyEnabled" should {
      val noReason = "No reasons given"
      val reason   = "This app should not be deleted"
      val date     = "12th Aug"

      "call the service to disable deletion restriction when a valid form is submitted for an admin user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        ApplicationQueryServiceMock.FetchApplication.returns(appWithDeleteRestrictionEnabled)

        ApplicationServiceMock.UpdateAllowDelete.succeeds()

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("confirm" -> "yes", "reason" -> reason, "reasonDate" -> date)

        val result = addToken(underTest.updateDeleteRestrictionPreviouslyEnabled(basicAppWithDeleteRestrictionEnabledId))(request)

        status(result) shouldBe OK

        verify(mockApplicationService).updateDeleteRestriction(eqTo(basicAppWithDeleteRestrictionEnabledId), eqTo(true), *, eqTo(noReason))(*)
      }

      "call the service to disable deletion restriction when a valid form is submitted for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)

        ApplicationQueryServiceMock.FetchApplication.returns(appWithDeleteRestrictionEnabled)

        ApplicationServiceMock.UpdateAllowDelete.succeeds()

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("confirm" -> "yes", "reason" -> reason, "reasonDate" -> date)

        val result = addToken(underTest.updateDeleteRestrictionPreviouslyEnabled(basicAppWithDeleteRestrictionEnabledId))(request)

        status(result) shouldBe OK

        verify(mockApplicationService).updateDeleteRestriction(eqTo(basicAppWithDeleteRestrictionEnabledId), eqTo(true), *, eqTo(noReason))(*)
      }

      "show manageDeleteRestrictionSuccessView when user selects 'no' on valid form by an admin user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        ApplicationQueryServiceMock.FetchApplication.returns(appWithDeleteRestrictionEnabled)

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("confirm" -> "no", "reason" -> reason, "reasonDate" -> date)

        val result = addToken(underTest.updateDeleteRestrictionPreviouslyEnabled(basicAppWithDeleteRestrictionEnabledId))(request)

        status(result) shouldBe OK
        contentAsString(result) should include(s"${basicAppWithDeleteRestrictionEnabled.name} will not be deleted if it is inactive")

        verify(mockApplicationService, never).updateDeleteRestriction(*[ApplicationId], *, *, *)(*)
      }

      "show manageDeleteRestrictionSuccessView when user selects 'no' on valid form by a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)

        ApplicationQueryServiceMock.FetchApplication.returns(appWithDeleteRestrictionEnabled)

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("confirm" -> "no", "reason" -> reason, "reasonDate" -> date)

        val result = addToken(underTest.updateDeleteRestrictionPreviouslyEnabled(basicAppWithDeleteRestrictionEnabledId))(request)

        status(result) shouldBe OK
        contentAsString(result) should include(s"${basicAppWithDeleteRestrictionEnabled.name} will not be deleted if it is inactive")

        verify(mockApplicationService, never).updateDeleteRestriction(*[ApplicationId], *, *, *)(*)
      }

      "return a bad request when an invalid form is submitted for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)

        ApplicationQueryServiceMock.FetchApplication.returns(appWithDeleteRestrictionEnabled)

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("confirm" -> "", "reason" -> reason, "reasonDate" -> date)

        val result = addToken(underTest.updateDeleteRestrictionPreviouslyEnabled(basicAppWithDeleteRestrictionEnabledId))(request)

        status(result) shouldBe BAD_REQUEST

        verify(mockApplicationService, never).updateDeleteRestriction(*[ApplicationId], *, *, *)(*)
      }

      "return a forbidden when form submitted for a normal user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        ApplicationQueryServiceMock.FetchApplication.returns(appWithDeleteRestrictionEnabled)

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("confirm" -> "yes", "reason" -> reason, "reasonDate" -> date)

        val result = addToken(underTest.updateDeleteRestrictionPreviouslyEnabled(basicAppWithDeleteRestrictionEnabledId))(request)

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).updateDeleteRestriction(*[ApplicationId], *, *, *)(*)
      }
    }

    "deleteApplicationPage" should {
      "show deleteApplicationView when application is not protected from deletion" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        ApplicationQueryServiceMock.FetchApplication.returns(application)

        val request = anAdminLoggedInRequest

        val result = addToken(underTest.deleteApplicationPage(applicationId))(request)

        status(result) shouldBe OK
        contentAsString(result) should include("You must check with all administrators before deleting this application.")
      }

      "show applicationProtectedFromDeletionView when application is protected from deletion" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        ApplicationQueryServiceMock.FetchApplication.returns(appWithDeleteRestrictionEnabled)

        val request = anAdminLoggedInRequest

        val result = addToken(underTest.deleteApplicationPage(basicAppWithDeleteRestrictionEnabledId))(request)

        status(result) shouldBe OK
        contentAsString(result) should include("You cannot delete")
        contentAsString(result) should include(appWithDeleteRestrictionEnabled.name.value)
        contentAsString(result) should include(appWithDeleteRestrictionEnabled.details.deleteRestriction.asInstanceOf[DoNotDelete].reason)
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
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        givenTheAppWillBeReturned()

        val result = underTest.manageIpAllowlistPage(applicationId)(aLoggedInRequest)

        status(result) shouldBe FORBIDDEN
        contentAsString(result) should include("You do not have permission")
      }
    }

    "manageIpAllowlistAction" should {
      val allowlistedIpToUpdate: String = "1.1.1.0/24"
      val required: Boolean             = false

      "manage the IP allowlist using the app service for an admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        givenTheAppWillBeReturned()
        ApplicationServiceMock.ManageIpAllowlist.succeeds()
        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("required" -> required.toString, "allowlistedIps" -> allowlistedIpToUpdate)

        val result = underTest.manageIpAllowlistAction(applicationId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString()}")
        verify(mockApplicationService).manageIpAllowlist(eqTo(application), eqTo(required), eqTo(Set(allowlistedIpToUpdate)), eqTo("Bobby Example"))(*)
      }

      "manage the IP allowlist using the app service for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()
        ApplicationServiceMock.ManageIpAllowlist.succeeds()
        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("required" -> required.toString, "allowlistedIps" -> allowlistedIpToUpdate)

        val result = underTest.manageIpAllowlistAction(applicationId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString()}")
        verify(mockApplicationService).manageIpAllowlist(eqTo(application), eqTo(required), eqTo(Set(allowlistedIpToUpdate)), eqTo("Bobby Example"))(*)
      }

      "clear the IP allowlist when allowlistedIps is empty" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()
        ApplicationServiceMock.ManageIpAllowlist.succeeds()
        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("required" -> required.toString, "allowlistedIps" -> "")

        val result = underTest.manageIpAllowlistAction(applicationId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString()}")
        verify(mockApplicationService).manageIpAllowlist(eqTo(application), eqTo(required), eqTo(Set()), eqTo("Bobby Example"))(*)
      }

      "fail validation when clearing a required IP allowlist" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()
        ApplicationServiceMock.ManageIpAllowlist.succeeds()
        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("required" -> "true", "allowlistedIps" -> "")

        val result = underTest.manageIpAllowlistAction(applicationId)(request)

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("The IP allow list is mandatory for this application")
        verify(mockApplicationService, times(0)).manageIpAllowlist(*, *, *, *)(*)
      }

      "return bad request for invalid values" in new Setup {
        val invalidAllowlistedIps: Seq[String] = Seq(
          "1.1.1.0",          // no mask
          "1.1.1.0/33",       // mask greater than 32
          "1.1.1.0/23",       // mask less than 24
          "1.1.1.0/",         // incomplete mask
          "1.1.1/24",         // IP address missing one octet
          "10.1.1.0/24",      // within a private network range
          "172.20.1.0/24",    // within a private network range
          "192.168.1.0/24",   // within a private network range
          "10.0.0.0/24",      // within a private network range, using the network address
          "10.255.255.255/24" // within a private network range, using the broadcast address
        )

        invalidAllowlistedIps.foreach { invalidAllowlistedIp =>
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
          givenTheAppWillBeReturned()
          val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("required" -> required.toString, "allowlistedIps" -> invalidAllowlistedIp)

          val result = underTest.manageIpAllowlistAction(applicationId)(request)

          status(result) shouldBe BAD_REQUEST
          verify(mockApplicationService, times(0)).manageIpAllowlist(*, *, *, *)(*)
        }
      }

      "return the forbidden page for a normal user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        givenTheAppWillBeReturned()
        val request = aLoggedInRequest.withFormUrlEncodedBody("required" -> required.toString, "allowlistedIps" -> allowlistedIpToUpdate)

        val result = underTest.manageIpAllowlistAction(applicationId)(request)

        status(result) shouldBe FORBIDDEN
        contentAsString(result) should include("You do not have permission")
      }
    }

    "manageOverrides" should {
      "fetch an app with Standard access for a super user" in new Setup {
        println(application)
        println(application.id)
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        ApplicationQueryServiceMock.FetchApplication.returns(application)

        val result = addToken(underTest.manageAccessOverrides(application.id))(aSuperUserLoggedInRequest)

        status(result) shouldBe OK
      }

      "return an error for a ROPC app" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        ApplicationQueryServiceMock.FetchApplication.returns(ropcApplication)

        val result = addToken(underTest.manageAccessOverrides(ropcApplication.id))(aSuperUserLoggedInRequest)
        status(result) shouldBe BAD_REQUEST
      }

      "return an error for a Privileged app" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        ApplicationQueryServiceMock.FetchApplication.returns(privilegedApplication)

        val result = addToken(underTest.manageAccessOverrides(privilegedApplication.id))(aSuperUserLoggedInRequest)
        status(result) shouldBe BAD_REQUEST
      }

      "return forbidden for a non-super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        ApplicationQueryServiceMock.FetchApplication.returns(application)

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
          "persistLoginEnabled"               -> "true",
          "originOverrideEnabled"             -> "true",
          "originOverrideValue"               -> "ngc",
          "grantWithoutConsentEnabled"        -> "true",
          "grantWithoutConsentScopes"         -> "hello, individual-benefits",
          "suppressIvForAgentsEnabled"        -> "true",
          "suppressIvForAgentsScopes"         -> "openid, email",
          "suppressIvForOrganisationsEnabled" -> "true",
          "suppressIvForOrganisationsScopes"  -> "address, openid:mdtp",
          "suppressIvForIndividualsEnabled"   -> "true",
          "suppressIvForIndividualsScopes"    -> "email, openid:hmrc-enrolments"
        )

        val result = addToken(underTest.updateAccessOverrides(applicationId))(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString()}")

        verify(mockApplicationService).updateOverrides(
          eqTo(application),
          eqTo(Set(
            OverrideFlag.PersistLogin,
            OverrideFlag.GrantWithoutConsent(Set("hello", "individual-benefits")),
            OverrideFlag.SuppressIvForAgents(Set("openid", "email")),
            OverrideFlag.SuppressIvForOrganisations(Set("address", "openid:mdtp")),
            OverrideFlag.SuppressIvForIndividuals(Set("email", "openid:hmrc-enrolments")),
            OverrideFlag.OriginOverride("ngc")
          )),
          eqTo("Bobby Example")
        )(*)

      }

      "return a bad request when an invalid form is submitted for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
          "persistLoginEnabled"        -> "true",
          "grantWithoutConsentEnabled" -> "true",
          "grantWithoutConsentScopes"  -> ""
        )

        val result = addToken(underTest.updateAccessOverrides(applicationId))(request)

        status(result) shouldBe BAD_REQUEST

        verify(mockApplicationService, never).updateOverrides(*, *, *)(*)

      }

      "return forbidden when a form is submitted for a non-super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        givenTheAppWillBeReturned()

        val request = aLoggedInRequest.withFormUrlEncodedBody("persistLoginEnabled" -> "true")

        val result = addToken(underTest.updateAccessOverrides(applicationId))(request)

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).updateOverrides(*, *, *)(*)
      }
    }

    "manageRateLimitTier" should {
      "fetch the app and return the page for an admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        ApplicationQueryServiceMock.FetchApplication.returns(application)

        val result = addToken(underTest.manageRateLimitTier(applicationId))(anAdminLoggedInRequest)

        status(result) shouldBe OK

      }

      "return forbidden for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        ApplicationQueryServiceMock.FetchApplication.returns(application)

        val result = addToken(underTest.manageRateLimitTier(applicationId))(aSuperUserLoggedInRequest)

        status(result) shouldBe FORBIDDEN
      }

      "return forbidden for a user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        ApplicationQueryServiceMock.FetchApplication.returns(application)

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
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString()}")

        verify(mockApplicationService).updateRateLimitTier(eqTo(basicApplication), eqTo(RateLimitTier.GOLD), eqTo("Bobby Example"))(*)
      }

      "return a bad request when an invalid form is submitted for an admin user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        givenTheAppWillBeReturned()

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody()

        val result = addToken(underTest.updateRateLimitTier(applicationId))(request)

        status(result) shouldBe BAD_REQUEST

        verify(mockApplicationService, never).updateRateLimitTier(*, *, *)(*)
      }

      "return forbidden when a form is submitted for a non-admin user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        givenTheAppWillBeReturned()

        val request = aLoggedInRequest.withFormUrlEncodedBody("tier" -> "GOLD")

        val result = addToken(underTest.updateRateLimitTier(applicationId))(request)

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).updateRateLimitTier(*, *, *)(*)
      }
    }

    "handleUpdateRateLimitTier" should {
      val tier = RateLimitTier.GOLD

      "change the rate limit for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        ApplicationQueryServiceMock.FetchApplication.returns(application)

        val appCaptor     = ArgumentCaptor.forClass(classOf[ApplicationWithCollaborators])
        val newTierCaptor = ArgumentCaptor.forClass(classOf[RateLimitTier])
        val hcCaptor      = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        val userCaptor    = ArgumentCaptor.forClass(classOf[String])

        when(mockApplicationService.updateRateLimitTier(appCaptor.capture(), newTierCaptor.capture(), userCaptor.capture())(hcCaptor.capture()))
          .thenReturn(successful(ApplicationUpdateSuccessResult))

        val result = underTest.handleUpdateRateLimitTier(applicationId)(aLoggedInRequest.withFormUrlEncodedBody(("tier", tier.toString)))
        status(result) shouldBe SEE_OTHER

        appCaptor.getValue shouldBe basicApplication
        newTierCaptor.getValue shouldBe tier
        userCaptor.getValue() shouldBe "Bobby Example"

        verify(mockApplicationService, times(1)).updateRateLimitTier(basicApplication, tier, "Bobby Example")(hcCaptor.getValue)

      }

      "not call the application connector for a normal user " in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        ApplicationQueryServiceMock.FetchApplication.returns(application)

        val result = underTest.handleUpdateRateLimitTier(applicationId)(aLoggedInRequest.withFormUrlEncodedBody(("tier", "GOLD")))
        status(result) shouldBe SEE_OTHER

        verify(mockApplicationService, never).updateRateLimitTier(*, *, *)(*)

      }
    }

    "createPrivApp" should {
      val appName              = ApplicationName("My New App")
      val privilegedAccessType = AccessType.PRIVILEGED
      val description          = "An application description"
      val adminEmail           = "emailAddress@example.com".toLaxEmail
      val clientId             = ClientId.random
      val totpSecret           = "THISISATOTPSECRETFORPRODUCTION"
      val totp                 = Some(TotpSecrets(totpSecret))
      val privAccess           = AppAccess(AccessType.PRIVILEGED, List.empty)

      "with invalid form fields" can {
        "show the correct error message when no environment is chosen" in new Setup {
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)

          val result = addToken(underTest.createPrivApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", ""),
              ("applicationName", appName.value),
              ("applicationDescription", description),
              ("adminEmail", adminEmail.text)
            )
          )

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Tell us what environment")
        }

        "show the correct error message when the app name is left empty" in new Setup {
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)

          val result = addToken(underTest.createPrivApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("applicationName", ""),
              ("applicationDescription", description),
              ("adminEmail", adminEmail.text)
            )
          )

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Application name must be between 2 and 50 characters and only use ASCII characters excluding")

        }

        "show the correct error message when the new prod app name already exists in prod" in new Setup {
          DeveloperServiceMock.SeekRegisteredUser.returnsFor(adminEmail)
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
          ApplicationServiceMock.ValidateNewApplicationName.duplicate()

          val result = addToken(underTest.createPrivApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("applicationName", "I Already Exist"),
              ("applicationDescription", description),
              ("adminEmail", adminEmail.text)
            )
          )

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "An application with this name already exists")
        }

        "show the correct error message when the new prod app name contains disallowed strings" in new Setup {
          DeveloperServiceMock.SeekRegisteredUser.returnsFor(adminEmail)
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
          ApplicationServiceMock.ValidateNewApplicationName.invalid()

          val result = addToken(underTest.createPrivApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("applicationName", "HMRC"),
              ("applicationDescription", description),
              ("adminEmail", adminEmail.text)
            )
          )

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "The application name is invalid - must not include HMRC or HM Revenue and Customs")
        }

        "show the correct error message when the new prod app name is not long enough" in new Setup {
          DeveloperServiceMock.SeekRegisteredUser.returnsFor(adminEmail)
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)

          val result = addToken(underTest.createPrivApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("applicationName", "P"),
              ("applicationDescription", description),
              ("adminEmail", adminEmail.text)
            )
          )

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Application name must be between 2 and 50 characters and only use ASCII characters excluding")
        }

        "show the correct error message when the new prod app name contains disallowed characters" in new Setup {
          DeveloperServiceMock.SeekRegisteredUser.returnsFor(adminEmail)
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)

          val result = addToken(underTest.createPrivApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("applicationName", "Pete£"),
              ("applicationDescription", description),
              ("adminEmail", adminEmail.text)
            )
          )

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Application name must be between 2 and 50 characters and only use ASCII characters excluding")
        }

        "allow creation of a sandbox app even when the name already exists in production" in new Setup {
          DeveloperServiceMock.SeekRegisteredUser.returnsFor(adminEmail)
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
          ApplicationServiceMock.ValidateNewApplicationName.succeeds()
          ApplicationServiceMock.CreatePrivApp.returns(CreatePrivAppSuccessResult(
            applicationId,
            ApplicationName("I Already Exist"),
            Environment.SANDBOX,
            clientId,
            totp,
            privAccess
          ))

          val result = addToken(underTest.createPrivApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.SANDBOX.toString),
              ("applicationName", "I Already Exist"),
              ("applicationDescription", description),
              ("adminEmail", adminEmail.text)
            )
          )

          status(result) shouldBe OK

          contentAsString(result) should include("Application added")
        }

        "allow creation of a sandbox app if name already exists in sandbox" in new Setup {
          DeveloperServiceMock.SeekRegisteredUser.returnsFor(adminEmail)
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
          ApplicationServiceMock.ValidateNewApplicationName.succeeds()
          ApplicationServiceMock.CreatePrivApp.returns(CreatePrivAppSuccessResult(
            applicationId,
            ApplicationName("I Already Exist"),
            Environment.SANDBOX,
            clientId,
            totp,
            privAccess
          ))

          val result = addToken(underTest.createPrivApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.SANDBOX.toString),
              ("applicationName", "I Already Exist"),
              ("applicationDescription", description),
              ("adminEmail", adminEmail.text)
            )
          )

          status(result) shouldBe OK

          contentAsString(result) should include("Application added")
        }

        "allow creation of a prod app if name already exists in sandbox" in new Setup {
          DeveloperServiceMock.SeekRegisteredUser.returnsFor(adminEmail)
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
          ApplicationServiceMock.ValidateNewApplicationName.succeeds()
          ApplicationServiceMock.CreatePrivApp.returns(CreatePrivAppSuccessResult(
            applicationId,
            ApplicationName("I Already Exist"),
            Environment.PRODUCTION,
            clientId,
            totp,
            privAccess
          ))

          val result = addToken(underTest.createPrivApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("applicationName", "I Already Exist"),
              ("applicationDescription", description),
              ("adminEmail", adminEmail.text)
            )
          )

          status(result) shouldBe OK

          contentAsString(result) should include("Application added")
        }

        "show the correct error message when app description is left empty" in new Setup {
          DeveloperServiceMock.SeekRegisteredUser.returnsFor("a@example.com".toLaxEmail)
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)

          val result = addToken(underTest.createPrivApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("applicationName", appName.value),
              ("applicationDescription", ""),
              ("adminEmail", adminEmail.text)
            )
          )

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Provide an application description")
        }

        "show the correct error message when admin email is left empty" in new Setup {
          DeveloperServiceMock.SeekRegisteredUser.returnsFor("a@example.com".toLaxEmail)
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)

          val result = addToken(underTest.createPrivApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("applicationName", appName.value),
              ("applicationDescription", description),
              ("adminEmail", "")
            )
          )

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Provide an email address")
        }

        "show the correct error message when admin email is invalid" in new Setup {
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)

          val result = addToken(underTest.createPrivApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("applicationName", appName.value),
              ("applicationDescription", description),
              ("adminEmail", "notAValidEmailAddress")
            )
          )

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Provide a valid email address")
        }
      }

      "with valid form fields" can {
        "but the user is not a superuser" should {
          "show 403 forbidden" in new Setup {
            val email = "a@example.com"
            DeveloperServiceMock.SeekRegisteredUser.returnsFor(email.toLaxEmail)
            StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

            val result = addToken(underTest.createPrivApplicationAction())(
              aLoggedInRequest.withFormUrlEncodedBody(
                ("environment", Environment.PRODUCTION.toString),
                ("accessType", privilegedAccessType.toString),
                ("applicationName", appName.value),
                ("applicationDescription", description),
                ("adminEmail", email)
              )
            )

            status(result) shouldBe FORBIDDEN
          }
        }

        "and the user is a superuser" should {
          "show the success page for a priv app in production" in new Setup {
            DeveloperServiceMock.SeekRegisteredUser.returnsFor("a@example.com".toLaxEmail)
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
            ApplicationServiceMock.ValidateNewApplicationName.succeeds()
            ApplicationServiceMock.CreatePrivApp.returns(CreatePrivAppSuccessResult(applicationId, appName, Environment.PRODUCTION, clientId, totp, privAccess))

            val result = addToken(underTest.createPrivApplicationAction())(
              aSuperUserLoggedInRequest.withFormUrlEncodedBody(
                ("environment", Environment.PRODUCTION.toString),
                ("accessType", privilegedAccessType.toString),
                ("applicationName", appName.value),
                ("applicationDescription", description),
                ("adminEmail", "a@example.com")
              )
            )

            status(result) shouldBe OK

            contentAsString(result) should include(appName.value)
            contentAsString(result) should include("Application added")
            contentAsString(result) should include("This is your only chance to copy and save this application's TOTP.")
            contentAsString(result) should include(applicationId.value.toString())
            contentAsString(result) should include("Production")
            contentAsString(result) should include("Privileged")
            contentAsString(result) should include(totpSecret)
            contentAsString(result) should include(clientId.value)

          }

          "show the success page for a priv app in sandbox" in new Setup {
            DeveloperServiceMock.SeekRegisteredUser.returnsFor("a@example.com".toLaxEmail)
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
            ApplicationServiceMock.ValidateNewApplicationName.succeeds()
            ApplicationServiceMock.CreatePrivApp.returns(CreatePrivAppSuccessResult(applicationId, appName, Environment.SANDBOX, clientId, totp, privAccess))

            val result = addToken(underTest.createPrivApplicationAction())(
              aSuperUserLoggedInRequest.withFormUrlEncodedBody(
                ("environment", Environment.SANDBOX.toString),
                ("accessType", privilegedAccessType.toString),
                ("applicationName", appName.value),
                ("applicationDescription", description),
                ("adminEmail", "a@example.com")
              )
            )

            status(result) shouldBe OK

            contentAsString(result) should include(appName.value)
            contentAsString(result) should include("Application added")
            contentAsString(result) should include("This is your only chance to copy and save this application's TOTP.")
            contentAsString(result) should include(applicationId.value.toString())
            contentAsString(result) should include("Sandbox")
            contentAsString(result) should include("Privileged")
            contentAsString(result) should include(totpSecret)
            contentAsString(result) should include(clientId.value)
          }
        }
      }
    }

    "applicationPage" should {
      "return the application details without subscription fields" in new Setup with ApplicationBuilder with ApiBuilder {

        val application2                    = DefaultApplication
        val applicationWithSubscriptionData = ApplicationWithSubscriptionFields(application2.details, application2.collaborators, Set.empty, Map.empty)
        val apiDefinition                   = DefaultApiDefinition.withName("API NAme").addVersion(VersionOne, DefaultVersionData)
        val possibleSubs                    = List(apiDefinition)

        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)
        ApmServiceMock.FetchApplicationById.returns(applicationWithSubscriptionData)

        ApmServiceMock.fetchAllPossibleSubscriptionsReturns(possibleSubs)
        ApplicationServiceMock.FetchStateHistory.returns(buildStateHistory(application2.id, State.PRODUCTION))
        ApplicationServiceMock.DoesApplicationHaveSubmissions.succeedsFalse()
        ApplicationServiceMock.DoesApplicationHaveTermsOfUseInvitation.succeedsFalse()

        DeveloperServiceMock.FetchDevelopersByEmails.returns(developers: _*)
        when(mockTermsOfUseService.getAgreementDetails(applicationWithSubscriptionData.details)).thenReturn(Some(TermsOfUseAgreementDisplayDetails(
          "ri@example.com",
          "12 March 2023",
          "2"
        )))

        val result = addToken(underTest.applicationPage(applicationId))(aLoggedInRequest)

        status(result) shouldBe OK
        contentAsString(result) should include(application2.name.value)
        contentAsString(result) should include("Manage")
        contentAsString(result) should include("v2 agreed by ri@example.com on 12 March 2023")
        contentAsString(result) should include("Delete application")
        contentAsString(result) should include("Block application")
      }

      "return the details for a deleted application" in new Setup with ApplicationBuilder with ApiBuilder {

        val application2                    = DefaultApplication.withState(ApplicationState(State.DELETED, updatedOn = instant))
        val applicationWithSubscriptionData = ApplicationWithSubscriptionFields(application2.details, application2.collaborators, Set.empty, Map.empty)
        val apiDefinition                   = DefaultApiDefinition.withName("API NAme").addVersion(VersionOne, DefaultVersionData)
        val possibleSubs                    = List(apiDefinition)

        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)
        ApmServiceMock.FetchApplicationById.returns(applicationWithSubscriptionData)

        ApmServiceMock.fetchAllPossibleSubscriptionsReturns(possibleSubs)
        ApplicationServiceMock.FetchStateHistory.returns(buildStateHistory(application2.id, State.PRODUCTION))
        ApplicationServiceMock.DoesApplicationHaveSubmissions.succeedsFalse()
        ApplicationServiceMock.DoesApplicationHaveTermsOfUseInvitation.succeedsFalse()

        DeveloperServiceMock.FetchDevelopersByEmails.returns(developers: _*)

        val result = addToken(underTest.applicationPage(applicationId))(aLoggedInRequest)

        status(result) shouldBe OK
        contentAsString(result) should include(application2.name.value)
        contentAsString(result) shouldNot include("Manage")
        contentAsString(result) shouldNot include("Delete application")
        contentAsString(result) shouldNot include("Block application")
      }
    }

    "blockApplicationPage" should {
      "return the page for block app if admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        ApplicationQueryServiceMock.FetchApplication.returns(application)

        val result = addToken(underTest.blockApplicationPage(applicationId))(anAdminLoggedInRequest)

        status(result) shouldBe OK
      }

      "return forbidden for a non-admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        ApplicationQueryServiceMock.FetchApplication.returns(application)

        val result = addToken(underTest.blockApplicationPage(applicationId))(aSuperUserLoggedInRequest)

        status(result) shouldBe FORBIDDEN
      }
    }

    "blockApplicationAction" should {
      "call the service to block application when a valid form is submitted for an admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        givenTheAppWillBeReturned()

        ApplicationServiceMock.BlockApplication.succeeds()

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("applicationNameConfirmation" -> application.name.value)

        val result = addToken(underTest.blockApplicationAction(applicationId))(request)

        status(result) shouldBe OK

        verify(mockApplicationService).blockApplication(eqTo(basicApplication), *)(*)
      }

      "call the service to block application when a valid form is submitted for an admin even when app name has trailing spaces" in new Setup {
        val applicationWithSpaces = basicApplication.modify(_.copy(name = ApplicationName(application.name.value + "  ")))
        ApplicationQueryServiceMock.FetchApplication.returns(applicationWithSpaces)
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        ApplicationServiceMock.BlockApplication.succeeds()

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("applicationNameConfirmation" -> application.name.value)

        val result = addToken(underTest.blockApplicationAction(applicationId))(request)

        status(result) shouldBe OK

        verify(mockApplicationService).blockApplication(eqTo(applicationWithSpaces), *)(*)
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
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        givenTheAppWillBeReturned()

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("applicationNameConfirmation" -> application.name.value)

        val result = addToken(underTest.blockApplicationAction(applicationId))(request)

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).blockApplication(*, *)(*)
      }

    }

    "unblockApplicationPage" should {
      "return the page for unblock app if admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        ApplicationQueryServiceMock.FetchApplication.returns(application)

        val result = addToken(underTest.unblockApplicationPage(applicationId))(anAdminLoggedInRequest)

        status(result) shouldBe OK
      }

      "return forbidden for a non-admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        ApplicationQueryServiceMock.FetchApplication.returns(application)

        val result = addToken(underTest.unblockApplicationPage(applicationId))(aSuperUserLoggedInRequest)

        status(result) shouldBe FORBIDDEN
      }
    }

    "unblockApplicationAction" should {
      "call the service to unblock application when a valid form is submitted for an admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        givenTheAppWillBeReturned()

        ApplicationServiceMock.UnblockApplication.succeeds()

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("applicationNameConfirmation" -> application.name.value)

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
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        givenTheAppWillBeReturned()

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("applicationNameConfirmation" -> application.name.value)

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
