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
import acceptance.pages.{DashboardPage, DeveloperPage}
import acceptance.{BaseSpec, SignInSugar}
import com.github.tomakehurst.wiremock.client.WireMock._
import component.matchers.CustomMatchers
import org.openqa.selenium.By
import org.scalatest.{Assertions, GivenWhenThen, Matchers}

class APIGatekeeperDeveloperSpec extends BaseSpec with SignInSugar with Matchers with CustomMatchers with MockDataSugar with GivenWhenThen with Assertions {

  info("AS A Product Owner")
  info("I WANT The SDST (Software Developer Support Team) to be able to select Users with an interest in a particular API")
  info("SO THAT The SDST can create and send email communications to Selected users")

  feature("API Filter for Email Recipients") {

    scenario("Ensure a user can view a list of Registered developers") {

      Given("I have successfully logged in to the API gatekeeper")
      stubApplicationList
      stubAPISubscription("individual-paye")
      stubRandomDevelopers(10)
      signInGatekeeper
      on(DashboardPage)

      When("I select the developer list link on the Dashboard page")
      DashboardPage.selectDeveloperList

      Then("I am successfully navigated to the Developer Page where I can view all developer list details by default")
      on(DeveloperPage)
      assertNumberOfDevelopersPerPage(10)
      assertResult(getResultEntriesCount)("Showing 1 to 10 of 10 entries")
    }

    scenario("Ensure a user can filter by an API Subscription") {

      Given("I have successfully logged in to the API gatekeeper and I am on the Developer List page")
      stubApplicationList
      stubAPISubscription("individual-paye")
      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(developerList).withStatus(200)))
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDeveloperList
      on(DeveloperPage)

      When("I select Individual PAYE from the API filter drop down")
      DeveloperPage.selectAPI(INDIVIDUALPAYE)

      Then("All developers subscribing to the Individual PAYE API are successfully displayed")
      DeveloperPage.bodyText should containInOrder(List(s"$devFirstName $devLastName $developer $statusVerified",
        s"$dev2FirstName $dev2LastName $developer2 $statusVerified",
        s"$dev3FirstName $dev3LastName $developer3 $statusUnverified",
        s"$dev4FirstName $dev4LastName $developer4 $statusUnverified"))
      assertNumberOfDevelopersPerPage(4)
      assertResult(getResultEntriesCount)("Showing 1 to 4 of 4 entries")
    }

    scenario("Ensure registered developers which are subscribing to any API are successfully displayed") {

      Given("I have successfully logged in to the API gatekeeper and I am on the Developer List page")
      stubApplicationList
      stubAPISubscription("any-api")
      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(developerList).withStatus(200)))
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDeveloperList
      on(DeveloperPage)

      When("I select Any from the API filter drop down")
      DeveloperPage.selectAPI(ANY)

      Then("All developers subscribing to any API are successfully displayed")
      DeveloperPage.bodyText should containInOrder(List(s"$devFirstName $devLastName $developer $statusVerified",
        s"$dev2FirstName $dev2LastName $developer2 $statusVerified",
        s"$dev3FirstName $dev3LastName $developer3 $statusUnverified",
        s"$dev4FirstName $dev4LastName $developer4 $statusUnverified"))
      assertNumberOfDevelopersPerPage(4)
      assertResult(getResultEntriesCount)("Showing 1 to 4 of 4 entries")
    }

    ignore("Ensure registered developers which are NOT subscribing to any API are successfully displayed") {

      // This needs to be improved. Need a way have a developer not subscribing to an api with out stubbing to an api context
      Given("I have successfully logged in to the API gatekeeper and I am on the Developer List page")
      stubApplicationList
      stubAPISubscription("none")
      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(developerList).withStatus(200)))
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDeveloperList
      on(DeveloperPage)

      When("I select None from the API filter drop down")
      DeveloperPage.selectAPI(NONE) // As per above comments, developers to be displayed here with out subscribing to the none - api context

      Then("All developers who are not subscribing to an API are successfully displayed")
      DeveloperPage.bodyText should containInOrder(List(s"$devFirstName $devLastName $developer $statusVerified",
        s"$dev2FirstName $dev2LastName $developer2 $statusVerified",
        s"$dev3FirstName $dev3LastName $developer3 $statusUnverified",
        s"$dev4FirstName $dev4LastName $developer4 $statusUnverified"))
      assertNumberOfDevelopersPerPage(4)
      assertResult(getResultEntriesCount)("Showing 1 to 4 of 4 entries")
    }

        ignore("No results returned for a specific API") {
          //NOT YET IMPLEMENTED

          Given("I have successfully logged in to the API gatekeeper and I am on the Developer List page")
          stubApplicationList
          stubAPISubscription("none-api")
          stubFor(get(urlEqualTo("/developers/all"))
            .willReturn(aResponse().withBody(developerList).withStatus(200)))
          signInGatekeeper
          on(DashboardPage)
          DashboardPage.selectDeveloperList
          on(DeveloperPage)

          When("I select National Insurance from the API filter drop down")
          DeveloperPage.selectAPI(NONE)

          Then("No subscribing developers are displayed for the National Insurance API")
          //Assert correct page / user friendly message is displayed: There are no developers for your selected filter.
          //                                                          Try selecting a different filter
        }
      }

    info("AS A Product Owner")
    info("I WANT any list of email recipients that is too large to fit on one page to be paginated")
    info("SO THAT The view of recipients is displayed in an easy to read way")

    feature("Pagination of Email Recipients") {

      scenario("Ensure that the page displays 10 developers by default") {

        Given("I have successfully logged in to the API Gatekeeper")
        stubApplicationList
        stubAPISubscription("individual-paye")
        stubRandomDevelopers(10)
        signInGatekeeper
        on(DashboardPage)

        When("I select the developer list link on the Dashboard page")
        DashboardPage.selectDeveloperList

        Then("I can view the default number of developers per page")
        on(DeveloperPage)
        assertNumberOfDevelopersPerPage(10)
        assertResult(getResultEntriesCount)("Showing 1 to 10 of 10 entries")
      }

      scenario("Ensure a user can view segments of 10, 20 and 100 results entries") {

        Given("I have successfully logged in to the API gatekeeper and I am on the Developer List page")
        stubApplicationList
        stubAPISubscription("individual-paye")
        stubRandomDevelopers(100)
        signInGatekeeper
        on(DashboardPage)
        //Thread.sleep(500000)
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
        stubAPISubscription("individual-paye")
        stubRandomDevelopers(30)
        signInGatekeeper
        on(DashboardPage)
        DashboardPage.selectDeveloperList
        on(DeveloperPage)
        assertNumberOfDevelopersPerPage(10)
        assertLinkIsDisabled("Previous")
        assertResult(getResultEntriesCount)("Showing 1 to 10 of 30 entries")

        When("I select to to view the the next set of result entries")
        DeveloperPage.showNextEntries()

        Then("The the page successfully displays the correct subsequent set of developers")
        assertNumberOfDevelopersPerPage(10)
        assertResult(getResultEntriesCount)("Showing 11 to 20 of 30 entries")

        When("I select to to view the the last set of result entries")
        DeveloperPage.showNextEntries()

        Then("The the page successfully displays the last subsequent set of developers")
        assertResult(getResultEntriesCount)("Showing 21 to 30 of 30 entries")
        assertNumberOfDevelopersPerPage(10)
        assertLinkIsDisabled("Next")

        When("I select to to view the the previous set of result entries")
        DeveloperPage.showPreviousEntries()

        Then("The page successfully displays the previous set of developers")
        assertNumberOfDevelopersPerPage(10)
        assertResult(getResultEntriesCount)("Showing 11 to 20 of 30 entries")
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
        .willReturn(aResponse().withBody(applicationResponse).withStatus(200)))

      stubFor(get(urlEqualTo(s"/api-definition"))
        .willReturn(aResponse().withStatus(200).withBody(apiDefinition)))
    }

    def stubRandomDevelopers(randomDevelopers: Int) = {
      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(developerListJsonGenerator(randomDevelopers).get).withStatus(200)))
    }

    private def assertNumberOfDevelopersPerPage(expected: Int) =
    {
      webDriver.findElements(By.cssSelector("tbody > tr")).size() shouldBe expected
    }

    private def getResultEntriesCount(): String =
    {
      val resultEntriesText = webDriver.findElement(By.cssSelector(".grid-layout__column--1-3.text--center")).getText
      return resultEntriesText
    }

    private def assertLinkIsDisabled(link: String) =
    {
      assertResult(find(linkText(s"[$link]")).isDefined)(false)
    }
}

