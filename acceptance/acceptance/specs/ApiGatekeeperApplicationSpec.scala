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

import acceptance.pages.{ApplicationsPage, DeveloperDetailsPage}
import com.github.tomakehurst.wiremock.client.WireMock._
import model.UserId
import org.openqa.selenium.By
import org.scalatest.Tag
import play.api.http.Status._
import acceptance.pages.ApplicationPage
import acceptance.testdata.{StateHistoryTestData, ApplicationWithSubscriptionDataTestData, ApplicationResponseTestData}
import model.RegisteredUser
import model.UserId

class ApiGatekeeperApplicationSpec extends ApiGatekeeperBaseSpec with StateHistoryTestData with ApplicationWithSubscriptionDataTestData with ApplicationResponseTestData {

  val developers = List[RegisteredUser](RegisteredUser("joe.bloggs@example.co.uk", UserId.random, "joe", "bloggs", false))

  feature("Application List for Search Functionality") {
    info("AS A Product Owner")
    info("I WANT The SDST (Software Developer Support Team) to be able to search for applications")
    info("SO THAT The SDST can review the status of the applications")

    scenario("Ensure a user can view a list of Applications", Tag("NonSandboxTest")) {
      Given("I have successfully logged in to the API Gatekeeper")
      stubPaginatedApplicationList()
      stubApiDefinition()
      signInGatekeeper()
      Then("I am successfully navigated to the Applications page where I can view all developer list details by default")
      on(ApplicationsPage)
    }
  }

  feature("Show applications information") {
    scenario("View a specific application") {
      Given("I have successfully logged in to the API Gatekeeper")
      stubPaginatedApplicationList()
      stubApiDefinition()
      signInGatekeeper()

      on(ApplicationsPage)
      stubApplication(applicationWithSubscriptionData.toJsonString, developers, stateHistories.toJsonString, applicationId)

      When("I select to navigate to the Automated Test Application page")
      ApplicationsPage.selectByApplicationName("My new app")

      Then("I am successfully navigated to the Automated Test Application page")
      on(ApplicationPage)
      verifyText("data-environment", "Production")
      verifyText("data-app-id", applicationId)
      verifyText("data-status", "Active")
      verifyText("data-rate-limit", "Bronze")
      verifyText("data-description-private", applicationDescription)
      verifyText("data-description-public", "")
      webDriver.findElement(By.cssSelector("td[data-privacy-url=''] > a")).getText shouldBe "http://localhost:22222/privacy"
      webDriver.findElement(By.cssSelector("td[data-terms-url=''] > a")).getText shouldBe "http://localhost:22222/terms"
      verifyText("data-access-type", "Standard")
      verifyText("data-subscriptions", "API Simulator 1.0 (Stable)\nHello World 1.0 (Stable)")
      verifyText("data-collaborator-email", "admin@example.com", 0)
      verifyText("data-collaborator-role", "Admin", 0)
      verifyText("data-collaborator-email", "purnima.fakename@example.com", 1)
      verifyText("data-collaborator-role", "Developer", 1)
      verifyText("data-collaborator-email", "Dixie.fakename@example.com", 2)
      verifyText("data-collaborator-role", "Developer", 2)
      verifyText("data-submitted-on", "22 August 2019")
      verifyText("data-submitted-by-email", "admin@example.com")
      verifyText("data-submission-contact-name", "Holly Golightly")
      verifyText("data-submission-contact-email", "holly.golightly@example.com")
      verifyText("data-submission-contact-telephone", "020 1122 3344")
      verifyText("data-checked-on", "22 July 2020")
      verifyText("data-checked-by", "gatekeeper.username")

      And("I can see the Copy buttons")
      verifyText("data-clip-text", "Copy all team member email addresses", 0)
      verifyText("data-clip-text", "Copy admin email addresses", 1)
    }
  }

  feature("Show an applications developer information") {
    scenario("View a specific developer on an application") {
      Given("I have successfully logged in to the API Gatekeeper")
      stubPaginatedApplicationList()
      stubApiDefinition()
      signInGatekeeper()

      on(ApplicationsPage)
      stubApplication(applicationWithSubscriptionData.toJsonString, developers, stateHistories.toJsonString, applicationId)

      When("I select to navigate to the Automated Test Application page")
      ApplicationsPage.selectByApplicationName("My new app")

      Then("I am successfully navigated to the Automated Test Application page")
      on(ApplicationPage)

      stubDeveloper()
      stubApplicationForDeveloper(unverifiedUser.userId)

      When("I select to navigate to a collaborator")
      ApplicationsPage.selectDeveloperByEmail(unverifiedUser.email)

      Then("I am successfully navigated to the developer details page")
      on(DeveloperDetailsPage)
    }
  }

  def stubDeveloper() = {
    stubFor(
      get(urlPathEqualTo("/developer"))
      .willReturn(
        aResponse().withStatus(OK).withBody(unverifiedUserJson)
      )
    )
  }

  def stubApplicationForDeveloper(userId: UserId) = {
    stubFor(
      get(urlPathEqualTo(s"/developer/${userId.asText}/applications"))
      .willReturn(aResponse().withBody(defaultApplicationResponse.toSeq.toJsonString).withStatus(OK)))
  }
}
