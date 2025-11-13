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

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.QueriedApplication
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.WireMockExtensions
import uk.gov.hmrc.gatekeeper.testdata.{ApplicationResponseTestData, MockDataSugar, StateHistoryTestData}

trait TpoApplicationQueryStub {
  self: WireMockExtensions with ApplicationResponseTestData with StateHistoryTestData =>

  import MockDataSugar._

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
      get(urlPathEqualTo("/environment/PRODUCTION/query"))
        .withQueryParam("pageNbr", equalTo("1"))
        .willReturn(aResponse().withBody(paginatedApplications).withStatus(OK))
    )
  }

  def stubFetchAllApplicationsList(): Unit = {
    stubFor(
      get(urlEqualTo("/environment/PRODUCTION/query"))   // urlEqualTo ensures no parameters
        .willReturn(aResponse().withBody(applicationsList.toString).withStatus(OK))
    )
    stubFor(
      get(urlEqualTo("/environment/SANDBOX/query"))   // urlEqualTo ensures no parameters
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
        .withQueryParam("applicationid", equalTo(id.toString()))
        .willReturn(
          aResponse()
            .withBody(response)
            .withStatus(OK)
        )
    )
    stubFor(
      get(urlPathEqualTo("/environment/PRODUCTION/query"))
        .withQueryParam("applicationid", equalTo(id.toString()))
        .willReturn(
          aResponse()
            .withBody(response)
            .withStatus(OK)
        )
    )
  }

  def stubApplicationForActionRefiner(appWithCollaborators: ApplicationWithCollaborators, appId: ApplicationId) = {
    stubFor(
      get(urlPathEqualTo("/environment/SANDBOX/query"))
        .withQueryParam("id", equalTo(appId.toString()))
        .withQueryParam("wantSubscriptions", WireMock.absent())
        .withQueryParam("wantSubscriptionFields", WireMock.absent())
        .withQueryParam("wantStateHistory", WireMock.absent())
        .willReturn(aResponse().withBody(appWithCollaborators.toJsonString).withStatus(OK))
    )
    stubFor(
      get(urlPathEqualTo("/environment/PRODUCTION/query"))
        .withQueryParam("id", equalTo(appId.toString()))
        .withQueryParam("wantSubscriptions", WireMock.absent())
        .withQueryParam("wantSubscriptionFields", WireMock.absent())
        .withQueryParam("wantStateHistory", WireMock.absent())
        .willReturn(aResponse().withBody(appWithCollaborators.toJsonString).withStatus(OK))
    )
  }

  def stubQueryWithStateHistory(appId: ApplicationId, appWithSubsFields: ApplicationWithSubscriptionFields, stateHistory: List[StateHistory]) = {
    val result1 = QueriedApplication(
      details = appWithSubsFields.details,
      collaborators = appWithSubsFields.collaborators,
      None,
      None,
      stateHistory = Some(stateHistory.withApplicationId(appWithSubsFields.id))
    )
    stubFor(
      get(urlPathEqualTo(s"/environment/SANDBOX/query"))
        .withQueryParam("wantSubscriptions", WireMock.absent())
        .withQueryParam("wantSubscriptionFields", WireMock.absent())
        .withQueryParam("wantStateHistory", matching(".*"))
        .willReturn(
          aResponse().withJsonBody(result1)
        )
    )
    stubFor(
      get(urlPathEqualTo(s"/environment/PRODUCTION/query"))
        .withQueryParam("wantSubscriptions", WireMock.absent())
        .withQueryParam("wantSubscriptionFields", WireMock.absent())
        .withQueryParam("wantStateHistory", matching(".*"))
        .willReturn(
          aResponse().withJsonBody(result1)
        )
    )

    val result2 = QueriedApplication(
      details = appWithSubsFields.details,
      collaborators = appWithSubsFields.collaborators,
      subscriptions = Some(appWithSubsFields.subscriptions),
      fieldValues = Some(appWithSubsFields.fieldValues),
      stateHistory = Some(stateHistory.withApplicationId(appWithSubsFields.id))
    )
    stubFor(
      get(urlPathEqualTo(s"/environment/SANDBOX/query"))
        .withQueryParam("wantSubscriptions", matching(".*"))
        .withQueryParam("wantSubscriptionFields", matching(".*"))
        .withQueryParam("wantStateHistory", matching(".*"))
        .willReturn(
          aResponse().withJsonBody(result2)
        )
    )
    stubFor(
      get(urlPathEqualTo(s"/environment/PRODUCTION/query"))
        .withQueryParam("wantSubscriptions", matching(".*"))
        .withQueryParam("wantSubscriptionFields", matching(".*"))
        .withQueryParam("wantStateHistory", matching(".*"))
        .willReturn(
          aResponse().withJsonBody(result2)
        )
    )

  }

  def stubBlockedApplication(): Unit = {
    stubApplicationById(blockedApplicationId, blockedApplicationResponse.toJsonString)
  }

  def stubApplicationForDeveloperDefault(): Unit = {
    stubApplicationForDeveloper(UserId(developer8Id), applicationResponseForEmail)
  }

  def stubUnblockedApplication(): Unit = {
    stubApplicationById(applicationId, defaultApplicationResponse.toJsonString)
  }

  def stubApplicationResponse(applicationId: ApplicationId, responseBody: String) = {
    stubApplicationById(applicationId, responseBody)
  }

}
