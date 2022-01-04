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

package mocks.connectors

import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import scala.concurrent.Future.successful
import model._
import connectors.ApmConnector

trait ApmConnectorMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>
  
  val mockApmConnector = mock[ApmConnector]
  val mockApmConnectorConfig = mock[ApmConnector.Config]

  object ApmConnectorConfigMock {
    object ServiceBaseUrl {
      def returns(url: String) = when(mockApmConnectorConfig.serviceBaseUrl).thenReturn(url)
    }
  }

  object ApmConnectorMock {
    object SubscribeToApi {
      def succeeds() = when(mockApmConnector.subscribeToApi(*[ApplicationId], *)(*)).thenReturn(successful(ApplicationUpdateSuccessResult))
    }

  }
}