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

import com.github.tomakehurst.wiremock.client.WireMock._

import play.api.http.Status._
import play.api.libs.json.Json

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaborators, StateHistory}
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.DispatchSuccessResult
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.WireMockExtensions
import uk.gov.hmrc.gatekeeper.connectors.ApplicationConnector
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.testdata.{ApplicationResponseTestData, StateHistoryTestData}

trait ThirdPartyApplicationStub {
  self: WireMockExtensions with ApplicationResponseTestData with StateHistoryTestData =>

  def stubHasTermsOfUseInvitation(appId: ApplicationId, has: Boolean = false): Unit = {
    if (has) {
      stubFor(get(urlEqualTo(s"/terms-of-use/application/$appId"))
        .willReturn(aResponse()
          .withBody("""{"applicationId": 1}""")
          .withStatus(OK)))
    } else {
      stubFor(get(urlEqualTo(s"/terms-of-use/application/$appId"))
        .willReturn(aResponse()
          .withStatus(NOT_FOUND)))
    }
  }

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

  def stubApplicationForBlockSuccess(applicationId: ApplicationId, gkAppResponse: ApplicationWithCollaborators) = {
    val response = DispatchSuccessResult(gkAppResponse)
    stubFor(
      patch(urlEqualTo(s"/applications/${applicationId.toString()}/dispatch"))
        .withRequestBody(containing("blockApplication"))
        .willReturn(aResponse().withStatus(OK).withBody(Json.toJson(response).toString()))
    )
  }

  def stubApplicationForUnblockSuccess(applicationId: ApplicationId, gkAppResponse: ApplicationWithCollaborators) = {
    val response = DispatchSuccessResult(gkAppResponse)
    stubFor(
      patch(urlEqualTo(s"/applications/${applicationId.toString()}/dispatch"))
        .withRequestBody(containing("unblockApplication"))
        .willReturn(aResponse().withStatus(OK).withBody(Json.toJson(response).toString()))
    )
  }

  def stubStateHistory(stateHistory: List[StateHistory], appId: ApplicationId) = {
    stubFor(get(urlEqualTo(s"/gatekeeper/application/${appId}/stateHistory")).willReturn(aResponse().withBody(stateHistory.toJsonString).withStatus(OK)))
  }

  def stubSubmissionLatestIsNotCompleted(appId: ApplicationId) = {
    stubFor(get(urlPathMatching("/submissions/latestiscompleted/.*")).willReturn(aResponse().withBody("false").withStatus(OK)))
  }

  def stubSubmissionLatestIsNotFound(appId: ApplicationId) = {
    stubFor(get(urlPathMatching("/submissions/latestiscompleted/.*")).willReturn(aResponse().withStatus(NOT_FOUND)))
  }

}
