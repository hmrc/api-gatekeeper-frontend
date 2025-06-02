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

import uk.gov.hmrc.gatekeeper.connectors.{ApiPublisherConnector, ProductionApiPublisherConnector, SandboxApiPublisherConnector}
import uk.gov.hmrc.gatekeeper.models._

trait ApiPublisherConnectorMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  val mockProductionApiPublisherConnector = mock[ProductionApiPublisherConnector]
  val mockSandboxApiPublisherConnector    = mock[SandboxApiPublisherConnector]

  trait ApiPublisherConnectorMock {
    def mock: ApiPublisherConnector

    object FetchAll {
      def returns(summaries: APIApprovalSummary*) = when(mock.fetchAll()(*)).thenReturn(successful(summaries.toList))
    }

    object SearchServices {
      def returns(summaries: APIApprovalSummary*) = when(mock.searchServices(*)(*)).thenReturn(successful(summaries.toList))
    }

    object FetchApprovalSummary {
      def returns(summary: APIApprovalSummary) = when(mock.fetchApprovalSummary(*)(*)).thenReturn(successful(summary))
    }

    object ApproveService {
      def succeeds() = when(mock.approveService(*, *, *)(*)).thenReturn(successful(()))
    }

    object DeclineService {
      def succeeds() = when(mock.declineService(*, *, *)(*)).thenReturn(successful(()))
    }

    object AddComment {
      def succeeds() = when(mock.addComment(*, *, *)(*)).thenReturn(successful(()))
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
