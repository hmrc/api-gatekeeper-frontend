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

import acceptance.pages.{DashboardPage, DeveloperDetailsPage, DeveloperPage}
import acceptance.{BaseSpec, SignInSugar}
import com.github.tomakehurst.wiremock.client.WireMock._
import component.matchers.CustomMatchers
import model.User
import org.openqa.selenium.{By, WebElement}
import org.scalatest.{Assertions, GivenWhenThen, Matchers, Tag}
import play.api.libs.json.Json

import scala.collection.immutable.List

class APIGatekeeperDeveloperDetailsSpec extends BaseSpec with SignInSugar with Matchers with CustomMatchers with MockDataSugar with GivenWhenThen with Assertions {

  info("AS A Gatekeeper superuser")
  info("I WANT to be able to view the applications an administrator/developer is on")
  info("SO THAT I can follow the correct process before deleting the administrator/developer")

  feature("Developer details page") {

    scenario("Ensure a user can select an individual developer", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper")
      stubApplicationList
      stubApiDefinition
      stubDevelopers
      signInGatekeeper
      on(DashboardPage)

      When("I select to navigate to the Developers page")
      DashboardPage.selectDevelopers

      Then("I am successfully navigated to the Developers page where I can view all developer list details by default")
      on(DeveloperPage)

      When("I select a developer email")
      DeveloperPage.selectByDeveloperEmail("Dixie.Upton@mail.com")

      Then("I an successfully navigated to the Developer Details page")
      on(DeveloperDetailsPage)
    }
  }

  def stubApplicationList() = {
    stubFor(get(urlEqualTo("/gatekeeper/applications"))
      .willReturn(aResponse().withBody(approvedApplications).withStatus(200)))

    stubFor(get(urlEqualTo("/application")).willReturn(aResponse()
      .withBody(applicationResponse).withStatus(200)))
  }

  def stubApplicationListWithNoDevelopers() = {
    stubFor(get(urlEqualTo("/gatekeeper/applications"))
      .willReturn(aResponse().withBody(approvedApplications).withStatus(200)))

    stubFor(get(urlEqualTo("/application")).willReturn(aResponse()
      .withBody(applicationResponseWithNoUsers).withStatus(200)))
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
//    val developersJson = developers.map(userList => Json.toJson(userList)).map(Json.stringify).get
    stubFor(get(urlEqualTo("/developers/all"))
      .willReturn(aResponse().withBody(allUsers).withStatus(200)))
  }

  private def assertNumberOfDevelopersPerPage(expected: Int) = {
    import scala.collection.JavaConversions._
    val elements: Seq[WebElement] = webDriver.findElements(By.cssSelector("tbody > tr"))

    elements.count(we => we.isDisplayed) shouldBe expected
  }

  private def assertLinkIsDisabled(link: String) = {
    assertResult(find(linkText(link)).isDefined)(false)
  }

  private def assertCopyToClipboardButtonIsDisabled(button:String) = {
    assertResult(find(cssSelector(button)).isDefined)(false)
  }

  private def assertButtonIsPresent(button: String) = {
    webDriver.findElement(By.cssSelector(button)).isDisplayed shouldBe true
  }

  private def assertTextPresent(attributeName: String, expected: String) = {
    webDriver.findElement(By.cssSelector(attributeName)).getText shouldBe expected
  }

  private def generateUsersList(users : List[User]) = {
    users.map(user => s"${user.firstName} ${user.lastName}${user.email}")
  }

  case class TestUser(firstName: String, lastName:String, emailAddress:String)

  private def generateUsersTuple(users : List[User]): List[TestUser] = {
    users.map(user => TestUser(user.firstName, user.lastName, user.email))
  }

  private def verifyUsersEmailAddress(button : String, attributeName : String, expected : String) {
    val emailAddresses = webDriver.findElement(By.cssSelector(button)).getAttribute(attributeName) shouldBe expected
  }

  private def verifyUsersEmail(button : String) {
    val emailAddresses = webDriver.findElement(By.cssSelector(button)).getAttribute("value")
  }

  private def assertDevelopersRandomList(devList: List[(TestUser, Int)]) = {
    for((dev, index) <- devList) {
      val fn = webDriver.findElement(By.id(s"dev-fn-$index")).getText shouldBe dev.firstName
      val sn = webDriver.findElement(By.id(s"dev-sn-$index")).getText shouldBe dev.lastName
      val em = webDriver.findElement(By.id(s"dev-email-$index")).getText shouldBe dev.emailAddress
    }
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

