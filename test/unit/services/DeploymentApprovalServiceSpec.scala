/*
 * Copyright 2019 HM Revenue & Customs
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

import java.util.UUID

import connectors.ApiPublisherConnector
import model.APIApprovalSummary
import org.mockito.BDDMockito.given
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.verify
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import services.DeploymentApprovalService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class DeploymentApprovalServiceSpec extends UnitSpec with ScalaFutures with MockitoSugar {

  val mockApiPublisherConnector = mock[ApiPublisherConnector]

  trait Setup {
    val serviceName = "ServiceName" + UUID.randomUUID

    implicit val hc = HeaderCarrier()

    val underTest = new DeploymentApprovalService(mockApiPublisherConnector)
  }

  "fetchUnapprovedServices" should {
    "fetch the unapproved services" in new Setup {

      val expectedSummaries = Seq(APIApprovalSummary(serviceName, "aName", Option("aDescription")))

      given(mockApiPublisherConnector.fetchUnapproved()(any[HeaderCarrier])).willReturn(Future.successful(expectedSummaries))

      val result = await(underTest.fetchUnapprovedServices())

      result shouldBe expectedSummaries

      verify(mockApiPublisherConnector).fetchUnapproved()(any[HeaderCarrier])
    }
  }

  "fetchApiDefinitionSummary" should {
    "fetch the Api definition summary" in new Setup {

      val expectedSummary = APIApprovalSummary(serviceName, "aName", Option("aDescription"))

      given(mockApiPublisherConnector.fetchApprovalSummary(any())(any[HeaderCarrier])).willReturn(Future.successful(expectedSummary))

      val result = await(underTest.fetchApiDefinitionSummary(serviceName))

      result shouldBe expectedSummary

      verify(mockApiPublisherConnector).fetchApprovalSummary(eqTo(serviceName))(any[HeaderCarrier])
    }

  }

  "approveService" should {
    "approve the service" in new Setup {

      given(mockApiPublisherConnector.approveService(any())(any[HeaderCarrier])).willReturn(Future.successful())

      val result = await(underTest.approveService(serviceName))

      verify(mockApiPublisherConnector).approveService(eqTo(serviceName))(any[HeaderCarrier])
    }
  }
}
