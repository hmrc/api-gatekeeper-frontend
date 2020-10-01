/*
 * Copyright 2020 HM Revenue & Customs
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

package acceptance.specs


import acceptance.matchers.CustomMatchers
import acceptance.testdata.{AllSubscribeableApisTestData, ApiDefinitionTestData}
import acceptance.pages.{ApplicationsPage, DashboardPage}
import acceptance.{BaseSpec, SignInSugar, WebPage}
import com.github.tomakehurst.wiremock.client.WireMock._
import model.User
import org.scalatest.{GivenWhenThen, Matchers}
import play.api.http.Status._
import play.api.libs.json.Json

import scala.io.Source

class ApiGatekeeperBaseSpec extends BaseSpec with SignInSugar with Matchers with CustomMatchers with GivenWhenThen with AllSubscribeableApisTestData with ApiDefinitionTestData {
  def stubNewApplication(application: String, appId: String) = {
    stubFor(get(urlEqualTo(s"/applications/$appId")).willReturn(aResponse().withBody(application).withStatus(OK)))
  }

  def stubStateHistory(stateHistory: String, appId: String) = {
    stubFor(get(urlEqualTo(s"/gatekeeper/application/$appId/stateHistory")).willReturn(aResponse().withBody(stateHistory).withStatus(OK)))
  }

  def stubApiDefintionsForApplication(apiDefinitions: String, appId: String) = {
    stubFor(get(urlEqualTo(s"/api-definitions?applicationId=$appId")).willReturn(aResponse().withBody(apiDefinitions).withStatus(OK)))
  }

  def stubDevelopers(developers: List[User]) = {
    stubFor(get(urlMatching(s"/developers")).willReturn(aResponse().withBody(Json.toJson(developers).toString())))
    stubFor(post(urlMatching(s"/developers/get-by-emails")).willReturn(aResponse().withBody(Json.toJson(developers).toString())))
  }

  def stubApplication(application: String, developers: List[User], stateHistory: String, appId: String) = {
    stubNewApplication(application, appId)
    stubStateHistory(stateHistory, appId)
    stubApiDefintionsForApplication(allSubscribeableApis, appId)
    stubDevelopers(developers)
  }

  def stubApplicationList() = {
    val paginatedApplications = Source.fromURL(getClass.getResource("/paginated-applications.json")).mkString.replaceAll("\n", "")
    stubFor(get(urlMatching("/applications\\?page.*")).willReturn(aResponse().withBody(paginatedApplications).withStatus(OK)))
  }

  def stubApiDefinition() = {
    stubFor(get(urlEqualTo("/api-definition")).willReturn(aResponse().withStatus(OK).withBody(apiDefinition)))
    stubFor(get(urlEqualTo("/api-definition?type=private")).willReturn(aResponse().withStatus(OK).withBody(apiDefinition)))
  }

  def navigateToApplicationPageAsAdminFor(applicationName: String, page: WebPage, developers: List[User]) = {
    Given("I have successfully logged in to the API Gatekeeper")
    stubApplicationList()

    stubApiDefinition()

    signInAdminUserGatekeeper
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
