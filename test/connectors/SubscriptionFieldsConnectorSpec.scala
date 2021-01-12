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

package connectors

import java.util.UUID

import config.AppConfig
import connectors.SubscriptionFieldsConnector._
import model.Environment.Environment
import model.SubscriptionFields._
import model.{ApiIdentifier, ApiContext, ApiVersion, ClientId, Environment, FieldsDeleteFailureResult, FieldsDeleteSuccessResult}
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import uk.gov.hmrc.http.{HttpResponse, _}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}
import model.FieldName
import model.FieldValue
import uk.gov.hmrc.play.test.WithFakeApplication

class SubscriptionFieldsConnectorSpec 
    extends UnitSpec 
    with ScalaFutures 
    with MockitoSugar 
    with WiremockSugar
    with WithFakeApplication {

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val clientId = ClientId.random
  private val apiContext = ApiContext.random
  private val apiVersion = ApiVersion.random
  private val fieldName = FieldName.random
  private val subscriptionDefinition = SubscriptionFieldDefinition(fieldName, "my-description", "my-hint", "my-type", "my-shortDescription")
  private val expectedSubscriptionDefinition = SubscriptionFieldDefinition(fieldName, "desc1", "hint1", "some type", "shortDescription")
  private val subscriptionFieldValue = SubscriptionFieldValue(subscriptionDefinition, FieldValue.random)
  private val fieldDefinition1 = FieldDefinition(fieldName, "desc1", "hint1", "some type", "shortDescription")
  private val fieldDefinition2 = fieldDefinition1.copy(name = FieldName.random)
  private val definitions = List(fieldDefinition1, fieldDefinition2)
  private val definitionsFromRestService = List(fieldDefinition1)

  private val apiIdentifier = ApiIdentifier(apiContext, apiVersion)
  private val fieldsId = UUID.randomUUID()
  private val urlPrefix = "/field"
  private val upstream500Response = Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)

  def subscriptionFieldsBaseUrl(clientId: ClientId) = s"$urlPrefix/application/${clientId.value}"

  trait Setup {
    implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

    val apiKey: String = UUID.randomUUID().toString
    val bearerToken: String = UUID.randomUUID().toString
    val http = fakeApplication.injector.instanceOf[HttpClient]
    val mockHttpClient: HttpClient = mock[HttpClient]
    val mockProxiedHttpClient: ProxiedHttpClient = mock[ProxiedHttpClient]
    val mockAppConfig: AppConfig = mock[AppConfig]

    when(mockAppConfig.subscriptionFieldsSandboxApiKey).thenReturn(apiKey)

    val subscriptionFieldsConnector = new ProductionSubscriptionFieldsConnector(mockAppConfig, mockHttpClient)

  }

  "fetchFieldsValuesWithPrefetchedDefinitions" should {
    val subscriptionFields =
      ApplicationApiFieldValues(clientId, apiContext, apiVersion, fieldsId, fields(subscriptionFieldValue.definition.name -> subscriptionFieldValue.value))

    val expectedResults = Seq(subscriptionFieldValue)

    val prefetchedDefinitions = Map(apiIdentifier -> Seq(subscriptionDefinition))

    val getUrl = s"${subscriptionFieldsBaseUrl(clientId)}/context/${apiContext.urlEncode()}/version/${apiVersion.urlEncode()}"

    "return subscription fields for an API" in new Setup {
      when(mockHttpClient
        .GET[ApplicationApiFieldValues](eqTo(getUrl))(*, *, *))
        .thenReturn(Future.successful(subscriptionFields))

      private val result = await(subscriptionFieldsConnector.fetchFieldsValuesWithPrefetchedDefinitions(clientId, apiIdentifier, prefetchedDefinitions))

      result shouldBe expectedResults
    }

    "fail when api-subscription-fields returns a 500" in new Setup {

      when(mockHttpClient.GET[ApplicationApiFieldValues](eqTo(getUrl))(*, *, *))
        .thenReturn(Future.failed(upstream500Response))

      intercept[UpstreamErrorResponse] {
        await(subscriptionFieldsConnector.fetchFieldsValuesWithPrefetchedDefinitions(clientId, apiIdentifier, prefetchedDefinitions))
      }
    }

    "return empty when api-subscription-fields returns a 404" in new Setup {

      when(mockHttpClient.GET[ApplicationApiFieldValues](eqTo(getUrl))(*, *, *))
        .thenReturn(Future.failed(new NotFoundException("")))

      private val result = await(subscriptionFieldsConnector.fetchFieldsValuesWithPrefetchedDefinitions(clientId, apiIdentifier, prefetchedDefinitions))
      result shouldBe Seq(subscriptionFieldValue.copy(value = FieldValue.empty))
    }
  }

  "fetchAllFieldDefinitions" should {

    val url = "/definition"

    "return all field definitions" in new Setup {

      private val validResponse = AllApiFieldDefinitions(apis = Seq(ApiFieldDefinitions(apiContext, apiVersion, definitions)))

      when(mockHttpClient.GET[AllApiFieldDefinitions](eqTo(url))(*, *, *))
        .thenReturn(Future.successful(validResponse))

      private val result = await (subscriptionFieldsConnector.fetchAllFieldDefinitions())

      val expectedResult = Map(apiIdentifier -> definitions.map(toDomain))

      result shouldBe expectedResult
    }

    "fail when api-subscription-fields returns a 500" in new Setup {

      when(mockHttpClient.GET[AllApiFieldDefinitions](eqTo(url))(*, *, *))
        .thenReturn(Future.failed(upstream500Response))

      intercept[UpstreamErrorResponse] {
        await(subscriptionFieldsConnector.fetchAllFieldDefinitions())
      }
    }

    "fail when api-subscription-fields returns unexpected response" in new Setup {

      when(mockHttpClient.GET[AllApiFieldDefinitions](eqTo(url))(*, *, *))
        .thenReturn(Future.failed(new NotFoundException("")))

      private val result = await (subscriptionFieldsConnector.fetchAllFieldDefinitions())

      result shouldBe Map.empty[String,String]
    }
  }

  "fetchFieldDefinitions" should {
    val url = s"/definition/context/${apiContext.urlEncode()}/version/${apiVersion.urlEncode()}"

    val expectedDefinitions = List(expectedSubscriptionDefinition)

    val validResponse = ApiFieldDefinitions(apiContext, apiVersion, definitionsFromRestService)

    "return definitions" in new Setup {
      when(mockHttpClient.GET[ApiFieldDefinitions](eqTo(url))(*, *, *))
        .thenReturn(Future.successful(validResponse))

      private val result = await(subscriptionFieldsConnector.fetchFieldDefinitions(apiContext, apiVersion))

      result shouldBe expectedDefinitions
    }

    "fail when api-subscription-fields returns a 500" in new Setup {

      when(mockHttpClient.GET[ApiFieldDefinitions](eqTo(url))(*, *, *))
        .thenReturn(Future.failed(upstream500Response))

      intercept[UpstreamErrorResponse] {
        await(subscriptionFieldsConnector.fetchFieldDefinitions(apiContext, apiVersion))
      }
    }
  }

  "fetchFieldValues" should {
    val definitionsUrl = s"/definition/context/${apiContext.urlEncode()}/version/${apiVersion.urlEncode()}"
    val valuesUrl = s"/field/application/${clientId.value}/context/${apiContext.urlEncode()}/version/${apiVersion.urlEncode()}"

    

    val validDefinitionsResponse: ApiFieldDefinitions = ApiFieldDefinitions(apiContext, apiVersion, definitionsFromRestService)

    "return field values" in new Setup {
      val expectedDefinitions = definitionsFromRestService.map(d => SubscriptionFieldDefinition(d.name, d.description, d.hint, d.`type`, d.shortDescription))
      val expectedFieldValues = expectedDefinitions.map(definition => SubscriptionFieldValue(definition, FieldValue.random))

      val fieldsValues: Map[FieldName, FieldValue] = fields(expectedFieldValues.map(v => v.definition.name -> v.value): _*)

      val validValuesResponse: ApplicationApiFieldValues = ApplicationApiFieldValues(clientId, apiContext, apiVersion, fieldsId, fieldsValues)

      when(mockHttpClient.GET[ApiFieldDefinitions](eqTo(definitionsUrl))(*, *, *))
        .thenReturn(Future.successful(validDefinitionsResponse))

      when(mockHttpClient.GET[ApplicationApiFieldValues](eqTo(valuesUrl))(*, *, *))
        .thenReturn(Future.successful(validValuesResponse))

      private val result = await(subscriptionFieldsConnector.fetchFieldValues(clientId, apiContext, apiVersion))

      result shouldBe expectedFieldValues
    }

    "fail when fetching field definitions returns a 500" in new Setup {
      when(mockHttpClient.GET[ApiFieldDefinitions](eqTo(definitionsUrl))(*, *, *))
        .thenReturn(Future.failed(upstream500Response))

      intercept[UpstreamErrorResponse] {
        await(subscriptionFieldsConnector.fetchFieldValues(clientId, apiContext, apiVersion))
      }
    }

    "fail when fetching field definition values returns a 500" in new Setup {
      when(mockHttpClient.GET[ApiFieldDefinitions](eqTo(definitionsUrl))(*, *, *))
        .thenReturn(Future.successful(validDefinitionsResponse))

      when(mockHttpClient.GET[ApiFieldDefinitions](eqTo(valuesUrl))(*, *, *))
        .thenReturn(Future.failed(upstream500Response))

      intercept[UpstreamErrorResponse] {
        await(subscriptionFieldsConnector.fetchFieldValues(clientId, apiContext, apiVersion))
      }
    }
  }

  "saveFieldValues" should {

    val fieldsValues = fields(FieldName.random -> FieldValue.random, FieldName.random -> FieldValue.random)
    val subFieldsPutRequest = SubscriptionFieldsPutRequest(clientId, apiContext, apiVersion, fieldsValues)

    val putUrl = s"${subscriptionFieldsBaseUrl(clientId)}/context/${apiContext.urlEncode()}/version/${apiVersion.urlEncode()}"

    "save the fields" in new Setup {
      when(mockHttpClient.PUT[SubscriptionFieldsPutRequest, HttpResponse](eqTo(putUrl), eqTo(subFieldsPutRequest), *)(*, *, *, *))
        .thenReturn(Future.successful(HttpResponse(OK)))

      await(subscriptionFieldsConnector.saveFieldValues(clientId, apiContext, apiVersion, fieldsValues))

      verify(mockHttpClient).PUT[SubscriptionFieldsPutRequest, HttpResponse](eqTo(putUrl), eqTo(subFieldsPutRequest), *)(*, *, *, *)
    }

    "fail when api-subscription-fields returns a 500" in new Setup {

      when(mockHttpClient.PUT[SubscriptionFieldsPutRequest, HttpResponse](*, *, *)(*, *, *, *))
        .thenReturn(Future.failed(upstream500Response))

      intercept[UpstreamErrorResponse] {
        await(subscriptionFieldsConnector.saveFieldValues(clientId, apiContext, apiVersion, fieldsValues))
      }
    }

    "fail when api-subscription-fields returns a 404" in new Setup {

      when(mockHttpClient.PUT[SubscriptionFieldsPutRequest, HttpResponse](*, *, *)(*,*,*,*))
        .thenReturn(Future.failed(new NotFoundException("")))

      intercept[NotFoundException] {
        await(subscriptionFieldsConnector.saveFieldValues(clientId, apiContext, apiVersion, fieldsValues))
      }
    }
  }

  "deleteFieldValues" should {

    val url = s"${subscriptionFieldsBaseUrl(clientId)}/context/${apiContext.urlEncode()}/version/${apiVersion.urlEncode()}"

    "return success after delete call has returned 204 NO CONTENT" in new Setup {

      when(mockHttpClient.DELETE(url))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT)))

      private val result = await(subscriptionFieldsConnector.deleteFieldValues(clientId, apiContext, apiVersion))

      result shouldBe FieldsDeleteSuccessResult
    }

    "return failure if api-subscription-fields returns unexpected status" in new Setup {

      when(mockHttpClient.DELETE(url))
        .thenReturn(Future.successful(HttpResponse(ACCEPTED)))

      private val result = await(subscriptionFieldsConnector.deleteFieldValues(clientId, apiContext, apiVersion))

      result shouldBe FieldsDeleteFailureResult
    }

    "return failure when api-subscription-fields returns a 500" in new Setup {

      when(mockHttpClient.DELETE(url))
        .thenReturn(Future.failed(upstream500Response))

      private val result = await(subscriptionFieldsConnector.deleteFieldValues(clientId, apiContext, apiVersion))

      result shouldBe FieldsDeleteFailureResult
    }

    "return success when api-subscription-fields returns a 404" in new Setup {

      when(mockHttpClient.DELETE(url))
        .thenReturn(Future.failed(new NotFoundException("")))

      private val result = await(subscriptionFieldsConnector.deleteFieldValues(clientId, apiContext, apiVersion))
      result shouldBe FieldsDeleteSuccessResult
    }
  }
}
