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

import scala.concurrent.ExecutionContext.Implicits.global

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils._
import uk.gov.hmrc.gatekeeper.builder.ApiBuilder
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

class ApmConnectorApiDefintionModuleSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with UrlEncoding
    with FixedClock {

  trait Setup extends ApiBuilder {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val httpClient = app.injector.instanceOf[HttpClientV2]

    val mockApmConnectorConfig: ApmConnector.Config = mock[ApmConnector.Config]
    when(mockApmConnectorConfig.serviceBaseUrl).thenReturn(wireMockUrl)

    val applicationId = ApplicationId.random

    val underTest: ApmConnectorApiDefinitionModule = new ApmConnector(httpClient, mockApmConnectorConfig)
  }

  "fetchAllPossibleSubscriptions" should {
    val url = "/api-definitions"

    "return all subscribeable API's and their ApiDefinition" in new Setup {
      val apiDefinition           = DefaultApiDefinition.addVersion(VersionOne, DefaultVersionData)
      val apiContext              = ApiContext("Api Context")
      val apiContextAndDefinition = Map(apiContext -> apiDefinition)
      val payload                 = Json.stringify(Json.toJson(apiContextAndDefinition))

      stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam(ApmConnectorApiDefinitionModule.ApplicationIdQueryParam, equalTo(encode(applicationId.value.toString)))
          .withQueryParam(ApmConnectorApiDefinitionModule.RestrictedQueryParam, equalTo("false"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(payload)
          )
      )

      val result = await(underTest.fetchAllPossibleSubscriptions(applicationId))
      result shouldBe List(apiDefinition)
    }

    "return all subscribeable API's and their ApiDefinition from map of definitions" in new Setup {
      val apiDefinition       = DefaultApiDefinition.addVersion(VersionOne, DefaultVersionData)
      val listOfApiDefinition = List(apiDefinition)
      val payload             = Json.stringify(Json.toJson(listOfApiDefinition))

      stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam(ApmConnectorApiDefinitionModule.ApplicationIdQueryParam, equalTo(encode(applicationId.value.toString)))
          .withQueryParam(ApmConnectorApiDefinitionModule.RestrictedQueryParam, equalTo("false"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(payload)
          )
      )

      val result = await(underTest.fetchAllPossibleSubscriptions(applicationId))
      result shouldBe List(apiDefinition)
    }
  }
}
