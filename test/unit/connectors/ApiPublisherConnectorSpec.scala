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

package unit.connectors

import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import config.AppConfig
import connectors.ApiPublisherConnector
import model._
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class ApiPublisherConnectorSpec extends UnitSpec with WiremockSugar with MockitoSugar with BeforeAndAfterEach with WithFakeApplication {

  implicit val hc = HeaderCarrier()


  trait Setup {

    val mockAppConfig = mock[AppConfig]
    val httpClient = fakeApplication.injector.instanceOf[HttpClient]

    val underTest = new ApiPublisherConnector(mockAppConfig, httpClient)

    when(mockAppConfig.apiPublisherBaseUrl).thenReturn(wireMockUrl)

  }

  "fetchUnapproved" should {
    val serviceName = "ServiceName" + UUID.randomUUID()
    val getUrl = "/services/unapproved"

    "return unapproved API approval summaries" in new Setup {

      val response = Seq(APIApprovalSummary(serviceName, "aName", None))

      stubFor(get(urlPathMatching(getUrl))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withHeader("Content-Type", "application/json")
            .withBody(Json.toJson(response).toString())))

      val result: Seq[APIApprovalSummary] = await(underTest.fetchUnapproved())

      result shouldBe response
    }

    "fail when api-subscription-fields returns an internal server error" in new Setup {

      stubFor(get(urlPathMatching(getUrl))
        .willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withHeader("Content-Type", "application/json")))

      intercept[UpdateApiDefinitionsFailed] {
        await(underTest.fetchUnapproved())
      }
    }
  }

  "fetchApprovalSummary" should {
    val serviceName = "ServiceName" + UUID.randomUUID()
    val url = s"/service/$serviceName/summary"

    "return subscription fields definition for an API" in new Setup {
      val validResponse = APIApprovalSummary(serviceName, "aName", Some("aDescription"))

      stubFor(get(urlPathMatching(url))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withHeader("Content-Type", "application/json")
            .withBody(Json.toJson(validResponse).toString())))

      val result = await(underTest.fetchApprovalSummary(serviceName))

      result shouldBe validResponse
    }

    "fail when api-subscription-fields returns an internal server error" in new Setup {
      stubFor(get(urlPathMatching(url))
        .willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withHeader("Content-Type", "application/json")))

      intercept[UpdateApiDefinitionsFailed] {
        await(underTest.fetchApprovalSummary(serviceName))
      }
    }
  }

  "approveService" should {

    val serviceName = "ServiceName" + UUID.randomUUID()
    val url = s"/service/$serviceName/approve"

    "save the fields" in new Setup {

      var approveServiceRequest = ApproveServiceRequest(serviceName)

      stubFor(post(urlPathMatching(url))
        .willReturn(
          aResponse()
            .withStatus(OK)))

      await(underTest.approveService(serviceName))

      verify(postRequestedFor(urlPathMatching(url)).withRequestBody(equalToJson(Json.toJson(approveServiceRequest).toString())))
    }

    "fail when api-subscription-fields returns an internal server error" in new Setup {

      stubFor(post(urlPathMatching(url))
        .willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withHeader("Content-Type", "application/json")))

      intercept[UpdateApiDefinitionsFailed] {
        await(underTest.approveService(serviceName))
      }
    }
  }
}
