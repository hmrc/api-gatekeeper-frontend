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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2

import uk.gov.hmrc.apiplatform.modules.common.utils._
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models.{ApiScope, FetchApiDefinitionsFailed}

class ApiScopeConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite {

  class Setup(proxyEnabled: Boolean = false) {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val httpClient = app.injector.instanceOf[HttpClientV2]

    val mockAppConfig: AppConfig = mock[AppConfig]
    when(mockAppConfig.apiScopeProductionBaseUrl).thenReturn(wireMockUrl)

    val connector = new ProductionApiScopeConnector(mockAppConfig, httpClient)
  }

  "fetchAll" should {
    val url = "/scope"

    "fetch a sequence of API scopes" in new Setup {
      val scopes  = Seq(ApiScope("aKey", "aName", "aDescription"))
      val payload = Json.toJson(scopes)

      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(payload.toString)
          )
      )

      val result = await(connector.fetchAll())

      result shouldBe scopes
    }

    "fail to fetch a sequence of API scopes" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )
      intercept[FetchApiDefinitionsFailed] {
        await(connector.fetchAll())
      }
    }
  }
}
