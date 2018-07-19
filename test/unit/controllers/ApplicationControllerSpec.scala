/*
 * Copyright 2018 HM Revenue & Customs
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

package unit.controllers

import java.util.UUID

import connectors.AuthConnector.InvalidCredentials
import controllers.ApplicationController
import model.Environment.Environment
import model.RateLimitTier.RateLimitTier
import model._
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito._
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito.{never, times, verify}
import org.scalatest.mockito.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.TokenProvider
import services.DeveloperService
import uk.gov.hmrc.crypto.Protected
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import unit.config.AppConfigSpec
import unit.utils.WithCSRFAddToken

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApplicationControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication with WithCSRFAddToken {

  implicit val materializer = fakeApplication.materializer

  running(fakeApplication) {

    trait Setup extends ControllerSetupBase {

      val csrfToken = "csrfToken" -> fakeApplication.injector.instanceOf[TokenProvider].generateToken
      override val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken)
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, superUserToken)

      val applicationWithOverrides = ApplicationWithHistory(
        basicApplication.copy(access = Standard(overrides = Set(PersistLogin()))), Seq.empty)

      val privilegedApplication = ApplicationWithHistory(
        basicApplication.copy(access = Privileged(scopes = Set("openid", "email"))), Seq.empty)

      val ropcApplication = ApplicationWithHistory(
        basicApplication.copy(access = Ropc(scopes = Set("openid", "email"))), Seq.empty)

      val mockDeveloperService = mock[DeveloperService]

      val underTest = new ApplicationController {
        val appConfig = mockConfig
        val authConnector = mockAuthConnector
        val authProvider = mockAuthProvider
        val applicationService = mockApplicationService
        val apiDefinitionService = mockApiDefinitionService
        val developerService = mockDeveloperService
      }

      given(mockConfig.superUsers).willReturn(Seq("superUserName"))
    }

    "applicationsPage" should {

      "on request all applications supplied" in new Setup {
        givenASuccessfulLogin
        val allSubscribedApplications: Seq[SubscribedApplicationResponse] = Seq.empty
        given(mockApplicationService.fetchAllSubscribedApplications(any[HeaderCarrier])).willReturn(Future(allSubscribedApplications))
        given(mockApiDefinitionService.fetchAllApiDefinitions(any[HeaderCarrier])).willReturn(Seq.empty[APIDefinition])
        given(mockConfig.title).willReturn("Unit Test Title")

        val eventualResult: Future[Result] = underTest.applicationsPage()(aLoggedInRequest)

        status(eventualResult) shouldBe OK
        titleOf(eventualResult) shouldBe "Unit Test Title - Applications"
        val responseBody = Helpers.contentAsString(eventualResult)
        responseBody should include("<h1>Applications</h1>")
        responseBody should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/applications\">Applications</a>")
        responseBody should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/developers\">Developers</a>")
      }

      "not show Dashboard tab in external test mode" in new Setup {
        givenASuccessfulLogin
        val allSubscribedApplications: Seq[SubscribedApplicationResponse] = Seq.empty
        given(mockApplicationService.fetchAllSubscribedApplications(any[HeaderCarrier])).willReturn(Future(allSubscribedApplications))
        given(mockApiDefinitionService.fetchAllApiDefinitions(any[HeaderCarrier])).willReturn(Seq.empty[APIDefinition])
        given(mockConfig.title).willReturn("Unit Test Title")
        given(mockConfig.isExternalTestEnvironment).willReturn(true)

        val eventualResult: Future[Result] = underTest.applicationsPage()(aLoggedInRequest)

        status(eventualResult) shouldBe OK
        titleOf(eventualResult) shouldBe "Unit Test Title - Applications"
        val responseBody = Helpers.contentAsString(eventualResult)
        responseBody should include("<h1>Applications</h1>")
        responseBody shouldNot include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/dashboard\">Dashboard</a>")
        responseBody should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/applications\">Applications</a>")
        responseBody should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/developers\">Developers</a>")
      }

      "go to unauthorised page if user is not authorised" in new Setup {
        givenAUnsuccessfulLogin

        val result = await(underTest.applicationsPage(aLoggedInRequest))

        status(result) shouldBe 401
        bodyOf(result) should include("Only Authorised users can access the requested page")
      }

      "go to loginpage with error if user is not authenticated" in new Setup {
        given(underTest.authConnector.login(any[LoginDetails])(any[HeaderCarrier]))
          .willReturn(Future.failed(new InvalidCredentials))
        val result = await(underTest.applicationsPage(aLoggedOutRequest))
        redirectLocation(result) shouldBe Some("/api-gatekeeper/login")
      }

      "show button to add Privileged or ROPC app to superuser" in new Setup {
        givenASuccessfulSuperUserLogin
        val allSubscribedApplications: Seq[SubscribedApplicationResponse] = Seq.empty
        given(mockApplicationService.fetchAllSubscribedApplications(any[HeaderCarrier])).willReturn(Future(allSubscribedApplications))
        given(mockApiDefinitionService.fetchAllApiDefinitions(any[HeaderCarrier])).willReturn(Seq.empty[APIDefinition])
        given(mockConfig.title).willReturn("Unit Test Title")

        val result = await(underTest.applicationsPage()(aSuperUserLoggedInRequest))
        status(result) shouldBe OK

        val body = bodyOf(result)

        body should include("Add privileged or ROPC application")
      }

      "not show button to add Privileged or ROPC app to non-superuser" in new Setup {
        givenASuccessfulLogin
        val allSubscribedApplications: Seq[SubscribedApplicationResponse] = Seq.empty
        given(mockApplicationService.fetchAllSubscribedApplications(any[HeaderCarrier])).willReturn(Future(allSubscribedApplications))
        given(mockApiDefinitionService.fetchAllApiDefinitions(any[HeaderCarrier])).willReturn(Seq.empty[APIDefinition])
        given(mockConfig.title).willReturn("Unit Test Title")

        val result = await(underTest.applicationsPage()(aLoggedInRequest))
        status(result) shouldBe OK

        val body = bodyOf(result)

        body shouldNot include("Add privileged or ROPC application")

      }
    }

    "resendVerification" should {
      "call backend with correct application id and gatekeeper id when resend verification is invoked" in new Setup {
        givenASuccessfulLogin
        val appIdCaptor = ArgumentCaptor.forClass(classOf[String])
        val gatekeeperIdCaptor = ArgumentCaptor.forClass(classOf[String])
        given(underTest.applicationService.resendVerification(appIdCaptor.capture(), gatekeeperIdCaptor.capture())(any[HeaderCarrier]))
          .willReturn(Future.successful(ResendVerificationSuccessful))

        val result = await(underTest.resendVerification(applicationId)(aLoggedInRequest))

        appIdCaptor.getValue shouldBe applicationId
        gatekeeperIdCaptor.getValue shouldBe userName
      }
    }

    "manageScopes" should {
      "fetch an app with Privileged access for a super user" in new Setup {
        givenASuccessfulSuperUserLogin
        givenTheAppWillBeReturned(privilegedApplication)

        val result = await(addToken(underTest.manageScopes(applicationId))(aSuperUserLoggedInRequest))

        status(result) shouldBe 200
      }

      "fetch an app with ROPC access for a super user" in new Setup {
        givenASuccessfulSuperUserLogin
        givenTheAppWillBeReturned(ropcApplication)

        val result = await(addToken(underTest.manageScopes(applicationId))(aSuperUserLoggedInRequest))

        status(result) shouldBe 200
      }

      "return an error for a Standard app" in new Setup {
        givenASuccessfulSuperUserLogin
        givenTheAppWillBeReturned(application)

        intercept[RuntimeException] {
          await(addToken(underTest.manageScopes(applicationId))(aSuperUserLoggedInRequest))
        }
      }

      "return unauthorised for a non-super user" in new Setup {
        givenASuccessfulSuperUserLogin
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.manageScopes(applicationId))(aLoggedInRequest))

        status(result) shouldBe 401
      }
    }

    "updateScopes" should {
      "call the service to update scopes when a valid form is submitted for a super user" in new Setup {
        givenASuccessfulSuperUserLogin
        givenTheAppWillBeReturned()

        given(underTest.applicationService.updateScopes(any[ApplicationResponse], any[Set[String]])(any[HeaderCarrier]))
          .willReturn(Future.successful(UpdateScopesSuccessResult))

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("scopes" -> "hello, individual-benefits")
        val result = await(addToken(underTest.updateScopes(applicationId))(request))

        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId}")

        verify(mockApplicationService)
          .updateScopes(eqTo(application.application), eqTo(Set("hello", "individual-benefits")))(any[HeaderCarrier])
      }

      "return a bad request when an invalid form is submitted for a super user" in new Setup {
        givenASuccessfulSuperUserLogin
        givenTheAppWillBeReturned()

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("scopes" -> "")
        val result = await(addToken(underTest.updateScopes(applicationId))(request))

        status(result) shouldBe 400

        verify(mockApplicationService, never).updateScopes(any[ApplicationResponse], any[Set[String]])(any[HeaderCarrier])
      }

      "return a bad request when the service indicates that the scopes are invalid" in new Setup {
        givenASuccessfulSuperUserLogin
        givenTheAppWillBeReturned()

        given(underTest.applicationService.updateScopes(any[ApplicationResponse], any[Set[String]])(any[HeaderCarrier]))
          .willReturn(Future.successful(UpdateScopesInvalidScopesResult))

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("scopes" -> "hello")
        val result = await(addToken(underTest.updateScopes(applicationId))(request))

        status(result) shouldBe 400
      }

      "return unauthorised when a form is submitted for a non-super user" in new Setup {
        givenASuccessfulSuperUserLogin
        givenTheAppWillBeReturned()

        val request = aLoggedInRequest.withFormUrlEncodedBody()
        val result = await(addToken(underTest.updateScopes(applicationId))(request))

        status(result) shouldBe 401

        verify(mockApplicationService, never).updateScopes(any[ApplicationResponse], any[Set[String]])(any[HeaderCarrier])
      }
    }

    "manageOverrides" should {
      "fetch an app with Standard access for a super user" in new Setup {
        givenASuccessfulSuperUserLogin
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.manageAccessOverrides(applicationId))(aSuperUserLoggedInRequest))

        status(result) shouldBe 200
      }

      "return an error for a ROPC app" in new Setup {
        givenASuccessfulSuperUserLogin
        givenTheAppWillBeReturned(ropcApplication)

        intercept[RuntimeException] {
          await(addToken(underTest.manageAccessOverrides(applicationId))(aSuperUserLoggedInRequest))
        }
      }

      "return an error for a Privileged app" in new Setup {
        givenASuccessfulSuperUserLogin
        givenTheAppWillBeReturned(privilegedApplication)

        intercept[RuntimeException] {
          await(addToken(underTest.manageAccessOverrides(applicationId))(aSuperUserLoggedInRequest))
        }
      }

      "return unauthorised for a non-super user" in new Setup {
        givenASuccessfulSuperUserLogin
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.manageAccessOverrides(applicationId))(aLoggedInRequest))

        status(result) shouldBe 401
      }
    }

    "updateOverrides" should {
      "call the service to update overrides when a valid form is submitted for a super user" in new Setup {
        givenASuccessfulSuperUserLogin
        givenTheAppWillBeReturned()

        given(underTest.applicationService.updateOverrides(any[ApplicationResponse], any[Set[OverrideFlag]])(any[HeaderCarrier]))
          .willReturn(Future.successful(UpdateOverridesSuccessResult))

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
          "persistLoginEnabled" -> "true",
          "grantWithoutConsentEnabled" -> "true", "grantWithoutConsentScopes" -> "hello, individual-benefits",
          "suppressIvForAgentsEnabled" -> "true", "suppressIvForAgentsScopes" -> "openid, email",
          "suppressIvForOrganisationsEnabled" -> "true", "suppressIvForOrganisationsScopes" -> "address, openid:mdtp")

        val result = await(addToken(underTest.updateAccessOverrides(applicationId))(request))

        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId}")

        verify(mockApplicationService).updateOverrides(
          eqTo(application.application),
          eqTo(Set(
            PersistLogin(),
            GrantWithoutConsent(Set("hello", "individual-benefits")),
            SuppressIvForAgents(Set("openid", "email")),
            SuppressIvForOrganisations(Set("address", "openid:mdtp"))
          )))(any[HeaderCarrier])
      }

      "return a bad request when an invalid form is submitted for a super user" in new Setup {
        givenASuccessfulSuperUserLogin
        givenTheAppWillBeReturned()

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
          "persistLoginEnabled" -> "true",
          "grantWithoutConsentEnabled" -> "true", "grantWithoutConsentScopes" -> "")

        val result = await(addToken(underTest.updateAccessOverrides(applicationId))(request))

        status(result) shouldBe 400

        verify(mockApplicationService, never).updateOverrides(any[ApplicationResponse], any[Set[OverrideFlag]])(any[HeaderCarrier])
      }

      "return unauthorised when a form is submitted for a non-super user" in new Setup {
        givenASuccessfulSuperUserLogin
        givenTheAppWillBeReturned()

        val request = aLoggedInRequest.withFormUrlEncodedBody("persistLoginEnabled" -> "true")

        val result = await(addToken(underTest.updateAccessOverrides(applicationId))(request))

        status(result) shouldBe 401

        verify(mockApplicationService, never).updateOverrides(any[ApplicationResponse], any[Set[OverrideFlag]])(any[HeaderCarrier])
      }
    }

    "subscribeToApi" should {
      "call the service to subscribe to the API when submitted for a super user" in new Setup {
        givenASuccessfulSuperUserLogin
        givenTheAppWillBeReturned()

        given(underTest.applicationService.subscribeToApi(anyString, anyString, anyString)(any[HeaderCarrier]))
          .willReturn(Future.successful(ApplicationUpdateSuccessResult))

        val result = await(addToken(underTest.subscribeToApi(applicationId, "hello", "1.0"))(aSuperUserLoggedInRequest))

        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId}/subscriptions")

        verify(underTest.applicationService).subscribeToApi(eqTo(applicationId), eqTo("hello"), eqTo("1.0"))(any[HeaderCarrier])
      }

      "return unauthorised when submitted for a non-super user" in new Setup {
        givenASuccessfulLogin
        givenTheAppWillBeReturned()

        val result = await(addToken(underTest.subscribeToApi(applicationId, "hello", "1.0"))(aLoggedInRequest))

        status(result) shouldBe 401

        verify(underTest.applicationService, never).subscribeToApi(anyString, anyString, anyString)(any[HeaderCarrier])
      }
    }

    "unsubscribeFromApi" should {
      "call the service to unsubscribe from the API when submitted for a super user" in new Setup {
        givenASuccessfulSuperUserLogin
        givenTheAppWillBeReturned()

        given(underTest.applicationService.unsubscribeFromApi(anyString, anyString, anyString)(any[HeaderCarrier]))
          .willReturn(Future.successful(ApplicationUpdateSuccessResult))

        val result = await(addToken(underTest.unsubscribeFromApi(applicationId, "hello", "1.0"))(aSuperUserLoggedInRequest))

        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId}/subscriptions")

        verify(underTest.applicationService).unsubscribeFromApi(eqTo(applicationId), eqTo("hello"), eqTo("1.0"))(any[HeaderCarrier])
      }

      "return unauthorised when submitted for a non-super user" in new Setup {
        givenASuccessfulLogin
        givenTheAppWillBeReturned()

        val result = await(addToken(underTest.unsubscribeFromApi(applicationId, "hello", "1.0"))(aLoggedInRequest))

        status(result) shouldBe 401

        verify(underTest.applicationService, never).unsubscribeFromApi(anyString, anyString, anyString)(any[HeaderCarrier])
      }
    }

    "manageRateLimitTier" should {
      "fetch the app and return the page for a super user" in new Setup {
        givenASuccessfulSuperUserLogin
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.manageRateLimitTier(applicationId))(aSuperUserLoggedInRequest))

        status(result) shouldBe 200
      }

      "return unauthorised for a non-super user" in new Setup {
        givenASuccessfulSuperUserLogin
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.manageRateLimitTier(applicationId))(aLoggedInRequest))

        status(result) shouldBe 401
      }
    }

    "updateRateLimitTier" should {
      "call the service to update the rate limit tier when a valid form is submitted for a super user" in new Setup {
        givenASuccessfulSuperUserLogin
        givenTheAppWillBeReturned()

        given(underTest.applicationService.updateRateLimitTier(anyString, any[RateLimitTier])(any[HeaderCarrier]))
          .willReturn(Future.successful(ApplicationUpdateSuccessResult))

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("tier" -> "GOLD")

        val result = await(addToken(underTest.updateRateLimitTier(applicationId))(request))

        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId}")

        verify(mockApplicationService).updateRateLimitTier(eqTo(applicationId), eqTo(RateLimitTier.GOLD))(any[HeaderCarrier])
      }

      "return a bad request when an invalid form is submitted for a super user" in new Setup {
        givenASuccessfulSuperUserLogin
        givenTheAppWillBeReturned()

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody()

        val result = await(addToken(underTest.updateRateLimitTier(applicationId))(request))

        status(result) shouldBe 400

        verify(mockApplicationService, never).updateRateLimitTier(anyString, any[RateLimitTier])(any[HeaderCarrier])
      }

      "return unauthorised when a form is submitted for a non-super user" in new Setup {
        givenASuccessfulSuperUserLogin
        givenTheAppWillBeReturned()

        val request = aLoggedInRequest.withFormUrlEncodedBody("tier" -> "GOLD")

        val result = await(addToken(underTest.updateRateLimitTier(applicationId))(request))

        status(result) shouldBe 401

        verify(mockApplicationService, never).updateRateLimitTier(anyString, any[RateLimitTier])(any[HeaderCarrier])
      }
    }

    "handleUplift" should {
      val applicationId = "applicationId"
      val userName = "userName"

      "call backend with correct application id and gatekeeper id when application is approved" in new Setup {
        val loginDetails = LoginDetails("userName", Protected("password"))
        val successfulAuthentication = SuccessfulAuthentication(BearerToken("bearer-token", DateTime.now().plusMinutes(10)), userName, None)
        given(underTest.authConnector.login(any[LoginDetails])(any[HeaderCarrier])).willReturn(Future.successful(successfulAuthentication))
        given(underTest.authConnector.authorized(any[Role])(any[HeaderCarrier])).willReturn(Future.successful(true))
        val appIdCaptor = ArgumentCaptor.forClass(classOf[String])
        val gatekeeperIdCaptor = ArgumentCaptor.forClass(classOf[String])
        given(underTest.applicationService.approveUplift(appIdCaptor.capture(), gatekeeperIdCaptor.capture())(any[HeaderCarrier])).willReturn(Future.successful(ApproveUpliftSuccessful))
        val result = await(underTest.handleUplift(applicationId)(aLoggedInRequest.withFormUrlEncodedBody(("action", "APPROVE"))))
        appIdCaptor.getValue shouldBe applicationId
        gatekeeperIdCaptor.getValue shouldBe userName
      }
    }

    "handleUpdateRateLimitTier" should {
      val applicationId = "applicationId"
      val tier = RateLimitTier.GOLD

      "change the rate limit for a super user" in new Setup {
        given(underTest.authConnector.authorized(any[Role])(any[HeaderCarrier])).willReturn(Future.successful(true))
        given(underTest.appConfig.superUsers).willReturn(Seq("userName"))

        val appIdCaptor = ArgumentCaptor.forClass(classOf[String])
        val newTierCaptor = ArgumentCaptor.forClass(classOf[RateLimitTier])
        val hcCaptor = ArgumentCaptor.forClass(classOf[HeaderCarrier])

        given(underTest.applicationService.updateRateLimitTier(appIdCaptor.capture(), newTierCaptor.capture())(hcCaptor.capture()))
          .willReturn(Future.successful(ApplicationUpdateSuccessResult))

        val result = await(underTest.handleUpdateRateLimitTier(applicationId)(aLoggedInRequest.withFormUrlEncodedBody(("tier", tier.toString))))
        status(result) shouldBe 303

        appIdCaptor.getValue shouldBe applicationId
        newTierCaptor.getValue shouldBe tier

        verify(underTest.applicationService, times(1)).updateRateLimitTier(applicationId, tier)(hcCaptor.getValue)
      }

      "not call the application connector for a normal user " in new Setup {
        given(underTest.authConnector.authorized(any[Role])(any[HeaderCarrier])).willReturn(Future.successful(true))
        given(underTest.appConfig.superUsers).willReturn(Seq.empty)

        val result = await(underTest.handleUpdateRateLimitTier(applicationId)(aLoggedInRequest.withFormUrlEncodedBody(("tier", "GOLD"))))
        status(result) shouldBe 303

        verify(underTest.applicationService, never()).updateRateLimitTier(anyString(), any[RateLimitTier])(any[HeaderCarrier])
      }
    }

    "createPrivOrROPCApp" should {

      val appId = "123456789"
      val appName = "My New App"
      val privilegedAccessType = AccessType.PRIVILEGED
      val ropcAccessType = AccessType.ROPC
      val description = "An application description"
      val adminEmail = "emailAddress@example.com"
      val clientId = "This-isac-lient-ID"
      val clientSecret = "THISISACLIENTSECRET"
      val totpSecret = "THISISATOTPSECRETFORPRODUCTION"
      val totp = Some(TotpSecrets(totpSecret, "THISISNOTUSED"))
      val privAccess = AppAccess(AccessType.PRIVILEGED, Seq())
      val ropcAccess = AppAccess(AccessType.ROPC, Seq())

      "with invalid form fields" can {
        "show the correct error message when no access type is chosen" in new Setup {
          givenASuccessfulSuperUserLogin()

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(("accessType", ""), ("applicationName", appName), ("applicationDescription", description), ("adminEmail", adminEmail))))

          status(result) shouldBe 400

          assertIncludesOneError(result, "Tell us what access type")
        }

        "show the correct error message when the app name is left empty" in new Setup {
          givenASuccessfulSuperUserLogin()

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(("accessType", privilegedAccessType.toString), ("applicationName", ""), ("applicationDescription", description), ("adminEmail", adminEmail))))

          status(result) shouldBe 400

          assertIncludesOneError(result, "Provide an application name")

        }

        "show the correct error message when the new prod app name already exists in prod" in new Setup {
          val collaborators = Set(Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR))
          val existingApp = ApplicationResponse(UUID.randomUUID(), "clientid1", "I Already Exist", "PRODUCTION", None, collaborators, DateTime.now(), Standard(), ApplicationState())

          givenASuccessfulSuperUserLogin()
          given(mockApplicationService.fetchApplications(any[HeaderCarrier])).willReturn(Future.successful(Seq(existingApp)))

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(("accessType", privilegedAccessType.toString), ("applicationName", "I Already Exist"), ("applicationDescription", description), ("adminEmail", adminEmail))))

          status(result) shouldBe 400

          assertIncludesOneError(result, "Provide an application name that does not already exist")
        }

        "allow creation of a sandbox app if name already exists in production" in new Setup {

          val collaborators = Set(Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR))
          val existingApp = ApplicationResponse(UUID.randomUUID(), "clientid1", "I Already Exist", "PRODUCTION", None, collaborators, DateTime.now(), Standard(), ApplicationState())

          givenASuccessfulSuperUserLogin()
          given(mockConfig.isExternalTestEnvironment).willReturn(true)
          given(mockApplicationService.fetchApplications(any[HeaderCarrier])).willReturn(Future.successful(Seq(existingApp)))
          given(mockApplicationService.createPrivOrROPCApp(any[Environment], any[String], any[String], any[Seq[Collaborator]], any[AppAccess])(any[HeaderCarrier]))
            .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(appId, "I Already Exist", "SANDBOX", clientId, totp, privAccess)))

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest
              .withFormUrlEncodedBody(("accessType", privilegedAccessType.toString), ("applicationName", "I Already Exist"), ("applicationDescription", description), ("adminEmail", adminEmail))))

          status(result) shouldBe 200

          bodyOf(result) should include("Application added")
        }

        "allow creation of a sandbox app if name already exists in sandbox" in new Setup {
          val collaborators = Set(Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR))
          val existingApp = ApplicationResponse(UUID.randomUUID(), "clientid1", "I Already Exist", "SANDBOX", None, collaborators, DateTime.now(), Standard(), ApplicationState())

          givenASuccessfulSuperUserLogin()
          given(mockConfig.isExternalTestEnvironment).willReturn(true)
          given(mockApplicationService.fetchApplications(any[HeaderCarrier])).willReturn(Future.successful(Seq(existingApp)))
          given(mockApplicationService.createPrivOrROPCApp(any[Environment], any[String], any[String], any[Seq[Collaborator]], any[AppAccess])(any[HeaderCarrier]))
            .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(appId, "I Already Exist", "SANDBOX", clientId, totp, privAccess)))

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(("accessType", privilegedAccessType.toString), ("applicationName", "I Already Exist"), ("applicationDescription", description), ("adminEmail", adminEmail))))

          status(result) shouldBe 200

          bodyOf(result) should include("Application added")
        }

        "allow creation of a prod app if name already exists in sandbox" in new Setup {
          val collaborators = Set(Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR))
          val existingApp = ApplicationResponse(UUID.randomUUID(), "clientid1", "I Already Exist", "SANDBOX", None, collaborators, DateTime.now(), Standard(), ApplicationState())

          givenASuccessfulSuperUserLogin()
          given(mockApplicationService.fetchApplications(any[HeaderCarrier])).willReturn(Future.successful(Seq(existingApp)))
          given(mockApplicationService.createPrivOrROPCApp(any[Environment], any[String], any[String], any[Seq[Collaborator]], any[AppAccess])(any[HeaderCarrier]))
            .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(appId, "I Already Exist", "PRODUCTION", clientId, totp, privAccess)))

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(("accessType", privilegedAccessType.toString), ("applicationName", "I Already Exist"), ("applicationDescription", description), ("adminEmail", adminEmail))))

          status(result) shouldBe 200

          bodyOf(result) should include("Application added")
        }

        "show the correct error message when app description is left empty" in new Setup {
          givenASuccessfulSuperUserLogin()

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(("accessType", privilegedAccessType.toString), ("applicationName", appName), ("applicationDescription", ""), ("adminEmail", adminEmail))))

          status(result) shouldBe 400

          assertIncludesOneError(result, "Provide an application description")
        }

        "show the correct error message when admin email is left empty" in new Setup {
          givenASuccessfulSuperUserLogin()

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(("accessType", privilegedAccessType.toString), ("applicationName", appName), ("applicationDescription", description), ("adminEmail", ""))))

          status(result) shouldBe 400

          assertIncludesOneError(result, "Provide an email address")
        }

        "show the correct error message when admin email is invalid" in new Setup {
          givenASuccessfulSuperUserLogin()

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(("accessType", privilegedAccessType.toString), ("applicationName", appName), ("applicationDescription", description), ("adminEmail", "notAValidEmailAddress"))))

          status(result) shouldBe 400

          assertIncludesOneError(result, "Provide a valid email address")
        }
      }

      "with valid form fields" can {
        "but the user is not a superuser" should {
          "show 401 forbidden" in new Setup {
            givenASuccessfulLogin
            given(mockApplicationService.fetchApplications(any[HeaderCarrier])).willReturn(Future.successful(Seq()))
            given(mockApplicationService.createPrivOrROPCApp(any[Environment], any[String], any[String], any[Seq[Collaborator]], any[AppAccess])(any[HeaderCarrier])).willReturn(Future.successful(CreatePrivOrROPCAppFailureResult))

            val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
              aLoggedInRequest.withFormUrlEncodedBody(("accessType", privilegedAccessType.toString), ("applicationName", appName), ("applicationDescription", description), ("adminEmail", "a@example.com"))))

            status(result) shouldBe 401
          }
        }

        "and the user is a superuser" should {
          "show the success page for a priv app in production" in new Setup {
            givenASuccessfulSuperUserLogin()
            given(mockApplicationService.fetchApplications(any[HeaderCarrier])).willReturn(Future.successful(Seq()))
            given(mockApplicationService.createPrivOrROPCApp(any[Environment], any[String], any[String], any[Seq[Collaborator]], any[AppAccess])(any[HeaderCarrier]))
              .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(appId, appName, "PRODUCTION", clientId, totp, privAccess)))

            val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
              aSuperUserLoggedInRequest.withFormUrlEncodedBody(("accessType", privilegedAccessType.toString), ("applicationName", appName), ("applicationDescription", description), ("adminEmail", "a@example.com"))))

            status(result) shouldBe 200

            bodyOf(result) should include(appName)
            bodyOf(result) should include("Application added")
            bodyOf(result) should include ("This is your only chance to copy and save this application's TOTP secret.")
            bodyOf(result) should include (appId)
            bodyOf(result) should include ("Production")
            bodyOf(result) should include ("Privileged")
            bodyOf(result) should include (totpSecret)
            bodyOf(result) should include (clientId)

          }

          "show the success page for a priv app in sandbox" in new Setup {
            givenASuccessfulSuperUserLogin()
            given(mockApplicationService.fetchApplications(any[HeaderCarrier])).willReturn(Future.successful(Seq()))
            given(mockApplicationService.createPrivOrROPCApp(any[Environment], any[String], any[String], any[Seq[Collaborator]], any[AppAccess])(any[HeaderCarrier]))
              .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(appId, appName, "SANDBOX", clientId, totp, privAccess)))

            val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
              aSuperUserLoggedInRequest.withFormUrlEncodedBody(("accessType", privilegedAccessType.toString), ("applicationName", appName), ("applicationDescription", description), ("adminEmail", "a@example.com"))))

            status(result) shouldBe 200

            bodyOf(result) should include(appName)
            bodyOf(result) should include("Application added")
            bodyOf(result) should include ("This is your only chance to copy and save this application's TOTP secret.")
            bodyOf(result) should include (appId)
            bodyOf(result) should include ("Sandbox")
            bodyOf(result) should include ("Privileged")
            bodyOf(result) should include (totpSecret)
            bodyOf(result) should include (clientId)

          }

          "show the success page for an ROPC app in production" in new Setup {
            givenASuccessfulSuperUserLogin()
            given(mockApplicationService.fetchApplications(any[HeaderCarrier])).willReturn(Future.successful(Seq()))
            given(mockApplicationService.createPrivOrROPCApp(any[Environment], any[String], any[String], any[Seq[Collaborator]], any[AppAccess])(any[HeaderCarrier]))
              .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(appId, appName, "PRODUCTION", clientId, None, ropcAccess)))
            given(mockApplicationService.getClientSecret(eqTo(appId))(any[HeaderCarrier])).willReturn(Future.successful(clientSecret))

            val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
              aSuperUserLoggedInRequest.withFormUrlEncodedBody(("accessType", ropcAccessType.toString), ("applicationName", appName), ("applicationDescription", description), ("adminEmail", "a@example.com"))))

            status(result) shouldBe 200

            bodyOf(result) should include(appName)
            bodyOf(result) should include("Application added")
            bodyOf(result) should include ("This is your only chance to copy and save this application's client secret.")
            bodyOf(result) should include (appId)
            bodyOf(result) should include ("Production")
            bodyOf(result) should include ("ROPC")
            bodyOf(result) should include (clientSecret)
            bodyOf(result) should include (clientId)

          }

          "show the success page for an ROPC app in sandbox" in new Setup {
            givenASuccessfulSuperUserLogin()
            given(mockApplicationService.fetchApplications(any[HeaderCarrier])).willReturn(Future.successful(Seq()))
            given(mockApplicationService.createPrivOrROPCApp(any[Environment], any[String], any[String], any[Seq[Collaborator]], any[AppAccess])(any[HeaderCarrier]))
              .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(appId, appName, "SANDBOX", clientId, totp, ropcAccess)))

            val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
              aSuperUserLoggedInRequest.withFormUrlEncodedBody(("accessType", ropcAccessType.toString), ("applicationName", appName), ("applicationDescription", description), ("adminEmail", "a@example.com"))))

            status(result) shouldBe 200

            bodyOf(result) should include(appName)
            bodyOf(result) should include("Application added")
            bodyOf(result) should include ("This is your only chance to copy and save this application's TOTP secret.")
            bodyOf(result) should include (appId)
            bodyOf(result) should include ("Sandbox")
            bodyOf(result) should include ("ROPC")
            bodyOf(result) should include (totpSecret)
            bodyOf(result) should include (clientId)

          }
        }
      }
    }


    def titleOf(result: Result) = {
      val titleRegEx = """<title[^>]*>(.*)</title>""".r
      val title = titleRegEx.findFirstMatchIn(bodyOf(result)).map(_.group(1))
      title.isDefined shouldBe true
      title.get
    }

    def assertIncludesOneError(result: Result, message: String) = {

      val body = bodyOf(result)

      body should include(message)
      assert(Jsoup.parse(body).getElementsByClass("form-field--error").size == 1)
    }
  }
}
