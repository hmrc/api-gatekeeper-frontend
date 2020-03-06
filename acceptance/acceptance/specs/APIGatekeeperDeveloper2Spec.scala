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

import acceptance.matchers.CustomMatchers
import acceptance.pages.Developer2Page.APIFilter
import acceptance.pages.{ApplicationsPage, Developer2Page}
import acceptance.{BaseSpec, SignInSugar}
import com.github.tomakehurst.wiremock.client.WireMock._
import model.User
import org.openqa.selenium.By
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

      val developers = List(User(email = developer4,
        firstName = dev4FirstName,
        lastName = dev4LastName,
        verified = Some(true)),
        User(email = developer5,
          firstName = dev5FirstName,
          lastName = dev5LastName,
          verified = Some(false)))

      Given("I have successfully logged in to the API Gatekeeper")
      stubApplicationList()
      stubApplicationSubscription()
      stubApplicationsCollaborators(developers)
      stubApiDefinition()
      stubRandomDevelopers(100)

      stubGetDevelopersByEmails(developers)

      stubDevelopersSearch("partialEmail", developers)

      signInGatekeeper()
      on(ApplicationsPage)

      When("I select to navigate to the Developers page")
      ApplicationsPage.selectDevelopers()

      Then("I am successfully navigated to the Developers page")
      on(Developer2Page)

      When("I enter a partial email to filter by")
      Developer2Page.writeInSearchBox("partialEmail")

      And("I pick a an API definition")
      Developer2Page.selectBySubscription(APIFilter.EMPLOYERSPAYE)

      And("I pick an environment")
      Developer2Page.selectByEnvironment("PRODUCTION")

      And("I pick a Developer Status")
      Developer2Page.selectByDeveloperStatus("VERIFIED")

      And("I submit my search")
      Developer2Page.submit()

      Then("I see a list of filtered developers")

      val expectedDeveloper: Seq[(String, String, String, String)] = List(
        (dev4FirstName, dev4LastName, developer4, statusVerified))

      val allDevs: Seq[((String, String, String, String), Int)] = expectedDeveloper.zipWithIndex

      assertDevelopersList(allDevs)

      assertDeveloperAtRowDoesNotExist(1)
    }
  }

  private def assertDeveloperAtRowDoesNotExist(rowIndex: Int) = {
    val elements = webDriver.findElements(By.id(s"dev-fn-$rowIndex"))
    elements.size() shouldBe 0
  }

  private def stubApplicationList(): Unit = {
    stubFor(get(urlEqualTo("/gatekeeper/applications"))
      .willReturn(aResponse()
        .withBody(approvedApplications)
        .withStatus(OK)))

    stubFor(get(urlEqualTo("/application"))
      .willReturn(aResponse()
      .withBody(applicationResponse)
        .withStatus(OK)))
  }

  private def stubApiDefinition(): Unit = {
    stubFor(get(urlEqualTo("/api-definition"))
      .willReturn(aResponse().withStatus(OK).withBody(apiDefinition)))

    stubFor(get(urlEqualTo("/api-definition?type=private"))
      .willReturn(aResponse().withStatus(OK).withBody(apiDefinition)))
  }

  private def stubApplicationSubscription(): Unit = {
    stubFor(get(urlEqualTo("/application/subscriptions"))
      .willReturn(aResponse()
        .withBody(applicationSubscription)
        .withStatus(OK)))
  }

  private def stubApplicationsCollaborators(developers: Seq[User]): Unit = {
    val developersJson = Json.toJson(developers.map(u => u.email)).toString

    stubFor(get(urlPathEqualTo("/collaborators"))
      .withQueryParam("context", equalTo("employers-paye"))
      .withQueryParam("version", equalTo("1.0"))
      .withQueryParam("partialEmailMatch", equalTo("partialEmail"))
      .willReturn(aResponse()
        .withBody(developersJson)
        .withStatus(OK)))
  }

  private def stubRandomDevelopers(randomDevelopersCount: Int): Unit = {
    val developersList: String = developerListJsonGenerator(randomDevelopersCount).get
    stubFor(get(urlEqualTo("/developers/all"))
      .willReturn(aResponse()
        .withBody(developersList)
        .withStatus(OK)))
  }

  private def stubDevelopersSearch(emailFilter: String, developers: Seq[User]): Unit = {
    val developersListJson: String = Json.toJson(developers).toString

    stubFor(
      get(urlPathEqualTo("/developers"))
        .withQueryParam("emailFilter", equalTo(emailFilter))
        .willReturn(aResponse()
          .withBody(developersListJson)
          .withStatus(OK))
    )
  }

  private def stubGetDevelopersByEmails(developers: Seq[User]): Unit = {
    val emailsResponseJson = Json.toJson(developers).toString()

    stubFor(
      post(urlPathEqualTo("/developers/get-by-emails"))
        .willReturn(aResponse()
          .withBody(emailsResponseJson)
          .withStatus(OK))
    )
  }

  private def assertDevelopersList(devList: Seq[((String, String, String, String), Int)]) {
    for ((dev, index) <- devList) {
      val fn = webDriver.findElement(By.id(s"dev-fn-$index")).getText shouldBe dev._1
      val sn = webDriver.findElement(By.id(s"dev-sn-$index")).getText shouldBe dev._2
      val em = webDriver.findElement(By.id(s"dev-email-$index")).getText shouldBe dev._3
      val st = webDriver.findElement(By.id(s"dev-status-$index")).getText shouldBe dev._4
    }
  }
}
