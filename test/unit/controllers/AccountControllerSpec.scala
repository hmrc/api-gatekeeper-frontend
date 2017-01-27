/*
 * Copyright 2017 HM Revenue & Customs
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

import controllers.AccountController
import connectors.AuthConnector
import connectors.AuthConnector.InvalidCredentials
import model.LoginDetails.{JsonStringDecryption, JsonStringEncryption}
import model.{BearerToken, GatekeeperSessionKeys, LoginDetails, SuccessfulAuthentication}
import org.joda.time.DateTime
import org.mockito.BDDMockito.given
import org.mockito.Matchers._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF.SignedTokenProvider
import uk.gov.hmrc.crypto.Protected
import uk.gov.hmrc.play.frontend.auth.AuthenticationProvider
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class AccountControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  trait Setup {
    val underTest = new AccountController {
      val authConnector = mock[AuthConnector]

      def authProvider = mock[AuthenticationProvider]
    }

    implicit val encryptedStringFormats = JsonStringEncryption
    implicit val decryptedStringFormats = JsonStringDecryption
    implicit val format = Json.format[LoginDetails]

    val csrfToken = "csrfToken" -> SignedTokenProvider.generateToken
    val authToken = SessionKeys.authToken -> "some-bearer-token"
    val userToken = GatekeeperSessionKeys.LoggedInUser -> "userName"

    val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken)
    val aLoggedOutRequest = FakeRequest().withSession(csrfToken)

  }

  "loginPage" should {
    "be loaded with not authenticated user" in new Setup {
      val result = await(underTest.loginPage()(aLoggedOutRequest))
      status(result) shouldBe 200
    }

    "be skipped with an authenticated user" in new Setup {
      val result = await(underTest.loginPage()(aLoggedInRequest))
      status(result) shouldBe 303
    }
  }

  "authenticateAction" should {
    "go to dashboard and set cookie when user authenticated successfully" in new Setup {
      val loginDetails = LoginDetails("userName", Protected("password"))
      val aValidFormJson = Json.toJson(loginDetails)
      val successfulAuthentication = SuccessfulAuthentication(BearerToken("bearer-token", DateTime.now().plusMinutes(10)), "userName", None)

      given(underTest.authConnector.login(any[LoginDetails])(any[HeaderCarrier])).willReturn(Future.successful(successfulAuthentication))

      val result = await(underTest.authenticate()(
        aLoggedOutRequest.withJsonBody(aValidFormJson)))
      redirectLocation(result) shouldBe Some("/api-gatekeeper/dashboard")

      result.header.headers.get("Set-Cookie") shouldBe defined

      session(result).get(SessionKeys.authToken) shouldBe Some("bearer-token")
    }

    "give 400 when an invalid login form is posted" in new Setup {
      val result = await(underTest.authenticate()(
        aLoggedOutRequest.withJsonBody(Json.toJson(LoginDetails("", Protected("password"))))))
      status(result) shouldBe 400
    }

    "go to login page if user failed to authenticate" in new Setup {
      val loginDetails = LoginDetails("userName", Protected("password"))
      val aValidFormJson = Json.toJson(loginDetails)
      given(underTest.authConnector.login(any[LoginDetails])(any[HeaderCarrier])).willReturn(Future.failed(new InvalidCredentials))

      val result = await(underTest.authenticate()(
        aLoggedOutRequest.withJsonBody(aValidFormJson)))

      status(result) shouldBe 401
      bodyOf(result) should include("Invalid user ID or password. Try again.")
    }
  }

  "logoutAction" should {
    "take to login page with cleared auth cookie" in new Setup {
      val result = await(underTest.logout()(aLoggedInRequest))
      redirectLocation(result) shouldBe Some("/api-gatekeeper/login")
    }
  }
}
