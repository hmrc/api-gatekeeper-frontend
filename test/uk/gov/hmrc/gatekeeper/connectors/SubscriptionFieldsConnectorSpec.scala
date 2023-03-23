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

import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HttpClient, _}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils._
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.connectors.SubscriptionFieldsConnector.JsonFormatters._
import uk.gov.hmrc.gatekeeper.connectors.SubscriptionFieldsConnector._
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields._
import uk.gov.hmrc.gatekeeper.models._

class SubscriptionFieldsConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite {

  implicit private val hc: HeaderCarrier = HeaderCarrier()

  private val clientId                       = ClientId.random
  private val apiContext                     = ApiContext.random
  private val apiVersion                     = ApiVersion.random
  private val fieldName                      = FieldName.random
  private val subscriptionDefinition         = SubscriptionFieldDefinition(fieldName, "my-description", "my-hint", "my-type", "my-shortDescription")
  private val expectedSubscriptionDefinition = SubscriptionFieldDefinition(fieldName, "desc1", "hint1", "some type", "shortDescription")
  private val subscriptionFieldValue         = SubscriptionFieldValue(subscriptionDefinition, FieldValue.random)
  private val fieldDefinition1               = FieldDefinition(fieldName, "desc1", "hint1", "some type", "shortDescription")
  private val fieldDefinition2               = fieldDefinition1.copy(name = FieldName.random)
  private val definitions                    = List(fieldDefinition1, fieldDefinition2)
  private val definitionsFromRestService     = List(fieldDefinition1)

  private val apiIdentifier = ApiIdentifier(apiContext, apiVersion)
  private val fieldsId      = UUID.randomUUID()

  val valueUrl      = SubscriptionFieldsConnector.urlSubscriptionFieldValues("")(clientId, apiContext, apiVersion)
  val definitionUrl = SubscriptionFieldsConnector.urlSubscriptionFieldDefinition("")(apiContext, apiVersion)

  trait Setup {
    implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

    val httpClient               = app.injector.instanceOf[HttpClient]
    val mockAppConfig: AppConfig = mock[AppConfig]
    when(mockAppConfig.subscriptionFieldsProductionBaseUrl).thenReturn(wireMockUrl)

    val subscriptionFieldsConnector = new ProductionSubscriptionFieldsConnector(mockAppConfig, httpClient)
  }

  "urlSubscriptionFieldValues" should {
    "return simple url" in {
      SubscriptionFieldsConnector.urlSubscriptionFieldValues("base")(ClientId("1"), ApiContext("path"), ApiVersion("1")) shouldBe "base/field/application/1/context/path/version/1"
    }
    "return complex encoded url" in {
      SubscriptionFieldsConnector.urlSubscriptionFieldValues("base")(
        ClientId("1 2"),
        ApiContext("path1/path2"),
        ApiVersion("1.0 demo")
      ) shouldBe "base/field/application/1+2/context/path1%2Fpath2/version/1.0+demo"
    }
  }

  "urlSubscriptionFieldDefinition" should {
    "return simple url" in {
      SubscriptionFieldsConnector.urlSubscriptionFieldDefinition("base")(ApiContext("path"), ApiVersion("1")) shouldBe "base/definition/context/path/version/1"
    }

    "return complex encoded url" in {
      SubscriptionFieldsConnector.urlSubscriptionFieldDefinition("base")(
        ApiContext("path1/path2"),
        ApiVersion("1.0 demo")
      ) shouldBe "base/definition/context/path1%2Fpath2/version/1.0+demo"
    }
  }

  "fetchFieldsValuesWithPrefetchedDefinitions" should {
    val subscriptionFields =
      ApplicationApiFieldValues(clientId, apiContext, apiVersion, fieldsId, fields(subscriptionFieldValue.definition.name -> subscriptionFieldValue.value))

    val expectedResults = List(subscriptionFieldValue)

    val prefetchedDefinitions = Map(apiIdentifier -> List(subscriptionDefinition))

    "return subscription fields for an API" in new Setup {
      val payload = Json.toJson(subscriptionFields).toString

      stubFor(
        get(urlEqualTo(valueUrl))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(payload)
          )
      )

      private val result = await(subscriptionFieldsConnector.fetchFieldsValuesWithPrefetchedDefinitions(clientId, apiIdentifier, prefetchedDefinitions))

      result shouldBe expectedResults
    }

    "fail when api-subscription-fields returns a 500" in new Setup {

      stubFor(
        get(urlEqualTo(valueUrl))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      intercept[UpstreamErrorResponse] {
        await(subscriptionFieldsConnector.fetchFieldsValuesWithPrefetchedDefinitions(clientId, apiIdentifier, prefetchedDefinitions))
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
    }

    "return empty when api-subscription-fields returns a 404" in new Setup {
      stubFor(
        get(urlEqualTo(valueUrl))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      private val result = await(subscriptionFieldsConnector.fetchFieldsValuesWithPrefetchedDefinitions(clientId, apiIdentifier, prefetchedDefinitions))
      result shouldBe Seq(subscriptionFieldValue.copy(value = FieldValue.empty))
    }
  }

  "fetchAllFieldDefinitions" should {

    val url = "/definition"

    "return all field definitions" in new Setup {

      private val validResponse = Json.toJson(AllApiFieldDefinitions(apis = List(ApiFieldDefinitions(apiContext, apiVersion, definitions)))).toString

      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(validResponse)
          )
      )
      private val result = await(subscriptionFieldsConnector.fetchAllFieldDefinitions())

      val expectedResult = Map(apiIdentifier -> definitions.map(toDomain))

      result shouldBe expectedResult
    }

    "fail when api-subscription-fields returns a 500" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      intercept[UpstreamErrorResponse] {
        await(subscriptionFieldsConnector.fetchAllFieldDefinitions())
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
    }

    "fail when api-subscription-fields returns unexpected response" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      private val result = await(subscriptionFieldsConnector.fetchAllFieldDefinitions())
      result shouldBe Map.empty[String, String]
    }
  }

  "fetchAllFieldValues" should {
    val url = "/field"

    "return all fields values" in new Setup {
      val fieldValues = Map.empty[FieldName, FieldValue]

      val expectedResult = List(ApplicationApiFieldValues(
        ClientId("clientId-1"),
        ApiContext("apiContext"),
        ApiVersion("1.0"),
        UUID.randomUUID(),
        fieldValues
      ))

      val data: AllApiFieldValues = AllApiFieldValues(expectedResult)

      private val validResponse = Json.toJson(data).toString()

      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(validResponse)
          )
      )
      private val result = await(subscriptionFieldsConnector.fetchAllFieldValues())

      result shouldBe data.subscriptions
    }
  }

  "fetchFieldDefinitions" should {
    val expectedDefinitions = List(expectedSubscriptionDefinition)

    val validResponse = Json.toJson(ApiFieldDefinitions(apiContext, apiVersion, definitionsFromRestService)).toString

    "return definitions" in new Setup {
      stubFor(
        get(urlEqualTo(definitionUrl))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(validResponse)
          )
      )

      private val result = await(subscriptionFieldsConnector.fetchFieldDefinitions(apiContext, apiVersion))

      result shouldBe expectedDefinitions
    }

    "fail when api-subscription-fields returns a 500" in new Setup {
      stubFor(
        get(urlEqualTo(definitionUrl))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      intercept[UpstreamErrorResponse] {
        await(subscriptionFieldsConnector.fetchFieldDefinitions(apiContext, apiVersion))
      }
    }
  }

  "fetchFieldValues" should {
    val validDefinitionsResponse = Json.toJson(ApiFieldDefinitions(apiContext, apiVersion, definitionsFromRestService)).toString

    "return field values" in new Setup {
      val expectedDefinitions = definitionsFromRestService.map(d => SubscriptionFieldDefinition(d.name, d.description, d.hint, d.`type`, d.shortDescription))
      val expectedFieldValues = expectedDefinitions.map(definition => SubscriptionFieldValue(definition, FieldValue.random))

      val fieldsValues: Map[FieldName, FieldValue] = fields(expectedFieldValues.map(v => v.definition.name -> v.value): _*)

      val validValuesResponse = Json.toJson(ApplicationApiFieldValues(clientId, apiContext, apiVersion, fieldsId, fieldsValues)).toString

      stubFor(
        get(urlEqualTo(definitionUrl))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(validDefinitionsResponse)
          )
      )

      stubFor(
        get(urlEqualTo(valueUrl))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(validValuesResponse)
          )
      )

      private val result = await(subscriptionFieldsConnector.fetchFieldValues(clientId, apiContext, apiVersion))

      result shouldBe expectedFieldValues
    }

    "fail when fetching field definitions returns a 500" in new Setup {
      stubFor(
        get(urlEqualTo(definitionUrl))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      intercept[UpstreamErrorResponse] {
        await(subscriptionFieldsConnector.fetchFieldValues(clientId, apiContext, apiVersion))
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
    }

    "fail when fetching field definition values returns a 500" in new Setup {
      stubFor(
        get(urlEqualTo(definitionUrl))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(validDefinitionsResponse)
          )
      )
      stubFor(
        get(urlEqualTo(valueUrl))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )
      intercept[UpstreamErrorResponse] {
        await(subscriptionFieldsConnector.fetchFieldValues(clientId, apiContext, apiVersion))
      }
    }
  }

  "saveFieldValues" should {
    val fieldsValues        = fields(FieldName.random -> FieldValue.random, FieldName.random -> FieldValue.random)
    val subFieldsPutRequest = Json.toJson(SubscriptionFieldsPutRequest(clientId, apiContext, apiVersion, fieldsValues)).toString

    "save the fields" in new Setup {
      stubFor(
        put(urlEqualTo(valueUrl))
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
        put(urlEqualTo(valueUrl))
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
        put(urlEqualTo(valueUrl))
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

  "deleteFieldValues" should {

    "return success after delete call has returned 204 NO CONTENT" in new Setup {
      stubFor(
        delete(urlEqualTo(valueUrl))
          .willReturn(
            aResponse()
              .withStatus(NO_CONTENT)
          )
      )

      private val result = await(subscriptionFieldsConnector.deleteFieldValues(clientId, apiContext, apiVersion))

      result shouldBe FieldsDeleteSuccessResult
    }

    "return failure if api-subscription-fields returns unexpected status" in new Setup {
      stubFor(
        delete(urlEqualTo(valueUrl))
          .willReturn(
            aResponse()
              .withStatus(ACCEPTED)
          )
      )

      private val result = await(subscriptionFieldsConnector.deleteFieldValues(clientId, apiContext, apiVersion))

      result shouldBe FieldsDeleteFailureResult
    }

    "return failure when api-subscription-fields returns a 500" in new Setup {
      stubFor(
        delete(urlEqualTo(valueUrl))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      private val result = await(subscriptionFieldsConnector.deleteFieldValues(clientId, apiContext, apiVersion))

      result shouldBe FieldsDeleteFailureResult
    }

    "return success when api-subscription-fields returns a 404" in new Setup {
      stubFor(
        delete(urlEqualTo(valueUrl))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      private val result = await(subscriptionFieldsConnector.deleteFieldValues(clientId, apiContext, apiVersion))
      result shouldBe FieldsDeleteSuccessResult
    }
  }
}
