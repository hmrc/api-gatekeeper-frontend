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

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global

import com.github.tomakehurst.wiremock.client.WireMock._
import org.joda.time.DateTime
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.common.utils.{AsyncHmrcSpec, _}
import uk.gov.hmrc.gatekeeper.builder.{ApiBuilder, ApplicationBuilder}
import uk.gov.hmrc.gatekeeper.models.APIAccessType.PUBLIC
import uk.gov.hmrc.gatekeeper.models.APIDefinitionFormatters._
import uk.gov.hmrc.gatekeeper.models.applications.ApplicationWithSubscriptionData
import uk.gov.hmrc.gatekeeper.models.pushpullnotifications._
import uk.gov.hmrc.gatekeeper.models.subscriptions.{ApiData, VersionData}
import uk.gov.hmrc.gatekeeper.models.{CombinedApi, _}
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

class ApmConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with UrlEncoding
    with ApplicationUpdateFormatters {

  trait Setup extends ApplicationBuilder with ApiBuilder {
    implicit val hc = HeaderCarrier()

    val httpClient = app.injector.instanceOf[HttpClient]

    val mockApmConnectorConfig: ApmConnector.Config = mock[ApmConnector.Config]
    when(mockApmConnectorConfig.serviceBaseUrl).thenReturn(wireMockUrl)

    val applicationId = ApplicationId.random

    val application = buildApplication(applicationId)

    val underTest = new ApmConnector(httpClient, mockApmConnectorConfig)

    val combinedRestApi1 = CombinedApi("displayName1", "serviceName1", List(CombinedApiCategory("CUSTOMS")), ApiType.REST_API, Some(PUBLIC))
    val combinedXmlApi2  = CombinedApi("displayName2", "serviceName2", List(CombinedApiCategory("VAT")), ApiType.XML_API, Some(PUBLIC))
    val combinedList     = List(combinedRestApi1, combinedXmlApi2)

    val boxSubscriber = BoxSubscriber("callbackUrl", DateTime.parse("2001-01-01T01:02:03"), SubscriptionType.API_PUSH_SUBSCRIBER)
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
      import uk.gov.hmrc.gatekeeper.models.APIDefinitionFormatters._
      implicit val versionDataWrites = Json.writes[VersionData]
      implicit val apiDataWrites     = Json.writes[ApiData]

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

  "applicationUpdate" should {
    val actor              = GatekeeperActor("Admin Powers")
    val apiIdentifier      = ApiIdentifier.random
    val unsubscribeFromApi = UnsubscribeFromApi(actor, apiIdentifier, LocalDateTime.now())
    val requestBody        = Json.toJsObject(unsubscribeFromApi) ++ Json.obj("updateType" -> "unsubscribeFromApi")

    "return OK if the request was successful" in new Setup {
      val url = s"/applications/${applicationId.value}"

      stubFor(
        patch(urlPathEqualTo(url))
          .withJsonRequestBody(requestBody)
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(application)
          )
      )

      val result = await(underTest.updateApplication(applicationId, unsubscribeFromApi))

      result shouldBe application
    }

    "fail if the request failed on the backend" in new Setup {
      val url = s"/applications/${applicationId.value}"

      stubFor(
        patch(urlPathEqualTo(url))
          .withJsonRequestBody(requestBody)
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      intercept[UpstreamErrorResponse] {
        await(underTest.updateApplication(applicationId, unsubscribeFromApi))
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "subscribeToApi" should {
    val actor          = GatekeeperActor("Admin Powers")
    val apiIdentifier  = ApiIdentifier.random
    val subscribeToApi = SubscribeToApi(actor, apiIdentifier, LocalDateTime.now())

    "send authorisation and return CREATED if the request was successful on the backend" in new Setup {
      val url = s"/applications/${applicationId.value.toString()}/subscriptionsAppUpdate"

      stubFor(
        post(urlPathEqualTo(url))
          .withQueryParam("restricted", equalTo("false"))
          .withJsonRequestBody(subscribeToApi)
          .willReturn(
            aResponse()
              .withStatus(CREATED)
          )
      )

      val result = await(underTest.subscribeToApi(applicationId, subscribeToApi))

      result shouldBe ApplicationUpdateSuccessResult
    }

    "fail if the request failed on the backend" in new Setup {
      val url = s"/applications/${applicationId.value.toString()}/subscriptionsAppUpdate"

      stubFor(
        post(urlPathEqualTo(url))
          .withQueryParam("restricted", equalTo("false"))
          .withJsonRequestBody(subscribeToApi)
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      intercept[UpstreamErrorResponse] {
        await(underTest.subscribeToApi(applicationId, subscribeToApi))
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "addTeamMember" should {
    val addTeamMemberRequest = AddTeamMemberRequest("admin@example.com", CollaboratorRole.DEVELOPER, None)

    "post the team member to the service" in new Setup {
      val url = s"/applications/${applicationId.value.toString()}/collaborators"

      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
          )
      )

      await(underTest.addTeamMember(applicationId, addTeamMemberRequest)) shouldBe (())
    }

    "throw TeamMemberAlreadyExists when the service returns 409 Conflict" in new Setup {
      val url = s"/applications/${applicationId.value.toString()}/collaborators"

      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(CONFLICT)
          )
      )

      intercept[TeamMemberAlreadyExists.type] {
        await(underTest.addTeamMember(applicationId, addTeamMemberRequest))
      }
    }

    "throw ApplicationNotFound when the service returns 404 Not Found" in new Setup {
      val url = s"/applications/${applicationId.value.toString()}/collaborators"

      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      intercept[ApplicationNotFound.type] {
        await(underTest.addTeamMember(applicationId, addTeamMemberRequest))
      }
    }

    "throw the error when the service returns any other error" in new Setup {
      val url = s"/applications/${applicationId.value.toString()}/collaborators"

      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      intercept[UpstreamErrorResponse] {
        await(underTest.addTeamMember(applicationId, addTeamMemberRequest))
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
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
    import play.api.libs.json.JodaWrites._
    implicit val writesBoxId         = Json.valueFormat[BoxId]
    implicit val writesBoxCreator    = Json.writes[BoxCreator]
    implicit val writesBoxSubscriber = Json.writes[BoxSubscriber]
    implicit val writesBox           = Json.writes[Box]

    "returns all boxes" in new Setup {
      val url = "/push-pull-notifications/boxes"

      val boxes = List(box)

      stubFor(
        get(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson(boxes).toString)
          )
      )

      val result = await(underTest.fetchAllBoxes())
      result shouldBe boxes
    }
  }
}
