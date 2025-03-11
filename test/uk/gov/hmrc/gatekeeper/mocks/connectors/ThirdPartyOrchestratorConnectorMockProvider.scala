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

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.ApplicationNameValidationResult
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{LaxEmailAddress, _}
import uk.gov.hmrc.gatekeeper.connectors._

trait ThirdPartyOrchestratorConnectorMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  object ThirdPartyOrchestratorConnectorMock {
    val aMock = mock[ThirdPartyOrchestratorConnector]

    object GetApplicationsByEmails {

      def returns(emails: List[LaxEmailAddress])(application: ApplicationWithCollaborators*) = when(aMock.getApplicationsByEmails(eqTo(emails))(*))
        .thenReturn(successful(application.toList))
    }

    object ValidateName {

      def succeedsWith(name: String, selfApplicationId: Option[ApplicationId], environment: Environment)(response: ApplicationNameValidationResult) = {
        when(aMock.validateName(eqTo(name), eqTo(selfApplicationId), eqTo(environment))(*)).thenReturn(successful(response))
      }
    }

    object ValidateApplicationName {

      def succeeds() =
        when(aMock.validateName(*, *, *)(*)).thenReturn(successful(ApplicationNameValidationResult.Valid))

      def invalid() =
        when(aMock.validateName(*, *, *)(*)).thenReturn(successful(ApplicationNameValidationResult.Invalid))

      def duplicate() =
        when(aMock.validateName(*, *, *)(*)).thenReturn(successful(ApplicationNameValidationResult.Duplicate))
    }
  }
}
