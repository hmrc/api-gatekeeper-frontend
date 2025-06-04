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

package uk.gov.hmrc.gatekeeper.services

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

import mocks.connectors.ApiPublisherConnectorMockProvider
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.gatekeeper.models.APIApprovalSummary
import uk.gov.hmrc.gatekeeper.models.ApprovalStatus.APPROVED

class DeploymentApprovalServiceSpec extends AsyncHmrcSpec {

  trait Setup extends MockitoSugar with ArgumentMatchersSugar with ApiPublisherConnectorMockProvider {
    val serviceName = "ServiceName" + UUID.randomUUID

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val service   = new DeploymentApprovalService(mockSandboxApiPublisherConnector, mockProductionApiPublisherConnector)
    val underTest = spy(service)
  }

  "fetchAllServices" should {
    "fetch all the services" in new Setup {
      val expectedProductionSummaries = List(APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(Environment.PRODUCTION)))
      val expectedSandboxSummaries    = List(APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(Environment.SANDBOX), status = APPROVED))
      ApiPublisherConnectorMock.Prod.FetchAll.returns(expectedProductionSummaries: _*)
      ApiPublisherConnectorMock.Sandbox.FetchAll.returns(expectedSandboxSummaries: _*)

      val result = await(underTest.fetchAllServices())

      result shouldBe expectedSandboxSummaries ++ expectedProductionSummaries

      verify(mockProductionApiPublisherConnector).fetchAll()(*)
    }
  }

  "searchServices" should {
    "call production Api publisher connector correctly" in new Setup {
      val expectedProductionSummaries = List(APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(Environment.PRODUCTION), status = APPROVED))
      val expectedSandboxSummaries    = List(APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(Environment.SANDBOX), status = APPROVED))
      ApiPublisherConnectorMock.Prod.SearchServices.returns(expectedProductionSummaries: _*)
      ApiPublisherConnectorMock.Sandbox.SearchServices.returns(expectedSandboxSummaries: _*)

      val result = await(underTest.searchServices(Seq("status" -> "NEW", "status" -> "APPROVED")))

      result shouldBe expectedSandboxSummaries ++ expectedProductionSummaries

      verify(mockProductionApiPublisherConnector).searchServices(eqTo(Seq("status" -> "NEW", "status" -> "APPROVED")))(*)
    }

    "returns results sorted in order of newest first (created date " in new Setup {
      val prodSummary1    = APIApprovalSummary(serviceName, "prodSummary1", Option("aDescription"), Some(Environment.PRODUCTION), status = APPROVED)
      val sandboxSummary1 = APIApprovalSummary(
        serviceName,
        "sandboxSummary1",
        Option("aDescription"),
        Some(Environment.SANDBOX),
        status = APPROVED,
        createdOn = prodSummary1.createdOn.map(_.plusSeconds(1))
      )
      val prodSummary2    = APIApprovalSummary(
        serviceName,
        "prodSummary2",
        Option("aDescription"),
        Some(Environment.PRODUCTION),
        status = APPROVED,
        createdOn = sandboxSummary1.createdOn.map(_.plusSeconds(1))
      )
      val sandboxSummary2 = APIApprovalSummary(
        serviceName,
        "sandboxSummary2",
        Option("aDescription"),
        Some(Environment.SANDBOX),
        status = APPROVED,
        createdOn = prodSummary2.createdOn.map(_.plusSeconds(1))
      )
      val sandboxSummary3 = APIApprovalSummary(serviceName, "sandboxSummary2", Option("aDescription"), Some(Environment.SANDBOX), status = APPROVED, createdOn = None)

      val expectedProductionSummaries = List(prodSummary1, prodSummary2)
      val expectedSandboxSummaries    = List(sandboxSummary1, sandboxSummary2, sandboxSummary3)

      ApiPublisherConnectorMock.Prod.SearchServices.returns(expectedProductionSummaries: _*)
      ApiPublisherConnectorMock.Sandbox.SearchServices.returns(expectedSandboxSummaries: _*)

      val result = await(underTest.searchServices(Seq("status" -> "NEW", "status" -> "APPROVED")))

      result shouldBe List(sandboxSummary2, prodSummary2, sandboxSummary1, prodSummary1, sandboxSummary3)

      verify(mockProductionApiPublisherConnector).searchServices(eqTo(Seq("status" -> "NEW", "status" -> "APPROVED")))(*)
    }
  }

  "fetchApiDefinitionSummary" should {
    "fetch the Api definition summary for sandbox" in new Setup {
      val expectedSummary = APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(Environment.SANDBOX))
      ApiPublisherConnectorMock.Sandbox.FetchApprovalSummary.returns(expectedSummary)

      val result = await(underTest.fetchApprovalSummary(serviceName, Environment.SANDBOX))

      result shouldBe expectedSummary

      verify(mockSandboxApiPublisherConnector).fetchApprovalSummary(eqTo(serviceName))(*)
      verify(mockProductionApiPublisherConnector, never).fetchApprovalSummary(*)(*)
    }

    "fetch the Api definition summary for production" in new Setup {
      val expectedSummary = APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(Environment.PRODUCTION))
      ApiPublisherConnectorMock.Prod.FetchApprovalSummary.returns(expectedSummary)

      val result = await(underTest.fetchApprovalSummary(serviceName, Environment.PRODUCTION))

      result shouldBe expectedSummary

      verify(mockProductionApiPublisherConnector).fetchApprovalSummary(eqTo(serviceName))(*)
      verify(mockSandboxApiPublisherConnector, never).fetchApprovalSummary(*)(*)
    }

  }

  "approveService" should {
    val gatekeeperUser: Actors.GatekeeperUser = Actors.GatekeeperUser("GK User")
    val notes                                 = Some("Service approved")
    "approve the service in sandbox" in new Setup {
      ApiPublisherConnectorMock.Sandbox.ApproveService.succeeds()

      await(underTest.approveService(serviceName, Environment.SANDBOX, gatekeeperUser, notes))

      verify(mockSandboxApiPublisherConnector).approveService(eqTo(serviceName), eqTo(gatekeeperUser), eqTo(notes))(*)
      verify(mockProductionApiPublisherConnector, never).approveService(*, *, *)(*)
    }

    "approve the service in production" in new Setup {
      ApiPublisherConnectorMock.Prod.ApproveService.succeeds()

      await(underTest.approveService(serviceName, Environment.PRODUCTION, gatekeeperUser, notes))

      verify(mockProductionApiPublisherConnector).approveService(eqTo(serviceName), eqTo(gatekeeperUser), eqTo(notes))(*)
      verify(mockSandboxApiPublisherConnector, never).approveService(*, *, *)(*)
    }
  }

  "declineService" should {
    val gatekeeperUser: Actors.GatekeeperUser = Actors.GatekeeperUser("GK User")
    val notes                                 = Some("Service decline")
    "decline the service in sandbox" in new Setup {
      ApiPublisherConnectorMock.Sandbox.DeclineService.succeeds()

      await(underTest.declineService(serviceName, Environment.SANDBOX, gatekeeperUser, notes))

      verify(mockSandboxApiPublisherConnector).declineService(eqTo(serviceName), eqTo(gatekeeperUser), eqTo(notes))(*)
      verify(mockProductionApiPublisherConnector, never).declineService(*, *, *)(*)
    }

    "decline the service in production" in new Setup {
      ApiPublisherConnectorMock.Prod.DeclineService.succeeds()

      await(underTest.declineService(serviceName, Environment.PRODUCTION, gatekeeperUser, notes))

      verify(mockProductionApiPublisherConnector).declineService(eqTo(serviceName), eqTo(gatekeeperUser), eqTo(notes))(*)
      verify(mockSandboxApiPublisherConnector, never).declineService(*, *, *)(*)
    }
  }

  "addComment" should {
    val gatekeeperUser: Actors.GatekeeperUser = Actors.GatekeeperUser("GK User")
    val notes                                 = "Service decline"

    "add comment in sandbox" in new Setup {
      ApiPublisherConnectorMock.Sandbox.AddComment.succeeds()

      await(underTest.addComment(serviceName, Environment.SANDBOX, gatekeeperUser, notes))

      verify(mockSandboxApiPublisherConnector).addComment(eqTo(serviceName), eqTo(gatekeeperUser), eqTo(notes))(*)
      verify(mockProductionApiPublisherConnector, never).addComment(*, *, *)(*)
    }

    "add comment in production" in new Setup {
      ApiPublisherConnectorMock.Prod.AddComment.succeeds()

      await(underTest.addComment(serviceName, Environment.PRODUCTION, gatekeeperUser, notes))

      verify(mockProductionApiPublisherConnector).addComment(eqTo(serviceName), eqTo(gatekeeperUser), eqTo(notes))(*)
      verify(mockSandboxApiPublisherConnector, never).addComment(*, *, *)(*)
    }
  }

  "connectorFor" should {
    "return the sandbox API publisher connector when asked for sandbox" in new Setup {
      val connector = underTest.connectorFor(Environment.SANDBOX)

      connector shouldBe mockSandboxApiPublisherConnector
    }

    "return the production API publisher connector when asked for production" in new Setup {
      val connector = underTest.connectorFor(Environment.PRODUCTION)

      connector shouldBe mockProductionApiPublisherConnector
    }
  }
}
