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

import com.github.tomakehurst.wiremock.client.WireMock._

import play.api.http.Status._

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.pages._
import uk.gov.hmrc.gatekeeper.testdata.{ApplicationWithStateHistoryTestData, ApplicationWithSubscriptionDataTestData, StateHistoryTestData}
class ApiGatekeeperDeleteApplicationSpec extends ApiGatekeeperBaseSpec with ApplicationWithSubscriptionDataTestData with StateHistoryTestData with ApplicationWithStateHistoryTestData {

  val developers = List[RegisteredUser](RegisteredUser("joe.bloggs@example.co.uk".toLaxEmail, UserId.random, "joe", "bloggs", false))

  Feature("Delete an application") {
    Scenario("I can delete an application") {

      stubApplicationForDeleteSuccess()

      When("I navigate to the Delete Page for an application")
      navigateThroughDeleteApplication()

      Then("I am successfully navigated to the Delete Application Success page")
      on(DeleteApplicationSuccessPage)
      assert(DeleteApplicationSuccessPage.bodyText.contains("Application deleted"))
    }

    Scenario("I cannot delete an application") {

      stubApplicationForDeleteFailure()

      When("I navigate to the Delete Page for an application")
      navigateThroughDeleteApplication()

      Then("I am successfully navigated to the Delete Application technical difficulties page")
      onTechDifficultiesFor(DeleteApplicationSuccessPage)
      assert(DeleteApplicationSuccessPage.bodyText.contains("Technical difficulties"))
    }
  }

  def navigateThroughDeleteApplication() = {
    Given("I have successfully logged in to the API Gatekeeper")
    stubPaginatedApplicationList()

    stubApiDefinition()

    signInSuperUserGatekeeper(app)
    on(ApplicationsPage)
    
    When("I select to navigate to the Applications page")
    DashboardPage.selectApplications()

    Then("I am successfully navigated to the Applications page where I can view all applications")
    on(ApplicationsPage)

    stubApplication(applicationWithSubscriptionData.toJsonString, developers, stateHistories.toJsonString, applicationId)

    When("I select to navigate to the Automated Test Application page")
    ApplicationsPage.selectByApplicationName(applicationName)

    Then("I am successfully navigated to the Automated Test Application page")
    on(ApplicationPage)

    stubApplicationToDelete()

    When("I select the Delete Application Button")
    ApplicationPage.selectDeleteApplication()

    Then("I am successfully navigated to the Delete Application page")
    on(DeleteApplicationPage)

    stubApplicationToDelete()

    When("I fill out the Delete Application Form correctly")
    DeleteApplicationPage.completeForm(applicationName)

    And("I select the Delete Application Button")
    DeleteApplicationPage.selectDeleteButton()
  }

  def stubApplicationToDelete() = {
    stubFor(get(urlEqualTo(s"/gatekeeper/application/${applicationId.value.toString()}")).willReturn(aResponse().withBody(defaultApplicationWithHistory.toJsonString).withStatus(OK)))
  }

  def stubApplicationForDeleteSuccess() = {
    stubFor(patch(urlEqualTo(s"/application/${applicationId.value.toString()}")).willReturn(aResponse().withStatus(NO_CONTENT)))
  }

  def stubApplicationForDeleteFailure() = {
    stubFor(patch(urlEqualTo(s"/application/${applicationId.value.toString()}")).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR)))
  }
}
