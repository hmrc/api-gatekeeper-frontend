/*
 * Copyright 2018 HM Revenue & Customs
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

class APIGatekeeperUnblockApplicationSpec extends APIGatekeeperBaseSpec {

  val developers = List[User]{new User("joe.bloggs@example.co.uk", "joe", "bloggs", None, None, false)}

  val appName = "Automated Test Application - Blocked"
  val unblockedAppName = "Automated Test Application"

  feature("Unblock an application") {
    scenario("I can unblock an application") {
      stubBlockedApplication(blockedApplication, developers)
      stubApplicationForUnblockSuccess()

      When("I navigate to the application page")
      navigateToApplicationPageFor(appName, BlockedApplicationPage, developers)

      And("I choose to unblock the application")
      selectToUnblockApplication()

      Then("I am successfully navigated to the Unblock Application Success page")
      on(UnblockApplicationSuccessPage)
    }

    scenario("I cannot unblock an application that is already unblocked") {
      stubApplication(unblockedApplication, developers)

      When("I navigate to the application page")
      navigateToApplicationPageFor(unblockedAppName, ApplicationPage, developers)

      Then("I cannot see the unblock button")
      ApplicationPage.bodyText.contains("Unblock application") shouldBe false
    }
  }

  def selectToUnblockApplication() = {
    When("I select the Unblock Application Button")
    BlockedApplicationPage.selectUnblockApplication()

    Then("I am successfully navigated to the Unblock Application page")
    on(UnblockApplicationPage)

    When("I fill out the Unblock Application Form correctly")
    UnblockApplicationPage.completeForm(appName)

    And("I select the Unblock Application Button")
    UnblockApplicationPage.selectUnblockButton()
  }

  def stubApplicationForUnblockSuccess() = {
    stubFor(post(urlEqualTo("/application/fa38d130-7c8e-47d8-abc0-0374c7f73217/unblock")).willReturn(aResponse().withStatus(OK)))
  }
}
