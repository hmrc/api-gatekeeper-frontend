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

import play.api.libs.json._
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithSubscriptionFields
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils._
import uk.gov.hmrc.gatekeeper.builder.ApplicationBuilder
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

class ApmConnectorApplicationModuleSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with UrlEncoding
    with FixedClock {

  trait Setup extends ApplicationBuilder {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val httpClient = app.injector.instanceOf[HttpClientV2]

    val mockApmConnectorConfig: ApmConnector.Config = mock[ApmConnector.Config]
    when(mockApmConnectorConfig.serviceBaseUrl).thenReturn(wireMockUrl)

    val applicationId = ApplicationId.random

    val application = DefaultApplication.modify(_.copy(id = applicationId))

    val underTest: ApmConnectorApplicationModule = new ApmConnector(httpClient, mockApmConnectorConfig)
  }

  "fetchApplicationById" should {
    "return ApplicationWithSubscriptionData" in new Setup {
      implicit val writesApplicationWithSubscriptionData: Writes[ApplicationWithSubscriptionFields] = Json.writes[ApplicationWithSubscriptionFields]

      val url                             = s"/applications/${applicationId.value.toString()}"
      val applicationWithSubscriptionData = ApplicationWithSubscriptionFields(application.details, application.collaborators, Set.empty, Map.empty)
      val payload                         = Json.toJson(applicationWithSubscriptionData)

      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(payload.toString)
          )
      )

      val result = await(underTest.fetchApplicationById(applicationId))
      result should not be None

      result.map { appWithSubsData =>
        appWithSubsData.details.id shouldBe application.id
      }
    }
  }
}
