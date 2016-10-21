/*
 * Copyright 2016 HM Revenue & Customs
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

import java.net.URLEncoder

import acceptance.pages.{ApprovedPage, DashboardPage, ReviewPage}
import acceptance.{BaseSpec, SignInSugar}
import com.github.tomakehurst.wiremock.client.WireMock._
import component.matchers.CustomMatchers
import org.openqa.selenium.By
import org.scalatest.{GivenWhenThen, Matchers}

class APIGatekeeperDashboardSpec extends BaseSpec with SignInSugar with Matchers with CustomMatchers with MockDataSugar with GivenWhenThen {


  feature("View applications pending gatekeeper approval on the dashboard") {

    info("In order to manage uplift application requests")
    info("As a gatekeeper")
    info("I see a list of applications pending approval")

    scenario("I see a list of pending applications in ascending order by submitted date") {

      Given("An application is created")
      stubFor(get(urlEqualTo("/gatekeeper/applications"))
        .willReturn(aResponse().withBody(applicationsPendingApproval).withStatus(200)))
      When("I sign in to the gatekeeper front end")
      signInGatekeeper
      on(DashboardPage)
      Then("I see a list of pending applications awaiting approval")
      DashboardPage.bodyText should containInOrder(List("First Application", "Second Application"))
      assertPendingApplication(appPendingApprovalId1, "First Application submitted: 22.03.2016 Review")
      assertPendingApplication(appPendingApprovalId2, "Second Application submitted: 24.03.2016 Review")
    }

    scenario("I see the message There are no pending applications when there are no applications awaiting uplift approval") {
      Given("No pending applications are created")
      stubFor(get(urlEqualTo("/gatekeeper/applications"))
        .willReturn(aResponse().withBody("[]").withStatus(200)))
      When("I sign in to the gatekeeper front end")
      signInGatekeeper
      on(DashboardPage)
      Then("I see a no pending applications message")
      assertNoPendingApplications()
    }

    scenario("I can click on the Review button to be taken to the review page for an application awaiting uplift approval") {
      Given("An application awaiting uplift is created")
      stubFor(get(urlEqualTo("/gatekeeper/applications"))
        .willReturn(aResponse().withBody(applicationsPendingApproval).withStatus(200)))

      stubFor(get(urlEqualTo(s"/gatekeeper/application/$appPendingApprovalId1"))
        .willReturn(aResponse().withBody(application).withStatus(200)))

      val encodedEmail = URLEncoder.encode(adminEmail, "UTF-8")

      stubFor(get(urlEqualTo(s"/developer?email=$encodedEmail"))
        .willReturn(aResponse().withBody(administrator()).withStatus(200)))
      When("I sign in to the gatekeeper front end")
      signInGatekeeper
      on(DashboardPage)
      When("I click the review button")
      clickOnLink(s"data-review-$appPendingApprovalId1")
      Then("I am navigated to the review page for applications awaiting uplift approval")
      on(ReviewPage(appPendingApprovalId1, "First Application"))
      verifyText("data-description", applicationDescription)
      verifyText("data-submitter-name", fullName)
      verifyText("data-submitter-email", adminEmail)
    }
  }

  feature("View approved applications on the dashboard") {

    info("In order to see the state of previously approved applications")
    info("As a gatekeeper")
    info("I see a list of applications which have already been approved")

    scenario("I see a list of approved applications in alphabetical order and their status") {
      Given("Given approved applications are created")
      stubFor(get(urlEqualTo("/gatekeeper/applications"))
        .willReturn(aResponse().withBody(approvedApplications).withStatus(200)))
      When("I sign in to the gatekeeper front end")
      signInGatekeeper
      on(DashboardPage)
      Then("I see a list of approved applications on the dashboard")
      DashboardPage.bodyText should containInOrder(List("Application", "BApplication", "RApplication", "ZApplication"))
      assertApprovedApplication(approvedApp1, "Application submitted: 24.03.2016 not yet verified")
      assertApprovedApplication(approvedApp4, "BApplication submitted: 24.03.2016 verified")
      assertApprovedApplication(approvedApp3, "RApplication submitted: 24.03.2016 not yet verified")
      assertApprovedApplication(approvedApp2, "ZApplication submitted: 22.03.2016 verified")
    }

    scenario("I see the message There are no approved applications when there no applications have been approved") {
      Given("Given no approved applications are created")
      stubFor(get(urlEqualTo("/gatekeeper/applications"))
        .willReturn(aResponse().withBody("[]").withStatus(200)))
      When("I sign in to the gatekeeper front end")
      signInGatekeeper
      on(DashboardPage)
      Then("I see a message advising that there are no approved applications")
      assertNoApprovedApplications()
    }

    scenario("I can click on the application name to be taken to the approved application page") {
      val encodedEmail = URLEncoder.encode(adminEmail, "UTF-8")
      val encodedAdminEmails = URLEncoder.encode(s"$adminEmail,$admin2Email", "UTF-8")
      val expectedAdmins = s"""[${administrator()},${administrator(admin2Email, "Admin", "McAdmin")}]""".stripMargin
      Given("Given approved applications are created")
      stubFor(get(urlEqualTo("/gatekeeper/applications"))
        .willReturn(aResponse().withBody(approvedApplications).withStatus(200)))

      stubFor(get(urlEqualTo(s"/gatekeeper/application/$approvedApp1"))
        .willReturn(aResponse().withBody(approvedApplication("application description")).withStatus(200)))

      stubFor(get(urlEqualTo(s"/developer?email=$encodedEmail"))
        .willReturn(aResponse().withBody(administrator()).withStatus(200)))

      stubFor(get(urlEqualTo(s"/developers?emails=$encodedAdminEmails"))
        .willReturn(aResponse().withBody(expectedAdmins).withStatus(200)))
      When("I sign in to the gatekeeper front end")
      signInGatekeeper
      on(DashboardPage)
      When("I click on an application name")
      clickOnLink(s"data-view-$approvedApp1")
      Then("I am navigated to the approved application page")
      on(ApprovedPage(approvedApp1, "Application"))
    }
  }

  private def assertPendingApplication(appId: String, expected: String) = {
    webDriver.findElement(By.cssSelector(s"[data-pending-$appId]")).getText.replaceAll("\n", " ") shouldBe expected
  }

  private def assertApprovedApplication(appId: String, expected: String) = {
    webDriver.findElement(By.cssSelector(s"[data-approved-$appId]")).getText.replaceAll("\n", " ") shouldBe expected
  }

  private def assertNoPendingApplications() = {
    webDriver.findElement(By.cssSelector(s"[data-pending-none]")).getText shouldBe "There are no pending applications."
  }

  private def assertNoApprovedApplications() = {
    webDriver.findElement(By.cssSelector(s"[data-approved-none]")).getText shouldBe "There are no approved applications."
  }
}
