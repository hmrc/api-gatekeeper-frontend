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

import java.util.UUID

import config.AppConfig
import model.Environment.Environment
import model.{ApiScope, FetchApiDefinitionsFailed}
import org.scalatest.Matchers
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ApiScopeConnectorSpec extends UnitSpec with MockitoSugar with ArgumentMatchersSugar with Matchers {
  private val baseUrl = "https://example.com"
  private val bearer = "TestBearerToken"
  private val apiKeyTest = UUID.randomUUID().toString

  class Setup(proxyEnabled: Boolean = false) {
    implicit val hc = HeaderCarrier()

    val mockHttpClient = mock[HttpClient]
    val mockProxiedHttpClient = mock[ProxiedHttpClient]
    val mockEnvironment = mock[Environment]
    val mockAppConfig: AppConfig = mock[AppConfig]

    when(mockProxiedHttpClient.withHeaders(*, *)).thenReturn(mockProxiedHttpClient)

    val underTest = new ApiScopeConnector() {
      val httpClient = mockHttpClient
      val proxiedHttpClient = mockProxiedHttpClient
      val serviceBaseUrl = baseUrl
      val useProxy = proxyEnabled
      val bearerToken = bearer
      val environment = mockEnvironment
      val appConfig = mockAppConfig
      val apiKey = apiKeyTest
      implicit val ec: ExecutionContext = ExecutionContext.global
    }
  }

  "fetchAll" should {

    val scopes = Seq(ApiScope("aKey", "aName", "aDescription"))

    "fetch a sequence of API scopes" in new Setup {

      when(mockHttpClient.GET[Seq[ApiScope]](eqTo(s"$baseUrl/scope"))(*, *, *)).thenReturn(Future.successful(scopes))

      val result = await(underTest.fetchAll())

      result shouldBe scopes
    }

    "fail to fetch a sequence of API scopes" in new Setup {
      when(mockHttpClient.GET[Seq[ApiScope]](eqTo(s"$baseUrl/scope"))(*, *, *))
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
