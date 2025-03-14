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

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.gatekeeper.common.WebPage
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.pages._
import uk.gov.hmrc.gatekeeper.testdata.{ApplicationResponseTestData, ApplicationWithStateHistoryTestData, ApplicationWithSubscriptionDataTestData, StateHistoryTestData}

class ApiGatekeeperBlockApplicationSpec extends ApiGatekeeperBaseSpec with ApplicationResponseTestData with ApplicationWithSubscriptionDataTestData with StateHistoryTestData
    with ApplicationWithStateHistoryTestData {

  val developers = List[RegisteredUser](RegisteredUser("joe.bloggs@example.co.uk".toLaxEmail, UserId.random, "joe", "bloggs", false))

  Feature("Block an application") {
    Scenario("I can block an application") {
      stubApplication(applicationWithSubscriptionData.toJsonString, developers, stateHistories.withApplicationId(applicationId).toJsonString, applicationId)
      stubApplicationForBlockSuccess(applicationId, defaultApplication)

      When("I navigate to the application page")
      navigateToApplicationPageAsAdminFor(applicationName.value, ApplicationPage)

      And("I choose to block the application")
      selectToBlockApplication()

      Then("I am successfully navigated to the Block Application Success page")
      on(BlockApplicationSuccessPage)
      assert(BlockApplicationSuccessPage.bodyText.contains("Application blocked"))
    }

    Scenario("I cannot block an application that is already blocked") {
      stubApplication(
        blockedApplicationWithSubscriptionData.toJsonString,
        developers,
        stateHistories.withApplicationId(blockedApplicationId).toJsonString,
        blockedApplicationId
      )

      When("I navigate to the application page")
      navigateToApplicationPageAsAdminFor(blockedApplicationName.value, BlockedApplicationPage)

      Then("I cannot see the block button")
      BlockedApplicationPage.bodyText.contains("Block application") shouldBe false
    }
  }

  def selectToBlockApplication() = {
    When("I click the Block Application Button")
    stubUnblockedApplication()
    ApplicationPage.clickBlockApplication()

    Then("I am successfully navigated to the Block Application page")
    on(BlockApplicationPage)

    When("I fill out the Block Application Form correctly")
    BlockApplicationPage.completeForm(applicationName.value)

    And("I select the Block Application Button")
    BlockApplicationPage.selectBlockButton()
  }

  def navigateToApplicationPageAsAdminFor(appName: String, page: WebPage) = {
    Given("I have successfully logged in to the API Gatekeeper")
    stubPaginatedApplicationList()

    stubApiDefinition()

    signInAdminUserGatekeeper(app)
    on(ApplicationsPage)

    When("I select to navigate to the Applications page")
    DashboardPage.selectApplications()

    Then("I am successfully navigated to the Applications page where I can view all applications")
    on(ApplicationsPage)

    When(s"I select to navigate to the application named $appName")
    ApplicationsPage.clickApplicationNameLink(appName)

    Then(s"I am successfully navigated to the application named $appName")
    on(page)
  }
}
