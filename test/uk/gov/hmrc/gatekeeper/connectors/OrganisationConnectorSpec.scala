/*
 * Copyright 2026 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock.{verify => _, _}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.libs.json.{Json, Writes}
import play.api.test.Helpers._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, InternalServerException}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{LaxEmailAddress, OrganisationId, UserId}
import uk.gov.hmrc.apiplatform.modules.common.utils._
import uk.gov.hmrc.apiplatform.modules.organisations.domain.models.{Organisation, OrganisationName}
import uk.gov.hmrc.gatekeeper.connectors.OrganisationConnector.ErrorMessage
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

class OrganisationConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with UrlEncoding
    with FixedClock {

  trait Setup {
    implicit val hc: HeaderCarrier                        = HeaderCarrier()
    implicit val writesErrorMessage: Writes[ErrorMessage] = Json.writes[ErrorMessage]

    val httpClient = app.injector.instanceOf[HttpClientV2]

    val mockConnectorConfig: OrganisationConnector.Config = mock[OrganisationConnector.Config]
    when(mockConnectorConfig.serviceBaseUrl).thenReturn(wireMockUrl)

    val userId       = UserId.random
    val email        = LaxEmailAddress("bob@example.com")
    val respIndiv    = uk.gov.hmrc.apiplatform.modules.organisations.domain.models.Collaborators.ResponsibleIndividual(userId)
    val organisation = Organisation(OrganisationId.random, OrganisationName("Org name"), Organisation.OrganisationType.UkLimitedCompany, instant, Set(respIndiv))

    val underTest = new OrganisationConnector(mockConnectorConfig, httpClient)
  }

  "fetchOrganisationsByUserId" should {
    "return list of organisations" in new Setup {
      val url = s"/organisation/user/$userId"

      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(List(organisation))
          )
      )

      val result = await(underTest.fetchOrganisationsByUserId(userId))

      result shouldBe List(organisation)
    }
  }

  "removeCollaboratorFromOrganisation" should {
    "return organisation when successful" in new Setup {
      val url = s"/organisation/${organisation.id.value}/member/$userId"

      stubFor(
        delete(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(organisation)
          )
      )

      val result = await(underTest.removeCollaboratorFromOrganisation(organisation.id, userId, email))

      result shouldBe organisation
    }

    "throw exception when error" in new Setup {
      val url = s"/organisation/${organisation.id.value}/member/$userId"

      stubFor(
        delete(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      intercept[InternalServerException] {
        await(underTest.removeCollaboratorFromOrganisation(organisation.id, userId, email))
      }
    }

    "throw exception when bad request" in new Setup {
      val url = s"/organisation/${organisation.id.value}/member/$userId"

      stubFor(
        delete(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withJsonBody(ErrorMessage("Error"))
          )
      )

      intercept[BadRequestException] {
        await(underTest.removeCollaboratorFromOrganisation(organisation.id, userId, email))
      }
    }
  }
}
