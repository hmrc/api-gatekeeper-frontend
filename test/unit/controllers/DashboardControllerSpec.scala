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

import config.AppConfig
import connectors.AuthConnector.InvalidCredentials
import connectors.{ApiDefinitionConnector, ApplicationConnector, AuthConnector, DeveloperConnector}
import controllers.DashboardController
import model.LoginDetails.{JsonStringDecryption, JsonStringEncryption}
import model.RateLimitTier.RateLimitTier
import model._
import org.joda.time.DateTime
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito.given
import org.mockito.Matchers._
import org.mockito.Mockito.{never, times, verify}
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.TokenProvider
import uk.gov.hmrc.crypto.Protected
import uk.gov.hmrc.play.frontend.auth.AuthenticationProvider
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class DashboardControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  implicit val materializer = fakeApplication.materializer

  Helpers.running(fakeApplication) {

    trait Setup {
      val underTest = new DashboardController {
        val appConfig = mock[AppConfig]
        val authConnector = mock[AuthConnector]
        val authProvider = mock[AuthenticationProvider]
        val applicationConnector = mock[ApplicationConnector]
        val developerConnector = mock[DeveloperConnector]
        val apiDefinitionConnector = mock[ApiDefinitionConnector]
      }

      implicit val encryptedStringFormats = JsonStringEncryption
      implicit val decryptedStringFormats = JsonStringDecryption
      implicit val format = Json.format[LoginDetails]

      val csrfToken = "csrfToken" -> fakeApplication.injector.instanceOf[TokenProvider].generateToken
      val authToken = GatekeeperSessionKeys.AuthToken -> "some-bearer-token"
      val userToken = GatekeeperSessionKeys.LoggedInUser -> "userName"

      val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken)
      val aLoggedOutRequest = FakeRequest().withSession(csrfToken)
    }


    "dashboardPage" should {

      "go to the login page with error if user is not authenticated" in new Setup {
        val loginDetails = LoginDetails("userName", Protected("password"))
        given(underTest.authConnector.login(any[LoginDetails])(any[HeaderCarrier])).willReturn(Future.failed(new InvalidCredentials))
        val result = await(underTest.dashboardPage()(aLoggedOutRequest))
        redirectLocation(result) shouldBe Some("/api-gatekeeper/login")
      }

      "load successfully if user is authenticated and authorised" in new Setup {
        val loginDetails = LoginDetails("userName", Protected("password"))
        val successfulAuthentication = SuccessfulAuthentication(BearerToken("bearer-token", DateTime.now().plusMinutes(10)), "userName", None)
        given(underTest.authConnector.login(any[LoginDetails])(any[HeaderCarrier])).willReturn(Future.successful(successfulAuthentication))
        given(underTest.authConnector.authorized(any[Role])(any[HeaderCarrier])).willReturn(Future.successful(true))
        given(underTest.applicationConnector.fetchApplicationsWithUpliftRequest()(any[HeaderCarrier])).willReturn(Future.successful(Seq.empty[ApplicationWithUpliftRequest]))
        given(underTest.appConfig.title).willReturn("Unit Test Title")
        val result = await(underTest.dashboardPage()(aLoggedInRequest))
        status(result) shouldBe 200
        titleOf(result) shouldBe "Unit Test Title - Dashboard"
        bodyOf(result) should include("<h1>Dashboard</h1>")
        bodyOf(result) should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/dashboard\">Dashboard</a>")
        bodyOf(result) should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/applications\">Applications</a>")
        bodyOf(result) should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/developers\">Developers</a>")
      }

      "go to application page if authenticated & authorised, but configured as external test" in new Setup {
        val loginDetails = LoginDetails("userName", Protected("password"))
        val successfulAuthentication = SuccessfulAuthentication(BearerToken("bearer-token", DateTime.now().plusMinutes(10)), "userName", None)
        given(underTest.authConnector.login(any[LoginDetails])(any[HeaderCarrier])).willReturn(Future.successful(successfulAuthentication))
        given(underTest.authConnector.authorized(any[Role])(any[HeaderCarrier])).willReturn(Future.successful(true))
        given(underTest.applicationConnector.fetchApplicationsWithUpliftRequest()(any[HeaderCarrier])).willReturn(Future.successful(Seq.empty[ApplicationWithUpliftRequest]))
        given(underTest.appConfig.isExternalTestEnvironment).willReturn(true)
        val result = await(underTest.dashboardPage()(aLoggedInRequest))
        redirectLocation(result) shouldBe Some("/api-gatekeeper/applications")
      }

      "go to unauthorised page if user is not authorised" in new Setup {
        val loginDetails = LoginDetails("userName", Protected("password"))
        val successfulAuthentication = SuccessfulAuthentication(BearerToken("bearer-token", DateTime.now().plusMinutes(10)), "userName", None)
        given(underTest.authConnector.login(any[LoginDetails])(any[HeaderCarrier])).willReturn(Future.successful(successfulAuthentication))
        given(underTest.authConnector.authorized(any[Role])(any[HeaderCarrier])).willReturn(Future.successful(false))
        val result = await(underTest.dashboardPage()(aLoggedInRequest))
        status(result) shouldBe 401
        bodyOf(result) should include("Only Authorised users can access the requested page")
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
        given(underTest.applicationConnector.approveUplift(appIdCaptor.capture(), gatekeeperIdCaptor.capture())(any[HeaderCarrier])).willReturn(Future.successful(ApproveUpliftSuccessful))
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

        given(underTest.applicationConnector.updateRateLimitTier(appIdCaptor.capture(), newTierCaptor.capture())(hcCaptor.capture()))
          .willReturn(Future.successful(ApplicationUpdateSuccessResult))

        val result = await(underTest.handleUpdateRateLimitTier(applicationId)(aLoggedInRequest.withFormUrlEncodedBody(("tier", tier.toString))))
        status(result) shouldBe 303

        appIdCaptor.getValue shouldBe applicationId
        newTierCaptor.getValue shouldBe tier

        verify(underTest.applicationConnector, times(1)).updateRateLimitTier(applicationId, tier)(hcCaptor.getValue)
      }

      "not call the application connector for a normal user " in new Setup {
        given(underTest.authConnector.authorized(any[Role])(any[HeaderCarrier])).willReturn(Future.successful(true))
        given(underTest.appConfig.superUsers).willReturn(Seq.empty)

        val result = await(underTest.handleUpdateRateLimitTier(applicationId)(aLoggedInRequest.withFormUrlEncodedBody(("tier", "GOLD"))))
        status(result) shouldBe 303

        verify(underTest.applicationConnector, never()).updateRateLimitTier(anyString(), any[RateLimitTier])(any[HeaderCarrier])
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
