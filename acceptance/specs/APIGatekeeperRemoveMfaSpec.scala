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

import java.net.URLEncoder

import pages._
import _root_.BaseSpec
import _root_.SignInSugar
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import component.matchers.CustomMatchers
import org.scalatest.{Assertions, GivenWhenThen, Matchers, Tag}
import play.api.http.Status._

import scala.io.Source

class APIGatekeeperRemoveMfaSpec extends BaseSpec with SignInSugar with Matchers with CustomMatchers with MockDataSugar with GivenWhenThen with Assertions {

  info("As a Gatekeeper superuser")
  info("I WANT to be able to remove MFA for a developer")
  info("SO THAT they can reset MFA if they lost their secret")

  feature("Remove MFA") {

    scenario("Ensure a super user can remove MFA from a developer", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper")
      initStubs()
      signInSuperUserGatekeeper()
      on(ApplicationsPage)

      When("I navigate to the Developer Details page")
      navigateToDeveloperDetails()

      Then("I can see the button to remove MFA")
      assert(DeveloperDetailsPage.removeMfaButton.get.text == "Remove 2SV")

      When("I click on remove MFA")
      DeveloperDetailsPage.removeMfa()

      Then("I am successfully navigated to the Remove MFA page")
      on(RemoveMfaPage)

      When("I confirm I want to remove MFA")
      RemoveMfaPage.removeMfa()

      Then("I am successfully navigated to the Remove MFA Success page")
      on(RemoveMfaSuccessPage)

      When("I click on Finish")
      RemoveMfaSuccessPage.finish()
      
      Then("I am successfully navigated to the Developers page")
      on(DeveloperPage)
    }

    scenario("Ensure a non-super user cannot remove MFA from a developer", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper")
      initStubs()
      signInGatekeeper()
      on(ApplicationsPage)

      When("I navigate to the Developer Details page")
      navigateToDeveloperDetails()

      Then("I cannot see the button to remove 2SV")
      assert(DeveloperDetailsPage.removeMfaButton.isEmpty)
    }
  }

  def initStubs(): Unit = {
    stubApplicationList()
    stubApplicationForEmail()
    stubApiDefinition()
    stubDevelopers()
    stubDeveloper()
    stubApplicationSubscription()
    stubRemoveMfa()
  }

  def navigateToDeveloperDetails(): Unit ={
    When("I select to navigate to the Developers page")
    ApplicationsPage.selectDevelopers()
    DeveloperPage.selectOldDevelopersPage()

    Then("I am successfully navigated to the Developers page")
    on(DeveloperPage)

    When("I select a developer email")
    DeveloperPage.selectByDeveloperEmail(developer8)

    Then("I am successfully navigated to the Developer Details page")
    on(DeveloperDetailsPage)
  }

  def stubApplicationList(): Unit = {
    val applicationsList = Source.fromURL(getClass.getResource("/applications.json")).mkString.replaceAll("\n","")
    stubFor(get(urlEqualTo(s"/application")).willReturn(aResponse().withBody(applicationsList).withStatus(OK)))
  }

  def stubApplicationForEmail(): Unit = {
    val encodedEmail = URLEncoder.encode(developer8, "UTF-8")

    stubFor(get(urlPathEqualTo("/developer/applications")).withQueryParam("emailAddress", equalTo(encodedEmail))
      .willReturn(aResponse().withBody(applicationResponseForEmail).withStatus(OK)))
  }

  def stubApiDefinition(): Unit = {
    stubFor(get(urlEqualTo("/api-definition"))
      .willReturn(aResponse().withStatus(OK).withBody(apiDefinition)))

    stubFor(get(urlEqualTo("/api-definition?type=private"))
      .willReturn(aResponse().withStatus(OK).withBody(apiDefinition)))
  }

  def stubApplicationSubscription(): Unit = {
    stubFor(get(urlEqualTo("/application/subscriptions")).willReturn(aResponse().withBody(applicationSubscription).withStatus(OK)))
  }

  def stubDevelopers(): Unit = {
    stubFor(get(urlEqualTo("/developers/all"))
      .willReturn(aResponse().withBody(allUsers).withStatus(OK)))
  }

  def stubDeveloper(): Unit = {
    val encodedEmail = URLEncoder.encode(developer8, "UTF-8")

    stubFor(get(urlEqualTo(s"""/developer?email=$encodedEmail"""))
      .willReturn(aResponse().withStatus(OK).withBody(user)))
  }

  def stubRemoveMfa(): Unit = {
    stubFor(WireMock.post(urlEqualTo(s"""/developer/$developer8/mfa/remove"""))
      .willReturn(aResponse().withStatus(OK).withBody(user)))
  }
}
