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

package specs

import com.github.tomakehurst.wiremock.client.WireMock._
import common.WebPage
import model.{UserId, _}
import pages._
import testdata.{ApplicationResponseTestData, ApplicationWithHistoryTestData, ApplicationWithSubscriptionDataTestData, StateHistoryTestData}

import play.api.http.Status._

class ApiGatekeeperBlockApplicationSpec extends ApiGatekeeperBaseSpec with ApplicationResponseTestData with ApplicationWithSubscriptionDataTestData with StateHistoryTestData with ApplicationWithHistoryTestData {

  val developers = List[RegisteredUser](RegisteredUser("joe.bloggs@example.co.uk", UserId.random, "joe", "bloggs", false))

  feature("Block an application") {
    scenario("I can block an application") {
      stubApplication(applicationWithSubscriptionData.toJsonString, developers, stateHistories.withApplicationId(ApplicationId(applicationId)).toJsonString, applicationId)
      stubApplicationForBlockSuccess()

      When("I navigate to the application page")
      navigateToApplicationPageAsAdminFor(applicationName, ApplicationPage)

      And("I choose to block the application")
      selectToBlockApplication()

      Then("I am successfully navigated to the Block Application Success page")
      on(BlockApplicationSuccessPage)
      assert(BlockApplicationSuccessPage.bodyText.contains("Application blocked"))
    }

    scenario("I cannot block an application that is already blocked") {
      stubApplication(blockedApplicationWithSubscriptionData.toJsonString, developers, stateHistories.withApplicationId(ApplicationId(blockedApplicationId)).toJsonString, blockedApplicationId)

      When("I navigate to the application page")
      navigateToApplicationPageAsAdminFor(blockedApplicationName, BlockedApplicationPage)

      Then("I cannot see the block button")
      BlockedApplicationPage.bodyText.contains("Block application") shouldBe false
    }
  }

  def selectToBlockApplication() = {
    When("I select the Block Application Button")
    stubUnblockedApplication()
    ApplicationPage.selectBlockApplication()

    Then("I am successfully navigated to the Block Application page")
    on(BlockApplicationPage)

    When("I fill out the Block Application Form correctly")
    BlockApplicationPage.completeForm(applicationName)

    And("I select the Block Application Button")
    BlockApplicationPage.selectBlockButton()
  }

  def stubApplicationForBlockSuccess() = {
    stubFor(post(urlEqualTo(s"/application/$applicationId/block")).willReturn(aResponse().withStatus(OK)))
  }

  def stubUnblockedApplication() {
    stubFor(get(urlEqualTo(s"/gatekeeper/application/$applicationId")).willReturn(aResponse().withBody(defaultApplicationWithHistory.toJsonString).withStatus(OK)))
  }

  def navigateToApplicationPageAsAdminFor(appName: String, page: WebPage) = {
    Given("I have successfully logged in to the API Gatekeeper")
    stubPaginatedApplicationList()

    stubApiDefinition()

    signInAdminUserGatekeeper
    on(ApplicationsPage)

    When("I select to navigate to the Applications page")
    DashboardPage.selectApplications()

    Then("I am successfully navigated to the Applications page where I can view all applications")
    on(ApplicationsPage)

    When(s"I select to navigate to the application named $appName")
    ApplicationsPage.selectByApplicationName(appName)

    Then(s"I am successfully navigated to the application named $appName")
    on(page)
  }
}
