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

package uk.gov.hmrc.apiplatform.modules.events.connectors

import scala.concurrent.ExecutionContext.Implicits.global

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import uk.gov.hmrc.apiplatform.modules.common.utils._
import uk.gov.hmrc.gatekeeper.testdata.{DisplayEventTestDataBuilder, DisplayEventsTestData}
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

class PrincipalApiPlatformEventsConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with UrlEncoding
    with DisplayEventsTestData
    with DisplayEventTestDataBuilder
    with FixedClock {

  trait Setup {
    val authToken                  = "Bearer Token"
    implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(("Authorization", authToken))

    val httpClient                                             = app.injector.instanceOf[HttpClient]
    val mockConfig: PrincipalApiPlatformEventsConnector.Config = mock[PrincipalApiPlatformEventsConnector.Config]
    when(mockConfig.serviceBaseUrl).thenReturn(wireMockUrl)

    val connector = new PrincipalApiPlatformEventsConnector(mockConfig, httpClient)
  }

  "calling fetchQueryableEventTags" should {
    val url = s"/application-event/${applicationId.value.toString()}/values"

    "return empty list when call returns NOT_FOUND" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      await(connector.fetchQueryableValues(applicationId)) shouldBe QueryableValues(List.empty, List.empty)
    }

    "return list of data when call returns OK" in new Setup {

      private val response: QueryableValues = QueryableValues(List(FilterValue("Team member", "TEAM_MEMBER")), List.empty)
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withJsonBody(response)
              .withStatus(OK)
          )
      )

      await(connector.fetchQueryableValues(applicationId)) shouldBe response
    }
  }

  "calling query" should {
    val url = s"/application-event/${applicationId.value.toString()}"

    "return empty list when NOT_FOUND" in new Setup {
      stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam("eventTag", equalTo("TEAM_MEMBER"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      await(connector.query(applicationId, Some("TEAM_MEMBER"), None)) shouldBe List.empty
    }

    "return list when OK" in new Setup {

      val sampleResponse = ApiPlatformEventsConnector.QueryResponse(
        List(event1, event2, event3)
      )

      stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam("eventTag", equalTo("TEAM_MEMBER"))
          .willReturn(
            aResponse()
              .withJsonBody(Some(sampleResponse))
              .withStatus(OK)
          )
      )

      val results = await(connector.query(applicationId, Some("TEAM_MEMBER"), None))

      results.length shouldBe 3
    }

    "return list when OK with two params" in new Setup {

      val sampleResponse = ApiPlatformEventsConnector.QueryResponse(
        List(event1, event2, event3)
      )

      stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam("eventTag", equalTo("TEAM_MEMBER"))
          .withQueryParam("actorType", equalTo("COLLABORATOR"))
          .willReturn(
            aResponse()
              .withJsonBody(Some(sampleResponse))
              .withStatus(OK)
          )
      )

      val results = await(connector.query(applicationId, Some("TEAM_MEMBER"), Some("COLLABORATOR")))

      results.length shouldBe 3
    }
  }
}
