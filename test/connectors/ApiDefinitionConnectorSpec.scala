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

import play.api.libs.json.Json
import com.github.tomakehurst.wiremock.client.WireMock._
import config.AppConfig
import model._
import play.api.http.Status._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import utils.{AsyncHmrcSpec, WireMockSugar}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class ApiDefinitionConnectorSpec
  extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite {

  class Setup(proxyEnabled: Boolean = false) {
    implicit val hc = HeaderCarrier()

    val httpClient = app.injector.instanceOf[HttpClient]

    val mockAppConfig: AppConfig = mock[AppConfig]
    when(mockAppConfig.apiDefinitionProductionBaseUrl).thenReturn(wireMockUrl)

    val connector = new ProductionApiDefinitionConnector(mockAppConfig, httpClient)
    val apiVersion1 = ApiVersion.random
  }

  import model.APIDefinitionFormatters._

  "fetchAll" should {
    val url = "/api-definition"

    "respond with 200 and convert body" in new Setup {
      val response = List(ApiDefinition(
        "dummyAPI", "http://localhost/",
        "dummyAPI", "dummy api.", ApiContext("dummy-api"),
        List(ApiVersionDefinition(apiVersion1, ApiStatus.STABLE, Some(ApiAccess(APIAccessType.PUBLIC)))), Some(false), None))

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
        "dummyAPI", "http://localhost/",
        "dummyAPI", "dummy api.", ApiContext("dummy-api"),
        List(ApiVersionDefinition(apiVersion1, ApiStatus.STABLE, Some(ApiAccess(APIAccessType.PUBLIC)))), Some(false), None))

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
        .withQueryParam("type",equalTo("private"))
        .willReturn(
          aResponse()
          .withStatus(INTERNAL_SERVER_ERROR)
        )
      )

      intercept[FetchApiDefinitionsFailed](await(connector.fetchPrivate()))
    }
  }

  "fetchAPICategories" should {
    val url = "/api-categories"

    "respond with 200 and convert body" in new Setup {
      val response = List(APICategoryDetails("Business", "Business"), APICategoryDetails("VAT", "Vat"))

      val payload = Json.toJson(response)

      stubFor(
        get(urlEqualTo(url))
        .willReturn(
          aResponse()
          .withStatus(OK)
          .withBody(payload.toString)
        )
      )

      await(connector.fetchAPICategories()) shouldBe response
    }

    "propagate 500 as FetchApiDefinitionsFailed exception" in new Setup {
      stubFor(
        get(urlEqualTo(url))
        .willReturn(
          aResponse()
          .withStatus(INTERNAL_SERVER_ERROR)
        )
      )
      
      intercept[FetchApiCategoriesFailed](await(connector.fetchAPICategories()))
    }
  }
}
