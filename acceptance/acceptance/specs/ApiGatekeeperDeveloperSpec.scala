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
import acceptance.mocks.ApiDefinitionMock
import acceptance.pages.DeveloperPage.APIFilter._
import acceptance.pages.DeveloperPage.StatusFilter._
import acceptance.pages.{ApplicationsPage, DeveloperPage}
import acceptance.{BaseSpec, SignInSugar}
import com.github.tomakehurst.wiremock.client.WireMock._
import model.User
import org.openqa.selenium.{By, WebElement}
import org.scalatest.{Assertions, GivenWhenThen, Matchers, Tag}
import play.api.http.Status._
import play.api.libs.json.Json

import scala.collection.immutable.List

class ApiGatekeeperDeveloperSpec extends BaseSpec with SignInSugar with Matchers with CustomMatchers with MockDataSugar with GivenWhenThen with Assertions with ApiDefinitionMock {

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

      signInGatekeeper()
      on(ApplicationsPage)

      When("I select to navigate to the Developers page")
      ApplicationsPage.selectDevelopers()

      DeveloperPage.selectOldDevelopersPage()

      Then("I am successfully navigated to the Developers page where I can view all developer list details by default")
      on(DeveloperPage)
    }

    scenario("Ensure a user can view ALL developers", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper")
      stubApplicationList()
      stubApiDefinition()
      stubApplicationSubscription()
      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(allUsers).withStatus(OK)))
      signInGatekeeper()
      on(ApplicationsPage)

      When("I select to navigate to the Developers page")
      ApplicationsPage.selectDevelopers()
      DeveloperPage.selectOldDevelopersPage()
      on(DeveloperPage)

      Then("all developers are successfully displayed and sorted correctly")
      val developers: Seq[(String, String, String,String)] = List((dev4FirstName,dev4LastName,developer4,statusVerified),
        (dev8FirstName, dev8LastName, developer8,statusUnverified),
        (dev9name, dev9name, developer9,statusUnregistered),
        (dev2FirstName, dev2LastName, developer2,statusVerified),
        (dev5FirstName, dev5LastName, developer5,statusUnverified),
        (dev7FirstName, dev7LastName, developer7,statusVerified),
        (devFirstName, devLastName, developer,statusVerified),
        (dev6FirstName, dev6LastName, developer6,statusVerified))


      val allDevs: Seq[((String, String, String, String), Int)] = developers.zipWithIndex

      assertDevelopersList(allDevs)

      When("I select verified from the status filter drop down")
      DeveloperPage.selectByStatus(VERIFIED)

      Then("all the verified developers are displayed")
      val developers2:Seq[(String, String, String,String)]=List((dev4FirstName, dev4LastName, developer4, statusVerified),
        (dev2FirstName, dev2LastName, developer2, statusVerified),
        (dev7FirstName, dev7LastName, developer7, statusVerified),
        (devFirstName, devLastName, developer, statusVerified),
        (dev6FirstName, dev6LastName, developer6, statusVerified))

      val verifiedDevs = developers2.zipWithIndex

      assertDevelopersList(verifiedDevs)

      When("I select unverified from the status filter drop down")
      DeveloperPage.selectByStatus(UNVERIFIED)

      Then("all the unverified developers are displayed")
      val developers3:Seq[(String, String, String,String)] = List((dev8FirstName, dev8LastName, developer8, statusUnverified),
        (dev5FirstName,dev5LastName, developer5, statusUnverified))
      val unverifiedDevs = developers3.zipWithIndex
      assertDevelopersList(unverifiedDevs)

      When("I select not registered from the status filter drop down")
      DeveloperPage.selectByStatus(NOTREGISTERED)

      Then("all the unregistered developers are displayed")
      val developers4 = List((dev9name, dev9name, developer9, statusUnregistered))
      val unregisteredDev = developers4.zipWithIndex
      assertDevelopersList(unregisteredDev)
    }

    scenario("Ensure a user can view all developers who are subscribed to one or more API", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper and I am on the Developers page")
      stubApplicationList()
      stubApiDefinition()
      stubApplicationSubscription()
      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(allUsers).withStatus(OK)))
      stubAPISubscription("employers-paye")
      stubNoAPISubscription()
      signInGatekeeper()

      on(ApplicationsPage)

      ApplicationsPage.selectDevelopers()
      DeveloperPage.selectOldDevelopersPage()
      on(DeveloperPage)

      When("I select one or more subscriptions from the filter drop down")
      DeveloperPage.selectBySubscription(ONEORMORESUBSCRIPTION)
      DeveloperPage.selectByStatus(ALL)

      Then("all verified and unverified developers are successfully displayed and sorted correctly")
      val developers = List((dev8FirstName, dev8LastName, developer8, statusUnverified),
        (dev9name, dev9name, developer9,statusUnregistered),
        (dev2FirstName, dev2LastName,developer2,statusVerified),
        (dev7FirstName, dev7LastName,developer7, statusVerified),
        (devFirstName, devLastName, developer, statusVerified))

      val allDevs: Seq[((String, String, String, String), Int)] = developers.zipWithIndex

      assertDevelopersList(allDevs)

      When("I select verified from the status filter drop down")
      DeveloperPage.selectByStatus(VERIFIED)

      Then("all verified developers are displayed successfully")
      val developers2 = List((dev2FirstName, dev2LastName, developer2, statusVerified),
        (dev7FirstName, dev7LastName, developer7,statusVerified),
        (devFirstName, devLastName, developer, statusVerified))

      val verifiedDevs = developers2.zipWithIndex

      assertDevelopersList(verifiedDevs)


      When("I select unverified from the status filter drop down")
      DeveloperPage.selectByStatus(UNVERIFIED)

      Then("all the unverified developers are displayed")
      val developers3 = List((dev8FirstName, dev8LastName,developer8,statusUnverified))
      val unverifiedDevs = developers3.zipWithIndex
      assertDevelopersList(unverifiedDevs)

      When("I select not registered from the status filter drop down")
      DeveloperPage.selectByStatus(NOTREGISTERED)

      Then("all the unregistered developers are displayed")
      val developers4 = List((dev9name, dev9name, developer9, statusUnregistered))
      val unregisteredDev = developers4.zipWithIndex
      assertDevelopersList(unregisteredDev)

    }

    scenario("Ensure a user can view all developers who have no subscription to an API", Tag("NonSandboxTest")){

      Given("I have successfully logged in to the API Gatekeeper and I am on the Developers page")
      stubApplicationList()
      stubApiDefinition()
      stubApplicationSubscription()
      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(allUsers).withStatus(OK)))
      stubNoAPISubscription()
      signInGatekeeper()
      on(ApplicationsPage)
      ApplicationsPage.selectDevelopers()
      DeveloperPage.selectOldDevelopersPage()
      on(DeveloperPage)

      When("I select no subscription from the filter drop down")
      DeveloperPage.selectBySubscription(NOSUBSCRIPTION)
      DeveloperPage.selectByStatus(ALL)

      Then("all verified and unverified developers are displayed and sorted correctly")
      val developers = List((dev4FirstName, dev4LastName, developer4, statusVerified),
        (dev5FirstName, dev5LastName, developer5,statusUnverified),
        (dev10name, dev10name, developer10, statusUnregistered),
        (dev6FirstName, dev6LastName, developer6, statusVerified))

      val allDevs: Seq[((String, String, String, String), Int)] = developers.zipWithIndex

      assertDevelopersList(allDevs)


      When("I select verified from the status filter drop down")
      DeveloperPage.selectByStatus(VERIFIED)

      Then("all verified developers and collaborators are successfully displayed")
      val developers2 = List((dev4FirstName, dev4LastName, developer4, statusVerified),
        (dev6FirstName, dev6LastName, developer6, statusVerified))

      val verifiedDevs = developers2.zipWithIndex
      assertDevelopersList(verifiedDevs)

      When("I select unverified from the status filter drop down")
      DeveloperPage.selectByStatus(UNVERIFIED)

      Then("all unverified developers are displayed")
      val developers3 = List((dev5FirstName, dev5LastName, developer5,statusUnverified))
      val unverifiedDevs = developers3.zipWithIndex
      assertDevelopersList(unverifiedDevs)

      When("I select not registered from the status filter drop down")
      DeveloperPage.selectByStatus(NOTREGISTERED)

      Then("all unregistered developers are displayed")
      val developers4 = List((dev10name, dev10name, developer10, statusUnregistered))
      val unregisteredDev = developers4.zipWithIndex
      assertDevelopersList(unregisteredDev)
    }

    scenario("Ensure a user can view all developers who has one or more application", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper and I am on the Developers page")
      stubApplicationList()
      stubApiDefinition()
      stubApplicationSubscription()
      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(allUsers).withStatus(OK)))
      signInGatekeeper()
      on(ApplicationsPage)
      ApplicationsPage.selectDevelopers()
      DeveloperPage.selectOldDevelopersPage()
      on(DeveloperPage)

      When("I select one or more applications from the filter drop down")
      DeveloperPage.selectBySubscription(ONEORMOREAPPLICATIONS)

      Then("all verified developers and unverified developers are displayed and sorted correctly")
      val developers = List((dev8FirstName, dev8LastName, developer8, statusUnverified),
        (dev9name, dev9name, developer9, statusUnregistered),
        (dev2FirstName, dev2LastName, developer2, statusVerified),
        (dev7FirstName, dev7LastName, developer7, statusVerified),
        (devFirstName, devLastName, developer, statusVerified))

      val allDevs: Seq[((String, String, String, String), Int)] = developers.zipWithIndex
      assertDevelopersList(allDevs)

      When("I select verified from the status filter drop down")
      DeveloperPage.selectByStatus(VERIFIED)

      Then("all verified developers are successfully displayed")
      val developers2 = List((dev2FirstName, dev2LastName, developer2,statusVerified),
        (dev7FirstName, dev7LastName, developer7, statusVerified),
        (devFirstName, devLastName, developer, statusVerified))
      val verifiedDevs = developers2.zipWithIndex
      assertDevelopersList(verifiedDevs)

      When("I select Unverified from the status filter drop down")
      DeveloperPage.selectByStatus(UNVERIFIED)

      Then("all unverified developers are displayed")
      val developers3 = List((dev8FirstName, dev8LastName, developer8, statusUnverified))
      val unverifiedDevs = developers3.zipWithIndex
      assertDevelopersList(unverifiedDevs)

      When("I select not registered from the Status filter drop down")
      DeveloperPage.selectByStatus(NOTREGISTERED)

      Then("All unregistered developers are displayed")
      val developers4 = List((dev9name, dev9name, developer9,statusUnregistered))
      val unregisteredDev = developers4.zipWithIndex
      assertDevelopersList(unregisteredDev)

    }

    scenario("Ensure a SDST can view all users who has no application", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper and I am on the Developers page")
      stubApplicationList()
      stubApiDefinition()
      stubApplicationSubscription()
      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(allUsers).withStatus(OK)))
      signInGatekeeper()
      on(ApplicationsPage)
      ApplicationsPage.selectDevelopers()
      DeveloperPage.selectOldDevelopersPage()
      on(DeveloperPage)

      When("I select no applications from the filter drop down")
      DeveloperPage.selectBySubscription(NOAPPLICATIONS)

      Then("all verified users and unverified developers are displayed and sorted correctly")
      val developers = List((dev4FirstName, dev4LastName, developer4, statusVerified),
        (dev5FirstName ,dev5LastName, developer5,statusUnverified),
        (dev6FirstName, dev6LastName, developer6, statusVerified))

      val allDevs: Seq[((String, String, String, String), Int)] = developers.zipWithIndex
      assertDevelopersList(allDevs)

      When("I select verified from the status filter drop down")
      DeveloperPage.selectByStatus(VERIFIED)

      Then("all verified developers are successfully displayed")
      val developers2 = List((dev4FirstName, dev4LastName, developer4,statusVerified),
        (dev6FirstName, dev6LastName, developer6, statusVerified))

      val verifiedDevs = developers2.zipWithIndex
      assertDevelopersList(verifiedDevs)

      When("I select unverified from the status filter drop down")
      DeveloperPage.selectByStatus(UNVERIFIED)

      Then("all unverified developers are displayed")
      val developers3 = List((dev5FirstName ,dev5LastName, developer5,statusUnverified))
      val unverifiedDevs = developers3.zipWithIndex
      assertDevelopersList(unverifiedDevs)

      When("I select not registered from the status filter drop down")
      DeveloperPage.selectByStatus(NOTREGISTERED)

      Then("No results should be displayed")

      And("The email developer and copy to clipboard buttons are disabled")
      assertCopyToClipboardButtonIsDisabled("#content div a.button")
    }

    scenario("Ensure a user can view all developers who are subscribed to the Employers-PAYE API", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper and I am on the Developers page")
      stubApplicationList()
      stubApiDefinition()
      stubApplicationSubscription()
      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(allUsers).withStatus(OK)))
      stubAPISubscription("employers-paye")
      signInGatekeeper()
      on(ApplicationsPage)
      ApplicationsPage.selectDevelopers()
      DeveloperPage.selectOldDevelopersPage()
      on(DeveloperPage)

      When("I select Employers PAYE from the API filter drop down")
      DeveloperPage.selectBySubscription(EMPLOYERSPAYE)

      Then("all verified and unverified developers subscribing to the Employers PAYE API are successfully displayed and sorted correctly")
      val developers = List((dev8FirstName, dev8LastName,developer8, statusUnverified),
        (dev9name, dev9name, developer9,statusUnregistered),
        (dev2FirstName, dev2LastName,developer2,statusVerified),
        (dev7FirstName, dev7LastName,developer7,statusVerified),
        (devFirstName, devLastName,developer, statusVerified))

      val allDevs: Seq[((String, String, String, String), Int)] = developers.zipWithIndex

      assertDevelopersList(allDevs)

      When("I select verified from the status filter drop down")
      DeveloperPage.selectByStatus(VERIFIED)

      Then("all verified developers are successfully displayed")
      val developers2 = List((dev2FirstName, dev2LastName, developer2, statusVerified),
        (dev7FirstName, dev7LastName, developer7, statusVerified),
        (devFirstName, devLastName, developer, statusVerified))

      val verifiedDevs: Seq[((String, String, String, String), Int)] = developers2.zipWithIndex
      assertDevelopersList(verifiedDevs)

      When("I select unverified from the status filter drop down")
      DeveloperPage.selectByStatus(UNVERIFIED)

      Then("all unverified developers are displayed")
      val developers3 = List((dev8FirstName, dev8LastName, developer8, statusUnverified))
      val unverifiedDevs = developers3.zipWithIndex
      assertDevelopersList(unverifiedDevs)

      When("I select not registered from the status filter drop down")
      DeveloperPage.selectByStatus(NOTREGISTERED)

      Then("all unregistered developers are displayed")
      val developers4 = List((dev9name, dev9name, developer9,statusUnregistered))
      val unregisteredDev = developers4.zipWithIndex
      assertDevelopersList(unregisteredDev)
    }

    scenario("Ensure a user can view the Copy to Clipboard buttons on the Developers page", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper")
      stubApplicationListWithNoDevelopers()
      stubApiDefinition()
      stubApplicationSubscription()
      stubRandomDevelopers(24)
      signInGatekeeper()
      on(ApplicationsPage)

      When("I select to navigate to the Developers page")
      ApplicationsPage.selectDevelopers()
      DeveloperPage.selectOldDevelopersPage()
      on(DeveloperPage)

      Then("I should be able to view the Copy to Clipboard buttons")
      assertButtonIsPresent("#content a.button")
    }

    scenario("Ensure all developer email addresses are successfully loaded into bcc", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper")
      stubApplicationList()
      stubApiDefinition()
      stubApplicationSubscription()
      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(allUsers).withStatus(OK)))
      signInGatekeeper()
      on(ApplicationsPage)

      When("I select to navigate to the Developers page")
      ApplicationsPage.selectDevelopers()
      DeveloperPage.selectOldDevelopersPage()
      on(DeveloperPage)

      Then("the copy to clipboard button should contain all of the developers email addresses")
      verifyUsersEmailAddress("#content a.button","data-clip-text", s"$developer4; $developer8; $developer9; $developer2; $developer5; $developer7; $developer; $developer6")
    }
  }

  def stubApplicationList() = {
    stubFor(get(urlEqualTo("/gatekeeper/applications"))
      .willReturn(aResponse().withBody(approvedApplications).withStatus(OK)))

    stubFor(get(urlEqualTo("/application")).willReturn(aResponse()
      .withBody(applicationResponse).withStatus(OK)))
  }


  def stubApplicationListWithNoDevelopers() = {
    stubFor(get(urlEqualTo("/gatekeeper/applications"))
      .willReturn(aResponse().withBody(approvedApplications).withStatus(OK)))

    stubFor(get(urlEqualTo("/application")).willReturn(aResponse()
      .withBody(applicationResponseWithNoUsers).withStatus(OK)))
  }

  def stubAPISubscription(apiContext: String, apiVersion: String = "1.0") = {
    stubFor(get(urlEqualTo(s"/application?subscribesTo=$apiContext&version=$apiVersion"))
      .willReturn(aResponse().withBody(applicationResponse).withStatus(OK)))
  }

  def stubNoAPISubscription() = {
    stubFor(get(urlEqualTo("/application?noSubscriptions=true"))
      .willReturn(aResponse().withBody(applicationResponsewithNoSubscription).withStatus(OK)))
  }

  def stubApiDefinition() = {
    stubFor(get(urlEqualTo("/api-definition"))
      .willReturn(aResponse().withStatus(OK).withBody(apiDefinition)))

    stubFor(get(urlEqualTo("/api-definition?type=private"))
      .willReturn(aResponse().withStatus(OK).withBody(apiDefinition)))
  }

  def stubApplicationSubscription() = {
    stubFor(get(urlEqualTo("/application/subscriptions")).willReturn(aResponse().withBody(applicationSubscription).withStatus(OK)))
  }


  def stubRandomDevelopers(randomDevelopers: Int) = {
    val developersList: String = developerListJsonGenerator(randomDevelopers).get
    stubFor(get(urlEqualTo("/developers/all"))
      .willReturn(aResponse().withBody(developersList).withStatus(OK)))
  }

  def stubDevelopers(developers: Option[List[User]]) = {
    val developersJson = developers.map(userList => Json.toJson(userList)).map(Json.stringify).get
    stubFor(get(urlEqualTo("/developers/all"))
      .willReturn(aResponse().withBody(developersJson).withStatus(OK)))
  }

  private def assertCopyToClipboardButtonIsDisabled(button:String) = {
    assertResult(find(cssSelector(button)).isDefined)(false)
  }

  private def assertButtonIsPresent(button: String) = {
    webDriver.findElement(By.cssSelector(button)).isDisplayed shouldBe true
  }

  case class TestUser(firstName: String, lastName:String, emailAddress:String)

  private def verifyUsersEmailAddress(button: String, attributeName: String, expected: String) {
    webDriver.findElement(By.cssSelector(button)).getAttribute(attributeName) shouldBe expected
  }

  private def assertDevelopersList(devList: Seq[((String, String, String, String), Int)]) {
    for ((dev, index) <- devList) {
      webDriver.findElement(By.id(s"dev-fn-$index")).getText shouldBe dev._1
      webDriver.findElement(By.id(s"dev-sn-$index")).getText shouldBe dev._2
      webDriver.findElement(By.id(s"dev-email-$index")).getText shouldBe dev._3
      webDriver.findElement(By.id(s"dev-status-$index")).getText shouldBe dev._4
    }
  }
}
