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

import utils.AsyncHmrcSpec
import uk.gov.hmrc.http.HttpClient
import scala.concurrent.ExecutionContext.Implicits.global
import model.applications.ApplicationWithSubscriptionData
import model.ApplicationId
import uk.gov.hmrc.http.HeaderCarrier
import builder.{ApplicationBuilder, ApiBuilder}
import model._
import model.subscriptions.ApiData
import play.api.test.Helpers._
import play.api.libs.json.Json
import model.subscriptions.VersionData
import model.APIDefinitionFormatters._
import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.http.Upstream5xxResponse
import utils.WireMockSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import ApiCataloguePublishConnector._

class ApiCataloguePublishConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with utils.UrlEncoding {

  trait Setup {
    implicit val hc = HeaderCarrier()

    val httpClient = app.injector.instanceOf[HttpClient]

    val mockApiCataloguePublishConnectorConfig: ApiCataloguePublishConnector.Config = mock[ApiCataloguePublishConnector.Config]
    when(mockApiCataloguePublishConnectorConfig.serviceBaseUrl).thenReturn(wireMockUrl)

    val underTest = new ApiCataloguePublishConnector(mockApiCataloguePublishConnectorConfig, httpClient)

    def primePost(url: String, status: Int) = {
      stubFor(
        post(urlEqualTo(url))
        .willReturn(
          aResponse()
          .withStatus(status)
        )
      )   
    }
  
    def primePostWithBody(url: String, status: Int, response: String) = {
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
      "return Right(PublishResponse)" in new Setup {
        val serviceName = "Hello-World"

        val url = s"/publish/${serviceName}"
        val expectedPublishResponse = PublishResponse("id", "publishReference", "platformType")
        val responseAsJsonString = Json.toJson(expectedPublishResponse).toString

        primePostWithBody(url, OK, responseAsJsonString)

        val result = await(underTest.publishByServiceName(serviceName))
        result match {
          case Right(response: PublishResponse) => response shouldBe expectedPublishResponse
          case Left(e: Throwable) => 
              println(e.getMessage)
              fail()
        }
      }

      "return Left if there is an error in the backend" in new Setup {
        val serviceName = "Hello-World"

        val url = s"/publish/${serviceName}"

        primePost(url, INTERNAL_SERVER_ERROR)

        val result = await(underTest.publishByServiceName(serviceName))
        result match {
          case Left(e: Upstream5xxResponse) => succeed
          case _ => fail
        }

      }
    }

    "publishAll" should {
      "return Right" in new Setup {

        val url = s"/publish-all"
        val expectedResponse = PublishAllResponse(message = "Publish all called and is working in the background, check application logs for progress")
        val expectedResponseAsString = Json.toJson(expectedResponse).toString

        primePostWithBody(url, OK, expectedResponseAsString)

        val result = await(underTest.publishAll)
        result match {
          case Right(response: PublishAllResponse) => response shouldBe expectedResponse
          case Left(e: Throwable) => 
              println(e.getMessage)
              fail()
        }
      }

      "rreturn Left if there is an error in the backend" in new Setup {

        val url = s"/publish-all"

        primePost(url, INTERNAL_SERVER_ERROR)

        val result = await(underTest.publishAll)
        result match {
          case Left(e: Upstream5xxResponse) => succeed
          case _ => fail
        }
      }
    }
  }
}
