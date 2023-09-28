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

import uk.gov.hmrc.apiplatform.modules.common.domain.models.Environment
import uk.gov.hmrc.gatekeeper.connectors._
import uk.gov.hmrc.gatekeeper.models._

trait ApiDefinitionConnectorMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  val mockProductionApiDefinitionConnector = mock[ProductionApiDefinitionConnector]
  when(mockProductionApiDefinitionConnector.environment).thenReturn(Environment.PRODUCTION)

  val mockSandboxApiDefinitionConnector = mock[SandboxApiDefinitionConnector]
  when(mockSandboxApiDefinitionConnector.environment).thenReturn(Environment.SANDBOX)

  trait ApiDefinitionConnectorMock {
    def mock: ApiDefinitionConnector

    object FetchPublic {
      def returns(defs: ApiDefinitionGK*) = when(mock.fetchPublic()(*)).thenReturn(successful(defs.toList))
    }

    object FetchPrivate {
      def returns(defs: ApiDefinitionGK*) = when(mock.fetchPrivate()(*)).thenReturn(successful(defs.toList))
    }
  }

  object ApiDefinitionConnectorMock {

    object Prod extends ApiDefinitionConnectorMock {
      val mock = mockProductionApiDefinitionConnector
    }

    object Sandbox extends ApiDefinitionConnectorMock {
      val mock = mockSandboxApiDefinitionConnector
    }
  }

}
