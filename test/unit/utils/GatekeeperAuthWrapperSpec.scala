/*
 * Copyright 2019 HM Revenue & Customs
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

package unit.utils

import config.AppConfig
import connectors.AuthConnector
import controllers.BaseController
import model.{GatekeeperSessionKeys, Role}
import org.mockito.BDDMockito._
import org.mockito.Matchers._
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.{Call, Request, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import utils.GatekeeperAuthWrapper

import scala.concurrent.Future

class GatekeeperAuthWrapperSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  trait Setup {

    implicit val appConfig = mock[config.AppConfig]

    val underTest = new BaseController with GatekeeperAuthWrapper {
      val authConnector = mock[AuthConnector]
    }
    val actionReturns200Body: (Request[_] => HeaderCarrier => Future[Result]) = _ => _ => Future.successful(Results.Ok)

    val role = new Role("scope", "role")
    val authToken = GatekeeperSessionKeys.AuthToken -> "some-bearer-token"
    val userToken = GatekeeperSessionKeys.LoggedInUser -> "userName"
    val superUserToken = GatekeeperSessionKeys.LoggedInUser -> "superUserName"

    val aLoggedInRequest = FakeRequest().withSession(authToken, userToken)
    val aSuperUserLoggedInRequest = FakeRequest().withSession(authToken, superUserToken)
    val aLoggedOutRequest = FakeRequest().withSession()

    given(underTest.appConfig.superUsers).willReturn(Seq("superUserName"))

    def theAuthConnectorWillReturn(result: Boolean) {
      given(underTest.authConnector.authorized(any[Role])(any[HeaderCarrier])).willReturn(Future.successful(result))
    }

    def theUserIsNotAuthorised() = theAuthConnectorWillReturn(false)
    def theUserIsAuthorised() = theAuthConnectorWillReturn(true)
  }

  "requiresLogin" should {
    "execute body if request contains valid logged in token" in new Setup {
      val result = underTest.requiresLogin(actionReturns200Body).apply(aLoggedInRequest)
      status(result) shouldBe 200
    }

    "redirect to login if the request does not contain a valid logged in token" in new Setup {
      val result = underTest.requiresLogin(actionReturns200Body).apply(aLoggedOutRequest)
      redirectLocation(result) shouldBe Some("/api-gatekeeper/login")
    }
  }

  "requiresRole" should {
    "redirect to login if the request does not contain a valid logged in token" in new Setup {
      val result = underTest.requiresRole(role)(actionReturns200Body).apply(aLoggedOutRequest)
      redirectLocation(result) shouldBe Some("/api-gatekeeper/login")
    }

    "redirect to unauthorised page if user with role is not authorised" in new Setup {
      theUserIsNotAuthorised

      val result = underTest.requiresRole(role)(actionReturns200Body).apply(aLoggedInRequest)
      status(result) shouldBe 401
    }

    "execute body if user with role is authorised" in new Setup {
      theUserIsAuthorised

      val result = underTest.requiresRole(role)(actionReturns200Body).apply(aLoggedInRequest)

      status(result) shouldBe 200
    }

    "redirect to unauthorised page if user with role is authorised but super user requirement is not met" in new Setup {
      theUserIsAuthorised

      val result = underTest.requiresRole(role, requiresSuperUser = true)(actionReturns200Body).apply(aLoggedInRequest)
      status(result) shouldBe 401
    }

    "execute body if user with role is authorised and the super user requirement is met" in new Setup {
      theUserIsAuthorised

      val result = underTest.requiresRole(role, requiresSuperUser = true)(actionReturns200Body).apply(aSuperUserLoggedInRequest)

      status(result) shouldBe 200
    }
  }

  "redirectIfLoggedIn" should {
    "redirect to the given page when user is logged in" in new Setup {
      val result = underTest.redirectIfLoggedIn(Call("GET", "/welcome-page"))(actionReturns200Body).apply(aLoggedInRequest)
      redirectLocation(result) shouldBe Some("/welcome-page")
    }

    "stay on page when user is logged out" in new Setup {
      val result = underTest.redirectIfLoggedIn(Call("GET", "/welcome-page"))(actionReturns200Body).apply(aLoggedOutRequest)
      status(result) shouldBe 200
    }
  }

  "isSuperUser" should {
    "return `true` if the current logged-in user is a super user" in new Setup {
      val isSuperUser = underTest.isSuperUser(aSuperUserLoggedInRequest)
      isSuperUser shouldBe true
    }

    "return `false` if the current logged-in user is not a super user" in new Setup {
      val isSuperUser = underTest.isSuperUser(aLoggedInRequest)
      isSuperUser shouldBe false
    }
  }

}
