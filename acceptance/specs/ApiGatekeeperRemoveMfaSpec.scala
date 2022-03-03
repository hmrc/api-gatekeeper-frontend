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

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.DeveloperConnector.{FindUserIdRequest, FindUserIdResponse}
import org.scalatest.{Assertions, Tag}
import pages._
import play.api.http.Status._
import play.api.libs.json.Json
import testdata.CommonTestData
import model.RegisteredUser
import model.UserId
import utils.WireMockExtensions

import java.util.UUID
import scala.io.Source

class ApiGatekeeperRemoveMfaSpec
  extends ApiGatekeeperBaseSpec
    with Assertions
    with CommonTestData
    with WireMockExtensions {

  import MockDataSugar._

  info("As a Gatekeeper superuser")
  info("I WANT to be able to remove MFA for a developer")
  info("SO THAT they can reset MFA if they lost their secret")

  Feature("Remove MFA") {

    Scenario("Ensure a super user can remove MFA from a developer", Tag("NonSandboxTest")) {

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
      on(Developer2Page)
    }

    Scenario("Ensure a non-super user CAN remove MFA from a developer", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper")
      initStubs()
      signInGatekeeper()
      on(ApplicationsPage)

      When("I navigate to the Developer Details page")
      navigateToDeveloperDetails()

      Then("I can see the button to remove MFA")
      assert(DeveloperDetailsPage.removeMfaButton.get.text == "Remove 2SV")
      assert(DeveloperDetailsPage.removeMfaButton.get.isEnabled == true)

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
      on(Developer2Page)
    }
  }

  def initStubs(): Unit = {
    stubFetchAllApplicationsList()
    stubPaginatedApplicationList()
    stubApplicationForDeveloper()
    stubApiDefinition()
    stubDevelopers()
    stubDevelopersSearch()
    stubDeveloper()
    stubGetAllXmlApis()
    stubGetXmlOrganisationsForUser(UserId(developer8Id))
    stubApplicationSubscription()
    stubRemoveMfa()
  }

  def navigateToDeveloperDetails(): Unit = {
    When("I select to navigate to the Developers page")
    ApplicationsPage.selectDevelopers()

    Then("I am successfully navigated to the Developers page")
    on(Developer2Page)

    When("I select a developer email")
    Developer2Page.searchByPartialEmail(developer8)
    Developer2Page.selectByDeveloperEmail(developer8)

    Then("I am successfully navigated to the Developer Details page")
    on(DeveloperDetailsPage)
  }

  def stubFetchAllApplicationsList(): Unit = {
    val applicationsList = Source.fromURL(getClass.getResource("/applications.json")).mkString.replaceAll("\n", "")
    stubFor(get(urlEqualTo(s"/application")).willReturn(aResponse().withBody(applicationsList).withStatus(OK)))
  }

  def stubApplicationForDeveloper(): Unit = {
    stubFor(
      get(urlPathEqualTo(s"/developer/${developer8Id.toString()}/applications"))
        .willReturn(
          aResponse()
            .withBody(applicationResponseForEmail)
            .withStatus(OK)
        )
    )
  }

  def stubApplicationSubscription(): Unit = {
    stubFor(get(urlEqualTo("/application/subscriptions")).willReturn(aResponse().withBody(applicationSubscription).withStatus(OK)))
  }

  def stubDevelopers(): Unit = {
    stubFor(get(urlEqualTo("/developers/all"))
      .willReturn(aResponse().withBody(allUsers).withStatus(OK)))
  }

  def stubDevelopersSearch(): Unit = {
    stubFor(post(urlEqualTo("/developers/search"))
      .willReturn(aResponse().withBody(allUsers).withStatus(OK)))
  }

  def stubGetAllXmlApis(): Unit = {
    stubFor(get(urlEqualTo("/api-platform-xml-services/xml/apis"))
      .willReturn(aResponse().withBody(xmlApis).withStatus(OK)))
  }

  def stubGetXmlOrganisationsForUser(userId: UserId): Unit = {
    stubFor(get(urlEqualTo(s"/api-platform-xml-services/organisations?userId=${userId.value}&sortBy=ORGANISATION_NAME"))
      .willReturn(aResponse().withBody(xmlOrganisations).withStatus(OK)))
  }

  def stubDeveloper(): Unit = {

    implicit val format = Json.writes[FindUserIdResponse]

    stubFor(
      post(urlEqualTo("/developers/find-user-id"))
        .withJsonRequestBody(FindUserIdRequest(developer8))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withJsonBody(FindUserIdResponse(UserId(developer8Id)))
        )
    )

    stubFor(
      get(urlPathEqualTo("/developer"))
        .withQueryParam("developerId", equalTo(encode(developer8Id.toString)))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withJsonBody(RegisteredUser(developer8, UserId(developer8Id), "Bob", "Smith", true, None, true))
        )
    )
  }

  def stubRemoveMfa(): Unit = {
    stubFor(WireMock.post(urlEqualTo(s"/developer/${developer8Id}/mfa/remove"))
      .willReturn(aResponse().withStatus(OK).withBody(user)))
  }
}
