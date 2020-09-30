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

import acceptance.pages._
import com.github.tomakehurst.wiremock.client.WireMock._
import model.User
import play.api.http.Status._
import acceptance.WebPage
import acceptance.mocks.{ApplicationWithSubscriptionDataMock, ApplicationResponseMock}
import acceptance.mocks.{StateHistoryMock, ApplicationWithHistoryMock}
import model.ApplicationId

class ApiGatekeeperBlockApplicationSpec extends ApiGatekeeperBaseSpec with ApplicationResponseMock with ApplicationWithSubscriptionDataMock with StateHistoryMock with ApplicationWithHistoryMock {

  val developers = List[User]{new User("joe.bloggs@example.co.uk", "joe", "bloggs", None, None, false)}

  feature("Block an application") {
    scenario("I can block an application") {
      stubApplication(newApplicationWithSubscriptionData.toJsonString, developers, stateHistories.withApplicationId(ApplicationId(newApplicationWithSubscriptionDataId)).toJsonString, newApplicationWithSubscriptionDataId)
      stubApplicationForBlockSuccess()

      When("I navigate to the application page")
      navigateToApplicationPageAsAdminFor(newApplicationName, ApplicationPage)

      And("I choose to block the application")
      selectToBlockApplication()

      Then("I am successfully navigated to the Block Application Success page")
      on(BlockApplicationSuccessPage)
      assert(BlockApplicationSuccessPage.bodyText.contains("Application blocked"))
    }

    scenario("I cannot block an application that is already blocked") {
      stubApplication(newBlockedApplicationWithSubscriptionData.toJsonString, developers, stateHistories.withApplicationId(ApplicationId(newBlockedApplicationWithSubscriptionDataId)).toJsonString, newBlockedApplicationWithSubscriptionDataId)

      When("I navigate to the application page")
      navigateToApplicationPageAsAdminFor(newBlockedApplicationName, BlockedApplicationPage)

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
    BlockApplicationPage.completeForm(newApplicationName)

    And("I select the Block Application Button")
    BlockApplicationPage.selectBlockButton()
  }

  def stubApplicationForBlockSuccess() = {
    stubFor(post(urlEqualTo(s"/application/$newApplicationWithSubscriptionDataId/block")).willReturn(aResponse().withStatus(OK)))
  }

  def stubUnblockedApplication() {
    stubFor(get(urlEqualTo(s"/gatekeeper/application/$newApplicationWithSubscriptionDataId")).willReturn(aResponse().withBody(applicationResponseForNewApplication).withStatus(OK)))
  }

  def navigateToApplicationPageAsAdminFor(appName: String, page: WebPage) = {
    Given("I have successfully logged in to the API Gatekeeper")
    stubApplicationList()

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
