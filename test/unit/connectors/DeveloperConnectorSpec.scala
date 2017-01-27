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

import java.net.URLEncoder

import com.github.tomakehurst.wiremock.client.WireMock._
import config.WSHttp
import connectors.DeveloperConnector
import model.User
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, Matchers}
import play.api.libs.json.Json
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.time.DateTimeUtils


class DeveloperConnectorSpec extends UnitSpec with Matchers with ScalaFutures with WiremockSugar with BeforeAndAfterEach with WithFakeApplication {


  trait Setup {
    implicit val hc = HeaderCarrier()

    val connector = new DeveloperConnector {
      override val developerBaseUrl: String = wireMockUrl
      override val http: HttpPost with HttpGet = WSHttp
    }
  }


  "fetchByEmail" should {
    val developer1Email = "developer1@email.com"
    val developer2Email = "developer2+test@email.com"

    val timeStamp = DateTimeUtils.now

    def encode(str: String) = URLEncoder.encode(str, "UTF-8")

    def aUserResponse(email: String) = User(email, "first", "last", Some(false))

    def verifyUserResponse(userResponse: User,
                           expectedEmail: String, expectedFirstName: String, expectedLastName: String) = {
      userResponse.email shouldBe expectedEmail
      userResponse.firstName shouldBe expectedFirstName
      userResponse.lastName shouldBe expectedLastName
    }


    "fetch developer by email" in new Setup {
      stubFor(get(urlEqualTo(s"/developer?email=${encode(developer1Email)}")).willReturn(
        aResponse().withStatus(200).withBody(
          Json.toJson(aUserResponse(developer1Email)).toString()))
      )
      val result = await(connector.fetchByEmail(developer1Email))
      verifyUserResponse(result,developer1Email,"first","last")
    }

    "fetch developer2 by email" in new Setup {
      stubFor(get(urlEqualTo(s"/developer?email=${encode(developer2Email)}")).willReturn(
        aResponse().withStatus(200).withBody(
          Json.toJson(aUserResponse(developer2Email)).toString()))
      )
      val result = await(connector.fetchByEmail(developer2Email))
      verifyUserResponse(result,developer2Email,"first","last")
    }

    "fetch all developers by emails" in new Setup {
      val encodedEmailsParam = encode(s"$developer1Email,$developer2Email")
      stubFor(get(urlEqualTo(s"/developers?emails=$encodedEmailsParam")).willReturn(
        aResponse().withStatus(200).withBody(
          Json.toJson(Seq(aUserResponse(developer1Email),aUserResponse(developer2Email))).toString()))
      )
      val result = await(connector.fetchByEmails(Seq(developer1Email,developer2Email)))
      verifyUserResponse(result(0),developer1Email,"first","last")
      verifyUserResponse(result(1),developer2Email,"first","last")
    }

    "fetch all developers" in new Setup {
      stubFor(get(urlEqualTo(s"/developers/all")).willReturn(
        aResponse().withStatus(200).withBody(
          Json.toJson(Seq(aUserResponse(developer1Email),aUserResponse(developer2Email))).toString()))
      )
      val result = await(connector.fetchAll())
      verifyUserResponse(result(0),developer1Email,"first","last")
      verifyUserResponse(result(1),developer2Email,"first","last")
    }
  }
}
