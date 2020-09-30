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
import acceptance.mocks.{ApplicationWithSubscriptionDataMock, StateHistoryMock, ApplicationWithHistoryMock}

class ApiGatekeeperDeleteApplicationSpec extends ApiGatekeeperBaseSpec with ApplicationWithSubscriptionDataMock with StateHistoryMock with ApplicationWithHistoryMock {

  val developers = List[User]{new User("joe.bloggs@example.co.uk", "joe", "bloggs", None, None, false)}

  feature("Delete an application") {
    scenario("I can delete an application") {

      stubApplicationForDeleteSuccess()

      When("I navigate to the Delete Page for an application")
      navigateThroughDeleteApplication()

      Then("I am successfully navigated to the Delete Application Success page")
      on(DeleteApplicationSuccessPage)
      assert(DeleteApplicationSuccessPage.bodyText.contains("Application deleted"))
    }

    scenario("I cannot delete an application") {

      stubApplicationForDeleteFailure()

      When("I navigate to the Delete Page for an application")
      navigateThroughDeleteApplication()

      Then("I am successfully navigated to the Delete Application technical difficulties page")
      on(DeleteApplicationSuccessPage)
      assert(DeleteApplicationSuccessPage.bodyText.contains("Technical difficulties"))
    }
  }

  def navigateThroughDeleteApplication() = {
    Given("I have successfully logged in to the API Gatekeeper")
    stubApplicationList()

    stubApplicationSubscription(List( ))
    stubApiDefinition()

    signInSuperUserGatekeeper
    on(ApplicationsPage)
    
    When("I select to navigate to the Applications page")
    DashboardPage.selectApplications()

    Then("I am successfully navigated to the Applications page where I can view all applications")
    on(ApplicationsPage)

    stubApplication(newApplicationWithSubscriptionData.toJsonString, developers, stateHistories.toJsonString, newApplicationWithSubscriptionDataId)

    When("I select to navigate to the Automated Test Application page")
    ApplicationsPage.selectByApplicationName(newApplicationName)

    Then("I am successfully navigated to the Automated Test Application page")
    on(ApplicationPage)

    stubApplicationToDelete()

    When("I select the Delete Application Button")
    ApplicationPage.selectDeleteApplication()

    Then("I am successfully navigated to the Delete Application page")
    on(DeleteApplicationPage)

    stubApplicationToDelete()

    When("I fill out the Delete Application Form correctly")
    DeleteApplicationPage.completeForm(newApplicationName)

    And("I select the Delete Application Button")
    DeleteApplicationPage.selectDeleteButton()
  }

  def stubApplicationToDelete() = {
    stubFor(get(urlEqualTo(s"/gatekeeper/application/$newApplicationWithSubscriptionDataId")).willReturn(aResponse().withBody(applicationResponseForNewApplication).withStatus(OK)))
  }

  def stubApplicationForDeleteSuccess() = {
    stubFor(post(urlEqualTo(s"/application/$newApplicationWithSubscriptionDataId/delete")).willReturn(aResponse().withStatus(NO_CONTENT)))
  }

  def stubApplicationForDeleteFailure() = {
    stubFor(post(urlEqualTo(s"/application/$newApplicationWithSubscriptionDataId/delete")).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR)))
  }
}
