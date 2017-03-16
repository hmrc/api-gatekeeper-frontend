/*
 * Copyright 2017 HM Revenue & Customs
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

import acceptance.pages.DeveloperPage.APIFilter._
import acceptance.pages.DeveloperPage.StatusFilter._
import acceptance.pages.{DashboardPage, DeveloperPage}
import acceptance.{BaseSpec, SignInSugar}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import component.matchers.CustomMatchers
import model.User
import org.openqa.selenium.{By, WebElement}
import org.scalatest.{Assertions, GivenWhenThen, Matchers, Tag}
import play.api.libs.json.Json

import scala.collection.immutable.List

class APIGatekeeperDeveloperSpec extends BaseSpec with SignInSugar with Matchers with CustomMatchers with MockDataSugar with GivenWhenThen with Assertions {

  info("AS A Product Owner")
  info("I WANT The SDST (Software Developer Support Team) to be able to select developers with an interest in a particular API")
  info("SO THAT The SDST can create and send email communications to selected developers")

  feature("API Filter for Email Recipients") {

    scenario("Ensure a user can view the list of registered developers", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper")
//      stubApplicationList
//      stubApiDefinition
//      stubRandomDevelopers(100)
      signInGatekeeper
      on(DashboardPage)

      When("I select to navigate to the Developers page")
      DashboardPage.selectDevelopers

      Then("I am successfully navigated to the Developers page where I can view all developer list details by default")
      on(DeveloperPage)
    }

    scenario("Ensure a user can view ALL developers", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper")
      //stubApplicationList
      //stubApiDefinition
//      val stubMapping: StubMapping = get(urlEqualTo("/developers/all")).atPriority(1)
//        .willReturn(aResponse().withBody(allUsers).withStatus(200)).build()

      signInGatekeeper
      on(DashboardPage)

      When("I select to navigate to the Developers page")
      DashboardPage.selectDevelopers
      on(DeveloperPage)

      Then("all developers are successfully displayed and sorted correctly")
      val developers: Seq[(String, String, String,String)] = List(("Adam","Cooper","adam.cooper@digital.hmrc.gov.uk",statusVerified),
        ("admin", "test", "admin2@email.com",statusVerified),
        ("admin", "test", "admin@email.com",statusVerified),
        ("Aivars", "Smaukstelis", "aivars.smaukstelis@digital.hmrc.gov.uk",statusVerified))

      val allDevs: Seq[((String, String, String, String), Int)] = developers.zipWithIndex

      assertDevelopersList(allDevs)

      When("I select verified from the status filter drop down")
      DeveloperPage.selectByStatus(VERIFIED)

      Then("all the verified developers are displayed")
      val developers2:Seq[(String, String, String,String)]= List(("Adam","Cooper","adam.cooper@digital.hmrc.gov.uk",statusVerified),
                                                 ("admin", "test", "admin2@email.com",statusVerified),
                                                 ("admin", "test", "admin@email.com",statusVerified),
                                                 ("Aivars", "Smaukstelis", "aivars.smaukstelis@digital.hmrc.gov.uk",statusVerified))

      val verifiedDevs = developers2.zipWithIndex

      assertDevelopersList(verifiedDevs)

      When("I select unverified from the status filter drop down")
      DeveloperPage.selectByStatus(UNVERIFIED)

      Then("all the unverified developers are displayed")
      val developers3:Seq[(String, String, String,String)] = List(("andy", "101", "andy101@mailinator.com", statusUnverified),
                                                         ("Ananth","Meta", "andymeta@mailinator.com", statusUnverified))

      val unverifiedDevs = developers3.zipWithIndex
      assertDevelopersList(unverifiedDevs)

      When("I select not registered from the status filter drop down")
      DeveloperPage.selectByStatus(NOTREGISTERED)

      Then("all the unregistered developers are displayed")
      val developers4 = List(("n/a", "n/a", "andywheeler@mailinator.com", statusUnregistered))
      val unregisteredDev = developers4.zipWithIndex
      assertDevelopersList(unregisteredDev)
    }

    scenario("Ensure a user can view all developers who are subscribed to one or more API", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper and I am on the Developers page")
//      stubApplicationList
//      stubApiDefinition
//      stubFor(get(urlEqualTo("/developers/all"))
//        .willReturn(aResponse().withBody(allUsers).withStatus(200)))
//      stubAPISubscription("employers-paye")
//      stubNoAPISubscription()
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDevelopers
      on(DeveloperPage)

      When("I select one or more subscriptions from the filter drop down")
      DeveloperPage.selectBySubscription(ONEORMORESUBSCRIPTION)
      DeveloperPage.selectByStatus(ALL)

      Then("all verified and unverified developers are successfully displayed and sorted correctly")
      val developers = List(("Adam","Cooper","adam.cooper@digital.hmrc.gov.uk",statusVerified),
                            ("admin", "test", "admin2@email.com",statusVerified),
                            ("admin", "test", "admin@email.com",statusVerified),
                            ("Aivars", "Smaukstelis", "aivars.smaukstelis@digital.hmrc.gov.uk",statusVerified))

      val allDevs: Seq[((String, String, String, String), Int)] = developers.zipWithIndex

      assertDevelopersList(allDevs)

      When("I select verified from the status filter drop down")
      DeveloperPage.selectByStatus(VERIFIED)

      Then("all verified developers are displayed successfully")
      val developers2 = List(("Adam","Cooper","adam.cooper@digital.hmrc.gov.uk",statusVerified),
        ("admin", "test", "admin2@email.com",statusVerified),
        ("admin", "test", "admin@email.com",statusVerified),
        ("Aivars", "Smaukstelis", "aivars.smaukstelis@digital.hmrc.gov.uk",statusVerified))

      val verifiedDevs = developers2.zipWithIndex

      assertDevelopersList(verifiedDevs)

      When("I select unverified from the status filter drop down")
      DeveloperPage.selectByStatus(UNVERIFIED)

      Then("all the unverified developers are displayed")
      val developers3 = List(("Joe","Bloggs","joe.bloggs@gmail.com",statusUnverified),
                             ("collaborator", "qa5", "unverifiedcollaboratorapisubsc@mailinator.com",statusUnverified),
                             ("collaborator", "qa5", "unverifiedcollaboratorapp@mailinator.com",statusUnverified))

      val unverifiedDevs = developers3.zipWithIndex
      assertDevelopersList(unverifiedDevs)

      When("I select not registered from the status filter drop down")
      DeveloperPage.selectByStatus(NOTREGISTERED)

      Then("all the unregistered developers are displayed")
      val developers4 = List(("n/a", "n/a", "andywheeler@mailinator.com", statusUnregistered))
      val unregisteredDev = developers4.zipWithIndex
      assertDevelopersList(unregisteredDev)

    }

    scenario("Ensure a user can view all developers who have no subscription to an API", Tag("NonSandboxTest")){

      Given("I have successfully logged in to the API Gatekeeper and I am on the Developers page")
//      stubApplicationList
//      stubApiDefinition()
//      stubFor(get(urlEqualTo("/developers/all"))
//        .willReturn(aResponse().withBody(allUsers).withStatus(200)))
//      stubNoAPISubscription
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDevelopers
      on(DeveloperPage)

      When("I select no subscription from the filter drop down")
      DeveloperPage.selectBySubscription(NOSUBSCRIPTION)
      DeveloperPage.selectByStatus(ALL)

      Then("all verified and unverified developers are displayed and sorted correctly")
      val developers = List(("Ananth", "AV", "ananth.vadiraj@digital.hmrc.gov.uk", statusVerified),
        ("n/a", "n/a", "anything@anything.com",statusUnregistered),
        ("n/a", "n/a", "anythingtest@mailinator.com", statusUnregistered),
        ("Test", "User5", "apiplatform.test.user.5@mailinator.com", statusVerified))

      val allDevs: Seq[((String, String, String, String), Int)] = developers.zipWithIndex

      assertDevelopersList(allDevs)

      When("I select verified from the status filter drop down")
      DeveloperPage.selectByStatus(VERIFIED)

      Then("all verified developers and collaborators are successfully displayed")
      val developers2 = List(("Ananth", "AV", "ananth.vadiraj@digital.hmrc.gov.uk", statusVerified),
        ("Test", "User5", "apiplatform.test.user.5@mailinator.com", statusVerified))

      val verifiedDevs = developers2.zipWithIndex
      assertDevelopersList(verifiedDevs)

      When("I select unverified from the status filter drop down")
      DeveloperPage.selectByStatus(UNVERIFIED)

      Then("all unverified developers are displayed")
      val developers3 = List(("Dev", "Hub", "devhubtest@mailinator.com",statusUnverified))
      val unverifiedDevs = developers3.zipWithIndex
      assertDevelopersList(unverifiedDevs)

      When("I select not registered from the status filter drop down")
      DeveloperPage.selectByStatus(NOTREGISTERED)

      Then("all unregistered developers are displayed")
      val developers4 = List(("n/a", "n/a", "anything@anything.com", statusUnregistered))
      val unregisteredDev = developers4.zipWithIndex
      assertDevelopersList(unregisteredDev)
    }

    scenario("Ensure a user can view all developers who has one or more application", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper and I am on the Developers page")
//      stubApplicationList
//      stubApiDefinition()
//      stubFor(get(urlEqualTo("/developers/all"))
//        .willReturn(aResponse().withBody(allUsers).withStatus(200)))
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDevelopers
      on(DeveloperPage)

      When("I select one or more applications from the filter drop down")
      DeveloperPage.selectBySubscription(ONEORMOREAPPLICATIONS)

      Then("all verified developers and unverified developers are displayed and sorted correctly")
      val developers = List(("Adam","Cooper","adam.cooper@digital.hmrc.gov.uk",statusVerified),
        ("admin", "test", "admin2@email.com",statusVerified),
        ("admin", "test", "admin@email.com",statusVerified),
        ("Aivars", "Smaukstelis", "aivars.smaukstelis@digital.hmrc.gov.uk",statusVerified))

      val allDevs: Seq[((String, String, String, String), Int)] = developers.zipWithIndex
      assertDevelopersList(allDevs)

      When("I select verified from the status filter drop down")
      DeveloperPage.selectByStatus(VERIFIED)

      Then("all verified developers are successfully displayed")
      val developers2 = List(("Adam","Cooper","adam.cooper@digital.hmrc.gov.uk",statusVerified),
        ("admin", "test", "admin2@email.com",statusVerified),
        ("admin", "test", "admin@email.com",statusVerified),
        ("Aivars", "Smaukstelis", "aivars.smaukstelis@digital.hmrc.gov.uk",statusVerified))
      val verifiedDevs = developers2.zipWithIndex
      assertDevelopersList(verifiedDevs)

      When("I select Unverified from the status filter drop down")
      DeveloperPage.selectByStatus(UNVERIFIED)

      Then("all unverified developers are displayed")
      val developers3 = List(("Dev", "Hub", "devhubtest@mailinator.com", statusUnverified),
                             ("Joe","Bloggs", "joe.bloggs@gmail.com",statusUnverified))
      val unverifiedDevs = developers3.zipWithIndex
      assertDevelopersList(unverifiedDevs)

      When("I select not registered from the Status filter drop down")
      DeveloperPage.selectByStatus(NOTREGISTERED)

      Then("All unregistered developers are displayed")
      val developers4 = List(("n/a", "n/a", "andywheeler@mailinator.com",statusUnregistered))
      val unregisteredDev = developers4.zipWithIndex
      assertDevelopersList(unregisteredDev)
    }

    scenario("Ensure a SDST can view all users who has no application", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper and I am on the Developers page")
//      stubApplicationList
//      stubApiDefinition()
//      stubFor(get(urlEqualTo("/developers/all"))
//        .willReturn(aResponse().withBody(allUsers).withStatus(200)))
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDevelopers
      on(DeveloperPage)

      When("I select no applications from the filter drop down")
      DeveloperPage.selectBySubscription(NOAPPLICATIONS)

      Then("all verified users and unverified developers are displayed and sorted correctly")
      val developers = List(("Alexander", "Browne", "alexmbrowne@gmail.com", statusVerified),
        ("Ananth" ,"Arlikatti Vadiraj", "ananth@mailinator.com",statusVerified),
        ("Andrew", "Dobby", "andy.dobby@digital.hmrc.gov.uk", statusVerified))

      val allDevs: Seq[((String, String, String, String), Int)] = developers.zipWithIndex
      assertDevelopersList(allDevs)

      When("I select verified from the status filter drop down")
      DeveloperPage.selectByStatus(VERIFIED)

      Then("all verified developers are successfully displayed")
      val developers2 = List(("Alexander", "Browne", "alexmbrowne@gmail.com", statusVerified),
        ("Ananth" ,"Arlikatti Vadiraj", "ananth@mailinator.com",statusVerified),
        ("Andrew", "Dobby", "andy.dobby@digital.hmrc.gov.uk", statusVerified))

      val verifiedDevs = developers2.zipWithIndex
      assertDevelopersList(verifiedDevs)

      When("I select unverified from the status filter drop down")
      DeveloperPage.selectByStatus(UNVERIFIED)

      Then("all unverified developers are displayed")
      val developers3 = List(("andy" ,"101", "andy101@mailinator.com",statusUnverified))
      val unverifiedDevs = developers3.zipWithIndex
      assertDevelopersList(unverifiedDevs)

      When("I select not registered from the status filter drop down")
      DeveloperPage.selectByStatus(NOTREGISTERED)

      Then("No results should be displayed")
      webDriver.findElement(By.className("dataTables_empty")).getText shouldBe "No data available in table"

      And("The email developer and copy to clipboard buttons are disabled")
      assertCopyToClipboardButtonIsDisabled("#content div a.button")
    }

    scenario("Ensure a user can view all developers who are subscribed to the Employers-PAYE API", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper and I am on the Developers page")
      stubApplicationList
      stubApiDefinition()
      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(allUsers).withStatus(200)))
      stubAPISubscription("employers-paye")
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDevelopers
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
      stubApplicationListWithNoDevelopers
      stubApiDefinition
      stubRandomDevelopers(24)
      signInGatekeeper
      on(DashboardPage)

      When("I select to navigate to the Developers page")
      DashboardPage.selectDevelopers
      on(DeveloperPage)

      Then("I should be able to view the Copy to Clipboard buttons")
      assertButtonIsPresent("#content a.button")
    }

    scenario("Ensure all developer email addresses are successfully loaded into bcc", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper")
      stubApplicationList
      stubApiDefinition
      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(allUsers).withStatus(200)))
      signInGatekeeper
      on(DashboardPage)

      When("I select to navigate to the Developers page")
      DashboardPage.selectDevelopers
      on(DeveloperPage)

      Then("the copy to clipboard button should contain all of the developers email addresses")
      verifyUsersEmailAddress("#content a.button","data-clip-text", s"$developer4; $developer8; $developer9; $developer2; $developer5; $developer7; $developer; $developer6")
    }
  }

  def stubApplicationList() = {
    stubFor(get(urlEqualTo("/gatekeeper/applications"))
      .willReturn(aResponse().withBody(approvedApplications).withStatus(200)))

    stubFor(get(urlEqualTo(s"/application")).willReturn(aResponse()
      .withBody(applicationResponse).withStatus(200)))
  }

  def stubApplicationListWithNoDevelopers() = {
    stubFor(get(urlEqualTo("/gatekeeper/applications"))
      .willReturn(aResponse().withBody(approvedApplications).withStatus(200)))

    stubFor(get(urlEqualTo(s"/application")).willReturn(aResponse()
      .withBody(applicationResponseWithNoUsers).withStatus(200)))
  }

  def stubAPISubscription(apiContext: String) = {
    stubFor(get(urlEqualTo(s"/application?subscribesTo=$apiContext"))
      .willReturn(aResponse().withBody(applicationResponse).withStatus(200)))
  }

  def stubNoAPISubscription() = {
    stubFor(get(urlEqualTo(s"/application?noSubscriptions=true"))
      .willReturn(aResponse().withBody(applicationResponsewithNoSubscription).withStatus(200)))
  }

  def stubApiDefinition() = {
    stubFor(get(urlEqualTo(s"/api-definition"))
      .willReturn(aResponse().withStatus(200).withBody(apiDefinition)))

    stubFor(get(urlEqualTo(s"/api-definition?type=private"))
      .willReturn(aResponse().withStatus(200).withBody(apiDefinition)))
  }


  def stubRandomDevelopers(randomDevelopers: Int) = {
    val developersList: String = developerListJsonGenerator(randomDevelopers).get
    stubFor(get(urlEqualTo("/developers/all"))
      .willReturn(aResponse().withBody(developersList).withStatus(200)))
  }

  def stubDevelopers(developers: Option[List[User]]) = {
    val developersJson = developers.map(userList => Json.toJson(userList)).map(Json.stringify).get
    stubFor(get(urlEqualTo("/developers/all"))
      .willReturn(aResponse().withBody(developersJson).withStatus(200)))
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
