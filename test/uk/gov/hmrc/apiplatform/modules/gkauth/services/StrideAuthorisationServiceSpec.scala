/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatform.modules.gkauth.services

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest.prop.TableDrivenPropertyChecks

import play.api.http.HeaderNames.LOCATION
import play.api.http.Status._
import play.api.mvc.Results._
import play.api.mvc.{MessagesRequest, Result}
import play.api.test.{FakeRequest, StubMessagesFactory}

import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatform.modules.gkauth.config.{StrideAuthConfig, StrideAuthRoles}
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.ForbiddenHandler
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.{GatekeeperRoles, LoggedInRequest}

class StrideAuthorisationServiceSpec extends AsyncHmrcSpec with StrideAuthConnectorMockModule with StubMessagesFactory with TableDrivenPropertyChecks {
  val strideAuthRoles = StrideAuthRoles(adminRole = "test-admin", superUserRole = "test-superUser", userRole = "test-user")
  val fakeRequest     = FakeRequest()
  val msgRequest      = new MessagesRequest(fakeRequest, stubMessagesApi())

  trait Setup {
    val strideAuthConfig = StrideAuthConfig(strideLoginUrl = "http:///www.example.com", successUrl = "", origin = "", roles = strideAuthRoles)

    val underTest = new StrideAuthorisationService(
      strideAuthConnector = StrideAuthConnectorMock.aMock,
      forbiddenHandler = new ForbiddenHandler { def handle(msgResult: MessagesRequest[_]): Result = Forbidden("No thanks") },
      strideAuthConfig = strideAuthConfig
    )
  }

  "refineStride" should {

    "return the appropriate results" in new Setup {
      import GatekeeperRoles._

      val cases = Table(
        ("requiredRole", "user has role", "expected outcome"),
        (ADMIN, ADMIN, Right(ADMIN)),
        (SUPERUSER, ADMIN, Right(ADMIN)),
        (USER, ADMIN, Right(ADMIN)),
        (ADMIN, SUPERUSER, Left(FORBIDDEN)),
        (SUPERUSER, SUPERUSER, Right(SUPERUSER)),
        (USER, SUPERUSER, Right(SUPERUSER)),
        (ADMIN, USER, Left(FORBIDDEN)),
        (SUPERUSER, USER, Left(FORBIDDEN)),
        (USER, USER, Right(USER))
      )

      forAll(cases) { case (requiredRole, userIsOfRole, expected) =>
        StrideAuthConnectorMock.Authorise.returnsFor(userIsOfRole)

        val result: Either[Result, LoggedInRequest[_]] = await(underTest.refineStride(requiredRole)(msgRequest))

        expected match {
          case Right(role)      => result.value.role shouldBe role
          case Left(statusCode) => result.left.value.header.status shouldBe statusCode
        }
      }
    }

    "return a redirect when there is no active session" in new Setup {
      StrideAuthConnectorMock.Authorise.failsWithNoActiveSession

      val result: Either[Result, LoggedInRequest[_]] = await(underTest.refineStride(GatekeeperRoles.USER)(msgRequest))

      result.left.value.header.status shouldBe SEE_OTHER
      result.left.value.header.headers(LOCATION) should startWith(strideAuthConfig.strideLoginUrl)
      result.left.value.header.headers(LOCATION) should include(s"successURL=${strideAuthConfig.successUrl}")
    }
  }
}
