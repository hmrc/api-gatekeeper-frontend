/*
 * Copyright 2017 HM Revenue & Customs
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

import acceptance.pages.ApplicationPage.APIFilter._
import acceptance.pages.ApplicationPage.StatusFilter._
import acceptance.pages.{ApplicationPage, DashboardPage}
import acceptance.{BaseSpec, SignInSugar}
import component.matchers.CustomMatchers
import org.openqa.selenium.By
import org.scalatest.{GivenWhenThen, Matchers, Tag}

class APIGatekeeperApplicationSpec extends BaseSpec with SignInSugar with Matchers with CustomMatchers with GivenWhenThen {

  feature("Application List for Search Functionality") {

    info("AS A Product Owner")
    info("I WANT The SDST (Software Developer Support Team) to be able to search for applications")
    info("SO THAT The SDST can review the status of the applications")

    scenario("Ensure a user can view a list of Applications", Tag("NonSandboxTest")) {
      Given("I have successfully logged in to the API Gatekeeper")
      signInGatekeeper
      on(DashboardPage)
      When("I select to navigate to the Applications page")
      DashboardPage.selectApplications()
      Then("I am successfully navigated to the Applications page where I can view all developer list details by default")
      on(ApplicationPage)
    }

    scenario("Ensure a user can view ALL applications", Tag("NonSandboxTest")) {
      Given("I have successfully logged in to the API Gatekeeper")
      signInGatekeeper
      on(DashboardPage)

      When("I select to navigate to the Applications page")
      DashboardPage.selectApplications()
      on(ApplicationPage)

      When("I select approved from the status filter drop down")
      ApplicationPage.selectByStatus(APPROVED)

      Then("I can only see Approved apps")
      webDriver.findElement(By.cssSelector("tbody")).getText should contain noneOf("Sandbox", "Pending")
      webDriver.findElement(By.cssSelector("tbody")).getText should include("Approved")

      When("I select pending from the status filter drop down")
      ApplicationPage.selectByStatus(PENDING)

      Then("I can only see Pending apps")
      webDriver.findElement(By.cssSelector("tbody")).getText should contain noneOf("Approved", "Sandbox")
      webDriver.findElement(By.cssSelector("tbody")).getText should include("Pending")

      When("I select sandbox from the status filter drop down")
      ApplicationPage.selectByStatus(SANDBOX)

      Then("I can only see Sandbox apps")
      webDriver.findElement(By.cssSelector("tbody")).getText should contain noneOf("Approved", "Pending")
      webDriver.findElement(By.cssSelector("tbody")).getText should include("Sandbox")
    }

    scenario("Ensure a user can view all applications who are subscribed to one or more API", Tag("NonSandboxTest")) {
      Given("I have successfully logged in to the API Gatekeeper")
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectApplications()
      on(ApplicationPage)

      When("I select one or more subscriptions from the filter drop down")
      ApplicationPage.selectBySubscription(ONEORMORESUBSCRIPTION)

      When("I select approved from the status filter drop down")
      ApplicationPage.selectByStatus(APPROVED)

      Then("I can only see Approved apps")
      webDriver.findElement(By.cssSelector("tbody")).getText should contain noneOf("Sandbox", "Pending")
      webDriver.findElement(By.cssSelector("tbody")).getText should include("Approved")

      When("I select pending from the status filter drop down")
      ApplicationPage.selectByStatus(PENDING)

      Then("I can only see Pending apps")
      webDriver.findElement(By.cssSelector("tbody")).getText should contain noneOf("Approved", "Sandbox")
      webDriver.findElement(By.cssSelector("tbody")).getText should include("Pending")

      When("I select sandbox from the status filter drop down")
      ApplicationPage.selectByStatus(SANDBOX)

      Then("I can only see Sandbox apps")
      webDriver.findElement(By.cssSelector("tbody")).getText should contain noneOf("Approved", "Pending")
      webDriver.findElement(By.cssSelector("tbody")).getText should include("Sandbox")
    }

    scenario("Ensure a user can view all applications who have no subscription to an API", Tag("NonSandboxTest")) {
      Given("I have successfully logged in to the API Gatekeeper")
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectApplications()
      on(ApplicationPage)

      When("I select no subscription from the filter drop down")
      ApplicationPage.selectBySubscription(NOSUBSCRIPTION)
      ApplicationPage.selectByStatus(ALL)

      When("I select approved from the status filter drop down")
      ApplicationPage.selectByStatus(APPROVED)

      Then("I can only see Approved apps")
      webDriver.findElement(By.cssSelector("tbody")).getText should contain noneOf("Sandbox", "Pending")
      webDriver.findElement(By.cssSelector("tbody")).getText should include("Approved")

      When("I select pending from the status filter drop down")
      ApplicationPage.selectByStatus(PENDING)

      Then("I can only see Pending apps")
      webDriver.findElement(By.cssSelector("tbody")).getText should contain noneOf("Approved", "Sandbox")
      webDriver.findElement(By.cssSelector("tbody")).getText should include("Pending")

      When("I select sandbox from the status filter drop down")
      ApplicationPage.selectByStatus(SANDBOX)

      Then("I can only see Sandbox apps")
      webDriver.findElement(By.cssSelector("tbody")).getText should contain noneOf("Approved", "Pending")
      webDriver.findElement(By.cssSelector("tbody")).getText should include("Sandbox")
    }

    scenario("Ensure a user can view all applications who are subscribed to the Hello world API", Tag("NonSandboxTest")) {
      Given("I have successfully logged in to the API Gatekeeper")
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectApplications()
      on(ApplicationPage)

      When("I select Employers_PAYE API from the filter drop down")
      ApplicationPage.selectBySubscription(HELLOWORLD)
      ApplicationPage.selectByStatus(ALL)

      When("I select approved from the status filter drop down")
      ApplicationPage.selectByStatus(APPROVED)

      Then("I can only see Approved apps")
      webDriver.findElement(By.cssSelector("tbody")).getText should contain noneOf("Sandbox", "Pending")
      webDriver.findElement(By.cssSelector("tbody")).getText should include("Approved")

      When("I select pending from the status filter drop down")
      ApplicationPage.selectByStatus(PENDING)

      Then("I can only see Pending apps")
      webDriver.findElement(By.cssSelector("tbody")).getText should contain noneOf("Approved", "Sandbox")
      webDriver.findElement(By.cssSelector("tbody")).getText should include("Pending")

      When("I select sandbox from the status filter drop down")
      ApplicationPage.selectByStatus(SANDBOX)

      Then("I can only see Sandbox apps")
      webDriver.findElement(By.cssSelector("tbody")).getText should contain noneOf("Approved", "Pending")
      webDriver.findElement(By.cssSelector("tbody")).getText should include("Sandbox")
    }
  }

  private def assertNumberOfApplicationsPerPage(expected: Int) = {
    webDriver.findElements(By.cssSelector("tbody > tr")).size() shouldBe expected
  }

  private def assertApplicationsList(devList: Seq[((String, String, String, String), Int)]) {
    for ((app, index) <- devList) {
      val fn = webDriver.findElement(By.id(s"app-name-$index")).getText shouldBe app._1
      val sn = webDriver.findElement(By.id(s"app-created-$index")).getText shouldBe app._2
      val em = webDriver.findElement(By.id(s"app-subs-$index")).getText shouldBe app._3
      val st = webDriver.findElement(By.id(s"app-status-$index")).getText shouldBe app._4
    }
  }

}
