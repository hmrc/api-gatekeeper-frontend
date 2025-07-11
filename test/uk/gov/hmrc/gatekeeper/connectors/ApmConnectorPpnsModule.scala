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

import java.time.{LocalDateTime, ZoneOffset}
import scala.concurrent.ExecutionContext.Implicits.global

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils._
import uk.gov.hmrc.gatekeeper.models.pushpullnotifications._
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

class ApmConnectorPpnsModuleSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with UrlEncoding
    with FixedClock {

  trait Setup extends ApplicationIdFixtures {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val httpClient = app.injector.instanceOf[HttpClientV2]

    val mockApmConnectorConfig: ApmConnector.Config = mock[ApmConnector.Config]
    when(mockApmConnectorConfig.serviceBaseUrl).thenReturn(wireMockUrl)

    val underTest: ApmConnectorPpnsModule = new ApmConnector(httpClient, mockApmConnectorConfig)

    val boxSubscriber = BoxSubscriber("callbackUrl", LocalDateTime.parse("2001-01-01T01:02:03").toInstant(ZoneOffset.UTC), SubscriptionType.API_PUSH_SUBSCRIBER)
    val box           = Box(BoxId("boxId"), "boxName", BoxCreator(ClientId("clientId")), Some(applicationIdOne), Some(boxSubscriber), Environment.PRODUCTION)

  }

  "fetchAllBoxes" should {
    "returns all boxes" in new Setup {
      val url = "/push-pull-notifications/boxes"

      val expectedBox = Box(
        BoxId("07787f13-dcae-4168-8685-c00a33b86134"),
        "someBoxName6",
        BoxCreator(ClientId("myClientIs6")),
        None,
        Some(BoxSubscriber("testurl.co.uk", LocalDateTime.parse("2001-01-01T01:02:03").toInstant(ZoneOffset.UTC), SubscriptionType.API_PUSH_SUBSCRIBER)),
        Environment.PRODUCTION
      )

      val text = """[{
                   |"boxId":"07787f13-dcae-4168-8685-c00a33b86134",
                   |"boxName":"someBoxName6",
                   |"boxCreator":{"clientId":"myClientIs6"},
                   |"subscriber":{"callBackUrl":"testurl.co.uk","subscribedDateTime":"2001-01-01T01:02:03.000+0000","subscriptionType":"API_PUSH_SUBSCRIBER"},
                   |"clientManaged":false,
                   |"environment": "PRODUCTION"
                   |}]""".stripMargin
      stubFor(
        get(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(text)
          )
      )

      val result = await(underTest.fetchAllBoxes())
      result shouldBe List(expectedBox)
    }
  }
}
