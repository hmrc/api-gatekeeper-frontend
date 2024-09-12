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

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.utils._
import uk.gov.hmrc.gatekeeper.connectors.ApiPlatformDeskproConnector.GetOrganisationsForUserRequest
import uk.gov.hmrc.gatekeeper.models.organisations.{DeskproOrganisation, DeskproPerson, OrganisationId}
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

class ApiPlatformDeskproConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with UrlEncoding
    with FixedClock {

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val httpClient = app.injector.instanceOf[HttpClientV2]

    val mockConnectorConfig: ApiPlatformDeskproConnector.Config = mock[ApiPlatformDeskproConnector.Config]
    when(mockConnectorConfig.serviceBaseUrl).thenReturn(wireMockUrl)

    val organisationId: OrganisationId = OrganisationId("1")
    val organisation                   = DeskproOrganisation(organisationId, "test org", List(DeskproPerson("Bob", "bob@example.com".toLaxEmail)))
    val organisationsForUser           = List(DeskproOrganisation(organisationId, "test org 1", List.empty), DeskproOrganisation(OrganisationId("2"), "test org 2", List.empty))

    val underTest = new ApiPlatformDeskproConnector(mockConnectorConfig, httpClient)
  }

  "getOrganisation" should {
    "return organisation" in new Setup {
      val url     = s"/organisation/1"
      val payload = Json.toJson(organisation)

      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(payload.toString)
          )
      )

      val result = await(underTest.getOrganisation(organisationId, hc))

      result shouldBe organisation
    }
  }

  "getOrganisationsForUser" should {
    "return organisation" in new Setup {
      val url              = "/organisation/query"
      val userEmailAddress = LaxEmailAddress("bobfleming@example.com")
      val payload          = Json.toJson(organisationsForUser)

      stubFor(
        post(urlEqualTo(url))
          .withJsonRequestBody(GetOrganisationsForUserRequest(userEmailAddress))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(payload.toString)
          )
      )

      val result = await(underTest.getOrganisationsForUser(userEmailAddress, hc))

      result shouldBe Some(organisationsForUser)
    }

    "return empty list if error" in new Setup {
      val url              = "/organisation/query"
      val userEmailAddress = LaxEmailAddress("bobfleming@example.com")

      stubFor(
        post(urlEqualTo(url))
          .withJsonRequestBody(GetOrganisationsForUserRequest(userEmailAddress))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      val result = await(underTest.getOrganisationsForUser(userEmailAddress, hc))

      result shouldBe None
    }
  }
}
