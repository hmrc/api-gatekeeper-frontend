/*
 * Copyright 2018 HM Revenue & Customs
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

import acceptance.pages.{ApplicationPage, ApplicationsPage, DeveloperDetailsPage}
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
      on(ApplicationsPage)


      stubApplication

      When("I select to navigate to the Automated Test Application page")
      ApplicationsPage.selectByApplicationName("Automated Test Application")

      Then("I am successfully navigated to the Automated Test Application page")
      on(ApplicationPage)

      verifyText("data-environment", "Production")
      verifyText("data-app-id", appPendingApprovalId1)
      verifyText("data-status", "Production")
      verifyText("data-rate-limit", "Bronze")
      verifyText("data-description-private", "application description")
      verifyText("data-description-public", "An application that is pending approval")
      webDriver.findElement(By.cssSelector("td[data-privacy-url=''] > a")).getText shouldBe "http://localhost:22222/privacy"
      webDriver.findElement(By.cssSelector("td[data-terms-url=''] > a")).getText shouldBe "http://localhost:22222/terms"
      verifyText("data-access-type", "Standard")
      verifyText("data-subscriptions", "")
      verifyText("data-collaborator-email", "admin@test.com", 0)
      verifyText("data-collaborator-role", "Admin", 0)
      verifyText("data-collaborator-email", "purnima.shanti@mail.com", 1)
      verifyText("data-collaborator-role", "Developer", 1)
      verifyText("data-submitted-on", "05 April 2016")
      verifyText("data-submitted-by-name", "Barry Scott" )
      verifyText("data-submitted-by-email", "barry.scott@example.com" )
      verifyText("data-submission-contact-name", "Harry Golightly")
      verifyText("data-submission-contact-email", "harry.golightly@example.com")
      verifyText("data-submission-contact-telephone", "020 1122 3345")
      verifyText("data-checked-on", "06 April 2016")
      verifyText("data-checked-by", "gatekeeperUserId")
    }
  }

  feature("Show an applications developer information") {
    scenario("View a specific developer on an application") {
      Given("I have successfully logged in to the API Gatekeeper")
      stubApplicationList

      val applicationsList = Source.fromURL(getClass.getResource("/applications.json")).mkString.replaceAll("\n","")

      stubFor(get(urlEqualTo("/application")).willReturn(aResponse().withBody(applicationsList).withStatus(200)))

      stubApplicationSubscription
      stubApiDefinition

      signInGatekeeper
      on(ApplicationsPage)



      stubApplication

      When("I select to navigate to the Automated Test Application page")
      ApplicationsPage.selectByApplicationName("Automated Test Application")

      Then("I am successfully navigated to the Automated Test Application page")
      on(ApplicationPage)

      stubDeveloper()
      stubApplicationForEmail()

      When("I select to navigate to a collaborator")
      ApplicationsPage.selectDeveloperByEmail("Dixie.Upton@mail.com")

      Then("I am successfully navigated to the developer details page")
      on(DeveloperDetailsPage)
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

  def stubDeveloper() = {
    val encodedEmail = URLEncoder.encode(developer8, "UTF-8")

    stubFor(get(urlEqualTo(s"""/developer?email=$encodedEmail"""))
      .willReturn(aResponse().withStatus(200).withBody(user)))
  }

  def stubApplicationForEmail() = {
    val encodedEmail = URLEncoder.encode(developer8, "UTF-8")

    stubFor(get(urlPathEqualTo("/developer/applications")).withQueryParam("emailAddress", equalTo(encodedEmail))
      .willReturn(aResponse().withBody(applicationResponseForEmail).withStatus(200)))
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
