/*
 * Copyright 2017 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock._
import config.WSHttp
import connectors.AuthConnector
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, Matchers}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}


class AuthConnectorSpec extends UnitSpec with Matchers with ScalaFutures with WiremockSugar with BeforeAndAfterEach with WithFakeApplication {

  trait Setup {
    implicit val hc = HeaderCarrier()

    val connector = new AuthConnector {
      override val http = WSHttp
      override val authUrl: String = s"$wireMockUrl/auth/authenticate/user"
    }
  }

  "authorised" should {

    "return true if only scope is sent and the response is 200" in new Setup {
      stubFor(get(urlEqualTo("/auth/authenticate/user/authorise?scope=api"))
        .willReturn(aResponse().withStatus(200)))

      val result = await(connector.authorized("api", None))
      verify(1, getRequestedFor(urlPathEqualTo("/auth/authenticate/user/authorise"))
        .withQueryParam("scope", equalTo("api")))

      result shouldBe true
    }

    "return true if scope and role are sent and the response is 200" in new Setup {
      stubFor(get(urlEqualTo("/auth/authenticate/user/authorise?scope=api&role=gatekeeper"))
        .willReturn(aResponse().withStatus(200)))

      val result = await(connector.authorized("api", Some("gatekeeper")))
      verify(1, getRequestedFor(urlPathEqualTo("/auth/authenticate/user/authorise"))
        .withQueryParam("scope", equalTo("api"))
        .withQueryParam("role", equalTo("gatekeeper")))

      result shouldBe true
    }

    "return false if scope and role are sent but the response is 401" in new Setup {
      stubFor(get(urlEqualTo("/auth/authenticate/user/authorise?scope=api&role=gatekeeper"))
        .willReturn(aResponse().withStatus(401)))

      val result = await(connector.authorized("api", Some("gatekeeper")))
      verify(1, getRequestedFor(urlPathEqualTo("/auth/authenticate/user/authorise"))
        .withQueryParam("scope", equalTo("api"))
        .withQueryParam("role", equalTo("gatekeeper")))

      result shouldBe false
    }
  }
}
