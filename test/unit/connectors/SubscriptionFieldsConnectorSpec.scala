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

import java.net.URLEncoder.encode
import java.util.UUID

import connectors._
import model.ApiSubscriptionFields._
import model.Environment._
import model.{FieldsDeleteFailureResult, FieldsDeleteSuccessResult}
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubscriptionFieldsConnectorSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with WithFakeApplication {
  private val baseUrl = "https://example.com"
  private val environmentName = "ENVIRONMENT"
  private val bearer = "TestBearerToken"

  implicit val hc = HeaderCarrier()
  val clientId: String = UUID.randomUUID().toString
  val apiContext: String = "i-am-a-test"
  val apiVersion: String = "1.0"

  private def urlEncode(str: String, encoding: String = "UTF-8") = encode(str, encoding)

  class Setup(proxyEnabled: Boolean = false) {
    val fieldsId = UUID.randomUUID()
    val testApiKey: String = UUID.randomUUID().toString
    val mockHttpClient = mock[HttpClient]
    val mockProxiedHttpClient = mock[ProxiedHttpClient]
    val mockEnvironment = mock[Environment]

    when(mockEnvironment.toString).thenReturn(environmentName)
    when(mockProxiedHttpClient.withHeaders(any(), any())).thenReturn(mockProxiedHttpClient)

    val underTest = new SubscriptionFieldsConnector {
      val httpClient = mockHttpClient
      val proxiedHttpClient = mockProxiedHttpClient
      val serviceBaseUrl = baseUrl
      val useProxy = proxyEnabled
      val bearerToken = bearer
      val environment = mockEnvironment
      val apiKey = testApiKey
    }
  }

  "fetchFieldValues" should {
    val url = s"$baseUrl/field/application/${urlEncode(clientId)}/context/${urlEncode(apiContext)}/version/${urlEncode(apiVersion)}"

    "return subscription fields for an API" in new Setup {
      val response = SubscriptionFields(clientId, apiContext, apiVersion, fieldsId, Map("field001" -> "field002"))

      when(mockHttpClient.GET[SubscriptionFields](meq(url))(any(), any(), any())).thenReturn(Future.successful(response))

      val result: Option[SubscriptionFields] = await(underTest.fetchFieldValues(clientId, apiContext, apiVersion))

      result shouldBe Some(response)
    }

    "fail when api-subscription-fields returns an internal server error" in new Setup {
      when(mockHttpClient.GET[SubscriptionFields](meq(url))(any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(underTest.fetchFieldValues(clientId, apiContext, apiVersion))
      }
    }

    "return None when api-subscription-fields returns a not found" in new Setup {
      when(mockHttpClient.GET[SubscriptionFields](meq(url))(any(), any(), any())).thenReturn(Future.failed(new NotFoundException("")))

      val result: Option[SubscriptionFields] = await(underTest.fetchFieldValues(clientId, apiContext, apiVersion))
      result shouldBe None
    }

  }

  "fetchFieldDefinitions" should {
    val fields = List(SubscriptionField("field1", "desc1", "hint1", "some type"), SubscriptionField("field2", "desc2", "hint2", "some other type"))
    val url = s"$baseUrl/definition/context/${urlEncode(apiContext)}/version/${urlEncode(apiVersion)}"
    val validResponse = Map("fieldDefinitions" -> fields)

    "return subscription fields definition for an API" in new Setup {
      when(mockHttpClient.GET[FieldDefinitionsResponse](meq(url))(any(), any(), any()))
        .thenReturn(Future.successful(FieldDefinitionsResponse(fields)))

      val result: Seq[SubscriptionField] = await(underTest.fetchFieldDefinitions(apiContext, apiVersion))

      result shouldBe fields
    }

    "fail when api-subscription-fields returns an internal server error" in new Setup {
      when(mockHttpClient.GET[FieldDefinitionsResponse](meq(url))(any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(underTest.fetchFieldDefinitions(apiContext, apiVersion))
      }
    }

    "return empty sequence when api-subscription-fields returns a not found" in new Setup {
      when(mockHttpClient.GET[FieldDefinitionsResponse](meq(url))(any(), any(), any()))
        .thenReturn(Future.failed(new NotFoundException("")))

      val result: Seq[SubscriptionField] = await(underTest.fetchFieldDefinitions(apiContext, apiVersion))
      result shouldBe Seq.empty[SubscriptionField]
    }

  }

  "saveFieldValues" should {

    val fieldsToSave = fields("field001" -> "value001", "field002" -> "value002")
    val subFieldsPutRequest = SubscriptionFieldsPutRequest(clientId, apiContext, apiVersion, fieldsToSave)

    val url = s"$baseUrl/field/application/${urlEncode(clientId)}/context/${urlEncode(apiContext)}/version/${urlEncode(apiVersion)}"

    "save the fields" in new Setup {
      when(mockHttpClient.PUT[SubscriptionFieldsPutRequest, HttpResponse](meq(url), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK)))

      await(underTest.saveFieldValues(clientId, apiContext, apiVersion, fieldsToSave))
    }

    "fail when api-subscription-fields returns an internal server error" in new Setup {
      when(mockHttpClient.PUT[SubscriptionFieldsPutRequest, HttpResponse](meq(url), any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(underTest.saveFieldValues(clientId, apiContext, apiVersion, fieldsToSave))
      }
    }

    "fail when api-subscription-fields returns a not found" in new Setup {
      when(mockHttpClient.PUT[SubscriptionFieldsPutRequest, HttpResponse](meq(url), any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(new NotFoundException("")))

      intercept[NotFoundException] {
        await(underTest.saveFieldValues(clientId, apiContext, apiVersion, fieldsToSave))
      }
    }
  }

  "deleteFieldValues" should {

    val url = s"$baseUrl/field/application/${urlEncode(clientId)}/context/${urlEncode(apiContext)}/version/${urlEncode(apiVersion)}"

    "return successful result after delete call has returned no content" in new Setup {
      when(mockHttpClient.DELETE[HttpResponse](meq(url))(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT)))

      val result = await(underTest.deleteFieldValues(clientId, apiContext, apiVersion))

      result shouldBe FieldsDeleteSuccessResult
    }

    "return failure result api-subscription-fields returns unexpected status" in new Setup {
      when(mockHttpClient.DELETE[HttpResponse](meq(url))(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(ACCEPTED)))

      val result = await(underTest.deleteFieldValues(clientId, apiContext, apiVersion))

      result shouldBe FieldsDeleteFailureResult
    }

    "fail when api-subscription-fields returns an internal server error" in new Setup {
      when(mockHttpClient.DELETE[HttpResponse](meq(url))(any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val result = await(underTest.deleteFieldValues(clientId, apiContext, apiVersion))
      result shouldBe FieldsDeleteFailureResult
    }

    "return successful result when api-subscription-fields returns a not found" in new Setup {
      when(mockHttpClient.DELETE[HttpResponse](meq(url))(any(), any(), any()))
        .thenReturn(Future.failed(new NotFoundException("")))

      val result = await(underTest.deleteFieldValues(clientId, apiContext, apiVersion))
      result shouldBe FieldsDeleteSuccessResult
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

        verify(mockProxiedHttpClient).withHeaders(bearer, testApiKey)
      }
    }
  }
}
