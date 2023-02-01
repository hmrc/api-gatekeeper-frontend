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

import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, Upstream5xxResponse}

import uk.gov.hmrc.apiplatform.modules.common.utils._
import uk.gov.hmrc.gatekeeper.connectors.ApiCataloguePublishConnector._
import uk.gov.hmrc.gatekeeper.utils.{UrlEncoding, _}

class ApiCataloguePublishConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with UrlEncoding {

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

    val mockApiCataloguePublishConnectorConfig: ApiCataloguePublishConnector.Config = mock[ApiCataloguePublishConnector.Config]
    when(mockApiCataloguePublishConnectorConfig.serviceBaseUrl).thenReturn(wireMockUrl)

    val underTest = new ApiCataloguePublishConnector(mockApiCataloguePublishConnectorConfig, httpClient)

    def primePost(url: String, status: Int): Unit = {
      stubFor(
        post(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(status)
          )
      )
    }

    def primePostWithBody(url: String, status: Int, response: String): Unit = {
      stubFor(
        post(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(status)
              .withBody(response)
          )
      )
    }
  }

  "ApiCataloguePublishConnector" when {

    "publishByServiceName" should {

      val serviceName = "Hello-World"
      val url         = s"/api-platform-api-catalogue-publish/publish/$serviceName"

      "return Right(PublishResponse)" in new Setup {

        val expectedPublishResponse: PublishResponse = PublishResponse("id", "publishReference", "platformType")
        val responseAsJsonString: String             = Json.toJson(expectedPublishResponse).toString

        primePostWithBody(url, OK, responseAsJsonString)
        val result: Either[Throwable, PublishResponse] = await(underTest.publishByServiceName(serviceName))
        result match {
          case Right(response: PublishResponse) => response shouldBe expectedPublishResponse
          case Left(_: Throwable)               => fail()
        }
      }

      "return Left if there is an error in the backend" in new Setup {

        primePost(url, INTERNAL_SERVER_ERROR)

        val result: Either[Throwable, PublishResponse] = await(underTest.publishByServiceName(serviceName))
        result match {
          case Left(_: Upstream5xxResponse) => succeed
          case _                            => fail
        }

      }
    }

    "publishAll" should {
      val publishAllUrl = "/api-platform-api-catalogue-publish/publish-all"

      "return Right" in new Setup {

        val expectedResponse: PublishAllResponse =
          PublishAllResponse(message = "Publish all called and is working in the background, check application logs for progress")
        val expectedResponseAsString: String     = Json.toJson(expectedResponse).toString

        primePostWithBody(publishAllUrl, OK, expectedResponseAsString)

        val result: Either[Throwable, PublishAllResponse] = await(underTest.publishAll)
        result match {
          case Right(response: PublishAllResponse) => response shouldBe expectedResponse
          case Left(_: Throwable)                  => fail()
        }
      }

      "return Left if there is an error in the backend" in new Setup {
        primePost(publishAllUrl, INTERNAL_SERVER_ERROR)
        val result: Either[Throwable, PublishAllResponse] = await(underTest.publishAll)
        result match {
          case Left(_: Upstream5xxResponse) => succeed
          case _                            => fail
        }
      }
    }
  }
}
