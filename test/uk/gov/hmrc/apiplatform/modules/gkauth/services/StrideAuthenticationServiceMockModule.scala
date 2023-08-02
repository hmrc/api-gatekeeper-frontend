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

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import play.api.mvc.Results._
import play.api.mvc.{MessagesRequest, Result}
import uk.gov.hmrc.auth.core.InvalidBearerToken

import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.{GatekeeperStrideRole, LoggedInRequest}

trait StrideAuthorisationServiceMockModule {
  self: MockitoSugar with ArgumentMatchersSugar =>

  protected trait BaseStrideAuthorisationServiceMock {
    def aMock: StrideAuthorisationService

    object Auth {

      private def wrap[A](fn: MessagesRequest[A] => Future[Either[Result, LoggedInRequest[A]]]) = {
        when(aMock.refineStride[A](*)).thenReturn(fn)
      }

      def succeeds[A](role: GatekeeperStrideRole) = {
        wrap[A]((msg) => successful(Right(new LoggedInRequest("Bobby Example", role, msg))))
      }

      def invalidBearerToken[A] = {
        wrap[A]((msg) => failed(new InvalidBearerToken))
      }

      def hasInsufficientEnrolments[A] = {
        wrap[A]((msg) => successful(Left(Forbidden("You do not have permission"))))
      }

      def sessionRecordNotFound[A] = {
        wrap[A]((msg) => successful(Left(Redirect("http://example.com"))))
      }
    }
  }

  object StrideAuthorisationServiceMock extends BaseStrideAuthorisationServiceMock {
    val aMock = mock[StrideAuthorisationService]
  }

}
