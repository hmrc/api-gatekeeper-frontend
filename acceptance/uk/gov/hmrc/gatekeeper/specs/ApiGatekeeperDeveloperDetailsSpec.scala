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

package uk.gov.hmrc.gatekeeper.specs

import uk.gov.hmrc.gatekeeper.pages._
import uk.gov.hmrc.gatekeeper.testdata.{ApiDefinitionTestData, ApplicationResponseTestData, ApplicationWithSubscriptionDataTestData, CommonTestData, StateHistoryTestData}
import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.gatekeeper.models._
import org.scalatest.{Assertions, Tag}
import play.api.http.Status._
import uk.gov.hmrc.gatekeeper.stubs.XmlServicesStub
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

import uk.gov.hmrc.gatekeeper.testdata.MockDataSugar
class ApiGatekeeperDeveloperDetailsSpec
    extends ApiGatekeeperBaseSpec
    with ApplicationWithSubscriptionDataTestData
    with ApplicationResponseTestData
    with StateHistoryTestData
    with Assertions
    with CommonTestData
    with ApiDefinitionTestData
    with UrlEncoding
    with XmlServicesStub {
  val developers = List[RegisteredUser](RegisteredUser("joe.bloggs@example.co.uk", UserId.random, "joe", "bloggs", false))

  info("AS A Gatekeeper superuser")
  info("I WANT to be able to view the applications an administrator/developer is on")
  info("SO THAT I can follow the correct process before deleting the administrator/developer")

  Feature("Developer details page") {

    Scenario("Ensure a user can select an individual developer", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper")
      stubPaginatedApplicationList()
      
      stubFor(get(urlEqualTo("/application")).willReturn(aResponse()
        .withBody(stubApplicationsList()).withStatus(OK)))
      stubApplicationForDeveloper(unverifiedUser.userId)
      stubApplication(applicationWithSubscriptionData.toJsonString, developers, stateHistories.toJsonString, applicationId)
      stubApiDefinition()
      stubDevelopers()
      stubDevelopersSearch()
      stubDeveloper(unverifiedUser)
      stubGetXmlApiForCategories()
      stubGetAllXmlApis()
      stubGetXmlOrganisationsForUser(unverifiedUser.userId)
      stubApplicationSubscription()

      signInGatekeeper(app)
      on(ApplicationsPage)

      When("I select to navigate to the Developers page")
      ApplicationsPage.selectDevelopers()

      Then("I am successfully navigated to the Developers page where I can view all developer list details by default")
      on(DeveloperPage)

      When("I select a developer email")
      DeveloperPage.searchByPartialEmail(unverifiedUser.email)
      DeveloperPage.selectByDeveloperEmail(unverifiedUser.email)

      Then("I am successfully navigated to the Developer Details page")
      on(DeveloperDetailsPage)

      And("I can see the developer's details and associated applications")
      assert(DeveloperDetailsPage.firstName == unverifiedUser.firstName)
      assert(DeveloperDetailsPage.lastName == unverifiedUser.lastName)
      assert(DeveloperDetailsPage.status == "not yet verified")
      assert(DeveloperDetailsPage.mfaHeading == "Multi-factor authentication")

      When("I select an associated application")
      DeveloperDetailsPage.selectByApplicationName("My new app")

      Then("I am successfully navigated to the Automated Test Application page")
      on(ApplicationPage)
    }
  }

  def stubApplicationForDeveloper(userId: UserId) = {
    stubFor(
      get(urlPathEqualTo(s"/gatekeeper/developer/${userId.asText}/applications"))
      .willReturn(aResponse().withBody(defaultApplicationResponse.toSeq.toJsonString).withStatus(OK)))
  }

  def stubAPISubscription(apiContext: String) = {
    stubFor(get(urlEqualTo(s"/application?subscribesTo=$apiContext"))
      .willReturn(aResponse().withBody(MockDataSugar.applicationResponse).withStatus(OK)))
  }

  def stubNoAPISubscription() = {
    stubFor(get(urlEqualTo("/application?noSubscriptions=true"))
      .willReturn(aResponse().withBody(MockDataSugar.applicationResponsewithNoSubscription).withStatus(OK)))
  }

  def stubApplicationSubscription() = {
    stubFor(get(urlEqualTo("/application/subscriptions")).willReturn(aResponse().withBody(MockDataSugar.applicationSubscription).withStatus(OK)))
  }

  def stubDevelopers() = {
    stubFor(get(urlEqualTo("/developers/all"))
      .willReturn(aResponse().withBody(MockDataSugar.allUsers).withStatus(OK)))
  }

  def stubDevelopersSearch(): Unit = {
    stubFor(post(urlEqualTo("/developers/search"))
      .willReturn(aResponse().withBody(MockDataSugar.allUsers).withStatus(OK)))
  }

  def stubDeveloper(user: RegisteredUser) = {
    stubFor(
      get(urlPathEqualTo("/developer"))
      .withQueryParam("developerId", equalTo(user.userId.value.toString))
      .willReturn(
        aResponse().withStatus(OK).withBody(unverifiedUserJson)
      )
    )
  }
}

