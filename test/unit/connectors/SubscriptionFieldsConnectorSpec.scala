/*
 * Copyright 2018 HM Revenue & Customs
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

import java.net.URLEncoder.encode
import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import config.WSHttp
import connectors.SubscriptionFieldsConnector
import model.ApiSubscriptionFields.{Fields, SubscriptionField, SubscriptionFields, SubscriptionFieldsPutRequest}
import model.{FieldsDeleteFailureResult, FieldsDeleteSuccessResult}
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, JsValidationException, NotFoundException, Upstream5xxResponse}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class SubscriptionFieldsConnectorSpec extends UnitSpec with WiremockSugar with BeforeAndAfterEach with WithFakeApplication {

  implicit val hc = HeaderCarrier()
  val urlPrefix = "/field"
  val clientId: String = UUID.randomUUID().toString
  val apiContext: String = "i-am-a-test"
  val apiVersion: String = "1.0"

  private def urlEncode(str: String, encoding: String = "UTF-8") = encode(str, encoding)

  trait Setup {
    val fieldsId = UUID.randomUUID()
    val underTest = new SubscriptionFieldsConnector {
      val subscriptionFieldsBaseUrl: String = wireMockUrl
      override val http = WSHttp
    }

  }

  "fetchFieldValues" should {
    val getUrl = s"$urlPrefix/application/${urlEncode(clientId)}/context/${urlEncode(apiContext)}/version/${urlEncode(apiVersion)}"

    "return subscription fields for an API" in new Setup {
      val response = SubscriptionFields(clientId, apiContext, apiVersion, fieldsId, Map("field001" -> "field002"))

      stubFor(get(urlPathMatching(getUrl))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withHeader("Content-Type", "application/json")
            .withBody(Json.toJson(response).toString())))

      val result: Option[SubscriptionFields] = await(underTest.fetchFieldValues(clientId, apiContext, apiVersion))

      result shouldBe Some(response)
    }

    "fail when api-subscription-fields returns an internal server error" in new Setup {

      stubFor(get(urlPathMatching(getUrl))
        .willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withHeader("Content-Type", "application/json")))

      intercept[Upstream5xxResponse] {
        await(underTest.fetchFieldValues(clientId, apiContext, apiVersion))
      }
    }

    "return None when api-subscription-fields returns a not found" in new Setup {

      stubFor(get(urlPathMatching(getUrl))
        .willReturn(
          aResponse()
            .withStatus(NOT_FOUND)))

      val result: Option[SubscriptionFields] = await(underTest.fetchFieldValues(clientId, apiContext, apiVersion))
      result shouldBe None
    }

  }

  "fetchFieldDefinitions" should {
    val fields = List(SubscriptionField("field1", "desc1", "hint1", "some type"), SubscriptionField("field2", "desc2", "hint2", "some other type"))
    val invalidResponse = Map("whatever" -> fields)
    val url = s"/definition/context/${urlEncode(apiContext)}/version/${urlEncode(apiVersion)}"
    val validResponse = Map("fieldDefinitions" -> fields)

    "return subscription fields definition for an API" in new Setup {

      stubFor(get(urlPathMatching(url))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withHeader("Content-Type", "application/json")
            .withBody(Json.toJson(validResponse).toString())))

      val result: Seq[SubscriptionField] = await(underTest.fetchFieldDefinitions(apiContext, apiVersion))

      result shouldBe fields
    }

    "fail when api-subscription-fields returns an internal server error" in new Setup {

      stubFor(get(urlPathMatching(url))
        .willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withHeader("Content-Type", "application/json")))

      intercept[Upstream5xxResponse] {
        await(underTest.fetchFieldDefinitions(apiContext, apiVersion))
      }
    }

    "return empty sequence when api-subscription-fields returns a not found" in new Setup {

      stubFor(get(urlPathMatching(url))
        .willReturn(
          aResponse()
            .withStatus(NOT_FOUND)))

      val result: Seq[SubscriptionField] = await(underTest.fetchFieldDefinitions(apiContext, apiVersion))
      result shouldBe Seq.empty[SubscriptionField]
    }

    "fail when api-subscription-fields returns unexpected response" in new Setup {

      stubFor(get(urlPathMatching(url))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withHeader("Content-Type", "application/json")
            .withBody(Json.toJson(invalidResponse).toString())))

      intercept[JsValidationException] {
        await(underTest.fetchFieldDefinitions(apiContext, apiVersion))
      }
    }

  }

  "saveFieldValues" should {

    val fields = Fields("field001" -> "value001", "field002" -> "value002")
    val subFieldsPutRequest = SubscriptionFieldsPutRequest(clientId, apiContext, apiVersion, fields)

    val putUrl = s"$urlPrefix/application/${urlEncode(clientId)}/context/${urlEncode(apiContext)}/version/${urlEncode(apiVersion)}"

    "save the fields" in new Setup {
      stubFor(put(urlPathMatching(putUrl))
        .willReturn(
          aResponse()
            .withStatus(OK)))

      await(underTest.saveFieldValues(clientId, apiContext, apiVersion, fields))

      verify(putRequestedFor(urlPathMatching(putUrl)).withRequestBody(equalToJson(Json.toJson(subFieldsPutRequest).toString())))
    }

    "fail when api-subscription-fields returns an internal server error" in new Setup {

      stubFor(put(urlPathMatching(putUrl))
        .willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withHeader("Content-Type", "application/json")))

      intercept[Upstream5xxResponse] {
        await(underTest.saveFieldValues(clientId, apiContext, apiVersion, fields))
      }
    }

    "fail when api-subscription-fields returns a not found" in new Setup {

      stubFor(put(urlPathMatching(putUrl))
        .willReturn(
          aResponse()
            .withStatus(NOT_FOUND)))

      intercept[NotFoundException] {
        await(underTest.saveFieldValues(clientId, apiContext, apiVersion, fields))
      }
    }
  }

  "deleteFieldValues" should {

    val url = s"$urlPrefix/application/${urlEncode(clientId)}/context/${urlEncode(apiContext)}/version/${urlEncode(apiVersion)}"

    "return successful result after delete call has returned no content" in new Setup {
      stubFor(delete(urlPathMatching(url))
        .willReturn(
          aResponse()
            .withStatus(NO_CONTENT)))

      val result = await(underTest.deleteFieldValues(clientId, apiContext, apiVersion))

      result shouldBe FieldsDeleteSuccessResult
    }

    "return failure result api-subscription-fields returns unexpected status" in new Setup {
      stubFor(delete(urlPathMatching(url))
        .willReturn(
          aResponse()
            .withStatus(ACCEPTED)))

      val result = await(underTest.deleteFieldValues(clientId, apiContext, apiVersion))

      result shouldBe FieldsDeleteFailureResult
    }

    "fail when api-subscription-fields returns an internal server error" in new Setup {

      stubFor(delete(urlPathMatching(url))
        .willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)))

      val result = await(underTest.deleteFieldValues(clientId, apiContext, apiVersion))
      result shouldBe FieldsDeleteFailureResult
    }

    "return successful result when api-subscription-fields returns a not found" in new Setup {

      stubFor(delete(urlPathMatching(url))
        .willReturn(
          aResponse()
            .withStatus(NOT_FOUND)))

      val result = await(underTest.deleteFieldValues(clientId, apiContext, apiVersion))
      result shouldBe FieldsDeleteSuccessResult
    }

  }

}
