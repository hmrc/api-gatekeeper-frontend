/*
 * Copyright 2016 HM Revenue & Customs
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

import connectors.AuthConnector
import controllers.ApplicationController
import model.LoginDetails.{JsonStringDecryption, JsonStringEncryption}
import model._
import org.joda.time.DateTime
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito._
import org.mockito.Matchers._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.SignedTokenProvider
import services.ApplicationService
import uk.gov.hmrc.crypto.Protected
import uk.gov.hmrc.play.frontend.auth.AuthenticationProvider
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class ApplicationControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  Helpers.running(fakeApplication) {

    trait Setup {
      val underTest = new ApplicationController {
        val authConnector = mock[AuthConnector]
        val authProvider = mock[AuthenticationProvider]
        val applicationService = mock[ApplicationService]
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

    "applicationController" should {
      val applicationId = "applicationId"
      val userName = "userName"

      "call backend with correct application id and gatekeeper id when resend verification is invoked" in new Setup {
        val loginDetails = LoginDetails("userName", Protected("password"))
        val successfulAuthentication = SuccessfulAuthentication(BearerToken("bearer-token", DateTime.now().plusMinutes(10)), userName, None)

        given(underTest.authConnector.login(any[LoginDetails])(any[HeaderCarrier])).willReturn(Future.successful(successfulAuthentication))
        given(underTest.authConnector.authorized(any[Role])(any[HeaderCarrier])).willReturn(Future.successful(true))

        val appIdCaptor = ArgumentCaptor.forClass(classOf[String])
        val gatekeeperIdCaptor = ArgumentCaptor.forClass(classOf[String])

        given(underTest.applicationService.resendVerification(appIdCaptor.capture(), gatekeeperIdCaptor.capture())(any[HeaderCarrier])).willReturn(Future.successful(ResendVerificationSuccessful))

        val result = await(underTest.resendVerification(applicationId)(aLoggedInRequest))

        appIdCaptor.getValue shouldBe applicationId
        gatekeeperIdCaptor.getValue shouldBe userName
      }
    }
  }
}
