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

import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, _}
import uk.gov.hmrc.apiplatform.modules.common.utils.{AsyncHmrcSpec, _}
import uk.gov.hmrc.gatekeeper.builder.{ApiBuilder, ApplicationBuilder}
import uk.gov.hmrc.gatekeeper.models.APIDefinitionFormatters._
import uk.gov.hmrc.gatekeeper.models.applications.ApplicationWithSubscriptionData
import uk.gov.hmrc.gatekeeper.models.pushpullnotifications._
import uk.gov.hmrc.gatekeeper.models.{CombinedApi, _}
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

class ApmConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with UrlEncoding
    with FixedClock {

  trait Setup extends ApplicationBuilder with ApiBuilder {
    implicit val hc = HeaderCarrier()

    val httpClient = app.injector.instanceOf[HttpClient]

    val mockApmConnectorConfig: ApmConnector.Config = mock[ApmConnector.Config]
    when(mockApmConnectorConfig.serviceBaseUrl).thenReturn(wireMockUrl)

    val applicationId = ApplicationId.random

    val application = buildApplication(applicationId)

    val underTest = new ApmConnector(httpClient, mockApmConnectorConfig)

    val combinedRestApi1 = CombinedApi("displayName1", "serviceName1", Set(ApiCategory.CUSTOMS), ApiType.REST_API, Some(ApiAccessType.PUBLIC))
    val combinedXmlApi2  = CombinedApi("displayName2", "serviceName2", Set(ApiCategory.VAT), ApiType.XML_API, Some(ApiAccessType.PUBLIC))
    val combinedList     = List(combinedRestApi1, combinedXmlApi2)

    val boxSubscriber = BoxSubscriber("callbackUrl", LocalDateTime.parse("2001-01-01T01:02:03").toInstant(ZoneOffset.UTC), SubscriptionType.API_PUSH_SUBSCRIBER)
    val box           = Box(BoxId("boxId"), "boxName", BoxCreator(ClientId("clientId")), Some(applicationId), Some(boxSubscriber), Environment.PRODUCTION)
  }

  "fetchApplicationById" should {
    "return ApplicationWithSubscriptionData" in new Setup {
      implicit val writesApplicationWithSubscriptionData = Json.writes[ApplicationWithSubscriptionData]

      val url                             = s"/applications/${applicationId.value.toString()}"
      val applicationWithSubscriptionData = ApplicationWithSubscriptionData(application, Set.empty, Map.empty)
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
        appWithSubsData.application.id shouldBe application.id
      }
    }
  }

  "fetchAllPossibleSubscriptions" should {
    val url = "/api-definitions"

    "return all subscribeable API's and their ApiData" in new Setup {
      val apiData              = DefaultApiData.addVersion(VersionOne, DefaultVersionData)
      val apiContext           = ApiContext("Api Context")
      val apiContextAndApiData = Map(apiContext -> apiData)
      val payload              = Json.stringify(Json.toJson(apiContextAndApiData))

      stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam(ApmConnector.applicationIdQueryParam, equalTo(encode(applicationId.value.toString)))
          .withQueryParam(ApmConnector.restrictedQueryParam, equalTo("false"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(payload)
          )
      )

      val result = await(underTest.fetchAllPossibleSubscriptions(applicationId))
      result(apiContext).name shouldBe "API Name"
    }
  }

  "getAllFieldDefinitions" should {
    "returns empty field definitions" in new Setup {
      val url = "/subscription-fields\\?environment=PRODUCTION"

      stubFor(
        get(urlMatching(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("{}")
          )
      )

      val result = await(underTest.getAllFieldDefinitions(Environment.PRODUCTION))
      result shouldBe Map.empty
    }
  }

  "fetchAllCombinedApis" should {
    "returns combined xml and rest apis" in new Setup {
      val url = "/combined-rest-xml-apis"

      stubFor(
        get(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson(combinedList).toString)
          )
      )

      val result = await(underTest.fetchAllCombinedApis())
      result shouldBe combinedList
    }

    "returns exception when backend returns error" in new Setup {
      val url = "/combined-rest-xml-apis"

      stubFor(
        get(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      intercept[UpstreamErrorResponse] {
        await(underTest.fetchAllCombinedApis())
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
    }
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
