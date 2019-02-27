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
import model.{GatekeeperRole, GatekeeperSessionKeys}
import org.mockito.BDDMockito._
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito.verify
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
    val adminRole = "AdminRole" + UUID.randomUUID()
    val userRole = "UserRole" + UUID.randomUUID()

    val aUserLoggedInRequest = LoggedInRequest[AnyContentAsEmpty.type](Some("username"), Enrolments(Set(Enrolment(userRole))), FakeRequest())
    val aSuperUserLoggedInRequest = LoggedInRequest[AnyContentAsEmpty.type](Some("superUserName"), Enrolments(Set(Enrolment(superUserRole))), FakeRequest())
    val anAdminLoggedInRequest = LoggedInRequest[AnyContentAsEmpty.type](Some("adminName"), Enrolments(Set(Enrolment(adminRole))), FakeRequest())

    given(underTest.appConfig.superUsers).willReturn(Seq("superUserName"))
    given(appConfig.adminRole).willReturn(adminRole)
    given(appConfig.superUserRole).willReturn(superUserRole)
    given(appConfig.userRole).willReturn(userRole)
    given(appConfig.strideLoginUrl).willReturn("https://aUrl")
    given(appConfig.appName).willReturn("appName123")
    given(appConfig.gatekeeperSuccessUrl).willReturn("successUrl_not_checked")

  }

  "requiresRole" should {

    "execute body if user is logged in" in new Setup {

      val response = Future.successful(new ~(Name(Some("Full Name"), None), Enrolments(Set(Enrolment(userRole)))))

      given(underTest.authConnector.authorise(any(), any[Retrieval[~[Name, Enrolments]]])(any[HeaderCarrier], any[ExecutionContext]))
        .willReturn(response)

      val result = underTest.requiresAtLeast(GatekeeperRole.USER)(actionReturns200Body).apply(aUserLoggedInRequest)

      status(result) shouldBe OK
    }

    "redirect to login page if user is not logged in" in new Setup {

      given(underTest.authConnector.authorise(any(), any[Retrieval[~[Name, Enrolments]]])(any[HeaderCarrier], any[ExecutionContext]))
        .willReturn(Future.failed(new SessionRecordNotFound))

      val result = underTest.requiresAtLeast(GatekeeperRole.SUPERUSER)(actionReturns200Body).apply(aUserLoggedInRequest)

      status(result) shouldBe SEE_OTHER
    }

    "return 401 FORBIDDEN if user is logged in and has insufficient enrolments" in new Setup {

      given(underTest.authConnector.authorise(any(), any[Retrieval[~[Name, Enrolments]]])(any[HeaderCarrier], any[ExecutionContext]))
        .willReturn(Future.failed(new InsufficientEnrolments))

      val result = underTest.requiresAtLeast(GatekeeperRole.SUPERUSER)(actionReturns200Body).apply(aUserLoggedInRequest)

      status(result) shouldBe FORBIDDEN
      verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole) or Enrolment(superUserRole)), any[Retrieval[Any]])(any(), any())
    }
  }

  "isAtLeastSuperUser" should {

    "return `true` if the current logged-in user is an admin" in new Setup {

      val isAtLeastSuperUser = underTest.isAtLeastSuperUser(anAdminLoggedInRequest)
      isAtLeastSuperUser shouldBe true
    }

    "return `true` if the current logged-in user is a super user" in new Setup {

      val isAtLeastSuperUser = underTest.isAtLeastSuperUser(aSuperUserLoggedInRequest)
      isAtLeastSuperUser shouldBe true
    }

    "return `false` if the current logged-in user is a non super-user" in new Setup {

      val isAtLeastSuperUser = underTest.isAtLeastSuperUser(aUserLoggedInRequest)
      isAtLeastSuperUser shouldBe false
    }
  }

  "isAdmin" should {

    "return `true` if the current logged-in user is an admin" in new Setup {

      val isAdmin = underTest.isAdmin(anAdminLoggedInRequest)
      isAdmin shouldBe true
    }

    "return `false` if the current logged-in user is a super user" in new Setup {

      val isAdmin = underTest.isAdmin(aSuperUserLoggedInRequest)
      isAdmin shouldBe false
    }

    "return `false` if the current logged-in user is a user" in new Setup {

      val isAdmin = underTest.isAdmin(aUserLoggedInRequest)
      isAdmin shouldBe false
    }
  }

  "authPredicate" should {

    "require an admin enrolment if requiresAdmin is true" in new Setup {

      val result = underTest.authPredicate(GatekeeperRole.ADMIN)
      result shouldBe Enrolment(adminRole)
    }

    "require either an admin or super-user enrolment if requiresSuperUser is true" in new Setup {
      val result = underTest.authPredicate(GatekeeperRole.SUPERUSER)
      result shouldBe (Enrolment(adminRole) or Enrolment(superUserRole))
    }

    "require any gatekeeper enrolment if neither admin or super user is required" in new Setup {
      val result = underTest.authPredicate(GatekeeperRole.USER)
      result shouldBe (Enrolment(adminRole) or Enrolment(superUserRole) or Enrolment(userRole))
    }
  }
}
