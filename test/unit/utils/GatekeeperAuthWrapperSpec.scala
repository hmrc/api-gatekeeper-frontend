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

import java.util.UUID

import connectors.AuthConnector
import controllers.BaseController
import model.{GatekeeperSessionKeys, Role}
import org.mockito.BDDMockito._
import org.mockito.Matchers._
import org.scalatest.mockito.MockitoSugar
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Name, Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import utils.{GatekeeperAuthWrapper, LoggedInRequest}

import scala.concurrent.{ExecutionContext, Future}

class GatekeeperAuthWrapperSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  trait Setup {

    implicit val appConfig = mock[config.AppConfig]

    val underTest = new BaseController with GatekeeperAuthWrapper {
      val authConnector = mock[AuthConnector]
    }
    val actionReturns200Body: (Request[_] => HeaderCarrier => Future[Result]) = _ => _ => Future.successful(Results.Ok)

    val authToken = GatekeeperSessionKeys.AuthToken -> "some-bearer-token"
    val userToken = GatekeeperSessionKeys.LoggedInUser -> "userName"
    val superUserRole = "SuperUserRole" + UUID.randomUUID()
    val userRole = "UserRole" + UUID.randomUUID()

    val aLoggedInRequest = LoggedInRequest[AnyContentAsEmpty.type]("username", Enrolments(Set(Enrolment(userRole))), FakeRequest())
    val aSuperUserLoggedInRequest = LoggedInRequest[AnyContentAsEmpty.type]("superUserName", Enrolments(Set(Enrolment(superUserRole))), FakeRequest())

    given(underTest.appConfig.superUsers).willReturn(Seq("superUserName"))
    given(appConfig.superUserRole).willReturn(superUserRole)
    given(appConfig.userRole).willReturn(userRole)
    given(appConfig.strideLoginUrl).willReturn("https://aUrl")
    given(appConfig.appName).willReturn("appName123")

  }

  "requiresRole" should {

    "execute body if user is logged in" in new Setup {

      val response = Future.successful(new ~(Name(Some("Full Name"), None), Enrolments(Set(Enrolment(userRole)))))

      given(underTest.authConnector.authorise(any(), any[Retrieval[~[Name, Enrolments]]])(any[HeaderCarrier], any[ExecutionContext]))
        .willReturn(response)

      val result = underTest.requiresRole()(actionReturns200Body).apply(aLoggedInRequest)

      status(result) shouldBe OK
    }

    "redirect to login page if user is not logged in" in new Setup {

      given(underTest.authConnector.authorise(any(), any[Retrieval[~[Name, Enrolments]]])(any[HeaderCarrier], any[ExecutionContext]))
        .willReturn(Future.failed(new SessionRecordNotFound))

      val result = underTest.requiresRole(requiresSuperUser = true)(actionReturns200Body).apply(aLoggedInRequest)

      status(result) shouldBe SEE_OTHER
    }

    "return 401 FORBIDDEN if user is logged in and the super user requirement is not met" in new Setup {

      given(underTest.authConnector.authorise(any(), any[Retrieval[~[Name, Enrolments]]])(any[HeaderCarrier], any[ExecutionContext]))
        .willReturn(Future.failed(new InsufficientEnrolments))

      val result = underTest.requiresRole(requiresSuperUser = true)(actionReturns200Body).apply(aLoggedInRequest)

      status(result) shouldBe FORBIDDEN
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
