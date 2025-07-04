/*
 * Copyright 2023 HM Revenue & Customs
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

import scala.concurrent.ExecutionContext

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils._
import uk.gov.hmrc.apiplatform.modules.subscriptionfields.domain.models.{FieldName, FieldValue}
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields._

class SubscriptionFieldsConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite {

  implicit private val hc: HeaderCarrier = HeaderCarrier()

  private val clientId   = ClientId.random
  private val apiContext = ApiContext.random
  private val apiVersion = ApiVersionNbr.random

  trait Setup {
    implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
    val httpClient                    = app.injector.instanceOf[HttpClientV2]
    val mockAppConfig: AppConfig      = mock[AppConfig]
    when(mockAppConfig.subscriptionFieldsProductionBaseUrl).thenReturn(wireMockUrl)

    val subscriptionFieldsConnector = new ProductionSubscriptionFieldsConnector(mockAppConfig, httpClient)
  }

  "urlSubscriptionFieldValues" should {
    "return simple url" in {
      val url = SubscriptionFieldsConnector.urlSubscriptionFieldValues("http://example.com")(
        ClientId("1"),
        ApiContext("path"),
        ApiVersionNbr("1")
      )
      url.toString shouldBe "http://example.com/field/application/1/context/path/version/1"
    }
    "return complex encoded url" in {
      val url = SubscriptionFieldsConnector.urlSubscriptionFieldValues("http://example.com")(
        ClientId("1 2"),
        ApiContext("path1/path2"),
        ApiVersionNbr("1.0 demo")
      )
      url.toString shouldBe "http://example.com/field/application/1%202/context/path1%2Fpath2/version/1.0%20demo"
    }
  }

  "saveFieldValues" should {
    val valuePath = SubscriptionFieldsConnector.urlSubscriptionFieldValues(wireMockUrl)(clientId, apiContext, apiVersion).getPath()

    val fieldsValues        = fields(FieldName.random -> FieldValue.random, FieldName.random -> FieldValue.random)
    val subFieldsPutRequest = Json.toJson(SubscriptionFieldsPutRequest(clientId, apiContext, apiVersion, fieldsValues)).toString

    "save the fields" in new Setup {
      stubFor(
        put(urlEqualTo(valuePath))
          .withRequestBody(equalTo(subFieldsPutRequest))
          .willReturn(
            aResponse()
              .withStatus(OK)
          )
      )
      private val result = await(subscriptionFieldsConnector.saveFieldValues(clientId, apiContext, apiVersion, fieldsValues))

      result shouldBe SaveSubscriptionFieldsSuccessResponse
    }

    "fail when api-subscription-fields returns a 500" in new Setup {
      stubFor(
        put(urlEqualTo(valuePath))
          .withRequestBody(equalTo(subFieldsPutRequest))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )
      intercept[UpstreamErrorResponse] {
        await(subscriptionFieldsConnector.saveFieldValues(clientId, apiContext, apiVersion, fieldsValues))
      }
    }

    "fail when api-subscription-fields returns a 404" in new Setup {
      stubFor(
        put(urlEqualTo(valuePath))
          .withRequestBody(equalTo(subFieldsPutRequest))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )
      intercept[UpstreamErrorResponse] {
        await(subscriptionFieldsConnector.saveFieldValues(clientId, apiContext, apiVersion, fieldsValues))
      }.statusCode shouldBe NOT_FOUND
    }
  }
}
