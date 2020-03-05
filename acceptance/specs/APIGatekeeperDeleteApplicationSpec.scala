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
import model.User
import play.api.http.Status._

import scala.io.Source

class APIGatekeeperDeleteApplicationSpec extends APIGatekeeperBaseSpec {

  val developers = List[User]{new User("joe.bloggs@example.co.uk", "joe", "bloggs", None, None, false)}

  val appName = "Automated Test Application"

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

    val paginatedApplications = Source.fromURL(getClass.getResource("/paginated-applications.json")).mkString.replaceAll("\n", "")

    stubFor(get(urlMatching("/applications.*")).willReturn(aResponse().withBody(paginatedApplications).withStatus(OK)))

    stubApplicationSubscription(List( ))
    stubApiDefinition()

    signInSuperUserGatekeeper
    on(ApplicationsPage)

    When("I select to navigate to the Applications page")
    DashboardPage.selectApplications()

    Then("I am successfully navigated to the Applications page where I can view all applications")
    on(ApplicationsPage)

    stubApplication(applicationToDelete, developers)

    When("I select to navigate to the Automated Test Application page")
    ApplicationsPage.selectByApplicationName(appName)

    Then("I am successfully navigated to the Automated Test Application page")
    on(ApplicationPage)

    When("I select the Delete Application Button")
    ApplicationPage.selectDeleteApplication()

    Then("I am successfully navigated to the Delete Application page")
    on(DeleteApplicationPage)

    When("I fill out the Delete Application Form correctly")
    DeleteApplicationPage.completeForm(appName)

    And("I select the Delete Application Button")
    DeleteApplicationPage.selectDeleteButton()
  }

  def stubApplicationForDeleteSuccess() = {
    stubFor(post(urlEqualTo("/application/fa38d130-7c8e-47d8-abc0-0374c7f73216/delete")).willReturn(aResponse().withStatus(NO_CONTENT)))
  }

  def stubApplicationForDeleteFailure() = {
    stubFor(post(urlEqualTo("/application/fa38d130-7c8e-47d8-abc0-0374c7f73216/delete")).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR)))
  }
}
