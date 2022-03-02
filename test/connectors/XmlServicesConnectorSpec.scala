/*
 * Copyright 2022 HM Revenue & Customs
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
import model._
import model.xml.XmlApi
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, Upstream5xxResponse, UpstreamErrorResponse}
import utils.{AsyncHmrcSpec, UrlEncoding, WireMockSugar}

import scala.concurrent.ExecutionContext.Implicits.global

class XmlServicesConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with UrlEncoding {

  val apiVersion1 = ApiVersion.random
  val applicationId = ApplicationId.random
  trait Setup {
    val authToken = "Bearer Token"
    implicit val hc = HeaderCarrier().withExtraHeaders(("Authorization", authToken))

    val httpClient = app.injector.instanceOf[HttpClient]
    val mockAppConfig: XmlServicesConnector.Config = mock[XmlServicesConnector.Config]
    when(mockAppConfig.serviceBaseUrl).thenReturn(wireMockUrl)

    val connector = new XmlServicesConnector(mockAppConfig, httpClient)

    val xmlApiOne = XmlApi(
      name = "xml api 1",
      serviceName = "service name",
      context = "context",
      description = "description")

    val xmlApiTwo = xmlApiOne.copy(name = "xml api 2")
    val xmlApis = Seq(xmlApiOne, xmlApiTwo)

  }

  "getAllApis" should {
    val url = s"/xml/apis"

    "return no APIs" in new Setup {
      stubFor(
        get(urlEqualTo(url))
        .willReturn(
          aResponse()
          .withStatus(OK)
          .withBody("[]")
        )
      )
      await(connector.getAllApis) shouldBe Seq.empty
    }

    "return APIs" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson(xmlApis).toString)
          )
      )
      await(connector.getAllApis) shouldBe xmlApis
    }

    "return UpstreamErrorResponse" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )
      intercept[UpstreamErrorResponse](await(connector.getAllApis)) match {
        case (e: UpstreamErrorResponse) => succeed
        case _                          => fail
      }
    }
  }

}
