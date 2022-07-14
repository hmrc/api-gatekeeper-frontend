/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.modules.sms.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.encryption.PayloadEncryption
import uk.gov.hmrc.gatekeeper.modules.sms.connectors.ThirdPartyDeveloperConnector.SendSmsResponse
import uk.gov.hmrc.gatekeeper.utils.{AsyncHmrcSpec, WireMockSugar, UrlEncoding}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext.Implicits.global

class ThirdPartyDeveloperConnectorSpec
  extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with UrlEncoding {

  trait Setup {
    implicit val hc = HeaderCarrier()

    val httpClient = app.injector.instanceOf[HttpClient]
    val mockAppConfig = mock[AppConfig]
    val mockPayloadEncryption = new PayloadEncryption("gvBoGdgzqG1AarzF1LY0zQ==")
    when(mockAppConfig.developerBaseUrl).thenReturn(wireMockUrl)

    val underTest = new ThirdPartyDeveloperConnector(mockAppConfig, httpClient, mockPayloadEncryption)
    val url = s"/notify/send-sms"
    val phoneNumber = "0123456789"

  }

  "sendSms" should {
    "return SendSmsResponse if the request was successful on the backend" in new Setup {

      val sendSmsResponse = SendSmsResponse("Your SMS was sent successfully")

      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson(sendSmsResponse).toString)
          )
      )

      await(underTest.sendSms(phoneNumber)) shouldBe Right(sendSmsResponse)
    }

    "fail if the request failed on the backend" in new Setup {

      val sendSmsResponse = SendSmsResponse("Missing Username")

      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody(Json.toJson(sendSmsResponse).toString)
          )
      )

      await(underTest.sendSms(phoneNumber)) shouldBe Right(SendSmsResponse(
        s"""POST of '$wireMockUrl$url' returned $INTERNAL_SERVER_ERROR. Response body: '{"message":"${sendSmsResponse.message}"}'"""
      ))
    }

    "fail if the request failed unexpectedly" in new Setup {

      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      val expectedMessage = s"""POST of '$wireMockUrl$url' returned $INTERNAL_SERVER_ERROR. Response body: ''"""
      await(underTest.sendSms(phoneNumber)) match {
        case Left(UpstreamErrorResponse(message, INTERNAL_SERVER_ERROR, _, _)) => message shouldBe expectedMessage
        case _ => fail()
      }
    }
  }

}
