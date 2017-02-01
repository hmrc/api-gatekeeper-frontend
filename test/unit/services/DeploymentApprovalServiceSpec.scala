/*
 * Copyright 2017 HM Revenue & Customs
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

package unit.services

import connectors.ApiDefinitionConnector
import model.{APIDefinitionSummary, ApproveServiceSuccessful}
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import services.DeploymentApprovalService
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class DeploymentApprovalServiceSpec extends UnitSpec with MockitoSugar {

  trait Setup {
    val testDeploymentApprovalService = new DeploymentApprovalService {
      val apiDefinitionConnector = mock[ApiDefinitionConnector]
    }
    implicit val hc = HeaderCarrier()
  }

  "Deployment Approval Service" should {

    "fetchUnapprovedServices calls apiDefintionConnector with appropriate parameters" in new Setup {

      val apiDefintionSummary = APIDefinitionSummary("api-calendar", "My Calendar", "My Calendar API")

      given(testDeploymentApprovalService.apiDefinitionConnector.fetchUnapproved()(any[HeaderCarrier])).willReturn(Future.successful(Seq(apiDefintionSummary)))
      val result = await(testDeploymentApprovalService.fetchUnapprovedServices)

      verify(testDeploymentApprovalService.apiDefinitionConnector).fetchUnapproved()(any[HeaderCarrier])
      result shouldBe Seq(apiDefintionSummary)
    }

    "fetchAPIDefintionSummary calls apiDefintionConnector with appropriate parameters" in new Setup {

      val apiDefinitionSummary = APIDefinitionSummary("api-calendar", "My Calendar", "My Calendar API")
      val serviceName = "api-calendar"

      val serviceNameCaptor = ArgumentCaptor.forClass(classOf[String])

      given(testDeploymentApprovalService.apiDefinitionConnector.fetchApiDefinitionSummary(serviceNameCaptor.capture())(any[HeaderCarrier])).willReturn(Future.successful(apiDefinitionSummary))

      val result = await(testDeploymentApprovalService.fetchApiDefinitionSummary(serviceName))

      verify(testDeploymentApprovalService.apiDefinitionConnector).fetchApiDefinitionSummary(any[String])(any[HeaderCarrier])
      result shouldBe apiDefinitionSummary
      serviceNameCaptor.getValue shouldBe serviceName
    }


    "approveService calls the apiDefinitionConnector with appropriate parameters" in new Setup {
      val apiDefinitionSummary = APIDefinitionSummary("api-calendar", "My Calendar", "My Calendar API")
      val serviceName = "api-calendar"

      val serviceNameCaptor = ArgumentCaptor.forClass(classOf[String])

      given(testDeploymentApprovalService.apiDefinitionConnector.approveService(serviceNameCaptor.capture())(any[HeaderCarrier])).willReturn(Future.successful(ApproveServiceSuccessful))

      val result = await(testDeploymentApprovalService.approveService(serviceName))

      verify(testDeploymentApprovalService.apiDefinitionConnector).approveService(any[String])(any[HeaderCarrier])
      result shouldBe ApproveServiceSuccessful
      serviceNameCaptor.getValue shouldBe serviceName
    }

  }
}
