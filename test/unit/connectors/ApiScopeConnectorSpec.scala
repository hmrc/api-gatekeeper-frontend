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

import connectors.{ApiScopeConnector, ProxiedHttpClient}
import model.{ApiScope, FetchApiDefinitionsFailed}
import model.Environment.Environment
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Mockito.when
import org.mockito.Matchers.{any, eq => mEq}
import play.api.http.Status.INTERNAL_SERVER_ERROR

import scala.concurrent.Future

class ApiScopeConnectorSpec extends UnitSpec with MockitoSugar with Matchers {
  private val baseUrl = "https://example.com"

  trait Setup {
    implicit val hc = HeaderCarrier()

    val mockHttpClient = mock[HttpClient]

    val underTest = new ApiScopeConnector{
      val httpClient = mockHttpClient
      val proxiedHttpClient = mock[ProxiedHttpClient]
      val serviceBaseUrl = baseUrl
      val useProxy = false
      val bearerToken = "TestBearerToken"
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
}
