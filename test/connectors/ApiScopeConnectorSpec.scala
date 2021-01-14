/*
 * Copyright 2021 HM Revenue & Customs
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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import config.AppConfig
import model.{ApiScope, FetchApiDefinitionsFailed}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec
import play.api.http.Status._

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.WithFakeApplication
import org.mockito.MockitoSugar

class ApiScopeConnectorSpec
  extends UnitSpec
    with MockitoSugar
    with WiremockSugar
    with WithFakeApplication {

  class Setup(proxyEnabled: Boolean = false) {
    implicit val hc = HeaderCarrier()

    val httpClient = fakeApplication.injector.instanceOf[HttpClient]

    val mockAppConfig: AppConfig = mock[AppConfig]
    when(mockAppConfig.apiScopeProductionBaseUrl).thenReturn(wireMockUrl)

    val connector = new ProductionApiScopeConnector(mockAppConfig, httpClient)
  }

  "fetchAll" should {
    val url = "/scope"
    
    "fetch a sequence of API scopes" in new Setup {
      val scopes = Seq(ApiScope("aKey", "aName", "aDescription"))
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
