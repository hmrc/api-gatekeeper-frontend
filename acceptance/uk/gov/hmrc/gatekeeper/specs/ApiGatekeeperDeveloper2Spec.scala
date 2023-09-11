/*
 * Copyright 2023 HM Revenue & Customs
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

import uk.gov.hmrc.gatekeeper.pages.DeveloperPage.APIFilter
import uk.gov.hmrc.gatekeeper.pages.{ApplicationsPage, DeveloperPage}
import com.github.tomakehurst.wiremock.client.WireMock._
import org.openqa.selenium.By
import org.scalatest.{Assertions, Tag}
import play.api.http.Status._
import play.api.libs.json.Json

import scala.collection.immutable.List
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.connectors.ApplicationConnector
import uk.gov.hmrc.apiplatform.modules.common.utils.WireMockExtensions
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.gatekeeper.testdata.MockDataSugar
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
class ApiGatekeeperDeveloper2Spec extends ApiGatekeeperBaseSpec with Assertions with WireMockExtensions {

  import MockDataSugar._

  info("AS A Product Owner")
  info("I WANT The SDST (Software Developer Support Team) to be able to select developers with an interest in a particular API")
  info("SO THAT The SDST can create and send email communications to selected developers")

  Feature("API Filter for Email Recipients") {

    Scenario("Ensure a user can view the list of registered developers", Tag("NonSandboxTest")) {

      val developers = List(
        RegisteredUser(
          email = developer4.toLaxEmail,
          userId = UserId.random,
          firstName = dev4FirstName,
          lastName = dev4LastName,
          verified = true
          ),
        RegisteredUser(
          email = developer5.toLaxEmail,
          userId = UserId.random,
          firstName = dev5FirstName,
          lastName = dev5LastName,
          verified = false
        )
      )

      Given("I have successfully logged in to the API Gatekeeper")
      stubApplicationList()
      stubApplicationSubscription()
      stubPaginatedApplicationList()
      stubApplicationsCollaborators(developers)
      stubApiDefinition()
      stubRandomDevelopers(100)

      stubGetDevelopersByEmails(developers)

      stubDevelopersSearch("partialEmail", developers)

      signInGatekeeper(app)
      on(ApplicationsPage)

      When("I select to navigate to the Developers page")
      ApplicationsPage.selectDevelopers()

      Then("I am successfully navigated to the Developers page")
      on(DeveloperPage)

      When("I enter a partial email to filter by")
      DeveloperPage.writeInSearchBox("partialEmail")

      And("I pick a an API definition")
      DeveloperPage.selectBySubscription(APIFilter.EMPLOYERSPAYE)

      And("I pick an environment")
      DeveloperPage.selectByEnvironment("PRODUCTION")

      And("I pick a Developer Status")
      DeveloperPage.selectByDeveloperStatus("VERIFIED")

      And("I submit my search")
      DeveloperPage.submit()

      Then("I see a list of filtered developers")

      val expectedDeveloper2: Seq[(String, String, String, String)] = List(
        (dev4FirstName, dev4LastName, developer4, statusVerified))

      val allDevs: Seq[((String, String, String, String), Int)] = expectedDeveloper2.zipWithIndex

      assertDevelopersList(allDevs)

      assertThereAreNoMoreThanNDevelopers(1)
    }
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

  private def stubApplicationSubscription(): Unit = {
    stubFor(get(urlEqualTo("/application/subscriptions"))
      .willReturn(aResponse()
        .withBody(applicationSubscription)
        .withStatus(OK)))
  }

  private def stubApplicationsCollaborators(developers: Seq[User]): Unit = {
    val developersJson = developers.map(u => u.email)
    val request = ApplicationConnector.SearchCollaboratorsRequest(ApiContext("employers-paye"), ApiVersionNbr("1.0"), Some("partialEmail"))
    
    stubFor(post(urlEqualTo("/collaborators"))
      .withJsonRequestBody(request)
      .willReturn(aResponse()
        .withJsonBody(developersJson)
        .withStatus(OK)))
  }

  private def stubRandomDevelopers(randomDevelopersCount: Int): Unit = {
    val developersList: String = developerListJsonGenerator(randomDevelopersCount).get
    stubFor(get(urlEqualTo("/developers/all"))
      .willReturn(aResponse()
        .withBody(developersList)
        .withStatus(OK)))
  }

  private def stubDevelopersSearch(emailFilter: String, developers: Seq[RegisteredUser]): Unit = {
    val developersListJson: String = Json.toJson(developers).toString

    val body = java.net.URLEncoder.encode("emailFilter="+emailFilter, "UTF-8")
    stubFor(
      get(urlPathEqualTo("/developers"))
        .withRequestBody(equalTo(body))
        .willReturn(aResponse()
          .withBody(developersListJson)
          .withStatus(OK))
    )
  }

  private def stubGetDevelopersByEmails(developers: Seq[RegisteredUser]): Unit = {
    val emailsResponseJson = Json.toJson(developers).toString()

    stubFor(
      post(urlPathEqualTo("/developers/get-by-emails"))
        .willReturn(aResponse()
          .withBody(emailsResponseJson)
          .withStatus(OK))
    )
  }

  private def assertDevelopersList(devList: Seq[((String, String, String, String), Int)]): Unit = {
    for ((dev, index) <- devList) {
      webDriver.findElement(By.id(s"dev-fn-$index")).getText() shouldBe dev._1
      webDriver.findElement(By.id(s"dev-sn-$index")).getText() shouldBe dev._2
      webDriver.findElement(By.id(s"dev-email-$index")).getText() shouldBe dev._3
      webDriver.findElement(By.id(s"dev-status-$index")).getText() shouldBe dev._4
    }
  }

  private def assertThereAreNoMoreThanNDevelopers(count: Int) = assertDeveloperAtRowDoesNotExist(count)
  
  private def assertDeveloperAtRowDoesNotExist(rowIndex: Int) = {
    val elements = webDriver.findElements(By.id(s"dev-fn-$rowIndex"))
    elements.size() shouldBe 0
  }

}
