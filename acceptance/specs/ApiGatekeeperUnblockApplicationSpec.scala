
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

import pages._
import com.github.tomakehurst.wiremock.client.WireMock._
import model.RegisteredUser
import play.api.http.Status._
import common.WebPage
import model.ApplicationId
import testdata.{ApplicationWithSubscriptionDataTestData, StateHistoryTestData, ApplicationResponseTestData, ApplicationWithHistoryTestData}
import model.UserId

class ApiGatekeeperUnblockApplicationSpec extends ApiGatekeeperBaseSpec with ApplicationWithSubscriptionDataTestData with StateHistoryTestData with ApplicationResponseTestData with ApplicationWithHistoryTestData {

  val developers = List[RegisteredUser](new RegisteredUser("joe.bloggs@example.co.uk", UserId.random, "joe", "bloggs", false))

  feature("Unblock an application") {
    scenario("I can unblock an application") {
      stubApplication(blockedApplicationWithSubscriptionData.toJsonString, developers, stateHistories.withApplicationId(ApplicationId(blockedApplicationId)).toJsonString, blockedApplicationId)
      stubApplicationForUnblockSuccess()

      When("I navigate to the application page")
      navigateToApplicationPageAsAdminFor(blockedApplicationName, BlockedApplicationPage, developers)

      And("I choose to unblock the application")
      selectToUnblockApplication()

      Then("I am successfully navigated to the Unblock Application Success page")
      on(UnblockApplicationSuccessPage)
    }

    scenario("I cannot unblock an application that is already unblocked") {

      When("I navigate to the application page")
      navigateToApplicationPageAsAdminFor(applicationName, ApplicationPage)

      Then("I cannot see the unblock button")
      ApplicationPage.bodyText.contains("Unblock application") shouldBe false
    }
  }

  def selectToUnblockApplication() = {
    stubBlockedApplication()
    When("I select the Unblock Application Button")
    BlockedApplicationPage.selectUnblockApplication()

    Then("I am successfully navigated to the Unblock Application page")
    on(UnblockApplicationPage)

    When("I fill out the Unblock Application Form correctly")
    UnblockApplicationPage.completeForm(blockedApplicationName)

    And("I select the Unblock Application Button")
    UnblockApplicationPage.selectUnblockButton()
  }

  def stubApplicationForUnblockSuccess() = {
    stubFor(post(urlEqualTo(s"/application/$blockedApplicationId/unblock")).willReturn(aResponse().withStatus(OK)))
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

  def stubBlockedApplication() {
    stubFor(get(urlEqualTo(s"/gatekeeper/application/$blockedApplicationId")).willReturn(aResponse().withBody(blockedApplicationWithHistory.toJsonString).withStatus(OK)))
  }
}
