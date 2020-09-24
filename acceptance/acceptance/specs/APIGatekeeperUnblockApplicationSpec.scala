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

class APIGatekeeperUnblockApplicationSpec extends APIGatekeeperBaseSpec with NewBlockedApplicationTestData with NewApplicationTestData {

  val developers = List[User]{new User("joe.bloggs@example.co.uk", "joe", "bloggs", None, None, false)}

  // val unblockedAppName = "Automated Test Application"

  feature("Unblock an application") {
    scenario("I can unblock an application") {
      stubApplication(newBlockedApplicationWithSubscriptionData, developers, newBlockedApplicationStateHistory, newBlockedApplicationWithSubscriptionDataId)
      stubApplicationForUnblockSuccess()

      When("I navigate to the application page")
      navigateToApplicationPageAsAdminFor(newBlockedApplicationName, NewBlockedApplicationPage, developers)

      And("I choose to unblock the application")
      selectToUnblockApplication()

      Then("I am successfully navigated to the Unblock Application Success page")
      on(NewUnblockApplicationSuccessPage)
    }

    scenario("I cannot unblock an application that is already unblocked") {

      When("I navigate to the application page")
      navigateToApplicationPageAsAdminFor(newApplicationName, NewApplicationPage)

      Then("I cannot see the unblock button")
      NewApplicationPage.bodyText.contains("Unblock application") shouldBe false
    }
  }

  def selectToUnblockApplication() = {
    stubBlockedApplication()
    When("I select the Unblock Application Button")
    NewBlockedApplicationPage.selectUnblockApplication()

    Then("I am successfully navigated to the Unblock Application page")
    on(NewUnblockApplicationPage)

    When("I fill out the Unblock Application Form correctly")
    NewUnblockApplicationPage.completeForm(newBlockedApplicationName)

    And("I select the Unblock Application Button")
    NewUnblockApplicationPage.selectUnblockButton()
  }

  def stubApplicationForUnblockSuccess() = {
    stubFor(post(urlEqualTo(s"/application/$newBlockedApplicationWithSubscriptionDataId/unblock")).willReturn(aResponse().withStatus(OK)))
  }

    def navigateToApplicationPageAsAdminFor(appName: String, page: WebPage) = {
    Given("I have successfully logged in to the API Gatekeeper")
    stubApplicationList()

    stubApplicationSubscription(developers)
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
    stubFor(get(urlEqualTo(s"/gatekeeper/application/$newBlockedApplicationWithSubscriptionDataId")).willReturn(aResponse().withBody(blockedApplicationResponseForNewApplication).withStatus(OK)))
  }
}
