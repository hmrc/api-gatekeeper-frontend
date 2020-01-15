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

package unit.connectors

import java.util.UUID

import akka.actor.ActorSystem
import config.AppConfig
import connectors.{ApiScopeConnector, ProxiedHttpClient}
import model.{ApiScope, FetchApiDefinitionsFailed}
import model.Environment.Environment
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Mockito.{verify, when}
import org.mockito.Matchers.{any, eq => mEq}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import utils.FutureTimeoutSupportImpl

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ApiScopeConnectorSpec extends UnitSpec with MockitoSugar with Matchers {
  private val baseUrl = "https://example.com"
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

    when(mockProxiedHttpClient.withHeaders(any(), any())).thenReturn(mockProxiedHttpClient)

    val underTest = new ApiScopeConnector() {
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
      implicit val ec: ExecutionContext = ExecutionContext.global
    }
  }

  "fetchAll" should {

    val scopes = Seq(ApiScope("aKey", "aName", "aDescription"))

    "fetch a sequence of API scopes" in new Setup {

      when(mockHttpClient.GET[Seq[ApiScope]](mEq(s"$baseUrl/scope"))(any(), any(), any())).thenReturn(Future.successful(scopes))

      val result = await(underTest.fetchAll())

      result shouldBe scopes
    }

    "when retry logic is enabled should retry on failure" in new Setup {

      when(mockAppConfig.retryCount).thenReturn(1)
      when(mockHttpClient.GET[Seq[ApiScope]](mEq(s"$baseUrl/scope"))(any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException("")),
        Future.successful(scopes))

      await(underTest.fetchAll()) shouldBe scopes
    }

    "fail to fetch a sequence of API scopes" in new Setup {

      when(mockHttpClient.GET[Seq[ApiScope]](mEq(s"$baseUrl/scope"))(any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[FetchApiDefinitionsFailed](await(underTest.fetchAll()))
    }
  }

  "http" when {
    "configured not to use the proxy" should {
      "use the HttpClient" in new Setup(proxyEnabled = false) {
        underTest.http shouldBe mockHttpClient
      }
    }

    "configured to use the proxy" should {
      "use the ProxiedHttpClient with the correct authorisation" in new Setup(proxyEnabled = true) {
        underTest.http shouldBe mockProxiedHttpClient

        verify(mockProxiedHttpClient).withHeaders(bearer, apiKeyTest)
      }
    }
  }
}
