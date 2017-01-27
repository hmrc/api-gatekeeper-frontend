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
import connectors.ApiDefinitionConnector
import model._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, Matchers}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class ApiDefinitionConnectorSpec extends UnitSpec with Matchers with ScalaFutures with WiremockSugar with BeforeAndAfterEach with WithFakeApplication {

  trait Setup {
    implicit val hc = HeaderCarrier()

    val connector = new ApiDefinitionConnector {
      override val http = WSHttp
      override val serviceBaseUrl: String = wireMockUrl
    }
  }

  "fetchAll" should {
    "responds with 200 and converts body" in new Setup {
      val applicationId = "anApplicationId"
      val gatekeeperId = "loggedin.gatekeeper"
      stubFor(get(urlEqualTo(s"/api-definition")).willReturn(aResponse().withStatus(200).withBody(
        """
          |[
          | {
          |   "serviceName": "dummyAPI",
          |   "serviceBaseUrl": "http://dummy-api.service/",
          |   "name": "dummyAPI",
          |   "description": "dummy api.",
          |   "context": "dummy-api",
          |   "versions": [
          |     {
          |       "version": "1.0",
          |       "status": "PUBLISHED",
          |       "access": {
          |         "type": "PUBLIC"
          |       },
          |       "endpoints": [
          |         {
          |           "uriPattern": "/arrgh",
          |           "endpointName": "dummyAPI",
          |           "method": "GET",
          |           "authType": "USER",
          |           "throttlingTier": "UNLIMITED",
          |           "scope": "read:dummy-api-2"
          |         }
          |       ]
          |     }
          |   ],
          |   "requiresTrust": false
          | }
          |]
        """.stripMargin)))
      val result: Seq[APIDefinition] = await(connector.fetchAll())

      result shouldBe Seq(APIDefinition(
        "dummyAPI", "http://dummy-api.service/",
        "dummyAPI", "dummy api.", "dummy-api",
        Seq(APIVersion("1.0", APIStatus.PUBLISHED, Some(APIAccess(APIAccessType.PUBLIC)))), Some(false)
      ))
    }

    "propagate FetchApiDefinitionsFailed exception" in new Setup {
      stubFor(get(urlEqualTo(s"/api-definition")).willReturn(aResponse().withStatus(500)))

      intercept[FetchApiDefinitionsFailed](await(connector.fetchAll()))
    }
  }

  "fetchUnapproved" should {

    "respond with a 200 and convert the response body" in new Setup {
      stubFor(get(urlEqualTo(s"/api-definition/unapproved")).willReturn(aResponse().withStatus(200).withBody(
        """
          |[
          | {
          |   "serviceName": "employmentAPI",
          |   "name": "Employment API",
          |   "description": "My Employment API"
          | },
          | {
          |   "serviceName": "incomeAPI",
          |   "name": "Income API",
          |   "description": "My Income API"
          | }
          | ]
        """.stripMargin)))
      val result: Seq[APIDefinitionSummary] = await(connector.fetchUnapproved())

      result shouldBe Seq(APIDefinitionSummary("employmentAPI", "Employment API", "My Employment API"),
        APIDefinitionSummary("incomeAPI", "Income API", "My Income API"))
    }

    "propagate FetchApiDefinitionsFailed exception when service fails" in new Setup {
      stubFor(get(urlEqualTo(s"/api-definition/unapproved")).willReturn(aResponse().withStatus(500)))

      intercept[FetchApiDefinitionsFailed](await(connector.fetchUnapproved()))
    }
  }

  "approveService" should {

    "respond with a 204" in new Setup {
      val serviceName = "my-test-service"

      stubFor(post(urlEqualTo(s"/api-definition/$serviceName/approve")).willReturn(aResponse().withStatus(204)))
      val result = await(connector.approveService(serviceName))
      verify(1, postRequestedFor(urlPathEqualTo(s"/api-definition/$serviceName/approve")))

      result shouldBe ApproveServiceSuccessful
    }

  }
}
