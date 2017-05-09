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

package unit.config

import config.{SessionTimeoutFilterWithWhitelist, WhitelistedCall}
import org.joda.time.Duration
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.mvc._
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class SessionTimeoutFilterWithWhitelistSpec extends UnitSpec with MockitoSugar with ScalaFutures with WithFakeApplication {

  trait Setup {
    val filter = new SessionTimeoutFilterWithWhitelist(
      timeoutDuration = Duration.standardSeconds(1),
      whitelistedCalls = Set(WhitelistedCall("/login", "GET")),
      onlyWipeAuthToken = false
    )

    val nextOperationFunction = mock[RequestHeader => Future[Result]]
    when(nextOperationFunction.apply(any())).thenReturn(
      Future.successful(Results.Ok.withSession(("authToken", "Bearer Token"))))
  }

  "apply" should {

    "leave the session keys intact when path in whitelist" in new Setup {
      val request = FakeRequest(method = "GET", path = "/login")

      whenReady(filter.apply(nextOperationFunction)(request.withSession("key" -> "value"))) { result =>
        result.session(request).data("authToken") shouldBe "Bearer Token"
      }

      verify(nextOperationFunction).apply(any())
    }

    "remove the session keys when path not in whitelist" in new Setup {
      val request = FakeRequest(method = "GET", path = "/dashboard")

      whenReady(filter.apply(nextOperationFunction)(request.withSession("key" -> "value"))) { result =>
        result.session(request).data shouldBe empty
      }

      verify(nextOperationFunction).apply(any())
    }

    "remove the session keys when path in whitelist with different method" in new Setup {
      val request = FakeRequest(method = "POST", path = "/login")

      whenReady(filter.apply(nextOperationFunction)(request.withSession("key" -> "value"))) { result =>
        result.session(request).data shouldBe empty
      }

      verify(nextOperationFunction).apply(any())
    }

  }

}
