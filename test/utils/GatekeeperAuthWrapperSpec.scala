/*
 * Copyright 2021 HM Revenue & Customs
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

package utils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import connectors.AuthConnector
import mocks.TestRoles._
import mocks.config.AppConfigMock
import model.{GatekeeperRole, GatekeeperSessionKeys, LoggedInRequest}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import views.html.{ErrorTemplate, ForbiddenView}

import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Name, Retrieval, ~}
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController

class GatekeeperAuthWrapperSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite {
  trait Setup extends AppConfigMock {
    val ec = global
    lazy val mcc = app.injector.instanceOf[MessagesControllerComponents]
    lazy val errorTemplate = app.injector.instanceOf[ErrorTemplate]
    val authConnectorMock = mock[AuthConnector]

    val underTest = new FrontendBaseController with GatekeeperAuthWrapper {
      override protected def controllerComponents: MessagesControllerComponents = mcc
      override val authConnector = authConnectorMock
      override val forbiddenView = app.injector.instanceOf[ForbiddenView]
    }
    val actionReturns200Body: Request[_] => Future[Result] = _ => Future.successful(Results.Ok)


    val authToken = GatekeeperSessionKeys.AuthToken -> "some-bearer-token"
    val userToken = GatekeeperSessionKeys.LoggedInUser -> "userName"

    val aUserLoggedInRequest = LoggedInRequest[AnyContentAsEmpty.type](Some("username"), Enrolments(Set(Enrolment(userRole))), FakeRequest())
    val aSuperUserLoggedInRequest = LoggedInRequest[AnyContentAsEmpty.type](Some("superUserName"), Enrolments(Set(Enrolment(superUserRole))), FakeRequest())
    val anAdminLoggedInRequest = LoggedInRequest[AnyContentAsEmpty.type](Some("adminName"), Enrolments(Set(Enrolment(adminRole))), FakeRequest())

    when(mockConfig.superUsers).thenReturn(Seq("superUserName"))
    when(mockConfig.adminRole).thenReturn(adminRole)
    when(mockConfig.superUserRole).thenReturn(superUserRole)
    when(mockConfig.userRole).thenReturn(userRole)
    when(mockConfig.strideLoginUrl).thenReturn("https://aUrl")
    when(mockConfig.appName).thenReturn("appName123")
    when(mockConfig.gatekeeperSuccessUrl).thenReturn("successUrl_not_checked")

  }

  "requiresRole" should {
    "execute body if user is logged in" in new Setup {
      val response = Future.successful(new ~(Some(Name(Some("Full Name"), None)), Enrolments(Set(Enrolment(userRole)))))

      when(underTest.authConnector.authorise(*, any[Retrieval[~[Option[Name], Enrolments]]])(*, *))
        .thenReturn(response)

      val result = underTest.requiresAtLeast(GatekeeperRole.USER)(actionReturns200Body).apply(aUserLoggedInRequest)

      status(result) shouldBe OK
    }

    "redirect to login page if user is not logged in" in new Setup {
      when(underTest.authConnector.authorise(*, any[Retrieval[~[Option[Name], Enrolments]]])(*, *))
        .thenReturn(Future.failed(new SessionRecordNotFound))

      val result = underTest.requiresAtLeast(GatekeeperRole.SUPERUSER)(actionReturns200Body).apply(aUserLoggedInRequest)

      status(result) shouldBe SEE_OTHER
    }

    "return 401 FORBIDDEN if user is logged in and has insufficient enrolments" in new Setup {
      when(underTest.authConnector.authorise(*, any[Retrieval[~[Option[Name], Enrolments]]])(*, *))
        .thenReturn(Future.failed(new InsufficientEnrolments))

      val result = underTest.requiresAtLeast(GatekeeperRole.SUPERUSER)(actionReturns200Body).apply(aUserLoggedInRequest)

      status(result) shouldBe FORBIDDEN
      verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole) or Enrolment(superUserRole)), any[Retrieval[Any]])(*, *)
    }
  }

  "isAtLeastSuperUser" should {

    "return `true` if the current logged-in user is an admin" in new Setup {

      val isAtLeastSuperUser = underTest.isAtLeastSuperUser(anAdminLoggedInRequest, implicitly)
      isAtLeastSuperUser shouldBe true
    }

    "return `true` if the current logged-in user is a super user" in new Setup {

      val isAtLeastSuperUser = underTest.isAtLeastSuperUser(aSuperUserLoggedInRequest, implicitly)
      isAtLeastSuperUser shouldBe true
    }

    "return `false` if the current logged-in user is a non super-user" in new Setup {

      val isAtLeastSuperUser = underTest.isAtLeastSuperUser(aUserLoggedInRequest, implicitly)
      isAtLeastSuperUser shouldBe false
    }
  }

  "isAdmin" should {

    "return `true` if the current logged-in user is an admin" in new Setup {

      val isAdmin = underTest.isAdmin(anAdminLoggedInRequest, implicitly)
      isAdmin shouldBe true
    }

    "return `false` if the current logged-in user is a super user" in new Setup {

      val isAdmin = underTest.isAdmin(aSuperUserLoggedInRequest, implicitly)
      isAdmin shouldBe false
    }

    "return `false` if the current logged-in user is a user" in new Setup {

      val isAdmin = underTest.isAdmin(aUserLoggedInRequest, implicitly)
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
