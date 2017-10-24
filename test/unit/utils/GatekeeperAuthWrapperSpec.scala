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

package unit.utils

import connectors.AuthConnector
import model.{GatekeeperSessionKeys, Role}
import org.mockito.BDDMockito._
import org.mockito.Matchers._
import org.scalatest.mock.MockitoSugar
import play.api.mvc.{Call, Request, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import utils.{GatekeeperAuthProvider, GatekeeperAuthWrapper}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class GatekeeperAuthWrapperSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  trait Setup {
    val underTest = new GatekeeperAuthWrapper with Results {
      val authConnector = mock[AuthConnector]
      val authProvider = GatekeeperAuthProvider
      implicit val appConfig = mock[config.AppConfig]
    }
    val actionReturns200Body: (Request[_] => HeaderCarrier => Future[Result]) = _ => _ => Future.successful(Results.Ok)

    val authToken = GatekeeperSessionKeys.AuthToken -> "some-bearer-token"
    val userToken = GatekeeperSessionKeys.LoggedInUser -> "userName"

    val aLoggedInRequest = FakeRequest().withSession(authToken, userToken)
    val aLoggedOutRequest = FakeRequest().withSession()
  }

  "requiresLogin" should {
    "execute body if request contains valid logged in token" in new Setup {
      val result = underTest.requiresLogin()(actionReturns200Body).apply(aLoggedInRequest)
      status(result) shouldBe 200
    }

    "redirect to login if the request does not contain a valid logged in token" in new Setup {
      val result = underTest.requiresLogin()(actionReturns200Body).apply(aLoggedOutRequest)
      redirectLocation(result) shouldBe Some("/api-gatekeeper/login")
    }
  }

  "requiresRole" should {
    "redirect to login if the request does not contain a valid logged in token" in new Setup {
      val result = underTest.requiresRole(new Role("scope", "role"))(actionReturns200Body).apply(aLoggedOutRequest)
      redirectLocation(result) shouldBe Some("/api-gatekeeper/login")
    }

    "redirect to unauthorised page if user with role is not authorised" in new Setup {
      given(underTest.authConnector.authorized(any[Role])(any[HeaderCarrier])).willReturn(Future.successful(false))

      val result = underTest.requiresRole(new Role("scope", "role"))(actionReturns200Body).apply(aLoggedInRequest)
      status(result) shouldBe 401
    }

    "execute body if user with role is authorised" in new Setup {
      given(underTest.authConnector.authorized(any[Role])(any[HeaderCarrier])).willReturn(Future.successful(true))

      val result = underTest.requiresRole(new Role("scope", "role"))(actionReturns200Body).apply(aLoggedInRequest)

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
      given(underTest.appConfig.superUsers).willReturn(Seq("userName"))

      val isSuperUser = underTest.isSuperUser(aLoggedInRequest)
      isSuperUser shouldBe true
    }

    "return `false` if the current logged-in user is not a super user" in new Setup {
      given(underTest.appConfig.superUsers).willReturn(Seq.empty)

      val isSuperUser = underTest.isSuperUser(aLoggedInRequest)
      isSuperUser shouldBe false
    }
  }

}
