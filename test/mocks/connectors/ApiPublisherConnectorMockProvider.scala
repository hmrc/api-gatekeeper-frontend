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

package mocks.connectors

import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import scala.concurrent.Future.{failed, successful}
import model._
import connectors.{ApiPublisherConnector,SandboxApiPublisherConnector,ProductionApiPublisherConnector}

trait ApiPublisherConnectorMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  val mockProductionApiPublisherConnector = mock[ProductionApiPublisherConnector]
  val mockSandboxApiPublisherConnector = mock[SandboxApiPublisherConnector]

  trait ApiPublisherConnectorMock {
    def mock: ApiPublisherConnector

    object FetchUnapproved {
      def returns(summaries: APIApprovalSummary*) = when(mock.fetchUnapproved()(*)).thenReturn(successful(summaries.toList))
    }

    object FetchApprovalSummary {
      def returns(summary: APIApprovalSummary) = when(mock.fetchApprovalSummary(*)(*)).thenReturn(successful(summary))
    }

    object ApproveService {
      def succeeds() = when(mock.approveService(*)(*)).thenReturn(successful(()))
    }
  }

  object ApiPublisherConnectorMock {
    object Prod extends ApiPublisherConnectorMock {
      val mock = mockProductionApiPublisherConnector
    }

    object Sandbox extends ApiPublisherConnectorMock {
      val mock = mockSandboxApiPublisherConnector
    }
  }

}