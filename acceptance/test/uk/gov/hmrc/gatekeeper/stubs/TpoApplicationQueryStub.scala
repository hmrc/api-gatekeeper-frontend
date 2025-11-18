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
import com.github.tomakehurst.wiremock.client.{ResponseDefinitionBuilder, _}
import com.github.tomakehurst.wiremock.stubbing.StubMapping

import play.api.http.Status._
import play.api.libs.json.Writes

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.QueriedApplication
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.WireMockExtensions
import uk.gov.hmrc.gatekeeper.testdata.{ApplicationResponseTestData, MockDataSugar, StateHistoryTestData}

trait TpoApplicationQueryStub {
  self: WireMockExtensions with ApplicationResponseTestData with StateHistoryTestData =>

  import MockDataSugar._

  val applicationsList =
    List(
      standardApp.withName(applicationName),
      standardApp2,
      blockedApplicationResponse,
      privilegedApp,
      ropcApp
    )

  def stubBoth(requestTransformer: MappingBuilder => MappingBuilder, response: ResponseDefinitionBuilder): StubMapping = {
    stubFor(
      requestTransformer(get(urlPathEqualTo("/environment/SANDBOX/query")))
        .willReturn(response)
    )

    stubFor(
      requestTransformer(get(urlPathEqualTo("/environment/PRODUCTION/query")))
        .willReturn(response)
    )
  }

  def stubBoth(requestTransformer: MappingBuilder => MappingBuilder, response: String): StubMapping = stubBoth(requestTransformer, aResponse().withBody(response).withStatus(OK))

  def stubBoth[T](requestTransformer: MappingBuilder => MappingBuilder, body: T)(implicit wrts: Writes[T]): StubMapping =
    stubBoth(requestTransformer, aResponse().withJsonBody(body).withStatus(OK))

  def stubPaginatedApplicationList() = {
    val paginatedApplicationsJson = PaginatedApplications(applicationsList, 1, 5, 7, 7)
    stubBoth(_.withQueryParam("pageNbr", equalTo("1")), paginatedApplicationsJson)
  }

  def stubFetchAllApplicationsList(): Unit = {
    stubFor(
      get(urlEqualTo("/environment/PRODUCTION/query")) // urlEqualTo ensures no parameters
        .willReturn(aResponse().withJsonBody(applicationsList).withStatus(OK))
    )
    stubFor(
      get(urlEqualTo("/environment/SANDBOX/query"))    // urlEqualTo ensures no parameters
        .willReturn(aResponse().withJsonBody(applicationsList).withStatus(OK))
    )
  }

  def stubApplicationForDeveloper(developerId: UserId, response: String): Unit = {
    stubBoth(_.withQueryParam("userId", equalTo(developerId.toString())), response)
  }

  def stubApplicationById(id: ApplicationId, app: ApplicationWithCollaborators): Unit = {
    stubBoth(
      _.withQueryParam("applicationId", equalTo(id.toString()))
        .withQueryParam("wantSubscriptions", WireMock.absent())
        .withQueryParam("wantSubscriptionFields", WireMock.absent())
        .withQueryParam("wantStateHistory", WireMock.absent()),
      app
    )
  }

  def stubAppQueryForActionBuilders(appId: ApplicationId, appWithSubsFields: ApplicationWithSubscriptionFields, stateHistory: List[StateHistory]) = {
    val result1 = QueriedApplication(
      details = appWithSubsFields.details,
      collaborators = appWithSubsFields.collaborators,
      None,
      None,
      None
    )

    stubBoth(
      _.withQueryParam("applicationId", equalTo(appId.toString()))
        .withQueryParam("wantSubscriptions", WireMock.absent())
        .withQueryParam("wantSubscriptionFields", WireMock.absent())
        .withQueryParam("wantStateHistory", WireMock.absent()),
      result1
    )

    val result2 = QueriedApplication(
      details = appWithSubsFields.details,
      collaborators = appWithSubsFields.collaborators,
      subscriptions = Some(appWithSubsFields.subscriptions),
      fieldValues = Some(appWithSubsFields.fieldValues),
      stateHistory = Some(stateHistory.withApplicationId(appWithSubsFields.id))
    )
    stubBoth(
      _.withQueryParam("applicationId", equalTo(appId.toString()))
        .withQueryParam("wantSubscriptions", matching(".*"))
        .withQueryParam("wantSubscriptionFields", matching(".*"))
        .withQueryParam("wantStateHistory", matching(".*")),
      result2
    )
  }

  def stubBlockedApplication(): Unit = {
    stubApplicationById(blockedApplicationId, blockedApplicationResponse)
  }

  def stubApplicationForDeveloperDefault(): Unit = {
    stubApplicationForDeveloper(UserId(developer8Id), applicationResponseForEmail)
  }

  def stubUnblockedApplication(): Unit = {
    stubApplicationById(applicationId, defaultApplicationResponse)
  }

}
