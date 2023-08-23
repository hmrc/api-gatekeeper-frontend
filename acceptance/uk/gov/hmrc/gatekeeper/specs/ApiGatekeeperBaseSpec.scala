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

package uk.gov.hmrc.gatekeeper.specs


import uk.gov.hmrc.gatekeeper.matchers.CustomMatchers
import uk.gov.hmrc.gatekeeper.testdata.{AllSubscribeableApisTestData, ApiDefinitionTestData}
import uk.gov.hmrc.gatekeeper.pages.{ApplicationsPage, DashboardPage}
import uk.gov.hmrc.gatekeeper.common.{BaseSpec, SignInSugar, WebPage}
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.GivenWhenThen
import play.api.http.Status._
import play.api.libs.json.Json

import scala.io.Source
import uk.gov.hmrc.gatekeeper.connectors.DeveloperConnector.{FindUserIdRequest, FindUserIdResponse}
import uk.gov.hmrc.gatekeeper.models.RegisteredUser
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.apiplatform.modules.events.connectors.ApiPlatformEventsConnector
import uk.gov.hmrc.apiplatform.modules.events.connectors.DisplayEvent
import uk.gov.hmrc.apiplatform.modules.events.connectors.QueryableValues

class ApiGatekeeperBaseSpec
    extends BaseSpec
    with SignInSugar
    with Matchers
    with CustomMatchers
    with GivenWhenThen
    with AllSubscribeableApisTestData
    with ApiDefinitionTestData
    with UrlEncoding {

  // Stub call to APM
  def stubNewApplication(application: String, appId: ApplicationId) = {
    stubFor(get(urlEqualTo(s"/applications/${appId.value.toString()}")).willReturn(aResponse().withBody(application).withStatus(OK)))
  }

  def stubApplicationForActionRefiner(applicationWithHistory: String, appId: ApplicationId) = {
    stubFor(get(urlEqualTo(s"/gatekeeper/application/${appId.value.toString()}")).willReturn(aResponse().withBody(applicationWithHistory).withStatus(OK)))
  }

  def stubStateHistory(stateHistory: String, appId: ApplicationId) = {
    stubFor(get(urlEqualTo(s"/gatekeeper/application/${appId.value.toString()}/stateHistory")).willReturn(aResponse().withBody(stateHistory).withStatus(OK)))
  }

  def stubApiDefintionsForApplication(apiDefinitions: String, appId: ApplicationId) = {
    stubFor(get(urlEqualTo(s"/api-definitions?applicationId=${appId.value.toString()}&restricted=false")).willReturn(aResponse().withBody(apiDefinitions).withStatus(OK)))
  }

  def stubDevelopers(developers: List[RegisteredUser]) = {
    stubFor(get(urlMatching(s"/developers")).willReturn(aResponse().withBody(Json.toJson(developers).toString())))
    stubFor(post(urlMatching(s"/developers/get-by-emails")).willReturn(aResponse().withBody(Json.toJson(developers).toString())))
  }

  def stubEvents(applicationId: ApplicationId, events: List[DisplayEvent]) = {
    val tags = Set("TEAM_MEMBER", "SUBSCIPTION")
    val queryResponse = Json.stringify(Json.toJson(QueryableValues(tags.toList)))
    stubFor(
      get(urlMatching(s"/application-event/${applicationId.value.toString}/values"))
      .willReturn(aResponse().withBody(queryResponse).withStatus(OK))
    )
    
    val eventResponse = Json.stringify(Json.toJson(ApiPlatformEventsConnector.QueryResponse(events)))
    stubFor(
      get(urlMatching(s"/application-event/${applicationId.value.toString}"))
      .willReturn(aResponse().withBody(eventResponse).withStatus(OK))
    )
  }

  def stubFilteredEvents(applicationId: ApplicationId, tag: String, events: List[DisplayEvent]) = {
    val eventResponse = Json.stringify(Json.toJson(ApiPlatformEventsConnector.QueryResponse(events)))
    stubFor(
      get(urlPathEqualTo(s"/application-event/${applicationId.value.toString}"))
      .withQueryParam("eventTag", equalTo(tag.toString()))
      .willReturn(
        aResponse().withBody(eventResponse).withStatus(OK)
      )
    )
  }

  def stubSubmissionLatestIsNotCompleted(appId: ApplicationId) = {
    stubFor(get(urlPathMatching(s"/submissions/latestiscompleted/.*")).willReturn(aResponse().withBody("false").withStatus(OK)))
  }

  def stubSubmissionLatestIsNotFound(appId: ApplicationId) = {
    stubFor(get(urlPathMatching(s"/submissions/latestiscompleted/.*")).willReturn(aResponse().withStatus(NOT_FOUND)))
  }

  def stubApplication(application: String, developers: List[RegisteredUser], stateHistory: String, appId: ApplicationId, events: List[DisplayEvent] = Nil) = {
    stubNewApplication(application, appId)
    stubStateHistory(stateHistory, appId)
    stubApiDefintionsForApplication(allSubscribeableApis, appId)
    stubDevelopers(developers)
    
    stubGetDeveloper(developers.head.email, Json.stringify(Json.toJson(developers.head)))
    stubSubmissionLatestIsNotFound(appId)
  }

  def stubApplicationsList() = {
    Source.fromURL(getClass.getResource("/applications.json")).mkString.replaceAll("\n","")
  }

  def stubPaginatedApplicationList() = {
    val paginatedApplications = Source.fromURL(getClass.getResource("/paginated-applications.json")).mkString.replaceAll("\n", "")
    
    stubFor(get(urlMatching("/applications\\?page.*")).willReturn(aResponse().withBody(paginatedApplications).withStatus(OK)))
  }

  protected def stubGetDeveloper(email: LaxEmailAddress, userJsonText: String, userId: UserId = UserId.random) = {
    implicit val format = Json.writes[FindUserIdResponse]
    val responseJson = Json.stringify(Json.toJson(FindUserIdResponse(userId)))

    stubFor(post(urlEqualTo("/developers/find-user-id"))
      .willReturn(aResponse().withStatus(OK).withBody(responseJson)))

    stubFor(
      get(urlPathEqualTo("/developer"))
      .withQueryParam("developerId", equalTo(encode(userId.value.toString)))
      .willReturn(
        aResponse().withStatus(OK).withBody(userJsonText)
      )
    )

  }
  
  def stubApiDefinition() = {
    stubFor(get(urlEqualTo("/api-definition")).willReturn(aResponse().withStatus(OK).withBody(apiDefinition)))
    stubFor(get(urlEqualTo("/api-definition?type=private")).willReturn(aResponse().withStatus(OK).withBody(apiDefinition)))
  }

  def navigateToApplicationPageAsAdminFor(applicationName: String, page: WebPage, developers: List[RegisteredUser]) = {
    Given("I have successfully logged in to the API Gatekeeper")
    stubPaginatedApplicationList()

    stubApiDefinition()

    signInAdminUserGatekeeper(app)
    on(ApplicationsPage)

    When("I select to navigate to the Applications page")
    DashboardPage.selectApplications()

    Then("I am successfully navigated to the Applications page where I can view all applications")
    on(ApplicationsPage)

    When(s"I select to navigate to the application named $applicationName")
    ApplicationsPage.selectByApplicationName(applicationName)

    Then(s"I am successfully navigated to the application named $applicationName")
    on(page)
  }
}
