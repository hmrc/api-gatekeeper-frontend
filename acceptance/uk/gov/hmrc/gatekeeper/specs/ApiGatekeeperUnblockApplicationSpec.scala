
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

package uk.gov.hmrc.gatekeeper.specs

import uk.gov.hmrc.gatekeeper.pages._
import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.gatekeeper.models.RegisteredUser
import play.api.http.Status._
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.gatekeeper.common.WebPage
import uk.gov.hmrc.gatekeeper.testdata.{ApplicationResponseTestData, ApplicationWithStateHistoryTestData, ApplicationWithSubscriptionDataTestData, StateHistoryTestData}


class ApiGatekeeperUnblockApplicationSpec extends ApiGatekeeperBaseSpec with ApplicationWithSubscriptionDataTestData with StateHistoryTestData with ApplicationResponseTestData with ApplicationWithStateHistoryTestData {

  val developers = List[RegisteredUser](new RegisteredUser("joe.bloggs@example.co.uk", UserId.random, "joe", "bloggs", false))

  Feature("Unblock an application") {
    Scenario("I can unblock an application") {
      stubApplication(blockedApplicationWithSubscriptionData.toJsonString, developers, stateHistories.withApplicationId(blockedApplicationId).toJsonString, blockedApplicationId)
      stubApplicationForUnblockSuccess()

      When("I navigate to the application page")
      navigateToApplicationPageAsAdminFor(blockedApplicationName, BlockedApplicationPage, developers)

      And("I choose to unblock the application")
      selectToUnblockApplication()

      Then("I am successfully navigated to the Unblock Application Success page")
      on(UnblockApplicationSuccessPage)
    }

    Scenario("I cannot unblock an application that is already unblocked") {

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
    stubFor(post(urlEqualTo(s"/application/${blockedApplicationId.value.toString()}/unblock")).willReturn(aResponse().withStatus(OK)))
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
    ApplicationsPage.selectByApplicationName(appName)

    Then(s"I am successfully navigated to the application named $appName")
    on(page)
  }

  def stubBlockedApplication() {
    stubFor(get(urlEqualTo(s"/gatekeeper/application/${blockedApplicationId.value.toString()}")).willReturn(aResponse().withBody(blockedApplicationWithHistory.toJsonString).withStatus(OK)))
  }
}
