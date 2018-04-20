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

import connectors.AuthConnector.InvalidCredentials
import controllers.ApplicationController
import model.RateLimitTier.RateLimitTier
import model._
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito._
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito.{never, verify}
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._
import play.filters.csrf.CSRF.TokenProvider
import services.DeveloperService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
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
        responseBody should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/dashboard\">Dashboard</a>")
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
  }

  def titleOf(result: Result) = {
    val titleRegEx = """<title[^>]*>(.*)</title>""".r
    val title = titleRegEx.findFirstMatchIn(bodyOf(result)).map(_.group(1))
    title.isDefined shouldBe true
    title.get
  }
}
