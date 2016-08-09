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

import acceptance.pages.{DashboardPage, DeveloperPage}
import acceptance.{BaseSpec, SignInSugar}
import com.github.tomakehurst.wiremock.client.WireMock._
import component.matchers.CustomMatchers
import org.openqa.selenium.By
import org.scalatest.{Assertions, Matchers}

class APIGatekeeperDeveloperSpec extends BaseSpec with SignInSugar with Matchers with CustomMatchers with MockDataSugar with Assertions {

  info(" AS A Product Owner")
  info("I WANT The SDST (Software Developer Support Team) to be able to select Users with an interest in a particular API")
  info("SO THAT The SDST can create and send email communications to Selected users")

  feature("API Filter for Email Recipients") {

    scenario("Ensure a user can view a list of Registered developers for a subscribing API") {

      stubApplicationListAndDevelopers

      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDeveloperList
      on(DeveloperPage)
      DeveloperPage.bodyText should containInOrder(List(s"$devFirstName $devLastName $verifiedUser1",
                                                        s"$dev2FirstName $dev2LastName $verifiedUser2",
                                                        s"$dev3FirstName $dev3LastName $verifiedUser3",
                                                        s"$dev4FirstName $dev4LastName $verifiedUser4"))

      DeveloperPage.bodyText should include("to open your external email client and create a new email with all emails as bcc.")
      assertNumberOfDevelopersPerPage(4)

   }

    scenario("Ensure a user can filter by an API Subscription") {

      stubApplicationListAndDevelopers
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDeveloperList
      on(DeveloperPage)
      DeveloperPage.bodyText should containInOrder(List(s"$devFirstName $devLastName $verifiedUser1",
                                                        s"$dev2FirstName $dev2LastName $verifiedUser2",
                                                        s"$dev3FirstName $dev3LastName $verifiedUser3",
                                                        s"$dev4FirstName $dev4LastName $verifiedUser4"))
    }

    scenario("Any API") {

      stubApplicationListAndDevelopers
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDeveloperList
      on(DeveloperPage)

    }

    scenario("None") {

      stubApplicationListAndDevelopers
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDeveloperList
      on(DeveloperPage)

    }

    scenario("No results returned for a specific API") {

      stubApplicationListAndDevelopers
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDeveloperList
      on(DeveloperPage)


    }
  }

  info("AS A Product Owner")
  info("I WANT any list of email recipients that is too large to fit on one page to be paginated")
  info("SO THAT The view of recipients is displayed in an easy to read way")

  feature("Pagination of Email Recipients") {

    scenario("Ensure that the page displays 10 developers by default") {

      stubRandomDevelopers(11)
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDeveloperList
      on(DeveloperPage)

      assertNumberOfDevelopersPerPage(10)

    }

    scenario("Ensure a user can view segments of 10, 50 and 100 results entries") {

      stubRandomDevelopers(10)   //need to stub more than 100 devs
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDeveloperList
      on(DeveloperPage)

      DeveloperPage.selectNoofRows("one")
      assertNumberOfDevelopersPerPage(10)
      assertResult(getResultEntriesCount)("Showing 1 to 10 of 100 entries")
      DeveloperPage.selectNoofRows("two")
      assertNumberOfDevelopersPerPage(50)
      assertResult(getResultEntriesCount)("Showing 1 to 50 of 100 entries")
      DeveloperPage.selectNoofRows("three")
      assertNumberOfDevelopersPerPage(100)
      assertResult(getResultEntriesCount)("Showing 1 to 100 of 100 entries")
    }

    scenario("Ensure user can navigate to Next and Previous pages to view result entries") {

      stubRandomDevelopers(30)
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDeveloperList
      on(DeveloperPage)

      // check if the Previous button is disabled
      assertLinkIsDisabled("Previous")
      assertNumberOfDevelopersPerPage(30)
      assertResult(getResultEntriesCount)("Showing 1 to 10 of 30 entries")
      DeveloperPage.showNextEntries()
      assertResult(getResultEntriesCount)("Showing 11 to 20 of 30 entries")
      DeveloperPage.showNextEntries()
      assertResult(getResultEntriesCount)("Showing 21 to 30 of 30 entries")
      // check if the Next button is disabled
      assertLinkIsDisabled("Next")
      DeveloperPage.showPreviousEntries()
      assertResult(getResultEntriesCount)("Showing 11 to 20 of 30 entries")

    }

    scenario("") {

    }
  }

  def stubApplicationListAndDevelopers() = {
    stubFor(get(urlEqualTo("/gatekeeper/applications"))
      .willReturn(aResponse().withBody(approvedApplications).withStatus(200)))

    stubFor(get(urlEqualTo("/developers/all"))
      .willReturn(aResponse().withBody(verifiedUsers).withStatus(200)))
  }

  def stubRandomDevelopers(randomUsers: Int) = {
    stubFor(get(urlEqualTo("/gatekeeper/applications"))
      .willReturn(aResponse().withBody(approvedApplications).withStatus(200)))

    stubFor(get(urlEqualTo("/developers/all"))
      .willReturn(aResponse().withBody(userListJsonGenerator(randomUsers).get).withStatus(200)))
  }

   private def assertNumberOfDevelopersPerPage(expected: Int) = {
    webDriver.findElements(By.cssSelector("tbody > tr")).size() shouldBe expected
  }

   def getResultEntriesCount() : String = {
    val resultEntriesText = webDriver.findElement(By.cssSelector("#content > .grid-layout__column.grid-layout__column--1-3.entries_status")).getText
    return resultEntriesText
  }

  private def assertLinkIsDisabled(linkText: String) = {
    webDriver.findElement(By.linkText(s"[$linkText]")).isEnabled shouldBe false
  }


}

