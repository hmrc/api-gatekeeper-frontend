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

package uk.gov.hmrc.gatekeeper.connectors

import java.util.UUID

import uk.gov.hmrc.http.Authorization
import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec
import play.api.http.HeaderNames
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Application
import play.api.Configuration

class ProxiedHttpClientSpec extends AsyncHmrcSpec with GuiceOneServerPerSuite {

  val stubConfig = Configuration(
    "metrics.enabled"                       -> true,
    "auditing.enabled"                      -> false,
    "proxy.proxyRequiredForThisEnvironment" -> false
  )

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(stubConfig)
      .build()

  trait Setup {
    val apiKey: String      = UUID.randomUUID().toString
    val bearerToken: String = UUID.randomUUID().toString
    val url                 = "http://example.com"

    val underTest: ProxiedHttpClient = app.injector.instanceOf[ProxiedHttpClient]
  }

  "withHeaders" should {

    "creates a ProxiedHttpClient with passed in bearer and key" in new Setup {

      private val result = underTest.withHeaders(bearerToken, apiKey)

      result.authorization shouldBe Some(Authorization(s"Bearer $bearerToken"))
      result.apiKeyHeader shouldBe Some(apiKey)
    }

    "when apiKey is empty String, apiKey header is None" in new Setup {

      private val result = underTest.withHeaders(bearerToken, "")

      result.apiKeyHeader shouldBe None
    }

    "when apiKey isn't provided, apiKey header is None" in new Setup {

      private val result = underTest.withHeaders(bearerToken)

      result.apiKeyHeader shouldBe None
    }

    "when building a request the api key is added" in new Setup {
      private val proxy = underTest.withHeaders(bearerToken, apiKey)

      val request = proxy.buildRequest("http://localhost:12345", Seq.empty)

      request.headers.get(ProxiedHttpClient.API_KEY_HEADER_NAME) shouldBe Some(Seq(apiKey))
    }

    "when building a request the accept header is added" in new Setup {
      private val proxy = underTest.withHeaders(bearerToken, apiKey)

      val request = proxy.buildRequest("http://localhost:12345", Seq.empty)

      request.headers.get(HeaderNames.ACCEPT) shouldBe Some(Seq(ProxiedHttpClient.ACCEPT_HMRC_JSON_HEADER._2))
    }

    "when building a request the authorisation is added" in new Setup {
      private val proxy = underTest.withHeaders(bearerToken, apiKey)

      val request = proxy.buildRequest("http://localhost:12345", Seq.empty)

      request.headers.get(HeaderNames.AUTHORIZATION) shouldBe Some(Seq(s"Bearer $bearerToken"))
    }

    "when building a request any additionaly headers are  added" in new Setup {
      private val proxy = underTest.withHeaders(bearerToken, apiKey)

      val request = proxy.buildRequest("http://localhost:12345", Seq(HeaderNames.USER_AGENT -> "ThisTest"))

      request.headers.get(HeaderNames.USER_AGENT) shouldBe Some(Seq("ThisTest"))
    }
  }
}
