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

package unit.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import config.WSHttp
import connectors.ApiPublisherConnector
import model._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, Matchers}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class ApiPublisherConnectorSpec extends UnitSpec with Matchers with ScalaFutures with WiremockSugar with BeforeAndAfterEach with WithFakeApplication {

  trait Setup {
    implicit val hc = HeaderCarrier()

    val connector = new ApiPublisherConnector {
      override val http = WSHttp
      override val serviceBaseUrl: String = wireMockUrl
    }
  }

  "fetchUnapproved" should {
    "respond with a 200 and convert the response body" in new Setup {
      stubFor(get(urlEqualTo(s"/unapproved")).willReturn(aResponse().withStatus(200).withBody(
        """
          |[
          | {
          |   "serviceName": "employmentAPI",
          |   "serviceUrl": "http://employment.service",
          |   "name": "Employment API",
          |   "description": "My Employment API",
          |   "approved": false
          | },
          | {
          |   "serviceName": "incomeAPI",
          |   "serviceUrl": "http://income.service",
          |   "name": "Income API",
          |   "description": "My Income API",
          |   "approved": false
          | }
          | ]
        """.stripMargin)))
      val result: Seq[APIApprovalSummary] = await(connector.fetchUnapproved())

      result shouldBe Seq(APIApprovalSummary("employmentAPI", "Employment API", Some("My Employment API")),
        APIApprovalSummary("incomeAPI", "Income API", Some("My Income API")))
    }

    "propagate FetchApiDefinitionsFailed exception when service fails" in new Setup {
      stubFor(get(urlEqualTo(s"/unapproved")).willReturn(aResponse().withStatus(500)))

      intercept[FetchApiDefinitionsFailed](await(connector.fetchUnapproved()))
    }
  }

  "approveService" should {

    "respond with a 204" in new Setup {
      val serviceName = "my-test-service"

      stubFor(post(urlEqualTo(s"/$serviceName/approve")).willReturn(aResponse().withStatus(204)))
      val result = await(connector.approveService(serviceName))
      verify(1, postRequestedFor(urlPathEqualTo(s"/$serviceName/approve")))

      result shouldBe ApproveServiceSuccessful
    }

  }
}
