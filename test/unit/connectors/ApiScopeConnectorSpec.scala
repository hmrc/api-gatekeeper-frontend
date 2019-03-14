/*
 * Copyright 2019 HM Revenue & Customs
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

import connectors.{ApiScopeConnector, ProxiedHttpClient}
import model.{ApiScope, FetchApiDefinitionsFailed}
import model.Environment.Environment
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Mockito.{verify, when}
import org.mockito.Matchers.{any, eq => mEq}
import play.api.http.Status.INTERNAL_SERVER_ERROR

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApiScopeConnectorSpec extends UnitSpec with MockitoSugar with Matchers {
  private val baseUrl = "https://example.com"
  private val bearer = "TestBearerToken"

  class Setup(proxyEnabled: Boolean = false) {
    implicit val hc = HeaderCarrier()

    val mockHttpClient = mock[HttpClient]
    val mockProxiedHttpClient = mock[ProxiedHttpClient]

    when(mockProxiedHttpClient.withAuthorization(any())).thenReturn(mockProxiedHttpClient)

    val underTest = new ApiScopeConnector() {
      val httpClient = mockHttpClient
      val proxiedHttpClient = mockProxiedHttpClient
      val serviceBaseUrl = baseUrl
      val useProxy = proxyEnabled
      val bearerToken = bearer
      val environment = mock[Environment]
    }
  }

  "fetchAll" should {
    "fetch a sequence of API scopes" in new Setup {

      val scopes = Seq(ApiScope("aKey", "aName", "aDescription"))

      when(mockHttpClient.GET[Seq[ApiScope]](mEq(s"$baseUrl/scope"))(any(), any(), any())).thenReturn(Future.successful(scopes))

      val result = await(underTest.fetchAll())

      result shouldBe scopes
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

        verify(mockProxiedHttpClient).withAuthorization(bearer)
      }
    }
  }
}
