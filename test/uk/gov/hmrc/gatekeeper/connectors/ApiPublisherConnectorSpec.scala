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

package uk.gov.hmrc.gatekeeper.connectors

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils._
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models.ApprovalState.APPROVED
import uk.gov.hmrc.gatekeeper.models._

class ApiPublisherConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite {

  class Setup(proxyEnabled: Boolean = false) {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val httpClient = app.injector.instanceOf[HttpClientV2]

    val mockAppConfig: AppConfig = mock[AppConfig]
    when(mockAppConfig.apiPublisherProductionBaseUrl).thenReturn(wireMockUrl)

    val connector   = new ProductionApiPublisherConnector(mockAppConfig, httpClient)
    val apiVersion1 = ApiVersionNbr.random
  }

  "fetchUnapproved" should {
    val serviceName = "ServiceName" + UUID.randomUUID()
    val url         = "/services/unapproved"

    "return unapproved API approval summaries" in new Setup {
      val response = Seq(APIApprovalSummary(serviceName, "aName", None, Some(Environment.PRODUCTION)))
      val payload  = Json.toJson(response)

      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(payload.toString)
          )
      )

      await(connector.fetchUnapproved()) shouldBe response
    }

    "fail when api-subscription-fields returns an internal server error" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      intercept[UpstreamErrorResponse] {
        await(connector.fetchUnapproved())
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "fetchAll" should {
    val serviceName = "ServiceName" + UUID.randomUUID()
    val url         = "/services"

    "return all API approval summaries" in new Setup {
      val response = Seq(APIApprovalSummary(serviceName, "aName", None, Some(Environment.PRODUCTION), state = APPROVED))
      val payload  = Json.toJson(response)

      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(payload.toString)
          )
      )

      await(connector.fetchAll()) shouldBe response
    }

    "fail when api-subscription-fields returns an internal server error" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      intercept[UpstreamErrorResponse] {
        await(connector.fetchAll())
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "fetchApprovalSummary" should {
    val serviceName = "ServiceName" + UUID.randomUUID()
    val url         = s"/service/$serviceName/summary"

    "return approval summary for an API" in new Setup {
      val validResponse = APIApprovalSummary(serviceName, "aName", Some("aDescription"), Some(Environment.PRODUCTION))

      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(validResponse)))
          )
      )

      await(connector.fetchApprovalSummary(serviceName)) shouldBe validResponse
    }

    "fail when fetch approval summary returns an internal server error" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      intercept[UpstreamErrorResponse] {
        await(connector.fetchApprovalSummary(serviceName))
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "approveService" should {
    val serviceName           = "ServiceName" + UUID.randomUUID()
    val url                   = s"/service/$serviceName/approve"
    val actor                 = Actors.GatekeeperUser("GK User")
    val approveServiceRequest = ApproveServiceRequest(serviceName, actor)

    "return ok for approve service" in new Setup {
      stubFor(
        post(urlEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(approveServiceRequest))))
          .willReturn(
            aResponse()
              .withStatus(OK)
          )
      )

      await(connector.approveService(serviceName, actor)) shouldBe ((): Unit)
    }

    "fail when approve service returns an internal server error" in new Setup {
      stubFor(
        post(urlEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(approveServiceRequest))))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      intercept[UpstreamErrorResponse] {
        await(connector.approveService(serviceName, actor))
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
