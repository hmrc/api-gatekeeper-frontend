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

package mocks.services

import scala.concurrent.Future.successful

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaborators, ApplicationWithSubscriptionFields}
import uk.gov.hmrc.gatekeeper.models.ApplicationWithSubscriptionFieldsAndStateHistory
import uk.gov.hmrc.gatekeeper.services.ApplicationQueryService

trait ApplicationQueryServiceMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  trait BaseApplicationQueryServiceMock {
    def aMock: ApplicationQueryService

    object FetchApplication {

      def returns(app: ApplicationWithCollaborators) =
        when(aMock.fetchApplication(eqTo(app.id))(*)).thenReturn(successful(Some(app)))
    }

    object FetchAppWithSubsFields {

      def returns(app: ApplicationWithSubscriptionFields) =
        when(aMock.fetchApplicationWithSubscriptionFields(eqTo(app.id))(*)).thenReturn(successful(Some(app)))
    }

    object FetchAppWithSubsFieldsAndHistory {

      def returns(app: ApplicationWithSubscriptionFieldsAndStateHistory) =
        when(aMock.fetchApplicationWithSubscriptionFieldsAndHistory(eqTo(app.id))(*)).thenReturn(successful(Some(app)))
    }
  }

  object ApplicationQueryServiceMock extends BaseApplicationQueryServiceMock {
    val aMock = mock[ApplicationQueryService]
  }
}
