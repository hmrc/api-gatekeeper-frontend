/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.modules.gkauth.services

import utils.AsyncHmrcSpec

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.modules.gkauth.config.StrideAuthRoles
import uk.gov.hmrc.modules.gkauth.domain.models.GatekeeperRoles
import play.api.test.{FakeRequest, StubMessagesFactory}
import play.api.mvc.MessagesRequest
import uk.gov.hmrc.modules.gkauth.domain.models.LoggedInRequest
import uk.gov.hmrc.modules.gkauth.config.StrideAuthConfig
import uk.gov.hmrc.modules.gkauth.controllers.actions.ForbiddenHandler
import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.http.Status._
import play.api.http.HeaderNames.LOCATION
import org.scalatest.prop.TableDrivenPropertyChecks

class StrideAuthorisationServiceSpec extends AsyncHmrcSpec with AuthConnectorMockModule with StubMessagesFactory with TableDrivenPropertyChecks {
  val strideAuthRoles = StrideAuthRoles(adminRole = "test-admin", superUserRole = "test-superUser", userRole = "test-user")
  val fakeRequest = FakeRequest()
  val msgRequest = new MessagesRequest(fakeRequest, stubMessagesApi())
  
  trait Setup {
    val strideAuthConfig = StrideAuthConfig(authBaseUrl = "", strideLoginUrl = "http:///www.example.com", successUrl = "", origin = "", roles = strideAuthRoles)
    
    val underTest = new StrideAuthorisationService(
      authConnector = AuthConnectorMock.aMock,
      forbiddenHandler = new ForbiddenHandler { def handle(msgResult: MessagesRequest[_]): Result = Forbidden("No thanks") },
      strideAuthConfig = strideAuthConfig
    )
  }

  "createStrideRefiner" should {
    // "return the full name in request" in new Setup {
    //   AuthConnectorMock.Authorise.returnsAdminEnrolledUserWhenSufficient()

    //   val result: Either[Result, LoggedInRequest[_]] = await(underTest.createStrideRefiner(GatekeeperRoles.ADMIN)(msgRequest))

    //   result.right.value.name.value shouldBe "Bobby Example"
    // }

    "return the appropriate results" in new Setup {
      import GatekeeperRoles._
      
      val cases = Table( 
        ( "requiredRole", "user has role",  "expected outcome"),
        ( ADMIN,          ADMIN,            Right(ADMIN)),
        ( SUPERUSER,      ADMIN,            Right(ADMIN)),
        ( USER,           ADMIN,            Right(ADMIN)),
        ( ADMIN,          SUPERUSER,        Left(FORBIDDEN)),
        ( SUPERUSER,      SUPERUSER,        Right(SUPERUSER)),
        ( USER,           SUPERUSER,        Right(SUPERUSER)),
        ( ADMIN,          USER,             Left(FORBIDDEN)),
        ( SUPERUSER,      USER,             Left(FORBIDDEN)),
        ( USER,           USER,             Right(USER))
      )

      forAll(cases) { case (requiredRole, userIsOfRole, expected) =>
        AuthConnectorMock.Authorise.returnsFor(userIsOfRole)

        val result: Either[Result, LoggedInRequest[_]] = await(underTest.createStrideRefiner(requiredRole)(msgRequest))
        expected match {
          case Right(role) => result.right.value.role shouldBe role
          case Left(statusCode) => result.left.value.header.status shouldBe statusCode
        }
      }
    }

    "return a redirect when there is no active session" in new Setup {
      AuthConnectorMock.Authorise.failsWithNoActiveSession

      val result: Either[Result, LoggedInRequest[_]] = await(underTest.createStrideRefiner(GatekeeperRoles.USER)(msgRequest))

      result.left.value.header.status shouldBe SEE_OTHER
      result.left.value.header.headers(LOCATION) should startWith(strideAuthConfig.strideLoginUrl)
      result.left.value.header.headers(LOCATION) should include(s"successURL=${strideAuthConfig.successUrl}")
    }
  }
}
