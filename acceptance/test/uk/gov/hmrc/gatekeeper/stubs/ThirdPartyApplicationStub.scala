/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.stubs

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._

import play.api.http.Status._
import play.api.libs.json.Json

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaborators, ApplicationWithSubscriptionFields, StateHistory}
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.QueriedApplication
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.DispatchSuccessResult
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.WireMockExtensions
import uk.gov.hmrc.gatekeeper.connectors.ApplicationConnector
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.testdata.{ApplicationWithStateHistoryTestData, MockDataSugar}

trait ThirdPartyApplicationStub extends WireMockExtensions with ApplicationWithStateHistoryTestData {
  import MockDataSugar._

  def stubApplicationsCollaborators(developers: Seq[AbstractUser]): Unit = {
    val developersJson = developers.map(u => u.email)
    val request        = ApplicationConnector.SearchCollaboratorsRequest(ApiContext("employers-paye"), ApiVersionNbr("1.0"))

    stubFor(post(urlEqualTo("/collaborators"))
      .withJsonRequestBody(request)
      .willReturn(aResponse()
        .withJsonBody(developersJson)
        .withStatus(OK)))
  }

  def stubApplicationApproveUplift(applicationId: ApplicationId, superUserGatekeeperId: String) = {
    val approveRequest =
      s"""
         |{
         |  "gatekeeperUserId":"$superUserGatekeeperId"
         |}
     """.stripMargin
    stubFor(
      post(
        urlMatching(s"/application/${applicationId.value.toString()}/approve-uplift")
      )
        .withRequestBody(equalToJson(approveRequest))
        .willReturn(aResponse().withStatus(OK))
    )
  }

  def stubApplicationRejectUplift(applicationId: ApplicationId, gatekeeperId: String) = {
    val rejectRequest =
      s"""
         |{
         |  "gatekeeperUserId":"$gatekeeperId",
         |  "reason":"A similar name is already taken by another application"
         |}
     """.stripMargin

    stubFor(post(urlMatching(s"/application/${applicationId.value.toString()}/reject-uplift"))
      .withRequestBody(equalToJson(rejectRequest))
      .willReturn(aResponse().withStatus(200)))
  }

  val applicationsList = Json.toJson(
    List(
      standardApp.withName(applicationName),
      standardApp2,
      blockedApplicationResponse,
      privilegedApp,
      ropcApp
    )
  )

  def stubPaginatedApplicationList() = {
    val paginatedApplications = Json.obj(
      "applications" -> applicationsList,
      "page"         -> 1,
      "pageSize"     -> 5,
      "total"        -> 7,
      "matching"     -> 7
    ).toString()

    stubFor(
      get(urlPathEqualTo("/environment/SANDBOX/query"))
        .withQueryParam("pageNbr", equalTo("1"))
        .willReturn(aResponse().withBody(paginatedApplications).withStatus(OK))
    )
    stubFor(
      get(urlPathEqualTo("/environment/PRODUCTIION/query"))
        .withQueryParam("pageNbr", equalTo("1"))
        .willReturn(aResponse().withBody(paginatedApplications).withStatus(OK))
    )
  }

  def stubFetchAllApplicationsList(): Unit = {
    stubFor(
      get(urlEqualTo("/environment/PRODUCTIION/query"))
        .withQueryParam("pageNbr", WireMock.absent())
        .willReturn(aResponse().withBody(applicationsList.toString).withStatus(OK))
    )
    stubFor(
      get(urlEqualTo("/environment/SANDBOX/query"))
        .withQueryParam("pageNbr", WireMock.absent())
        .willReturn(aResponse().withBody(applicationsList.toString).withStatus(OK))
    )
  }

  def stubApplicationForDeveloper(developerId: UserId, response: String): Unit = {
    stubFor(
      get(urlPathEqualTo("/environment/SANDBOX/query"))
        .withQueryParam("userId", equalTo(developerId.toString()))
        .willReturn(
          aResponse()
            .withBody(response)
            .withStatus(OK)
        )
    )
    stubFor(
      get(urlPathEqualTo("/environment/PRODUCTION/query"))
        .withQueryParam("userId", equalTo(developerId.toString()))
        .willReturn(
          aResponse()
            .withBody(response)
            .withStatus(OK)
        )
    )
  }

  def stubApplicationById(id: ApplicationId, response: String): Unit = {
    stubFor(
      get(urlPathEqualTo("/environment/SANDBOX/query"))
        .withQueryParam("id", equalTo(id.toString()))
        .willReturn(
          aResponse()
            .withBody(response)
            .withStatus(OK)
        )
    )
    stubFor(
      get(urlPathEqualTo("/environment/PRODUCTION/query"))
        .withQueryParam("userId", equalTo(id.toString()))
        .willReturn(
          aResponse()
            .withBody(response)
            .withStatus(OK)
        )
    )
  }  

  def stubBlockedApplication(): Unit = {
    stubApplicationById(blockedApplicationId, blockedApplicationWithHistory.toJsonString)
  }

  def stubApplicationForDeveloperDefault(): Unit = {
    stubApplicationForDeveloper(UserId(developer8Id), applicationResponseForEmail)
  }

  def stubApplicationToDelete(applicationId: ApplicationId) = {
    stubApplicationById(applicationId, defaultApplicationWithHistory.toJsonString)
  }

  def stubApplicationForUnblockSuccess(applicationId: ApplicationId, gkAppResponse: ApplicationWithCollaborators) = {
    val response = DispatchSuccessResult(gkAppResponse)
    stubFor(
      patch(urlEqualTo(s"/applications/${applicationId.toString()}/dispatch"))
        .withRequestBody(containing("unblockApplication"))
        .willReturn(aResponse().withStatus(OK).withBody(Json.toJson(response).toString()))
    )
  }

  def stubApplicationToReview(applicationId: ApplicationId) = {
    stubApplicationById(applicationId, pendingApprovalApplicationWithHistory.toJsonString)
  }

  def stubApplicationForActionRefiner(applicationWithHistory: String, appId: ApplicationId) = {
    stubFor(
      get(urlPathEqualTo("/environment/SANDBOX/query"))
        .withQueryParam("id", equalTo(appId.toString()))
        .willReturn(aResponse().withBody(applicationWithHistory).withStatus(OK))
    )
  }

  def stubStateHistory(stateHistory: String, appId: ApplicationId) = {
    stubFor(get(urlEqualTo(s"/gatekeeper/application/${appId}/stateHistory")).willReturn(aResponse().withBody(stateHistory).withStatus(OK)))
  }

  def stubQueryWithStateHistory(appId: ApplicationId, applicationWithHistory: String, stateHistory: String) = {
    val appWH  = Json.parse(applicationWithHistory).as[ApplicationWithSubscriptionFields]
    val stateH = Json.parse(stateHistory).as[List[StateHistory]]
    val result = QueriedApplication(
      details = appWH.details,
      collaborators = appWH.collaborators,
      None,
      None,
      stateHistory = Some(stateH)
    )
    stubFor(
      get(urlPathEqualTo(s"/environment/PRODUCTION/query"))
        .withQueryParam("wantStateHistory", matching(".*"))
        .willReturn(
          aResponse().withJsonBody(result)
        )
    )
  }

  def stubSubmissionLatestIsNotCompleted(appId: ApplicationId) = {
    stubFor(get(urlPathMatching(s"/submissions/latestiscompleted/.*")).willReturn(aResponse().withBody("false").withStatus(OK)))
  }

  def stubSubmissionLatestIsNotFound(appId: ApplicationId) = {
    stubFor(get(urlPathMatching(s"/submissions/latestiscompleted/.*")).willReturn(aResponse().withStatus(NOT_FOUND)))
  }

  def stubApplicationForBlockSuccess(applicationId: ApplicationId, gkAppResponse: ApplicationWithCollaborators) = {
    val response = DispatchSuccessResult(gkAppResponse)
    stubFor(
      patch(urlEqualTo(s"/applications/${applicationId.toString()}/dispatch"))
        .withRequestBody(containing("blockApplication"))
        .willReturn(aResponse().withStatus(OK).withBody(Json.toJson(response).toString()))
    )
  }

  def stubUnblockedApplication(): Unit = {
    stubApplicationById(applicationId, defaultApplicationWithHistory.toJsonString)
  }

  def stubApplicationResponse(applicationId: ApplicationId, responseBody: String) = {
    stubApplicationById(applicationId, responseBody)
  }

}
