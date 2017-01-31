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

import acceptance.pages.{ApplicationPage, DashboardPage}
import acceptance.{BaseSpec, SignInSugar}
import com.github.tomakehurst.wiremock.client.WireMock._
import component.matchers.CustomMatchers
import org.openqa.selenium.By
import org.scalatest.{GivenWhenThen, Matchers, Tag}
import scala.io.Source

class APIGatekeeperApplicationSpec extends BaseSpec with SignInSugar with Matchers with CustomMatchers with MockDataSugar with GivenWhenThen {

  feature("Application List for Search Functionality") {

    info("AS A Product Owner")
    info("I WANT The SDST (Software Developer Support Team) to be able to search for applications")
    info("SO THAT The SDST can review the status of the applications")

    scenario("Ensure a user can view a list of Applications", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper")
      stubApplicationList()
      val applicationsList = Source.fromURL(getClass.getResource("/applications.json")).mkString.replaceAll("\n","")

      stubFor(get(urlEqualTo(s"/application")).willReturn(aResponse()
        .withBody(applicationsList).withStatus(200)))
      stubApplicationSubscription
      stubApiDefinition

      signInGatekeeper
      on(DashboardPage)
      When("I select to navigate to the Applications page")
      DashboardPage.selectApplications()
      Then("I am successfully navigated to the Applications page where I can view all developer list details by default")
      on(ApplicationPage)
    }

//    scenario("Ensure a user can view ALL applications") {
//
//      Given("I have successfully logged in to the API Gatekeeper")
//      stubApplicationList()
//      stubApplicationSubscription
//      stubApiDefinition
//      signInGatekeeper
//      on(DashboardPage)
//
//      When("I select to navigate to the Applications page")
//      DashboardPage.selectApplications()
//      on(ApplicationPage)
////      Thread.sleep(5000)
//
//      Then("all applications are successfully displayed and sorted correctly")
//      val applications: Seq[(String, String, String,String)] = List(("A Wonderful Application", "08.04.2016","Employers paye","Pending"),
//                                                                    ("An application for my user", "08.06.2016","Inheritance tax","Sandbox"),
//                                                                    ("Any App", "14.04.2016","Employers paye","Approved"),
//                                                                    ("Imrans Application", "24.03.2016", "", "Approved"),
//                                                                    ("Mango", "21.07.2016", "Self assessment api","Pending"),
//                                                                    ("Mark App","31.05.2016","Employers paye, Marriage allowance", "Sandbox"),
//                                                                    ("My new app", "08.04.2016", "Individual benefits","Approved"),
//                                                                    ("Purnimas Application", "24.03.2016", "","Approved"))
//
//      val allApps: Seq[((String, String, String, String), Int)] = applications.zipWithIndex
//      //assertApplicationsList(allApps)
//
//      When("I select approved from the status filter drop down")
//      ApplicationPage.selectByStatus(APPROVED)
//
//      Then("all the approved applications are displayed")
//      val applications2: Seq[(String, String, String,String)]= List(("Any App", "14.04.2016","Employers paye","Approved"),
//                                                                    ("Imrans Application", "24.03.2016", "", "Approved"),
//                                                                    ("My new app", "08.04.2016", "Individual benefits","Approved"),
//                                                                    ("Purnimas Application", "24.03.2016", "","Approved"))
//
//      val approvedApps: Seq[((String, String, String, String), Int)] = applications2.zipWithIndex
//
//      assertApplicationsList(approvedApps)
//
//      When("I select pending from the status filter drop down")
//      ApplicationPage.selectByStatus(PENDING)
//
//      Then("all the pending applications are displayed")
//      val applications3: Seq[(String, String, String,String)] = List(("A Wonderful Application", "08.04.2016","Employers paye","Pending"),
//                                                                     ("Mango", "21.07.2016", "Self assessment api","Pending"))
//
//      val pendingApps: Seq[((String, String, String, String), Int)] = applications3.zipWithIndex
//      assertApplicationsList(pendingApps)
//
//      When("I select sandbox from the status filter drop down")
//      ApplicationPage.selectByStatus(SANDBOX)
//
//      Then("all the sandbox applications are displayed")
//      val applications4 = List(("An application for my user","08.06.2016","","Sandbox"),
//                               ("Mark App","31.05.2016","Employers paye, Marriage allowance", "Sandbox"))
//
//      val sandboxApps = applications4.zipWithIndex
//      assertApplicationsList(sandboxApps)
//    }
//
//    scenario("Ensure a user can view all applications who are subscribed to one or more API") {
//      Given("I have successfully logged in to the API Gatekeeper")
//      stubApplicationList()
//      stubApplicationSubscription
//      stubApiDefinition
//      signInGatekeeper
//      on(DashboardPage)
//      DashboardPage.selectApplications()
//      on(ApplicationPage)
//
//      When("I select one or more subscriptions from the filter drop down")
//      ApplicationPage.selectBySubscription(ONEORMORESUBSCRIPTION)
//      ApplicationPage.selectByStatus(ALL)
//      Thread.sleep(800000)
//
//      Then("all applications who are subscribed to one or more APIs are successfully displayed and sorted correctly")
//      val applications: Seq[(String, String, String,String)] = List(("A Wonderful Application", "08.04.2016","Employers paye","Pending"),
//                                                                    ("An application for my user", "08.06.2016","Inheritance tax","Sandbox"),
//                                                                    ("Any App", "14.04.2016","Employers paye","Approved"),
//                                                                    ("Mango", "21.07.2016", "Self assessment api","Pending"),
//                                                                    ("Mark App","31.05.2016","Employers paye, Marriage allowance", "Sandbox"),
//                                                                    ("My new app", "08.04.2016", "Individual benefits","Approved"))
//
//      val allApps: Seq[((String, String, String, String), Int)] = applications.zipWithIndex
//      assertApplicationsList(allApps)
//
//      When("I select approved from the status filter drop down")
//      ApplicationPage.selectByStatus(APPROVED)
//
//      Then("all the approved applications are displayed")
//      val applications2: Seq[(String, String, String,String)]= List(("Any App", "14.04.2016","Employers paye","Approved"),
//                                                                    ("My new app", "08.04.2016", "Individual benefits","Approved"))
//
//      val approvedApps: Seq[((String, String, String, String), Int)] = applications2.zipWithIndex
//
//       assertApplicationsList(approvedApps)
//
//       When("I select pending from the status filter drop down")
//       ApplicationPage.selectByStatus(PENDING)
//
//       Then("all the pending applications are displayed")
//      val applications3: Seq[(String, String, String,String)] = List(("A Wonderful Application", "08.04.2016","Employers paye","Pending"),
//                                                                     ("Mango", "21.07.2016", "Self assessment api","Pending"))
//
//       val pendingApps: Seq[((String, String, String, String), Int)] = applications3.zipWithIndex
//       assertApplicationsList(pendingApps)
//
//       When("I select sandbox from the status filter drop down")
//       ApplicationPage.selectByStatus(SANDBOX)
//
//       Then("all the sandbox applications are displayed")
//      val applications4 = List(("An application for my user","08.06.2016","","Sandbox"),
//                               ("Mark App","31.05.2016","Employers paye, Marriage allowance", "Sandbox"))
//
//       val sandboxApps = applications4.zipWithIndex
//       assertApplicationsList(sandboxApps)
//    }
//
//    scenario("Ensure a user can view all applications who have no subscription to an API") {
//      Given("I have successfully logged in to the API Gatekeeper")
//      stubApplicationListWithNoSubs
//      stubApplicationSubscription
//      stubApiDefinition
//      signInGatekeeper
//      on(DashboardPage)
//      DashboardPage.selectApplications()
//      on(ApplicationPage)
//
//      When("I select no subscription from the filter drop down")
//      ApplicationPage.selectBySubscription(NOSUBSCRIPTION)
//      ApplicationPage.selectByStatus(ALL)
//
//      Thread.sleep(90000)
//
//      Then("all applications who has no subscription are successfully displayed and sorted correctly")
//      val applications: Seq[(String, String, String,String)] = List(("Imrans Application", "24.03.2016", "", "Approved"),
//                                                                    ("Purnimas Application", "24.03.2016", "","Approved"),
//                                                                    ("QA User App 2", "28.07.2016", "", "Pending"),
//                                                                    ("Test Application", "28.07.2016", "", "Pending"),
//                                                                    ("Tim", "13.04.2016", "", "Sandbox"))
//
//
//      val allApps: Seq[((String, String, String, String), Int)] = applications.zipWithIndex
//      assertApplicationsList(allApps)
//
//      When("I select approved from the status filter drop down")
//      ApplicationPage.selectByStatus(APPROVED)
//
//      Then("all the approved applications are displayed")
//      val applications2: Seq[(String, String, String,String)]= List(("Imrans Application", "24.03.2016", "", "Approved"),
//                                                                    ("Purnimas Application", "24.03.2016", "","Approved"))
//
//      val approvedApps: Seq[((String, String, String, String), Int)] = applications2.zipWithIndex
//
//      assertApplicationsList(approvedApps)
//
//      When("I select pending from the status filter drop down")
//      ApplicationPage.selectByStatus(PENDING)
//
//      Then("all the pending applications are displayed")
//      val applications3: Seq[(String, String, String,String)] = List(("QA User App 2", "28.07.2016", "", "Pending"),
//                                                                     ("Test Application", "28.07.2016", "", "Pending"))
//
//      val pendingApps: Seq[((String, String, String, String), Int)] = applications3.zipWithIndex
//      assertApplicationsList(pendingApps)
//
//      When("I select sandbox from the status filter drop down")
//      ApplicationPage.selectByStatus(SANDBOX)
//
//      Then("all the sandbox applications are displayed")
//      val applications4 = List(("Tim", "13.04.2016", "", "Sandbox"))
//
//      val sandboxApps = applications4.zipWithIndex
//      assertApplicationsList(sandboxApps)
//    }
//
//    scenario("Ensure a user can view all applications who are subscribed to the Employers-PAYE API") {
//      Given("I have successfully logged in to the API Gatekeeper")
//      stubApplicationList()
//      stubApplicationSubscription
//      stubApiDefinition
//      signInGatekeeper
//      on(DashboardPage)
//      DashboardPage.selectApplications()
//      on(ApplicationPage)
//
//      Thread.sleep(90000)
//
//      When("I select Employers_PAYE API from the filter drop down")
//      ApplicationPage.selectBySubscription(EMPLOYERSPAYE)
//      ApplicationPage.selectByStatus(ALL)
//
//      Then("all applications subscribing to the Employers PAYE API are successfully displayed and sorted correctly")
//      val applications: Seq[(String, String, String,String)] = List(("A Wonderful Application", "08.04.2016","Employers paye","Pending"),
//                                                                    ("Any App", "14.04.2016","Employers paye","Approved"),
//                                                                    ("Mark App","31.05.2016","Employers paye, Marriage allowance", "Sandbox"))
//
//
//      val allApps: Seq[((String, String, String, String), Int)] = applications.zipWithIndex
//      assertApplicationsList(allApps)
//
//      When("I select approved from the status filter drop down")
//      ApplicationPage.selectByStatus(APPROVED)
//
//      Then("all the approved applications are displayed")
//      val applications2: Seq[(String, String, String,String)]= List(("Any App", "14.04.2016","Employers paye","Approved"))
//
//      val approvedApps: Seq[((String, String, String, String), Int)] = applications2.zipWithIndex
//
//      assertApplicationsList(approvedApps)
//
//      When("I select pending from the status filter drop down")
//      ApplicationPage.selectByStatus(PENDING)
//
//      Then("all the pending applications are displayed")
//      val applications3: Seq[(String, String, String,String)] = List(("A Wonderful Application", "08.04.2016","Employers paye","Pending"))
//
//      val pendingApps: Seq[((String, String, String, String), Int)] = applications3.zipWithIndex
//      assertApplicationsList(pendingApps)
//
//      When("I select sandbox from the status filter drop down")
//      ApplicationPage.selectByStatus(SANDBOX)
//
//      Then("all the sandbox applications are displayed")
//      val applications4 = List(("Mark App","31.05.2016","Employers paye, Marriage allowance", "Sandbox"))
//
//      val sandboxApps = applications4.zipWithIndex
//      assertApplicationsList(sandboxApps)
//    }

  }

  def stubApplicationList() = {
    stubFor(get(urlEqualTo("/gatekeeper/applications"))
      .willReturn(aResponse().withBody(approvedApplications).withStatus(200)))

    stubFor(get(urlEqualTo(s"/application")).willReturn(aResponse()
      .withBody(applications).withStatus(200)))
  }

  def stubApplicationListWithNoSubs() = {
    stubFor(get(urlEqualTo("/gatekeeper/applications"))
      .willReturn(aResponse().withBody(approvedApplications).withStatus(200)))

    stubFor(get(urlEqualTo(s"/application")).willReturn(aResponse()
      .withBody(applicationWithNoSubscription).withStatus(200)))
  }

  def stubApiDefinition() = {
    stubFor(get(urlEqualTo(s"/api-definition"))
      .willReturn(aResponse().withStatus(200).withBody(apiDefinition)))
  }

  def stubApplicationSubscription() = {
    stubFor(get(urlEqualTo("/application/subscriptions"))
      .willReturn(aResponse().withBody(applicationSubscription).withStatus(200)))
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
