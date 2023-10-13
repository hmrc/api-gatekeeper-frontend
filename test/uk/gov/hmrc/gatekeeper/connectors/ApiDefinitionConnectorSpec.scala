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

import scala.concurrent.ExecutionContext.Implicits.global

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HttpClient, _}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.{AsyncHmrcSpec, WireMockSugar}
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models._

class ApiDefinitionConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite {

  class Setup(proxyEnabled: Boolean = false) {
    implicit val hc = HeaderCarrier()

    val httpClient = app.injector.instanceOf[HttpClient]

    val mockAppConfig: AppConfig = mock[AppConfig]
    when(mockAppConfig.apiDefinitionProductionBaseUrl).thenReturn(wireMockUrl)

    val connector   = new ProductionApiDefinitionConnector(mockAppConfig, httpClient)
    val apiVersion1 = ApiVersionNbr.random
  }

  "fetchAll" should {
    val url = "/api-definition"

    "respond with 200 and convert body" in new Setup {
      val response = List(ApiDefinition(
        ServiceName("dummyAPI"),
        "http://localhost/",
        "dummyAPI",
        "dummy api.",
        ApiContext("dummy-api"),
        List(ApiVersion(apiVersion1, ApiStatus.STABLE, ApiAccess.PUBLIC, List.empty, false, None, ApiVersionSource.UNKNOWN)),
        false,
        false,
        None,
        List(ApiCategory.OTHER)
      ))

      val payload = Json.toJson(response)

      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(payload.toString)
          )
      )

      await(connector.fetchPublic()) shouldBe response
    }

    "propagate 500 as FetchApiDefinitionsFailed exception" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      intercept[FetchApiDefinitionsFailed](await(connector.fetchPublic()))
    }
  }

  "fetchPrivate" should {
    val url = "/api-definition"

    "respond with 200 and convert body" in new Setup {
      val response = List(ApiDefinition(
        ServiceName("dummyAPI"),
        "http://localhost/",
        "dummyAPI",
        "dummy api.",
        ApiContext("dummy-api"),
        List(ApiVersion(apiVersion1, ApiStatus.STABLE, ApiAccess.PUBLIC, List.empty, false, None, ApiVersionSource.UNKNOWN)),
        false,
        false,
        None,
        List(ApiCategory.OTHER)
      ))

      val payload = Json.toJson(response)

      stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam("type", equalTo("private"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(payload.toString)
          )
      )

      await(connector.fetchPrivate()) shouldBe response
    }

    "propagate 500 as FetchApiDefinitionsFailed exception" in new Setup {
      stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam("type", equalTo("private"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      intercept[FetchApiDefinitionsFailed](await(connector.fetchPrivate()))
    }
  }
}
