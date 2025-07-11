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
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields._

class ApmConnectorSubscriptionFieldsModuleSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with ClientIdFixtures
    with ApiIdentifierFixtures {

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val httpClient                 = app.injector.instanceOf[HttpClientV2]

    val mockApmConnectorConfig: ApmConnector.Config = mock[ApmConnector.Config]
    when(mockApmConnectorConfig.serviceBaseUrl).thenReturn(wireMockUrl)

    implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

    val underTest: ApmConnectorSubscriptionFieldsModule = new ApmConnector(httpClient, mockApmConnectorConfig)
  }

  "urlSubscriptionFieldValues" should {
    "return simple url" in {
      val url = ApmConnectorSubscriptionFieldsModule.urlSubscriptionFieldValues("http://example.com/subscription-fields")(
        Environment.PRODUCTION,
        ClientId("1"),
        ApiContext("path"),
        ApiVersionNbr("1")
      )
      url.toString shouldBe "http://example.com/subscription-fields/field/application/1/context/path/version/1?environment=PRODUCTION"
    }
    "return complex encoded url" in {
      val url = ApmConnectorSubscriptionFieldsModule.urlSubscriptionFieldValues("http://example.com/subscription-fields")(
        Environment.PRODUCTION,
        ClientId("1 2"),
        ApiContext("path1/path2"),
        ApiVersionNbr("1.0 demo")
      )
      url.toString shouldBe "http://example.com/subscription-fields/field/application/1%202/context/path1%2Fpath2/version/1.0%20demo?environment=PRODUCTION"
    }
  }

  "saveFieldValues" should {
    val valuePath = ApmConnectorSubscriptionFieldsModule.urlSubscriptionFieldValues(wireMockUrl + "/subscription-fields")(
      Environment.PRODUCTION,
      clientIdOne,
      apiContextOne,
      apiVersionNbrOne
    ).getPath()

    val fieldsValues        = fields(FieldName.random -> FieldValue.random, FieldName.random -> FieldValue.random)
    val subFieldsPutRequest = Json.toJson(SubscriptionFieldsPutRequest(clientIdOne, apiContextOne, apiVersionNbrOne, fieldsValues)).toString

    "save the fields" in new Setup {
      stubFor(
        put(urlPathEqualTo(valuePath))
          .withQueryParam("environment", equalTo("PRODUCTION"))
          .withRequestBody(equalTo(subFieldsPutRequest))
          .willReturn(
            aResponse()
              .withStatus(OK)
          )
      )
      val result = await(underTest.saveFieldValues(Environment.PRODUCTION, clientIdOne, apiContextOne, apiVersionNbrOne, fieldsValues))

      result shouldBe SaveSubscriptionFieldsSuccessResponse
    }

    "fail when api-subscription-fields returns a 500" in new Setup {
      stubFor(
        put(urlPathEqualTo(valuePath))
          .withQueryParam("environment", equalTo("PRODUCTION"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )
      intercept[UpstreamErrorResponse] {
        await(underTest.saveFieldValues(Environment.PRODUCTION, clientIdOne, apiContextOne, apiVersionNbrOne, fieldsValues))
      }
    }

    "fail when api-subscription-fields returns a 404" in new Setup {
      stubFor(
        put(urlPathEqualTo(valuePath))
          .withQueryParam("environment", equalTo("PRODUCTION"))
          .withRequestBody(equalTo(subFieldsPutRequest))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )
      intercept[UpstreamErrorResponse] {
        await(underTest.saveFieldValues(Environment.PRODUCTION, clientIdOne, apiContextOne, apiVersionNbrOne, fieldsValues))
      }.statusCode shouldBe NOT_FOUND
    }
  }
}
