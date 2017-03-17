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

import acceptance.pages.{ApprovedPage, DashboardPage, ReviewPage}
import acceptance.{BaseSpec, SignInSugar}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.common.Json
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import component.matchers.CustomMatchers
import org.openqa.selenium.By
import org.scalatest.{Matchers, Tag}
import utils.MessClient

class APIGatekeeperDashboardSpec extends BaseSpec with SignInSugar with Matchers with CustomMatchers with MessClient {

  val approvedApp1 = "d11fd0de-0ce7-4990-b33a-63a4c4dd4f2c"
  val approvedApp2 = "48d7a640-cc40-44f3-95c7-f71e0bcc1f8c"
  val approvedApp3 = "fa9ed720-f0e1-4268-8287-e23e03ae11cd"
  val approvedApp4 = "f0e2611e-2f45-4326-8cd2-6eefebec77b7"
    val approvedApp5 =  "58dd6642-08c9-4422-8a84-058e8731d44a"
      val approvedApp6 = "2ac92223-f255-4e59-bc2b-ac8d1ab2fef5"

  feature("View applications pending gatekeeper approval on the dashboard") {

    info("In order to manage uplift application requests")
    info("As a gatekeeper")
    info("I see a list of applications pending approval")

    scenario("I see a list of pending applications in ascending order by submitted date", Tag("NonSandboxTest")) {
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.bodyText should containInOrder(List("Friendly Taxman1", "new app 1"))
      assertPendingApplication(approvedApp1, "Friendly Taxman1 submitted: 09.06.2016 Review")
      assertPendingApplication(approvedApp2, "new app 1 submitted: 09.06.2016 Review")
      webDriver.findElement(By.linkText("Sign out")).click()
    }

    scenario("I see the message There are no pending applications when there are no applications awaiting uplift approval", Tag("NonSandboxTest")) {
      val stubMapping: StubMapping = get(urlEqualTo("/gatekeeper/applications")).atPriority(1)
        .willReturn(aResponse().withBody("[]").withStatus(200)).build()

      val noApplications: String = Json.write(stubMapping)

      val stubNoApplications = postRequest("http://localhost:9999/__admin/mappings", noApplications)

      import play.api.libs.json.Json

      val id = (Json.parse(stubNoApplications.body) \ "id").asOpt[String].get

      signInGatekeeper
      on(DashboardPage)
      assertNoPendingApplications()
      webDriver.findElement(By.linkText("Sign out")).click()

      val deleteStub = deleteRequest(s"http://localhost:9999/__admin/mappings/${id}")
      deleteStub.code shouldBe 200
    }

    scenario("I can click on the Review button to be taken to the review page for an application awaiting uplift approval", Tag("NonSandboxTest")) {
      signInGatekeeper
      on(DashboardPage)
      clickOnLink(s"data-review-$approvedApp1")
      on(ReviewPage(approvedApp1, "Friendly Taxman1"))
      verifyText("data-description", "application description")
      verifyText("data-submitter-name", "temp 999")
      verifyText("data-submitter-email", "temp909@mailinator.com")
      webDriver.findElement(By.linkText("Sign out")).click()
    }
  }

  feature("View approved applications on the dashboard") {

    info("In order to see the state of previously approved applications")
    info("As a gatekeeper")
    info("I see a list of applications which have already been approved")

    scenario("I see a list of approved applications in alphabetical order and their status", Tag("NonSandboxTest")) {
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.bodyText should containInOrder(List("Agents Test", "ala_qa_client", "App To Approve", "Apprenticeship Levy"))
      assertApprovedApplication(approvedApp3, "Agents Test submitted: 04.10.2016 verified")
      assertApprovedApplication(approvedApp4, "ala_qa_client submitted: 20.07.2016 verified")
      assertApprovedApplication(approvedApp5, "App To Approve submitted: 19.05.2016 verified")
      assertApprovedApplication(approvedApp6, "Apprenticeship Levy submitted: 30.09.2016 verified")
      webDriver.findElement(By.linkText("Sign out")).click()
    }

    scenario("I see the message There are no approved applications when there no applications have been approved", Tag("NonSandboxTest")) {
      val stubMapping: StubMapping = get(urlEqualTo("/gatekeeper/applications")).atPriority(1)
        .willReturn(aResponse().withBody("[]").withStatus(200)).build()
      val noApplications: String = Json.write(stubMapping)
      val stubNoApplications = postRequest("http://localhost:9999/__admin/mappings", noApplications)
      println(stubNoApplications)

      import play.api.libs.json.Json

      val id = (Json.parse(stubNoApplications.body) \ "id").asOpt[String].get
      signInGatekeeper
      on(DashboardPage)
      assertNoApprovedApplications()
      webDriver.findElement(By.linkText("Sign out")).click()
      val deleteStub = deleteRequest(s"http://localhost:9999/__admin/mappings/${id}")
      deleteStub.code shouldBe 200
    }

    scenario("I can click on the application name to be taken to the approved application page", Tag("NonSandboxTest")) {
      signInGatekeeper
      on(DashboardPage)
      clickOnLink(s"data-view-$approvedApp3")
      on(ApprovedPage(approvedApp3, "Agents Test"))
      webDriver.findElement(By.linkText("Sign out")).click()
    }
  }

  private def assertPendingApplication(appId: String, expected: String) = {
    webDriver.findElement(By.cssSelector(s"[data-pending-$appId]")).getText.replaceAll("\\s+"," ") shouldBe expected
  }

  private def assertApprovedApplication(appId: String, expected: String) = {
    webDriver.findElement(By.cssSelector(s"[data-approved-$appId]")).getText.replaceAll("\\s+", " ") shouldBe expected
  }

  private def assertNoPendingApplications() = {
    webDriver.findElement(By.cssSelector(s"[data-pending-none]")).getText shouldBe "There are no pending applications."
  }

  private def assertNoApprovedApplications() = {
    webDriver.findElement(By.cssSelector(s"[data-approved-none]")).getText shouldBe "There are no approved applications."
  }
}
