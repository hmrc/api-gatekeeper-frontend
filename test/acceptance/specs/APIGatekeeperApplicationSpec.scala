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

import acceptance.pages.{ApplicationPage, ApplicationsPage, DashboardPage}
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
      stubApplicationList
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
      on(ApplicationsPage)
    }
  }

  feature("Show applications information") {
    scenario("View a specific application") {
      Given("I have successfully logged in to the API Gatekeeper")
      stubApplicationList

      val applicationsList = Source.fromURL(getClass.getResource("/applications.json")).mkString.replaceAll("\n","")

      stubFor(get(urlEqualTo("/application")).willReturn(aResponse().withBody(applicationsList).withStatus(200)))

      stubApplicationSubscription
      stubApiDefinition

      signInGatekeeper
      on(DashboardPage)

      When("I select to navigate to the Applications page")
      DashboardPage.selectApplications()

      Then("I am successfully navigated to the Applications page where I can view all applications")
      on(ApplicationsPage)

      stubApplication

      When("I select to navigate to the Automated Test Application page")
      ApplicationsPage.selectByApplicationName("Automated Test Application")

      Then("I am successfully navigated to the Automated Test Application page")
      on(ApplicationPage)

      webDriver.findElement(By.cssSelector("td[data-privacy-url=''] > a")).getText shouldBe "http://localhost:22222/privacy"
      webDriver.findElement(By.cssSelector("td[data-terms-url=''] > a")).getText shouldBe "http://localhost:22222/terms"
    }
  }

  def stubApplicationList = {
    stubFor(get(urlEqualTo("/gatekeeper/applications")).willReturn(aResponse().withBody(approvedApplications).withStatus(200)))
    stubFor(get(urlEqualTo("/application")).willReturn(aResponse().withBody(applications).withStatus(200)))
  }

  def stubApplication = {
    stubFor(get(urlEqualTo("/gatekeeper/application/fa38d130-7c8e-47d8-abc0-0374c7f73216")).willReturn(aResponse().withBody(application).withStatus(200)))
    stubFor(get(urlEqualTo("/application/fa38d130-7c8e-47d8-abc0-0374c7f73216")).willReturn(aResponse().withBody(application).withStatus(200)))
    stubFor(get(urlEqualTo("/gatekeeper/application/fa38d130-7c8e-47d8-abc0-0374c7f73216/subscription")).willReturn(aResponse().withBody("[]").withStatus(200)))
    stubFor(get(urlEqualTo("/application/fa38d130-7c8e-47d8-abc0-0374c7f73216/subscription")).willReturn(aResponse().withBody("[]").withStatus(200)))
  }

  def stubApplicationListWithNoSubs = {
    stubFor(get(urlEqualTo("/gatekeeper/applications")).willReturn(aResponse().withBody(approvedApplications).withStatus(200)))
    stubFor(get(urlEqualTo("/application")).willReturn(aResponse().withBody(applicationWithNoSubscription).withStatus(200)))
  }

  def stubApiDefinition = {
    stubFor(get(urlEqualTo("/api-definition")).willReturn(aResponse().withStatus(200).withBody(apiDefinition)))
    stubFor(get(urlEqualTo("/api-definition?type=private")).willReturn(aResponse().withStatus(200).withBody(apiDefinition)))
  }

  def stubApplicationSubscription = {
    stubFor(get(urlEqualTo("/application/subscriptions")).willReturn(aResponse().withBody(applicationSubscription).withStatus(200)))
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
