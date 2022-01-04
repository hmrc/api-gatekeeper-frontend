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

package services

import java.util.UUID

import model.APIApprovalSummary
import model.Environment._
import uk.gov.hmrc.http.HeaderCarrier
import utils.AsyncHmrcSpec

import scala.concurrent.ExecutionContext.Implicits.global
import mocks.connectors.ApiPublisherConnectorMockProvider
import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar

class DeploymentApprovalServiceSpec extends AsyncHmrcSpec {
  trait Setup extends MockitoSugar with ArgumentMatchersSugar with ApiPublisherConnectorMockProvider {
    val serviceName = "ServiceName" + UUID.randomUUID

    implicit val hc = HeaderCarrier()

    val service = new DeploymentApprovalService(mockSandboxApiPublisherConnector, mockProductionApiPublisherConnector)
    val underTest = spy(service)
  }

  "fetchUnapprovedServices" should {
    "fetch the unapproved services" in new Setup {
      val expectedProductionSummaries = List(APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(PRODUCTION)))
      val expectedSandboxSummaries = List(APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(SANDBOX)))
      ApiPublisherConnectorMock.Prod.FetchUnapproved.returns(expectedProductionSummaries: _*)
      ApiPublisherConnectorMock.Sandbox.FetchUnapproved.returns(expectedSandboxSummaries: _*)

      val result = await(underTest.fetchUnapprovedServices())

      result shouldBe expectedSandboxSummaries ++ expectedProductionSummaries

      verify(mockProductionApiPublisherConnector).fetchUnapproved()(*)
    }
  }

  "fetchApiDefinitionSummary" should {
    "fetch the Api definition summary for sandbox" in new Setup {
      val expectedSummary = APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(SANDBOX))
      ApiPublisherConnectorMock.Sandbox.FetchApprovalSummary.returns(expectedSummary)

      val result = await(underTest.fetchApprovalSummary(serviceName, SANDBOX))

      result shouldBe expectedSummary

      verify(mockSandboxApiPublisherConnector).fetchApprovalSummary(eqTo(serviceName))(*)
      verify(mockProductionApiPublisherConnector, never).fetchApprovalSummary(*)(*)
    }

    "fetch the Api definition summary for production" in new Setup {
      val expectedSummary = APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(PRODUCTION))
      ApiPublisherConnectorMock.Prod.FetchApprovalSummary.returns(expectedSummary)

      val result = await(underTest.fetchApprovalSummary(serviceName, PRODUCTION))

      result shouldBe expectedSummary

      verify(mockProductionApiPublisherConnector).fetchApprovalSummary(eqTo(serviceName))(*)
      verify(mockSandboxApiPublisherConnector, never).fetchApprovalSummary(*)(*)
    }

  }

  "approveService" should {
    "approve the service in sandbox" in new Setup {
      ApiPublisherConnectorMock.Sandbox.ApproveService.succeeds()

      await(underTest.approveService(serviceName, SANDBOX))

      verify(mockSandboxApiPublisherConnector).approveService(eqTo(serviceName))(*)
      verify(mockProductionApiPublisherConnector, never).approveService(*)(*)
    }

    "approve the service in production" in new Setup {
      ApiPublisherConnectorMock.Prod.ApproveService.succeeds()

      await(underTest.approveService(serviceName, PRODUCTION))

      verify(mockProductionApiPublisherConnector).approveService(eqTo(serviceName))(*)
      verify(mockSandboxApiPublisherConnector, never).approveService(*)(*)
    }
  }

  "connectorFor" should {
    "return the sandbox API publisher connector when asked for sandbox" in new Setup {
      val connector = underTest.connectorFor(SANDBOX)

      connector shouldBe mockSandboxApiPublisherConnector
    }

    "return the production API publisher connector when asked for production" in new Setup {
      val connector = underTest.connectorFor(PRODUCTION)

      connector shouldBe mockProductionApiPublisherConnector
    }
  }
}
