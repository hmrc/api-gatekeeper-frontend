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
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils._
import uk.gov.hmrc.gatekeeper.models.applications.{ApplicationWithUsers, User}
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

class ApiPlatformAdminApiConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with UrlEncoding
    with FixedClock {

  trait Setup {
    val hc: HeaderCarrier = HeaderCarrier()
    val httpClient        = app.injector.instanceOf[HttpClientV2]
    val authToken         = "1234"

    val mockConnectorConfig: ApiPlatformAdminApiConnector.Config = mock[ApiPlatformAdminApiConnector.Config]
    when(mockConnectorConfig.serviceBaseUrl).thenReturn(wireMockUrl)
    when(mockConnectorConfig.authToken).thenReturn(authToken)

    val applicationId = ApplicationId.random

    val application = buildApplication(applicationId, UserId.random)

    val underTest = new ApiPlatformAdminApiConnector(httpClient, mockConnectorConfig)
  }

  "getApplication" should {
    "return application" in new Setup {
      val url     = s"/applications/${applicationId}"
      val payload = Json.toJson(application)

      stubFor(
        get(urlEqualTo(url))
          .withHeader(HeaderNames.authorisation, equalTo(authToken))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(payload.toString)
          )
      )

      val result = await(underTest.getApplication(applicationId, hc))
      result should not be None

      result.map { app =>
        app.applicationId shouldBe application.applicationId
      }
    }
  }

  private def buildApplication(applicationId: ApplicationId, userId: UserId): ApplicationWithUsers = {
    ApplicationWithUsers(
      applicationId = applicationId,
      name = "Petes test application",
      environment = Environment.PRODUCTION,
      users = Set(User(userId, LaxEmailAddress("bob@example.com"), "Bob", "Fleming"))
    )
  }
}
