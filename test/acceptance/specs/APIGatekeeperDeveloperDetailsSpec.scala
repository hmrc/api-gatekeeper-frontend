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

import acceptance.pages.{ApplicationPage, DashboardPage, DeveloperDetailsPage, DeveloperPage}
import acceptance.{BaseSpec, SignInSugar}
import com.github.tomakehurst.wiremock.client.WireMock._
import component.matchers.CustomMatchers
import org.scalatest.{Assertions, GivenWhenThen, Matchers, Tag}

class APIGatekeeperDeveloperDetailsSpec extends BaseSpec with SignInSugar with Matchers with CustomMatchers with MockDataSugar with GivenWhenThen with Assertions {

  info("AS A Gatekeeper superuser")
  info("I WANT to be able to view the applications an administrator/developer is on")
  info("SO THAT I can follow the correct process before deleting the administrator/developer")

  feature("Developer details page") {

    scenario("Ensure a user can select an individual developer", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper")
      stubApplicationList
      stubApplicationForEmail
      stubApplication
      stubApiDefinition
      stubDevelopers
      stubDeveloper
      signInGatekeeper
      on(DashboardPage)

      When("I select to navigate to the Developers page")
      DashboardPage.selectDevelopers

      Then("I am successfully navigated to the Developers page where I can view all developer list details by default")
      on(DeveloperPage)

      When("I select a developer email")
      DeveloperPage.selectByDeveloperEmail(developer8)

      Then("I am successfully navigated to the Developer Details page")
      on(DeveloperDetailsPage)

      And("I can see the developer's details and associated applications")
      assert(DeveloperDetailsPage.firstName == "Dixie")
      assert(DeveloperDetailsPage.lastName == "Upton")
      assert(DeveloperDetailsPage.status == "not yet verified")

      When("I select an associated application")
      DeveloperDetailsPage.selectByApplicationName("Automated Test Application")

      Then("I am successfully navigated to the Automated Test Application page")
      on(ApplicationPage)
    }
  }

  def stubApplicationList() = {
    stubFor(get(urlEqualTo("/gatekeeper/applications"))
      .willReturn(aResponse().withBody(approvedApplications).withStatus(200)))

    stubFor(get(urlEqualTo("/application")).willReturn(aResponse()
      .withBody(applicationResponse).withStatus(200)))
  }

  def stubApplication() = {
    stubFor(get(urlEqualTo("/gatekeeper/application/fa38d130-7c8e-47d8-abc0-0374c7f73216")).willReturn(aResponse().withBody(applicationToDelete).withStatus(200)))
    stubFor(get(urlEqualTo("/application/fa38d130-7c8e-47d8-abc0-0374c7f73216")).willReturn(aResponse().withBody(applicationToDelete).withStatus(200)))
    stubFor(get(urlEqualTo("/application/fa38d130-7c8e-47d8-abc0-0374c7f73216/subscription")).willReturn(aResponse().withBody("[]").withStatus(200)))
  }

  def stubApplicationForEmail() = {
    val encodedEmail = URLEncoder.encode(developer8, "UTF-8")

    stubFor(get(urlPathEqualTo("/developer/applications")).withQueryParam("emailAddress", equalTo(encodedEmail))
      .willReturn(aResponse().withBody(applicationResponseForEmail).withStatus(200)))
  }

  def stubAPISubscription(apiContext: String) = {
    stubFor(get(urlEqualTo(s"/application?subscribesTo=$apiContext"))
      .willReturn(aResponse().withBody(applicationResponse).withStatus(200)))
  }

  def stubNoAPISubscription() = {
    stubFor(get(urlEqualTo("/application?noSubscriptions=true"))
      .willReturn(aResponse().withBody(applicationResponsewithNoSubscription).withStatus(200)))
  }

  def stubApiDefinition() = {
    stubFor(get(urlEqualTo("/api-definition"))
      .willReturn(aResponse().withStatus(200).withBody(apiDefinition)))

    stubFor(get(urlEqualTo("/api-definition?type=private"))
      .willReturn(aResponse().withStatus(200).withBody(apiDefinition)))
  }

  def stubDevelopers = {
    stubFor(get(urlEqualTo("/developers/all"))
      .willReturn(aResponse().withBody(allUsers).withStatus(200)))
  }

  def stubDeveloper = {
    val encodedEmail = URLEncoder.encode(developer8, "UTF-8")

    stubFor(get(urlEqualTo(s"""/developer?email=$encodedEmail"""))
      .willReturn(aResponse().withStatus(200).withBody(user)))
  }

  case class TestUser(firstName: String, lastName:String, emailAddress:String)
}

