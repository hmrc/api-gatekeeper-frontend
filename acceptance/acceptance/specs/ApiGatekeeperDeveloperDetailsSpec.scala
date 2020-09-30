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

package acceptance.specs

import java.net.URLEncoder

import acceptance.pages._
import acceptance.mocks.TestData
import com.github.tomakehurst.wiremock.client.WireMock._
import model.User
import org.scalatest.{Assertions, Tag}
import play.api.http.Status._
import acceptance.mocks.{ApplicationWithSubscriptionDataMock, ApplicationResponseMock, StateHistoryMock}

import scala.io.Source

class ApiGatekeeperDeveloperDetailsSpec extends ApiGatekeeperBaseSpec with ApplicationWithSubscriptionDataMock with ApplicationResponseMock with StateHistoryMock with Assertions with TestData {

  val developers = List[User] {
    new User("joe.bloggs@example.co.uk", "joe", "bloggs", None, None, false)
  }

  info("AS A Gatekeeper superuser")
  info("I WANT to be able to view the applications an administrator/developer is on")
  info("SO THAT I can follow the correct process before deleting the administrator/developer")

  feature("Developer details page") {

    scenario("Ensure a user can select an individual developer", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper")
      stubApplicationList()
      val applicationsList = Source.fromURL(getClass.getResource("/applications.json")).mkString.replaceAll("\n","")

      stubFor(get(urlEqualTo(s"/application")).willReturn(aResponse()
        .withBody(applicationsList).withStatus(OK)))
      stubApplicationForEmail()
      stubApplication(newApplicationWithSubscriptionData.toJsonString, developers, stateHistories.toJsonString, newApplicationWithSubscriptionDataId)
      stubApiDefinition()
      stubDevelopers()
      stubDeveloper()
      stubApplicationSubscription()

      signInGatekeeper()
      on(ApplicationsPage)

      When("I select to navigate to the Developers page")
      ApplicationsPage.selectDevelopers()
      DeveloperPage.selectOldDevelopersPage()

      Then("I am successfully navigated to the Developers page where I can view all developer list details by default")
      on(DeveloperPage)

      When("I select a developer email")
      DeveloperPage.selectByDeveloperEmail(unverifiedUser.email)

      Then("I am successfully navigated to the Developer Details page")
      on(DeveloperDetailsPage)

      And("I can see the developer's details and associated applications")
      assert(DeveloperDetailsPage.firstName == s"${unverifiedUser.firstName}")
      assert(DeveloperDetailsPage.lastName == s"${unverifiedUser.lastName}")
      assert(DeveloperDetailsPage.status == "not yet verified")
      assert(DeveloperDetailsPage.mfaEnabled == "Yes")

      When("I select an associated application")
      DeveloperDetailsPage.selectByApplicationName("My new app")

      Then("I am successfully navigated to the Automated Test Application page")
      on(ApplicationPage)
    }
  }

  def stubApplicationForEmail() = {
    val encodedEmail = URLEncoder.encode(unverifiedUser.email, "UTF-8")

    stubFor(get(urlPathEqualTo("/developer/applications")).withQueryParam("emailAddress", equalTo(encodedEmail))
      .willReturn(aResponse().withBody(applicationResponseTest.toSeq.toJsonString).withStatus(OK)))
  }

  def stubAPISubscription(apiContext: String) = {
    stubFor(get(urlEqualTo(s"/application?subscribesTo=$apiContext"))
      .willReturn(aResponse().withBody(applicationResponse).withStatus(OK)))
  }

  def stubNoAPISubscription() = {
    stubFor(get(urlEqualTo("/application?noSubscriptions=true"))
      .willReturn(aResponse().withBody(applicationResponsewithNoSubscription).withStatus(OK)))
  }

  def stubApplicationSubscription() = {
    stubFor(get(urlEqualTo("/application/subscriptions")).willReturn(aResponse().withBody(applicationSubscription).withStatus(OK)))
  }

  def stubDevelopers() = {
    stubFor(get(urlEqualTo("/developers/all"))
      .willReturn(aResponse().withBody(allUsers).withStatus(OK)))
  }

  def stubDeveloper() = {
    val encodedEmail = URLEncoder.encode(unverifiedUser.email, "UTF-8")

    stubFor(get(urlEqualTo(s"""/developer?email=$encodedEmail"""))
      .willReturn(aResponse().withStatus(OK).withBody(newApplicationUser)))
  }
}

