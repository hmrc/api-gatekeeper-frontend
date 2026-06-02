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

import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{OrganisationId, UserId}
import uk.gov.hmrc.apiplatform.modules.common.utils._
import uk.gov.hmrc.apiplatform.modules.organisations.domain.models.{Organisation, OrganisationName}
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

class OrganisationConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with UrlEncoding
    with FixedClock {

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val httpClient = app.injector.instanceOf[HttpClientV2]

    val mockConnectorConfig: OrganisationConnector.Config = mock[OrganisationConnector.Config]
    when(mockConnectorConfig.serviceBaseUrl).thenReturn(wireMockUrl)

    val userId       = UserId.random
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
}
