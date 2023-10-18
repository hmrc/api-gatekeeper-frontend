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

package mocks.connectors

import scala.concurrent.Future.successful

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiData, ApiDefinition}
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.gatekeeper.connectors.ApmConnector
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.applications.ApplicationWithSubscriptionData
import uk.gov.hmrc.gatekeeper.models.pushpullnotifications.Box

trait ApmConnectorMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  val mockApmConnectorConfig = mock[ApmConnector.Config]

  trait ApmConnectorMock {
    def aMock: ApmConnector

    object FetchAllCombinedApis {
      def returns(combinedApis: List[CombinedApi]) = when(aMock.fetchAllCombinedApis()(*)).thenReturn(successful(combinedApis))
    }

    object FetchApplicationById {

      def returns(applicationWithSubscriptionData: Option[ApplicationWithSubscriptionData]) =
        when(aMock.fetchApplicationById(*[ApplicationId])(*)).thenReturn(successful(applicationWithSubscriptionData))
    }

    object FetchAllPossibleSubscriptions {

      def returns(possibleSubscriptions: Map[ApiContext, ApiData]) =
        when(aMock.fetchAllPossibleSubscriptions(*[ApplicationId])(*)).thenReturn(successful(possibleSubscriptions))
    }

    object GetAllFieldDefinitions {
      def returns(fieldDefinitions: ApiDefinitions.Alias) = when(aMock.getAllFieldDefinitions(*)(*)).thenReturn(successful(fieldDefinitions))
    }

    object FetchAllBoxes {
      def returns(allBoxes: List[Box]) = when(aMock.fetchAllBoxes()(*)).thenReturn(successful(allBoxes))
    }

    object FetchAllApiDefinitions {

      def returnsFor(env: Environment)(apis: ApiDefinition*) =
        when(aMock.fetchAllApis(eqTo(env))(*)).thenReturn(successful(apis.toList))

      def verifyNeverCalledFor(env: Environment) =
        verify(aMock, never).fetchAllApis(eqTo(env))(*)

      def verifyCalledFor(env: Environment) =
        verify(aMock, atLeastOnce).fetchAllApis(eqTo(env))(*)
    }
  }

  object ApmConnectorMock extends ApmConnectorMock {
    val aMock = mock[ApmConnector]
  }

  object ApmConnectorConfigMock {

    object ServiceBaseUrl {

      def returns(url: String) =
        when(mockApmConnectorConfig.serviceBaseUrl).thenReturn(url)
    }
  }
}
