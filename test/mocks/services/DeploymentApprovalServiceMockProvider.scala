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

package mocks.services

import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import model._
import scala.concurrent.Future.{failed, successful}
import services.DeploymentApprovalService

trait DeploymentApprovalServiceMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  val mockDeploymentApprovalService = mock[DeploymentApprovalService]

  object DeploymentApprovalServiceMock {
    object FetchUnapprovedServices {
      def returns(approvalSummaries: APIApprovalSummary*) =
        when(mockDeploymentApprovalService.fetchUnapprovedServices()(*)).thenReturn(successful(approvalSummaries.toList))
    }

    object FetchApprovalSummary {
      def returnsForEnv(environment: Environment.Value)(approvalSummary: APIApprovalSummary) =
        when(mockDeploymentApprovalService.fetchApprovalSummary(*, eqTo(environment))(*)).thenReturn(successful(approvalSummary))
    }

    object ApproveService {
      def succeeds() =
        when(mockDeploymentApprovalService.approveService(*, *)(*)).thenReturn(successful(()))
    }
  }
}