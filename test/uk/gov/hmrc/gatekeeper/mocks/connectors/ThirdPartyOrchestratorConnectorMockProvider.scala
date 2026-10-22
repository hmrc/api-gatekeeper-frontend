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

package uk.gov.hmrc.gatekeeper.mocks.connectors

import scala.concurrent.Future.successful

import org.mockito.Mockito.verify as mockitoVerify
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.ApplicationNameValidationResult
import uk.gov.hmrc.apiplatform.modules.applications.query.domain.models.{ApplicationQuery, ParamName}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{LaxEmailAddress, *}
import uk.gov.hmrc.gatekeeper.connectors.*

trait ThirdPartyOrchestratorConnectorMockProvider {
  self: MockitoSugar & ArgumentMatchersSugar =>

  object TPOConnectorMock {
    val aMock = mock[ThirdPartyOrchestratorConnector]

    object GetApplicationsByEmails {

      def returns(emails: List[LaxEmailAddress])(application: ApplicationWithCollaborators*) = when(aMock.getApplicationsByEmails(eqTo(emails))(using *))
        .thenReturn(successful(application.toList))
    }

    object ValidateName {

      def succeedsWith(name: String, selfApplicationId: Option[ApplicationId], environment: Environment)(response: ApplicationNameValidationResult) = {
        when(aMock.validateName(eqTo(name), eqTo(selfApplicationId), eqTo(environment))(using *)).thenReturn(successful(response))
      }
    }

    object ValidateApplicationName {

      def succeeds() =
        when(aMock.validateName(*, *, *)(using *)).thenReturn(successful(ApplicationNameValidationResult.Valid))

      def invalid() =
        when(aMock.validateName(*, *, *)(using *)).thenReturn(successful(ApplicationNameValidationResult.Invalid))

      def duplicate() =
        when(aMock.validateName(*, *, *)(using *)).thenReturn(successful(ApplicationNameValidationResult.Duplicate))
    }

    object Query {
      def returnsFor[T](env: Environment)(t: T) = when(aMock.query[T](eqTo(env))(*)(using *, *)).thenReturn(successful(t))

      def returnsForQry[T](env: Environment)(query: ApplicationQuery, t: T) = when(aMock.query[T](eqTo(env))(eqTo(query))(using *, *)).thenReturn(successful(t))

      def verify[T](query: ApplicationQuery)                   = mockitoVerify(aMock, atLeastOnce).query[T](*)(eqTo(query))(using *, *)
      def verify[T](env: Environment, query: ApplicationQuery) = mockitoVerify(aMock, atLeastOnce).query[T](eqTo(env))(eqTo(query))(using *, *)
    }

    object RawQuery {
      def returnsFor[T](env: Environment)(t: T) = when(aMock.rawQuery[T](eqTo(env))(*)(using *, *)).thenReturn(successful(t))

      def returnsForParams[T](env: Environment)(params: Map[ParamName, String], t: T) = when(aMock.rawQuery[T](eqTo(env))(eqTo(params))(using *, *)).thenReturn(successful(t))
    }
  }
}
