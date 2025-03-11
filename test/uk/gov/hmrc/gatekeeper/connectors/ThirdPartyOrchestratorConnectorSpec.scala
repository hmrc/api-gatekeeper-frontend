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

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils._
import uk.gov.hmrc.gatekeeper.mocks.ApplicationResponseBuilder
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

class ThirdPartyOrchestratorConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with UrlEncoding
    with FixedClock {

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val httpClient = app.injector.instanceOf[HttpClientV2]

    val mockConnectorConfig: ThirdPartyOrchestratorConnector.Config = mock[ThirdPartyOrchestratorConnector.Config]
    when(mockConnectorConfig.serviceBaseUrl).thenReturn(wireMockUrl)

    val applicationId = ApplicationId.random

    val application: ApplicationWithCollaborators = ApplicationResponseBuilder.buildApplication(applicationId, ClientId.random, UserId.random)

    val underTest = new ThirdPartyOrchestratorConnector(httpClient, mockConnectorConfig)
  }

  "getApplication" should {
    "return application" in new Setup {
      val url     = s"/applications/${applicationId}"
      val payload = Json.toJson(application)

      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(payload.toString)
          )
      )

      val result = await(underTest.getApplication(applicationId))
      result should not be None

      result.map { app =>
        app.id shouldBe application.id
      }
    }
  }

  "getApplicationsByEmails" should {
    "return applications" in new Setup {
      val url     = s"/developer/applications"
      val email   = "test@email.com".toLaxEmail
      val payload = Json.toJson(List(application))

      stubFor(
        post(urlEqualTo(url))
          .withJsonRequestBody(ApplicationsByRequest(List(email)))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(payload.toString)
          )
      )

      val result = await(underTest.getApplicationsByEmails(List(email)))

      result shouldBe List(application)
    }
  }

  "validateName" should {
    val url = s"/environment/PRODUCTION/application/name/validate"

    "returns a valid response" in new Setup {
      val applicationName                                   = "my valid application name"
      val appId                                             = ApplicationId.random
      val expectedRequest: ApplicationNameValidationRequest = ChangeApplicationNameValidationRequest(applicationName, appId)
      val expectedResponse: ApplicationNameValidationResult = ApplicationNameValidationResult.Valid

      stubFor(
        post(urlEqualTo(url))
          .withJsonRequestBody(expectedRequest)
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(expectedResponse)
          )
      )
      val result = await(underTest.validateName(applicationName, Some(appId), Environment.PRODUCTION))
      result shouldBe ApplicationNameValidationResult.Valid
    }

    "returns a invalid response" in new Setup {

      val applicationName                                   = "my invalid application name"
      val expectedRequest: ApplicationNameValidationRequest = NewApplicationNameValidationRequest(applicationName)
      val expectedResponse: ApplicationNameValidationResult = ApplicationNameValidationResult.Invalid

      stubFor(
        post(urlEqualTo(url))
          .withJsonRequestBody(expectedRequest)
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(expectedResponse)
          )
      )
      val result = await(underTest.validateName(applicationName, None, Environment.PRODUCTION))
      result shouldBe ApplicationNameValidationResult.Invalid
    }

  }

}
