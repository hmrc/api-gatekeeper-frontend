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

package uk.gov.hmrc.apiplatform.modules.common.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.apiplatform.modules.common.utils._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.apiplatform.modules.events.connectors.ApiPlatformEventsConnector
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.gatekeeper.models.ApplicationId
import play.api.test.Helpers._
import uk.gov.hmrc.apiplatform.modules.events.domain.models.EventType
import uk.gov.hmrc.apiplatform.modules.events.domain.models.QueryableValues

class ApiPlatformEventsConnectorSpec 
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with UrlEncoding {
  
  private val applicationId = ApplicationId.random

  trait Setup {
    val authToken = "Bearer Token"
    implicit val hc = HeaderCarrier().withExtraHeaders(("Authorization", authToken))

    val httpClient = app.injector.instanceOf[HttpClient]
    val mockConfig: ApiPlatformEventsConnector.Config = mock[ApiPlatformEventsConnector.Config]
    when(mockConfig.baseUrl).thenReturn(wireMockUrl)

    val connector = new ApiPlatformEventsConnector(httpClient, mockConfig)
  }

  "calling fetchEventQueryValues" should {
    val url = s"/application-event/${applicationId.value}/values"

    "return None when call returns NOT_FOUND" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      await(connector.fetchEventQueryValues(applicationId)) shouldBe None
    }

    "return Some data when call returns OK" in new Setup {

      val fakeResponse = QueryableValues(1900,1950, List(EventType.TEAM_MEMBER_ADDED, EventType.TEAM_MEMBER_REMOVED), List("bob@example.com", "iam@gatekeeper.com"))

      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withJsonBody(fakeResponse)
              .withStatus(OK)
          )
      )

      await(connector.fetchEventQueryValues(applicationId)).value shouldBe fakeResponse
    }
  }

  "calling query" should {
    val url = s"/application-event/${applicationId.value}"

    "send year parameter when present" in new Setup {
      stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam("year", equalTo("1900"))
          .willReturn(
            aResponse()
              .withJsonBody(ApiPlatformEventsConnector.QueryResponse(Seq.empty))
              .withStatus(OK)
          )
      )

      await(connector.query(applicationId, Some(1900), None, None))
    }

    "send eventType parameter when present" in new Setup {
      stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam("eventType", equalTo("TEAM_MEMBER_ADDED"))
          .willReturn(
            aResponse()
              .withJsonBody(ApiPlatformEventsConnector.QueryResponse(Seq.empty))
              .withStatus(OK)
          )
      )

      await(connector.query(applicationId, None, Some(EventType.TEAM_MEMBER_ADDED), None))
    }

    "send actor parameter when present" in new Setup {
      stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam("actor", equalTo("bob@example.com"))
          .willReturn(
            aResponse()
              .withJsonBody(ApiPlatformEventsConnector.QueryResponse(Seq.empty))
              .withStatus(OK)
          )
      )

      await(connector.query(applicationId, None, None, Some("bob@example.com")))
    }
  }
}