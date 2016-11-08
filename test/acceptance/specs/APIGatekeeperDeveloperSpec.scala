/*
 * Copyright 2016 HM Revenue & Customs
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
import component.matchers.CustomMatchers
import model.User
import org.openqa.selenium.{By}
import org.scalatest.{Assertions, GivenWhenThen, Matchers}
import play.api.libs.json.Json
import scala.collection.immutable.List

class APIGatekeeperDeveloperSpec extends BaseSpec with SignInSugar with Matchers with CustomMatchers with MockDataSugar with GivenWhenThen with Assertions {

  info("AS A Product Owner")
  info("I WANT The SDST (Software Developer Support Team) to be able to select developers with an interest in a particular API")
  info("SO THAT The SDST can create and send email communications to selected developers")

  feature("API Filter for Email Recipients") {

    scenario("Ensure a user can view a list of registered developers") {

      Given("I have successfully logged in to the API Gatekeeper")
      stubApplicationList
      stubApiDefinition()
      stubRandomDevelopers(11)
      signInGatekeeper
      on(DashboardPage)

      When("I select to navigate to the Developers page")
      DashboardPage.selectDevelopers

      Then("I am successfully navigated to the Developers page where I can view all developer list details by default")
      on(DeveloperPage)
      assertNumberOfDevelopersPerPage(10)
      assertResult(getResultEntriesCount)("Showing 1 to 10 of 16 entries")
    }

    scenario("Ensure a user can view ALL developers") {

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

      Then("all developers are successfully displayed and sorted correctly")
      DeveloperPage.bodyText should containInOrder(List(s"$dev2FirstName $dev2LastName $developer2 $statusVerified",
                                                        s"$dev5FirstName $dev5LastName $developer5 $statusUnverified",
                                                        s"$dev4FirstName $dev4LastName $developer4 $statusVerified",
                                                        s"$dev7FirstName $dev7LastName $developer7 $statusVerified",
                                                        s"$devFirstName $devLastName $developer $statusVerified",
                                                        s"$dev8FirstName $dev8LastName $developer8 $statusUnverified",
                                                        s"$dev6FirstName $dev6LastName $developer6 $statusVerified",
                                                        s"$dev9name $developer9 $statusUnregistered"))

      When("I select verified from the status filter drop down")
      DeveloperPage.selectByStatus(VERIFIED)

      Then("all the verified developers are displayed")
      DeveloperPage.bodyText should containInOrder(List(s"$dev2FirstName $dev2LastName $developer2 $statusVerified",
                                                        s"$dev4FirstName $dev4LastName $developer4 $statusVerified",
                                                        s"$dev7FirstName $dev7LastName $developer7 $statusVerified",
                                                        s"$devFirstName $devLastName $developer $statusVerified",
                                                        s"$dev6FirstName $dev6LastName $developer6 $statusVerified"))

      When("I select unverified from the status filter drop down")
      DeveloperPage.selectByStatus(UNVERIFIED)

      Then("all the unverified developers are displayed")
      DeveloperPage.bodyText should containInOrder(List(s"$dev5FirstName $dev5LastName $developer5 $statusUnverified",
                                                        s"$dev8FirstName $dev8LastName $developer8 $statusUnverified"))

      When("I select not registered from the status filter drop down")
      DeveloperPage.selectByStatus(NOTREGISTERED)

      Then("all the unregistered developers are displayed")
      DeveloperPage.bodyText should containInOrder(List(s"$dev9name $developer9 $statusUnregistered"))
    }

    scenario("Ensure a user can view all developers who are subscribed to one or more API") {

      Given("I have successfully logged in to the API Gatekeeper and I am on the Developers page")
      stubApplicationList
      stubApiDefinition
      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(allUsers).withStatus(200)))
      stubAPISubscription("employers-paye")
      stubNoAPISubscription()
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDevelopers
      on(DeveloperPage)

      When("I select one or more subscriptions from the filter drop down")
      DeveloperPage.selectBySubscription(ONEORMORESUBSCRIPTION)
      DeveloperPage.selectByStatus(ALL)

      Then("all verified and unverified developers are successfully displayed and sorted correctly")
      DeveloperPage.bodyText should containInOrder(List(s"$dev2FirstName $dev2LastName $developer2 $statusVerified",
                                                        s"$dev7FirstName $dev7LastName $developer7 $statusVerified",
                                                        s"$devFirstName $devLastName $developer $statusVerified",
                                                        s"$dev8FirstName $dev8LastName $developer8 $statusUnverified",
                                                        s"$dev9name $developer9 $statusUnregistered"))

      When("I select verified from the status filter drop down")
      DeveloperPage.selectByStatus(VERIFIED)

      Then("all verified developers are displayed successfully")
      DeveloperPage.bodyText should containInOrder(List(s"$dev2FirstName $dev2LastName $developer2 $statusVerified",
                                                        s"$dev7FirstName $dev7LastName $developer7 $statusVerified",
                                                        s"$devFirstName $devLastName $developer $statusVerified"))

      When("I select unverified from the status filter drop down")
      DeveloperPage.selectByStatus(UNVERIFIED)

      Then("all the unverified developers are displayed")
      DeveloperPage.bodyText should containInOrder(List(s"$dev8FirstName $dev8LastName $developer8 $statusUnverified"))

      When("I select not registered from the status filter drop down")
      DeveloperPage.selectByStatus(NOTREGISTERED)

      Then("all the unregistered developers are displayed")
      DeveloperPage.bodyText should containInOrder(List(s"$dev9name $developer9 $statusUnregistered"))
    }

    scenario("Ensure a user can view all developers who have no subscription to an API"){

      Given("I have successfully logged in to the API Gatekeeper and I am on the Developers page")
      stubApplicationList
      stubApiDefinition()
      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(allUsers).withStatus(200)))
      stubNoAPISubscription
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDevelopers
      on(DeveloperPage)

      When("I select no subscriptions from the filter drop down")
      DeveloperPage.selectBySubscription(NOSUBSCRIPTION)
      DeveloperPage.selectByStatus(ALL)

      Then("all verified and unverified developers are displayed and sorted correctly")
      DeveloperPage.bodyText should containInOrder(List(s"$dev5FirstName $dev5LastName $developer5 $statusUnverified",
                                                        s"$dev4FirstName $dev4LastName $developer4 $statusVerified",
                                                        s"$dev6FirstName $dev6LastName $developer6 $statusVerified",
                                                        s"$dev10name $developer10 $statusUnregistered"))

      When("I select verified from the status filter drop down")
      DeveloperPage.selectByStatus(VERIFIED)

      Then("all verified developers and collaborators are successfully displayed")
      DeveloperPage.bodyText should containInOrder(List(s"$dev4FirstName $dev4LastName $developer4 $statusVerified",
                                                        s"$dev6FirstName $dev6LastName $developer6 $statusVerified"))

      When("I select unverified from the status filter drop down")
      DeveloperPage.selectByStatus(UNVERIFIED)

      Then("all unverified developers are displayed")
      DeveloperPage.bodyText should containInOrder(List(s"$dev5FirstName $dev5LastName $developer5 $statusUnverified"))


      When("I select not registered from the status filter drop down")
      DeveloperPage.selectByStatus(NOTREGISTERED)

      Then("all unregistered developers are displayed")
      DeveloperPage.bodyText should containInOrder(List(s"$dev10name $developer10 $statusUnregistered"))
    }

    scenario("Ensure a user can view all developers who has one or more application") {

      Given("I have successfully logged in to the API Gatekeeper and I am on the Developers page")
      stubApplicationList
      stubApiDefinition()
      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(allUsers).withStatus(200)))
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDevelopers
      on(DeveloperPage)

      When("I select no applications from the filter drop down")
      DeveloperPage.selectBySubscription(ONEORMOREAPPLICATIONS)

      Then("all verified developers and unverified developers are displayed and sorted correctly")
      DeveloperPage.bodyText should containInOrder(List(s"$dev2FirstName $dev2LastName $developer2 $statusVerified",
                                                        s"$dev7FirstName $dev7LastName $developer7 $statusVerified",
                                                        s"$devFirstName $devLastName $developer $statusVerified",
                                                        s"$dev8FirstName $dev8LastName $developer8 $statusUnverified",
                                                        s"$dev9name $developer9 $statusUnregistered"))

      When("I select verified from the status filter drop down")
      DeveloperPage.selectByStatus(VERIFIED)

      Then("all verified developers are successfully displayed")
      DeveloperPage.bodyText should containInOrder(List(s"$dev2FirstName $dev2LastName $developer2 $statusVerified",
                                                        s"$dev7FirstName $dev7LastName $developer7 $statusVerified",
                                                        s"$devFirstName $devLastName $developer $statusVerified"))

      When("I select Unverified from the status filter drop down")
      DeveloperPage.selectByStatus(UNVERIFIED)

      Then("all unverified developers are displayed")
      DeveloperPage.bodyText should containInOrder(List(s"$dev8FirstName $dev8LastName $developer8 $statusUnverified"))

      When("I select not registered from the Status filter drop down")
      DeveloperPage.selectByStatus(NOTREGISTERED)

      Then("All unregistered developers are displayed")
      DeveloperPage.bodyText should containInOrder(List(s"$dev9name $developer9 $statusUnregistered"))
    }

    scenario("Ensure a SDST can view all users who has no application") {

      Given("I have successfully logged in to the API Gatekeeper and I am on the Developers page")
      stubApplicationList
      stubApiDefinition()
      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(allUsers).withStatus(200)))
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDevelopers
      on(DeveloperPage)

      When("I select no applications from the filter drop down")
      DeveloperPage.selectBySubscription(NOAPPLICATIONS)

      Then("all verified users and unverified developers are displayed and sorted correctly")
      DeveloperPage.bodyText should containInOrder(List(s"$dev5FirstName $dev5LastName $developer5 $statusUnverified",
                                                        s"$dev4FirstName $dev4LastName $developer4 $statusVerified",
                                                        s"$dev6FirstName $dev6LastName $developer6 $statusVerified"))

      When("I select verified from the status filter drop down")
      DeveloperPage.selectByStatus(VERIFIED)

      Then("all verified developers are successfully displayed")
      DeveloperPage.bodyText should containInOrder(List(s"$dev4FirstName $dev4LastName $developer4 $statusVerified",
                                                        s"$dev6FirstName $dev6LastName $developer6 $statusVerified"))

      When("I select unverified from the status filter drop down")
      DeveloperPage.selectByStatus(UNVERIFIED)

      Then("all unverified developers are displayed")
      DeveloperPage.bodyText should containInOrder(List(s"$dev5FirstName $dev5LastName $developer5 $statusUnverified"))

      When("I select not registered from the status filter drop down")
      DeveloperPage.selectByStatus(NOTREGISTERED)

      Then("No results should be displayed")
      println("No results returned for this selection")
    }

    scenario("Ensure a user can view all developers who are subscribed to the Employers-PAYE API") {

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
      DeveloperPage.bodyText should containInOrder(List(s"$dev2FirstName $dev2LastName $developer2 $statusVerified",
                                                        s"$dev7FirstName $dev7LastName $developer7 $statusVerified",
                                                        s"$devFirstName $devLastName $developer $statusVerified",
                                                        s"$dev8FirstName $dev8LastName $developer8 $statusUnverified",
                                                        s"$dev9name $developer9 $statusUnregistered"))

      When("I select verified from the status filter drop down")
      DeveloperPage.selectByStatus(VERIFIED)

      Then("all verified developers are successfully displayed")
      DeveloperPage.bodyText should containInOrder(List(s"$dev2FirstName $dev2LastName $developer2 $statusVerified",
                                                        s"$dev7FirstName $dev7LastName $developer7 $statusVerified",
                                                        s"$devFirstName $devLastName $developer $statusVerified"))

      When("I select unverified from the status filter drop down")
      DeveloperPage.selectByStatus(UNVERIFIED)

      Then("all unverified developers are displayed")
      DeveloperPage.bodyText should containInOrder(List(s"$dev8FirstName $dev8LastName $developer8 $statusUnverified"))

      When("I select not registered from the status filter drop down")
      DeveloperPage.selectByStatus(NOTREGISTERED)

      Then("all unregistered developers are displayed")
      DeveloperPage.bodyText should containInOrder(List(s"$dev9name $developer9 $statusUnregistered"))
    }

    scenario("Ensure a user can view the Email and Copy to Clipboard buttons on the Developers page") {

      Given("I have successfully logged in to the API Gatekeeper")
      stubApplicationListWithNoDevelopers
      stubApiDefinition
      stubRandomDevelopers(24)
      signInGatekeeper
      on(DashboardPage)

      When("I select to navigate to the Developers page")
      DashboardPage.selectDevelopers
      on(DeveloperPage)

      Then("I should be able to view the Email and Copy to Clipboard buttons")
      assertButtonIsPresent("#content div a:nth-child(1)")
      assertButtonIsPresent("#content div a:nth-child(2)")
    }

    scenario("Ensure the correct number of developers is displayed on the Email Developers button") {

      Given("I have successfully logged in to the API Gatekeeper")
      stubApplicationListWithNoDevelopers
      stubApiDefinition
      stubRandomDevelopers(25)
      signInGatekeeper
      on(DashboardPage)

      When("I select to navigate to the Developers page")
      DashboardPage.selectDevelopers
      on(DeveloperPage)

      Then("the Email Developers button should display the correct number of developers matching the total number of results")
      assertTextPresent("#content div p a:nth-child(1)", "Email 25 developers")
   }

    scenario("Ensure all developer email addresses are successfully loaded into bcc") {

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

      Then("the email button should contain all of the developers email addresses")
      verifyUsersEmailAddress("#content p a:nth-child(1)","href", s"mailto:?bcc=$developer2;%20$developer5;%20$developer4;%20$developer7;%20$developer;%20$developer8;%20$developer6;%20$developer9")

      And("the copy to clipboard button should contain all of the developers email addresses")
      verifyUsersEmailAddress("#content p a:nth-child(2)","onclick", s"copyTextToClipboard('$developer2; $developer5; $developer4; $developer7; $developer; $developer8; $developer6; $developer9'); return false;")
    }
  }

    info("AS A Product Owner")
    info("I WANT any list of email recipients that is too large to fit on one page to be paginated")
    info("SO THAT The view of recipients is displayed in an easy to read manner")

    feature("Pagination of Email Recipients") {

      scenario("Ensure that the page displays 10 developers by default") {

        Given("I have successfully logged in to the API Gatekeeper")
        stubApplicationListWithNoDevelopers
        stubApiDefinition
        stubRandomDevelopers(10)
        signInGatekeeper
        on(DashboardPage)

        When("I select to navigate to the Developers page")
        DashboardPage.selectDevelopers

        Then("I can view the default number of developers (10) per page")
        on(DeveloperPage)
        assertNumberOfDevelopersPerPage(10)
        assertResult(getResultEntriesCount)("Showing 1 to 10 of 10 entries")
      }

      scenario("Ensure a user can view segments of 10, 50 and 100 results entries") {

        Given("I have successfully logged in to the API Gatekeeper and I am on the Developers page")
        stubApplicationListWithNoDevelopers
        stubApiDefinition
        stubRandomDevelopers(100)
        signInGatekeeper
        on(DashboardPage)
        DashboardPage.selectDevelopers
        on(DeveloperPage)
        assertNumberOfDevelopersPerPage(10)
        assertResult(getResultEntriesCount)("Showing 1 to 10 of 100 entries")

        When("I select to view 50 result entries")
        DeveloperPage.selectNoofRows("50")

        Then("50 developers are successfully displayed on the page")
        assertNumberOfDevelopersPerPage(50)
        assertResult(getResultEntriesCount)("Showing 1 to 50 of 100 entries")

        When("I select to view 100 result entries")
        DeveloperPage.selectNoofRows("100")

        Then("100 developers are successfully displayed on the page")
        assertNumberOfDevelopersPerPage(100)
        assertResult(getResultEntriesCount)("Showing 1 to 100 of 100 entries")
      }

      scenario("Ensure that a user can navigate to Next and Previous pages to view result entries") {

        Given("I have successfully logged in to the API Gatekeeper and I am on the Developers page")
        //stubApplicationList
        stubApplicationListWithNoDevelopers
        stubApiDefinition
        val developers: Option[List[User]] = userListGenerator(30).sample.map(_.sortWith((userA, userB) => userB.lastName.toLowerCase > userA.lastName.toLowerCase))
        stubDevelopers(developers)
        signInGatekeeper
        on(DashboardPage)
        DashboardPage.selectDevelopers
        on(DeveloperPage)
        assertNumberOfDevelopersPerPage(10)
        assertLinkIsDisabled("Previous")
        assertResult(getResultEntriesCount)("Showing 1 to 10 of 30 entries")
        val first10: List[User] = developers.get.take(10)
        DeveloperPage.bodyText should containInOrder(generateUsersList(first10))

        When("I select to to view the the next set of result entries")
        DeveloperPage.showNextEntries()

        Then("the page successfully displays the correct subsequent set of developers")
        assertNumberOfDevelopersPerPage(10)
        assertResult(getResultEntriesCount)("Showing 11 to 20 of 30 entries")
        val second10 : List[User] = developers.get.slice(11,20)
        DeveloperPage.bodyText should containInOrder(generateUsersList(second10))

        When("I select to to view the the last set of result entries")
        DeveloperPage.showNextEntries()

        Then("the page successfully displays the last subsequent set of developers")
        assertResult(getResultEntriesCount)("Showing 21 to 30 of 30 entries")
        assertNumberOfDevelopersPerPage(10)
        val third10 : List[User] = developers.get.slice(21,30)
        DeveloperPage.bodyText should containInOrder(generateUsersList(third10))
        assertLinkIsDisabled("Next")

        When("I select to to view the the previous set of result entries")
        DeveloperPage.showPreviousEntries()

        Then("The page successfully displays the previous set of developers")
        assertNumberOfDevelopersPerPage(10)
        assertResult(getResultEntriesCount)("Showing 11 to 20 of 30 entries")
        DeveloperPage.bodyText should containInOrder(generateUsersList(second10))
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
      webDriver.findElements(By.cssSelector("tbody > tr")).size() shouldBe expected
    }

    private def getResultEntriesCount(): String = {
      val resultEntriesText = webDriver.findElement(By.cssSelector(".grid-layout__column--1-3.entries_status")).getText
      return resultEntriesText
    }

    private def assertLinkIsDisabled(link: String) = {
      assertResult(find(linkText(link)).isDefined)(false)
    }

    private def assertButtonIsPresent(button: String) = {
     webDriver.findElement(By.cssSelector(button)).isDisplayed shouldBe true
    }

    private def assertTextPresent(attributeName: String, expected: String) = {
      webDriver.findElement(By.cssSelector(attributeName)).getText shouldBe expected
    }

    private def generateUsersList(users : List[User]) = {
      users.map(user => s"${user.firstName} ${user.lastName} ${user.email}")
    }

    private def verifyUsersEmailAddress(button : String, attributeName : String, expected : String) {
      val emailAddresses = webDriver.findElement(By.cssSelector(button)).getAttribute(attributeName) shouldBe expected
    }

    private def verifyUsersEmail(button : String) {
      val emailAddresses = webDriver.findElement(By.cssSelector(button)).getAttribute("value")
    }

}

