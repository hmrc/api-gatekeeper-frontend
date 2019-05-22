/*
 * Copyright 2019 HM Revenue & Customs
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

import acceptance.pages.{ApplicationsPage, Developer2Page, DeveloperPage}
import acceptance.{BaseSpec, SignInSugar}
import com.github.tomakehurst.wiremock.client.WireMock._
import component.matchers.CustomMatchers
import model.User
import org.scalatest.{Assertions, GivenWhenThen, Matchers, Tag}
import play.api.http.Status._
import play.api.libs.json.Json

import scala.collection.immutable.List

class APIGatekeeperDeveloper2Spec extends BaseSpec with SignInSugar with Matchers with CustomMatchers with MockDataSugar with GivenWhenThen with Assertions {

  info("AS A Product Owner")
  info("I WANT The SDST (Software Developer Support Team) to be able to select developers with an interest in a particular API")
  info("SO THAT The SDST can create and send email communications to selected developers")

  feature("API Filter for Email Recipients") {

    scenario("Ensure a user can view the list of registered developers", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper")
      stubApplicationList()
      stubApplicationSubscription()
      stubApiDefinition()
      stubRandomDevelopers(100)

      val users = List(User(email = developer4,
        firstName = dev4FirstName,
        lastName = dev4LastName,
        verified = Some(true)))

      stubDevelopersSearch("partialEmail", users)

      signInGatekeeper()
      on(ApplicationsPage)

      When("I select to navigate to the Developers page")
      ApplicationsPage.selectDevelopers()
      DeveloperPage.selectNewDevelopersPage()

      Then("I am successfully navigated to the Developers page")
      on(Developer2Page)

      When("I enter a partial email to filter by")
      Developer2Page.searchByPartialEmail("partialEmail")

      Then("I see a list of filtered developers")

      val expectedDeveloper: Seq[(String, String, String, String)] = List(
        (dev4FirstName, dev4LastName, developer4, statusVerified))

      val allDevs: Seq[((String, String, String, String), Int)] = expectedDeveloper.zipWithIndex

      assertDevelopersList(allDevs)
    }
  }

  private def stubApplicationList(): Unit = {
    stubFor(get(urlEqualTo("/gatekeeper/applications"))
      .willReturn(aResponse().withBody(approvedApplications).withStatus(OK)))

    stubFor(get(urlEqualTo("/application")).willReturn(aResponse()
      .withBody(applicationResponse).withStatus(OK)))
  }

  private def stubApiDefinition(): Unit = {
    stubFor(get(urlEqualTo("/api-definition"))
      .willReturn(aResponse().withStatus(OK).withBody(apiDefinition)))

    stubFor(get(urlEqualTo("/api-definition?type=private"))
      .willReturn(aResponse().withStatus(OK).withBody(apiDefinition)))
  }

  private def stubApplicationSubscription(): Unit = {
    stubFor(get(urlEqualTo("/application/subscriptions")).willReturn(aResponse().withBody(applicationSubscription).withStatus(OK)))
  }

  private def stubRandomDevelopers(randomDevelopersCount: Int): Unit = {
    val developersList: String = developerListJsonGenerator(randomDevelopersCount).get
    stubFor(get(urlEqualTo("/developers/all"))
      .willReturn(aResponse().withBody(developersList).withStatus(OK)))
  }

  private def stubDevelopersSearch(emailFilter: String, developers: Seq[User]): Unit = {
    developers
      .map(developer => Map(
        "email" -> developer.email,
        "firstName" -> developer.firstName,
        "lastName" -> developer.lastName,
        "verified" -> developer.verified
      ))

    val developersListJson: String = Json.toJson(developers).toString

    stubFor(
      get(urlPathEqualTo("/developers"))
        .withQueryParam("emailFilter", equalTo(emailFilter))
        .willReturn(aResponse().withBody(developersListJson).withStatus(OK))
    )
  }

  private def assertDevelopersList(devList: Seq[((String, String, String, String), Int)]) {
    for ((_, _) <- devList) {
    }
  }
}
