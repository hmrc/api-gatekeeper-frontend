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

import scala.io.Source

import com.github.tomakehurst.wiremock.client.WireMock._

import play.api.http.Status._
import play.api.libs.json.Json

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.GKApplicationResponse
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.DispatchSuccessResult
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.WireMockExtensions
import uk.gov.hmrc.gatekeeper.connectors.ApplicationConnector
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.testdata.{ApplicationWithStateHistoryTestData, MockDataSugar}

trait ThirdPartyApplicationStub extends WireMockExtensions with ApplicationWithStateHistoryTestData {
  import MockDataSugar._

  def stubApplicationList(body: String): Unit = {
    stubFor(get(urlEqualTo("/gatekeeper/applications"))
      .willReturn(aResponse()
        .withBody(body)
        .withStatus(OK)))
  }

  def stubApplication(applicationResponse: String): Unit = {
    stubFor(get(urlEqualTo("/application"))
      .willReturn(aResponse()
        .withBody(applicationResponse)
        .withStatus(OK)))
  }

  def stubApplicationSubscription(applicationSubscriptionResponse: String): Unit = {
    stubFor(
      get(
        urlEqualTo("/application/subscriptions")
      )
        .willReturn(aResponse()
          .withBody(applicationSubscriptionResponse)
          .withStatus(OK))
    )
  }

  def stubApiSubscription(apiContext: String) = {
    stubFor(get(urlEqualTo(s"/application?subscribesTo=$apiContext"))
      .willReturn(aResponse().withBody(applicationResponse).withStatus(OK)))
  }

  def stubApplicationsCollaborators(developers: Seq[User]): Unit = {
    val developersJson = developers.map(u => u.email)
    val request        = ApplicationConnector.SearchCollaboratorsRequest(ApiContext("employers-paye"), ApiVersionNbr("1.0"), Some("partialEmail"))

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

  def stubApplicationsList() = {
    Source.fromURL(getClass.getResource("/applications.json")).mkString.replaceAll("\n", "")
  }

  def stubPaginatedApplicationList() = {
    val paginatedApplications = Source.fromURL(getClass.getResource("/paginated-applications.json")).mkString.replaceAll("\n", "")

    stubFor(get(urlMatching("/applications\\?page.*")).willReturn(aResponse().withBody(paginatedApplications).withStatus(OK)))
  }

  def stubFetchAllApplicationsList(): Unit = {
    val applicationsList = Source.fromURL(getClass.getResource("/applications.json")).mkString.replaceAll("\n", "")
    stubFor(get(urlEqualTo(s"/application")).willReturn(aResponse().withBody(applicationsList).withStatus(OK)))
  }

  def stubApplicationForDeveloper(developerId: UserId, response: String): Unit = {
    stubFor(
      get(urlPathEqualTo(s"/gatekeeper/developer/${developerId}/applications"))
        .willReturn(
          aResponse()
            .withBody(response)
            .withStatus(OK)
        )
    )
  }

  def stubBlockedApplication(): Unit = {
    stubFor(
      get(
        urlEqualTo(s"/gatekeeper/application/${blockedApplicationId.value.toString()}")
      )
        .willReturn(
          aResponse()
            .withBody(blockedApplicationWithHistory.toJsonString)
            .withStatus(OK)
        )
    )
  }

  def stubApplicationForDeveloperDefault(): Unit = {
    stubApplicationForDeveloper(UserId(developer8Id), applicationResponseForEmail)
  }

  def stubApplicationExcludingDeletedForDeveloper(): Unit = {
    stubFor(
      get(urlPathEqualTo(s"/developer/${developer8Id.toString()}/applications"))
        .willReturn(
          aResponse()
            .withBody(applicationResponseForEmail)
            .withStatus(OK)
        )
    )
  }

  def stubApplicationToDelete(applicationId: ApplicationId) = {
    stubFor(get(urlEqualTo(s"/gatekeeper/application/${applicationId.value.toString()}")).willReturn(aResponse().withBody(defaultApplicationWithHistory.toJsonString).withStatus(OK)))
  }

  def stubApplicationForUnblockSuccess(applicationId: ApplicationId, gkAppResponse: GKApplicationResponse) = {
    val response = DispatchSuccessResult(gkAppResponse)
    stubFor(
      patch(urlEqualTo(s"/applications/${applicationId.toString()}/dispatch"))
        .withRequestBody(containing("unblockApplication"))
        .willReturn(aResponse().withStatus(OK).withBody(Json.toJson(response).toString()))
    )
  }

  def stubApplicationToReview(applicationId: ApplicationId) = {
    stubFor(get(urlEqualTo(s"/gatekeeper/application/${applicationId.value.toString()}")).willReturn(
      aResponse().withBody(pendingApprovalApplicationWithHistory.toJsonString).withStatus(OK)
    ))
  }

  def stubApplicationForActionRefiner(applicationWithHistory: String, appId: ApplicationId) = {
    stubFor(get(urlEqualTo(s"/gatekeeper/application/${appId}")).willReturn(aResponse().withBody(applicationWithHistory).withStatus(OK)))
  }

  def stubStateHistory(stateHistory: String, appId: ApplicationId) = {
    stubFor(get(urlEqualTo(s"/gatekeeper/application/${appId}/stateHistory")).willReturn(aResponse().withBody(stateHistory).withStatus(OK)))
  }

  def stubSubmissionLatestIsNotCompleted(appId: ApplicationId) = {
    stubFor(get(urlPathMatching(s"/submissions/latestiscompleted/.*")).willReturn(aResponse().withBody("false").withStatus(OK)))
  }

  def stubSubmissionLatestIsNotFound(appId: ApplicationId) = {
    stubFor(get(urlPathMatching(s"/submissions/latestiscompleted/.*")).willReturn(aResponse().withStatus(NOT_FOUND)))
  }

  def stubApplicationForBlockSuccess(applicationId: ApplicationId, gkAppResponse: GKApplicationResponse) = {
    val response = DispatchSuccessResult(gkAppResponse)
    stubFor(
      patch(urlEqualTo(s"/applications/${applicationId.toString()}/dispatch"))
        .withRequestBody(containing("blockApplication"))
        .willReturn(aResponse().withStatus(OK).withBody(Json.toJson(response).toString()))
    )
  }

  def stubUnblockedApplication(): Unit = {
    stubFor(get(urlEqualTo(s"/gatekeeper/application/${applicationId.value.toString()}")).willReturn(aResponse().withBody(defaultApplicationWithHistory.toJsonString).withStatus(OK)))
  }

  def stubApplicationResponse(applicationId: String, responseBody: String) = {
    stubFor(get(urlEqualTo(s"/gatekeeper/application/$approvedApp1"))
      .willReturn(aResponse().withBody(responseBody).withStatus(OK)))
  }

}
