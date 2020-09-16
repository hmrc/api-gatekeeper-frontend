/*
 * Copyright 2020 HM Revenue & Customs
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

import java.util.UUID

import akka.actor.ActorSystem
import config.AppConfig
import model.Environment._
import model._
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec
import utils.FutureTimeoutSupportImpl

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApiDefinitionConnectorSpec extends UnitSpec with MockitoSugar with ArgumentMatchersSugar with ScalaFutures with BeforeAndAfterEach {
  private val baseUrl = "https://example.com"
  private val environmentName = "ENVIRONMENT"
  private val bearer = "TestBearerToken"
  private val futureTimeoutSupport = new FutureTimeoutSupportImpl
  private val actorSystemTest = ActorSystem("test-actor-system")
  private val apiKeyTest = UUID.randomUUID().toString


  class Setup(proxyEnabled: Boolean = false) {
    implicit val hc = HeaderCarrier()

    val mockHttpClient = mock[HttpClient]
    val mockProxiedHttpClient = mock[ProxiedHttpClient]
    val mockEnvironment = mock[Environment]
    val mockAppConfig: AppConfig = mock[AppConfig]

    when(mockEnvironment.toString).thenReturn(environmentName)
    when(mockProxiedHttpClient.withHeaders(*, *)).thenReturn(mockProxiedHttpClient)

    val connector = new ApiDefinitionConnector {
      val httpClient = mockHttpClient
      val proxiedHttpClient = mockProxiedHttpClient
      val serviceBaseUrl = baseUrl
      val useProxy = proxyEnabled
      val bearerToken = bearer
      val environment = mockEnvironment
      val appConfig = mockAppConfig
      val actorSystem = actorSystemTest
      val futureTimeout = futureTimeoutSupport
      val apiKey = apiKeyTest
      implicit val ec = global
    }

    val apiVersion1 = ApiVersion.random
  }

  "fetchAll" should {
    val url = s"$baseUrl/api-definition"

    "respond with 200 and convert body" in new Setup {
      val response = Seq(APIDefinition(
        "dummyAPI", "http://localhost/",
        "dummyAPI", "dummy api.", ApiContext("dummy-api"),
        Seq(ApiVersionDefinition(apiVersion1, APIStatus.STABLE, Some(APIAccess(APIAccessType.PUBLIC)))), Some(false), None))

      when(mockHttpClient.GET[Seq[APIDefinition]](eqTo(url))( *, *, *)).thenReturn(Future.successful(response))

      await(connector.fetchPublic()) shouldBe response
    }

    "when retry logic is enabled should retry on failure" in new Setup {

      val response = Seq(APIDefinition(
        "dummyAPI", "http://localhost/",
        "dummyAPI", "dummy api.", ApiContext("dummy-api"),
        Seq(ApiVersionDefinition(apiVersion1, APIStatus.STABLE, Some(APIAccess(APIAccessType.PUBLIC)))), Some(false), None))

      when(mockAppConfig.retryCount).thenReturn(1)
      when(mockHttpClient.GET[Seq[APIDefinition]](eqTo(url))( *, *, *)).thenReturn(
        Future.failed(new BadRequestException("")),
        Future.successful(response)
      )

      await(connector.fetchPublic()) shouldBe response
    }

    "propagate FetchApiDefinitionsFailed exception" in new Setup {
      when(mockHttpClient.GET[Seq[APIDefinition]](eqTo(url))(*, *, *))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[FetchApiDefinitionsFailed](await(connector.fetchPublic()))
    }
  }

  "fetchPrivate" should {
    val url = s"$baseUrl/api-definition?type=private"

    "respond with 200 and convert body" in new Setup {
      val response = Seq(APIDefinition(
        "dummyAPI", "http://localhost/",
        "dummyAPI", "dummy api.", ApiContext("dummy-api"),
        Seq(ApiVersionDefinition(apiVersion1, APIStatus.STABLE, Some(APIAccess(APIAccessType.PRIVATE)))), Some(false), None))

      when(mockHttpClient.GET[Seq[APIDefinition]](eqTo(url))(*, *, *)).thenReturn(Future.successful(response))

      await(connector.fetchPrivate()) shouldBe response
    }

    "when retry logic is enabled should retry on failure" in new Setup {
      val response = Seq(APIDefinition(
        "dummyAPI", "http://localhost/",
        "dummyAPI", "dummy api.", ApiContext("dummy-api"),
        Seq(ApiVersionDefinition(apiVersion1, APIStatus.STABLE, Some(APIAccess(APIAccessType.PRIVATE)))), Some(false), None))

      when(mockAppConfig.retryCount).thenReturn(1)
      when(mockHttpClient.GET[Seq[APIDefinition]](eqTo(url))( *, *, *)).thenReturn(
        Future.failed(new BadRequestException("")),
        Future.successful(response)
      )

      await(connector.fetchPrivate()) shouldBe response
    }

    "propagate FetchApiDefinitionsFailed exception" in new Setup {
      when(mockHttpClient.GET[Seq[APIDefinition]](eqTo(url))(*, *, *))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[FetchApiDefinitionsFailed](await(connector.fetchPrivate()))
    }
  }

  "fetchAPICategories" should {
    val url = s"$baseUrl/api-categories"
    "respond with 200 and convert body" in new Setup {
      val response = List(APICategoryDetails("Business", "Business"), APICategoryDetails("VAT", "Vat"))

      when(mockHttpClient.GET[List[APICategoryDetails]](eqTo(url))(*, *, *)).thenReturn(Future.successful(response))

      await(connector.fetchAPICategories()) shouldBe response
    }

    "when retry logic is enabled should retry on failure" in new Setup {
      val response = List(APICategoryDetails("Business", "Business"), APICategoryDetails("VAT", "Vat"))

      when(mockAppConfig.retryCount).thenReturn(1)
      when(mockHttpClient.GET[List[APICategoryDetails]](eqTo(url))(*, *, *)).thenReturn(Future.failed(new BadRequestException("")),
        Future.successful(response))

      await(connector.fetchAPICategories()) shouldBe response
    }

    "propagate FetchApiCategoriesFailed exception" in new Setup {
      when(mockHttpClient.GET[List[APICategory]](eqTo(url))(*, *, *))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[FetchApiCategoriesFailed](await(connector.fetchAPICategories()))
    }

  }

  "http" when {
    "configured not to use the proxy" should {
      "use the HttpClient" in new Setup(proxyEnabled = false) {
        connector.http shouldBe mockHttpClient
      }
    }

    "configured to use the proxy" should {
      "use the ProxiedHttpClient with the correct authorisation" in new Setup(proxyEnabled = true) {
        connector.http shouldBe mockProxiedHttpClient

        verify(mockProxiedHttpClient).withHeaders(bearer, apiKeyTest)
      }
    }
  }
}
