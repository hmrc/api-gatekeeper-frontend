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

import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.services.DeploymentApprovalService
import uk.gov.hmrc.apiplatform.modules.common.domain.models.Environment

trait DeploymentApprovalServiceMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  val mockDeploymentApprovalService = mock[DeploymentApprovalService]

  object DeploymentApprovalServiceMock {

    object FetchUnapprovedServices {

      def returns(approvalSummaries: APIApprovalSummary*) =
        when(mockDeploymentApprovalService.fetchUnapprovedServices()(*)).thenReturn(successful(approvalSummaries.toList))
    }

    object FetchApprovalSummary {

      def returnsForEnv(environment: Environment)(approvalSummary: APIApprovalSummary) =
        when(mockDeploymentApprovalService.fetchApprovalSummary(*, eqTo(environment))(*)).thenReturn(successful(approvalSummary))
    }

    object ApproveService {

      def succeeds() =
        when(mockDeploymentApprovalService.approveService(*, *)(*)).thenReturn(successful(()))
    }
  }
}
