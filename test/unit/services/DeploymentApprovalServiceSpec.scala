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

import connectors.ApiPublisherConnector
import model.{APIApprovalSummary, ApproveServiceSuccessful}
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
      val apiPublisherConnector = mock[ApiPublisherConnector]
    }
    implicit val hc = HeaderCarrier()
  }

  "Deployment Approval Service" should {

    "fetchUnapprovedServices calls apiDefintionConnector with appropriate parameters" in new Setup {

      val apiDefintionSummary = APIApprovalSummary("api-calendar", "My Calendar", Some("My Calendar API"))

      given(testDeploymentApprovalService.apiPublisherConnector.fetchUnapproved()(any[HeaderCarrier])).willReturn(Future.successful(Seq(apiDefintionSummary)))
      val result = await(testDeploymentApprovalService.fetchUnapprovedServices)

      verify(testDeploymentApprovalService.apiPublisherConnector).fetchUnapproved()(any[HeaderCarrier])
      result shouldBe Seq(apiDefintionSummary)
    }

    "fetchAPIDefintionSummary calls apiDefintionConnector with appropriate parameters" in new Setup {

      val apiDefinitionSummary = APIApprovalSummary("api-calendar", "My Calendar", Some("My Calendar API"))
      val serviceName = "api-calendar"

      val serviceNameCaptor = ArgumentCaptor.forClass(classOf[String])

      given(testDeploymentApprovalService.apiPublisherConnector.fetchApprovalSummary(serviceNameCaptor.capture())(any[HeaderCarrier])).willReturn(Future.successful(apiDefinitionSummary))

      val result = await(testDeploymentApprovalService.fetchApiDefinitionSummary(serviceName))

      verify(testDeploymentApprovalService.apiPublisherConnector).fetchApprovalSummary(any[String])(any[HeaderCarrier])
      result shouldBe apiDefinitionSummary
      serviceNameCaptor.getValue shouldBe serviceName
    }


    "approveService calls the apiDefinitionConnector with appropriate parameters" in new Setup {
      val apiDefinitionSummary = APIApprovalSummary("api-calendar", "My Calendar", Some("My Calendar API"))
      val serviceName = "api-calendar"

      val serviceNameCaptor = ArgumentCaptor.forClass(classOf[String])

      given(testDeploymentApprovalService.apiPublisherConnector.approveService(serviceNameCaptor.capture())(any[HeaderCarrier])).willReturn(Future.successful(ApproveServiceSuccessful))

      val result = await(testDeploymentApprovalService.approveService(serviceName))

      verify(testDeploymentApprovalService.apiPublisherConnector).approveService(any[String])(any[HeaderCarrier])
      result shouldBe ApproveServiceSuccessful
      serviceNameCaptor.getValue shouldBe serviceName
    }

  }
}
