/*
 * Copyright 2021 HM Revenue & Customs
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

package connectors

import utils.AsyncHmrcSpec
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import scala.concurrent.ExecutionContext.Implicits.global
import model.applications.ApplicationWithSubscriptionData
import model.ApplicationId
import uk.gov.hmrc.http.HeaderCarrier
import builder.{ApplicationBuilder, ApiBuilder}
import model._
import model.subscriptions.ApiData
import play.api.test.Helpers._
import play.api.libs.json.Json
import model.subscriptions.VersionData
import model.APIDefinitionFormatters._
import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.WireMockSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class ApmConnectorSpec 
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with utils.UrlEncoding {

  trait Setup extends ApplicationBuilder with ApiBuilder {
    implicit val hc = HeaderCarrier()

    val httpClient = app.injector.instanceOf[HttpClient]

    val mockApmConnectorConfig: ApmConnector.Config = mock[ApmConnector.Config]
    when(mockApmConnectorConfig.serviceBaseUrl).thenReturn(wireMockUrl)

    val applicationId = ApplicationId.random

    val application = buildApplication(applicationId)

    val underTest = new ApmConnector(httpClient, mockApmConnectorConfig)
  }

  "fetchApplicationById" should {
    "return ApplicationWithSubscriptionData" in new Setup {
      implicit val writesApplicationWithSubscriptionData = Json.writes[ApplicationWithSubscriptionData]

      val url = s"/applications/${applicationId.value}"
      val applicationWithSubscriptionData = ApplicationWithSubscriptionData(application, Set.empty, Map.empty)
      val payload = Json.toJson(applicationWithSubscriptionData)

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
      import model.APIDefinitionFormatters._
      implicit val versionDataWrites = Json.writes[VersionData]
      implicit val apiDataWrites = Json.writes[ApiData]

      val apiData = DefaultApiData.addVersion(VersionOne, DefaultVersionData)
      val apiContext = ApiContext("Api Context")
      val apiContextAndApiData = Map(apiContext -> apiData)
      val payload = Json.stringify(Json.toJson(apiContextAndApiData))

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

  "subscribeToApi" should {
    val apiContext = ApiContext.random
    val apiVersion = ApiVersion.random
    val apiIdentifier = ApiIdentifier(apiContext, apiVersion)
      
    "send authorisation and return CREATED if the request was successful on the backend" in new Setup {
      val url = s"/applications/${applicationId.value}/subscriptions"

      stubFor(
        post(urlPathEqualTo(url))
        .withQueryParam("restricted", equalTo("false"))
        .willReturn(
          aResponse()
          .withStatus(CREATED)
        )
      )
        
      val result = await(underTest.subscribeToApi(applicationId, apiIdentifier))

      result shouldBe ApplicationUpdateSuccessResult
    }

    "fail if the request failed on the backend" in new Setup {
      val url = s"/applications/${applicationId.value}/subscriptions"

      stubFor(
        post(urlPathEqualTo(url))
        .withQueryParam("restricted", equalTo("false"))
        .willReturn(
          aResponse()
          .withStatus(INTERNAL_SERVER_ERROR)
        )
      )

      intercept[UpstreamErrorResponse] {
        await(underTest.subscribeToApi(applicationId, apiIdentifier))
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "addTeamMember" should {
    val addTeamMemberRequest = AddTeamMemberRequest("admin@example.com", CollaboratorRole.DEVELOPER, None)

    "post the team member to the service" in new Setup {
      val url = s"/applications/${applicationId.value}/collaborators"

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
      val url = s"/applications/${applicationId.value}/collaborators"

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
      val url = s"/applications/${applicationId.value}/collaborators"

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
      val url = s"/applications/${applicationId.value}/collaborators"

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
}
