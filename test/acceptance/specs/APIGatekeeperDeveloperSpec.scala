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
import org.openqa.selenium.By
import org.scalatest.{Assertions, GivenWhenThen, Matchers}
import play.api.libs.json.Json

class APIGatekeeperDeveloperSpec extends BaseSpec with SignInSugar with Matchers with CustomMatchers with MockDataSugar with GivenWhenThen with Assertions {

  info("AS A Product Owner")
  info("I WANT The SDST (Software Developer Support Team) to be able to select Users with an interest in a particular API")
  info("SO THAT The SDST can create and send email communications to Selected users")

  feature("API Filter for Email Recipients") {

    scenario("Ensure a user can view a list of Registered developers") {

      Given("I have successfully logged in to the API gatekeeper")
      stubApplicationList
      stubDevelopersListAndAPISubscription()
      stubRandomDevelopers(11)
      signInGatekeeper
      on(DashboardPage)

      When("I select to navigate to the Developer List page")
      DashboardPage.selectDeveloperList

      Then("I am successfully navigated to the Developer Page where I can view all developer list details by default")
      on(DeveloperPage)
      assertNumberOfDevelopersPerPage(10)
      assertResult(getResultEntriesCount)("Showing 1 to 10 of 11 entries")
    }

    scenario("Ensure a SDST can view ALL verified users, verified and unverified collaborators") {

      Given("I have successfully logged in to the API gatekeeper and I am on the Developer List page")
      stubApplicationList
      stubDevelopersListAndAPISubscription
      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(developerListWithApp).withStatus(200)))
      stubAPISubscription("self-assessment")
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDeveloperList
      on(DeveloperPage)

      Then("all developers are successfully displayed and sorted correctly")
      DeveloperPage.bodyText should containInOrder(List(s"$dev2FirstName $dev2LastName $developer2 $statusVerified",
                                                        s"$dev3FirstName $dev3LastName $developer3 $statusUnverified",
                                                        s"$dev5FirstName $dev5LastName $developer5 $statusVerified",
                                                        s"$dev4FirstName $dev4LastName $developer4 $statusUnverified",
                                                        s"$devFirstName $devLastName $developer $statusVerified",
                                                        s"$dev6FirstName $dev6LastName $developer6 $statusVerified"))

      When("I select Verified from the Status filter drop down")
      DeveloperPage.selectByStatus(VERIFIED)

      Then("all the verified users and collaborators are displayed")
      DeveloperPage.bodyText should containInOrder(List(s"$dev2FirstName $dev2LastName $developer2 $statusVerified",
                                                        s"$dev5FirstName $dev5LastName $developer5 $statusVerified",
                                                        s"$devFirstName $devLastName $developer $statusVerified",
                                                        s"$dev6FirstName $dev6LastName $developer6 $statusVerified"))

      When("I select Unverified from the Status filter drop down")
      DeveloperPage.selectByStatus(UNVERIFIED)

      Then("all the verified users and collaborators are displayed")
      DeveloperPage.bodyText should containInOrder(List(s"$dev3FirstName $dev3LastName $developer3 $statusUnverified",
                                                        s"$dev4FirstName $dev4LastName $developer4 $statusUnverified"))

    }

    scenario("Ensure a SDST can view verified users, verified and unverified collaborators who has no association with an API") {

      Given("I have successfully logged in to the API gatekeeper")
      stubApplicationList
      stubDevelopersListAndAPISubscription
      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(developerListWithoutApp).withStatus(200)))
      signInGatekeeper
      on(DashboardPage)

      When("I select to navigate to the Developer List page")
      DashboardPage.selectDeveloperList
      on(DeveloperPage)

      When("I select NONE and ALL from the filter drop down")
      DeveloperPage.selectBySubscription(NONE)
      DeveloperPage.selectByStatus(ALLSTATUS)

      Then("all developers are successfully displayed and sorted correctly")
      DeveloperPage.bodyText should containInOrder(List(s"$dev11FirstName $dev11LastName $developer11 $statusVerified",
                                                        s"$dev7FirstName $dev7LastName $developer7 $statusVerified",
                                                        s"$dev9FirstName $dev9LastName $developer9 $statusUnverified",
                                                        s"$dev8FirstName $dev8LastName $developer8 $statusUnverified",
                                                        s"$dev10FirstName $dev10LastName $developer10 $statusVerified"))

      When("I select Verified from the Status filter drop down")
      DeveloperPage.selectByStatus(VERIFIED)

      Then("all verified users and collaborators are displayed successfully")

      When("I select Unverified from the Status filter drop down")
      DeveloperPage.selectByStatus(UNVERIFIED)

      Then("all unverified users and collaborators are displayed successfully")

    }

    scenario("Ensure a user who is associated with an app but has no subscription to an API are listed under NONE filter"){

      //have to stub users and collaborators with an app but no API.

    }

    scenario("Ensure a user can filter by an API Subscription") {

      Given("I have successfully logged in to the API gatekeeper and I am on the Developer List page")
      stubApplicationList
      stubDevelopersListAndAPISubscription()
      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(developerListWithApp).withStatus(200)))
      stubAPISubscription("employers-paye")
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDeveloperList
      on(DeveloperPage)
      Thread.sleep(700000)

      When("I select Employers PAYE from the API filter drop down")
      DeveloperPage.selectBySubscription(EMPLOYERSPAYE)

      Then("all developers subscribing to the Employers PAYE API are successfully displayed and sorted correctly")
      DeveloperPage.bodyText should containInOrder(List(s"$dev5FirstName $dev5LastName $developer5 $statusVerified",
                                                        s"$dev6FirstName $dev6LastName $developer6 $statusVerified"))
      assertNumberOfDevelopersPerPage(2)
      assertResult(getResultEntriesCount)("Showing 1 to 2 of 2 entries")

      When("I select verified from the Status filter drop down")
      DeveloperPage.selectByStatus(VERIFIED)

      Then("all verified users and collaborators are successfully displayed")
      DeveloperPage.bodyText should containInOrder(List(s"$dev5FirstName $dev5LastName $developer5 $statusVerified",
                                                        s"$dev6FirstName $dev6LastName $developer6 $statusVerified"))
      assertNumberOfDevelopersPerPage(2)
      assertResult(getResultEntriesCount)("Showing 1 to 2 of 2 entries")

      When("I select unverified from the Status filter drop down")
      DeveloperPage.selectByStatus(UNVERIFIED)

      Then("all unverified collaborators are displayed")

    }

    scenario("Ensure a user can view the Email and Copy to Clipboard buttons on the Developer page") {

      Given("I have successfully logged in to the API gatekeeper")
      stubApplicationList
      stubDevelopersListAndAPISubscription
      stubRandomDevelopers(24)
      signInGatekeeper
      on(DashboardPage)

      When("I select to navigate to the Developer List page")
      DashboardPage.selectDeveloperList
      on(DeveloperPage)

      Then("I should be able to view the Email and Copy to Clipboard buttons")
      assertButtonIsPresent("#content div a:nth-child(1)")
      assertButtonIsPresent("#content div a:nth-child(2)")

    }

   scenario("Ensure the correct number of developers is displayed on the Email Developers button ") {

      Given("I have successfully logged in to the API gatekeeper")
      stubApplicationList
      stubDevelopersListAndAPISubscription
      stubRandomDevelopers(24)
      signInGatekeeper
      on(DashboardPage)

      When("I select to navigate to the Developer List page")
      DashboardPage.selectDeveloperList
      on(DeveloperPage)

      Then("the Email Developers button should display the corrent number of developers matching the total number of results")
      assertTextPresent("#content div p a:nth-child(1)", "Email 24 developers")

   }

    scenario("Ensure all developer email addresses are successfully loaded into bcc") {

      Given("I have successfully logged in to the API gatekeeper")
      stubApplicationList
      stubDevelopersListAndAPISubscription
      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(developerListWithApp).withStatus(200)))
      signInGatekeeper
      on(DashboardPage)

      When("I select to navigate to the Developer List page")
      DashboardPage.selectDeveloperList
      on(DeveloperPage)

      Then("the email button should contain all of the developers email addresses")
      verifyUsersEmailAddress("a:nth-child(1).button","href", s"mailto:?bcc=$developer2;%20$developer3;%20$developer5;%20$developer4;%20$developer;%20$developer6")

      And("the copy to clipbard button should contain all of the developers email addresses")
      // have to input the copy to clipboard text

    }

  }

    info("AS A Product Owner")
    info("I WANT any list of email recipients that is too large to fit on one page to be paginated")
    info("SO THAT The view of recipients is displayed in an easy to read way")

    feature("Pagination of Email Recipients") {

      scenario("Ensure that the page displays 10 developers by default") {

        Given("I have successfully logged in to the API Gatekeeper")
        stubApplicationList
        stubDevelopersListAndAPISubscription
        stubRandomDevelopers(10)
        signInGatekeeper
        on(DashboardPage)

        When("I select to navigate to the Developer List page")
        DashboardPage.selectDeveloperList

        Then("I can view the default number of developers per page")
        on(DeveloperPage)
        assertNumberOfDevelopersPerPage(10)
        assertResult(getResultEntriesCount)("Showing 1 to 10 of 10 entries")
      }

      scenario("Ensure a user can view segments of 10, 20 and 100 results entries") {

        Given("I have successfully logged in to the API gatekeeper and I am on the Developer List page")
        stubApplicationList
        stubDevelopersListAndAPISubscription
        stubRandomDevelopers(100)
        signInGatekeeper
        on(DashboardPage)
        DashboardPage.selectDeveloperList
        on(DeveloperPage)
        assertNumberOfDevelopersPerPage(10)

        When("I select to view 20 result entries")
        DeveloperPage.selectNoofRows("20")

        Then("10 developers are successfully displayed on the page")
        assertNumberOfDevelopersPerPage(20)
        assertResult(getResultEntriesCount)("Showing 1 to 20 of 100 entries")

        When("I select to view 100 result entries")
        DeveloperPage.selectNoofRows("100")

        Then("100 developers are successfully displayed on the page")
        assertNumberOfDevelopersPerPage(100)
        assertResult(getResultEntriesCount)("Showing 1 to 100 of 100 entries")
      }

      scenario("Ensure that a user can navigate to Next and Previous pages to view result entries") {

        Given("I have successfully logged in to the API gatekeeper and I am on the Developer List page")
        stubApplicationList
        stubDevelopersListAndAPISubscription
        val developers: Option[List[User]] = userListGenerator(30).sample.map(_.sortWith((userA, userB) => userB.lastName.toLowerCase > userA.lastName.toLowerCase))
        stubDevelopers(developers)
        signInGatekeeper
        on(DashboardPage)
        DashboardPage.selectDeveloperList
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

    def stubAPISubscription(apiContext: String) = {
       stubFor(get(urlEqualTo(s"/application?subscribesTo=$apiContext"))
         .willReturn(aResponse().withBody(applicationResponsewithAPI).withStatus(200)))
    }

    def stubDevelopersListAndAPISubscription() = {
       stubFor(get(urlEqualTo(s"/application?subscribesTo="))
         .willReturn(aResponse().withBody(applicationResponse).withStatus(200)))

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
      val resultEntriesText = webDriver.findElement(By.cssSelector(".grid-layout__column--1-3.text--center")).getText
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

