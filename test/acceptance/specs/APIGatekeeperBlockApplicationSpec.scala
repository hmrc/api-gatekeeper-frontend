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
import play.api.http.Status._

class APIGatekeeperBlockApplicationSpec extends APIGatekeeperBaseSpec {

  val appName = "Automated Test Application"

  feature("Block an application") {
    scenario("I can block an application") {
      stubApplication(unblockedApplication)
      stubApplicationForBlockSuccess()

      When("I navigate to the application page")
      navigateToApplicationPageFor(appName, ApplicationPage)

      And("I choose to block the application")
      selectToBlockApplication()

      Then("I am successfully navigated to the Block Application Success page")
      on(BlockApplicationSuccessPage)
      assert(BlockApplicationSuccessPage.bodyText.contains("Application blocked"))
    }

    scenario("I cannot block an application that is already blocked") {
      stubApplication(blockedApplication)

      When("I navigate to the application page")
      navigateToApplicationPageFor(appName, ApplicationPage)

      Then("I cannot see the block button")
      ApplicationPage.bodyText.contains("Block application") shouldBe false
    }
  }

  def selectToBlockApplication() = {
    When("I select the Block Application Button")
    ApplicationPage.selectBlockApplication()

    Then("I am successfully navigated to the Block Application page")
    on(BlockApplicationPage)

    When("I fill out the Block Application Form correctly")
    BlockApplicationPage.completeForm(appName)

    And("I select the Block Application Button")
    BlockApplicationPage.selectBlockButton()
  }

  def stubApplicationForBlockSuccess() = {
    stubFor(post(urlEqualTo("/application/fa38d130-7c8e-47d8-abc0-0374c7f73216/block")).willReturn(aResponse().withStatus(OK)))
  }
}
