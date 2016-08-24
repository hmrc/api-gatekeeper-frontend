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

      //stubApplicationList

      stubRandomDevelopers(20, "individual-paye")

      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(developerList).withStatus(200)))

      signInGatekeeper
      on(DashboardPage)

      When("I select the developer list link on the Dashboard page")
      DashboardPage.selectDeveloperList

      Then("I am successfully navigated to the Developer Page where I can view the developer list details")
      on(DeveloperPage)
      Thread.sleep(5000)
      //DeveloperPage.bodyText should include("to open your external email client and create a new email with all emails as bcc.")
      assertNumberOfDevelopersPerPage(10)
      assertResult(getResultEntriesCount)("Showing 1 to 10 of 12 entries")
      DeveloperPage.showNextEntries()
      assertNumberOfDevelopersPerPage(2)
      assertResult(getResultEntriesCount)("Showing 11 to 12 of 12 entries")   // Remover unnecessary assertions
   }

    scenario("Ensure a user can filter by an API Subscription") {  //INDIVIDUAL PAYE\\

      stubRandomDevelopers(10, "individual-api")

      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(tempDeveloperList).withStatus(200)))

      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDeveloperList
      on(DeveloperPage)
      DeveloperPage.selectAPI(INDIVIDUALPAYE)
      DeveloperPage.bodyText should containInOrder(List(s"$devFirstName $devLastName $developer",
                                                        s"$dev2FirstName $dev2LastName $developer2",
                                                        s"$dev3FirstName $dev3LastName $developer3"))
      assertNumberOfDevelopersPerPage(3)
    }

    scenario("Ensure registered developers which are subscribing to any API are successfully displayed") {

      stubRandomDevelopers(10, "any-api")

      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(tempDeveloperList).withStatus(200)))

      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDeveloperList
      on(DeveloperPage)

      DeveloperPage.selectAPI(ANY)
      DeveloperPage.bodyText should containInOrder(List(s"$devFirstName $devLastName $developer",
                                                        s"$dev2FirstName $dev2LastName $developer2",
                                                        s"$dev3FirstName $dev3LastName $developer3"))
      assertNumberOfDevelopersPerPage(3)

    }

    scenario("Ensure registered developers which are NOT subscribing to any API are successfully displayed") {

      stubRandomDevelopers(10, "none-api") // This needs to be improved. Need a way have a developer not subscribing to an api with out stubbing to an api context

      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(tempDeveloperList).withStatus(200)))

      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDeveloperList
      on(DeveloperPage)

      DeveloperPage.selectAPI(NONE) // As per above comments, developers to be displayed here with out subscribing to the none - api context
      DeveloperPage.bodyText should containInOrder(List(s"$devFirstName $devLastName $developer",
                                                        s"$dev2FirstName $dev2LastName $developer2",
                                                        s"$dev3FirstName $dev3LastName $developer3"))
      assertNumberOfDevelopersPerPage(3)

    }

    scenario("No results returned for a specific API") {

      stubRandomDevelopers(10, "any-api")

     stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(tempDeveloperList).withStatus(200)))

      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDeveloperList
      on(DeveloperPage)

      DeveloperPage.selectAPI(INDIVIDUALPAYE)

    }
  }

  info("AS A Product Owner")
  info("I WANT any list of email recipients that is too large to fit on one page to be paginated")
  info("SO THAT The view of recipients is displayed in an easy to read way")

  feature("Pagination of Email Recipients") {

    scenario("Ensure that the page displays 10 developers by default") {

      Given("I have successfully logged in to the API Gatekeeper")
      stubRandomDevelopers(11, "individual-paye")

      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(developerList).withStatus(200)))

      signInGatekeeper
      on(DashboardPage)

      When("I select the developer list link on the Dashboard page")
      DashboardPage.selectDeveloperList

      Then("I can view the default number of developers per page")
      on(DeveloperPage)
      assertNumberOfDevelopersPerPage(10)

    }

    scenario("Ensure a user can view segments of 10, 20 and 100 results entries") {

      Given("I have successfully logged in to the API gatekeeper and I am on the Dashboard Page")
      stubRandomDevelopers(11, "individual-paye")

      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(developerList).withStatus(200)))

      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDeveloperList
      on(DeveloperPage)
     // Thread.sleep(50000)
      When("I select to view 10 result entries")
      DeveloperPage.selectNoofRows("20")

      Thread.sleep(30000)

      Then("10 developers are successfully displayed on the page")
     // assertNumberOfDevelopersPerPage()
      assertResult(getResultEntriesCount)("Showing 1 to 10 of 12 entries")

      When("I select to view 20 result entries")
      DeveloperPage.selectNoofRows("20")

      Then("20 developers are successfully displayed on the page")
      assertNumberOfDevelopersPerPage(12)
      assertResult(getResultEntriesCount)("Showing 11 to 12 of 12 entries")

      //When("I select to view 50 result entries")
      //DeveloperPage.selectNoofRows("three")

      //Then("100 developers are successfully displayed on the page")
      //assertNumberOfDevelopersPerPage(100)
      //assertResult(getResultEntriesCount)("Showing 1 to 100 of 100 entries")
    }

    scenario("Ensure user can navigate to Next and Previous pages to view result entries") {

      Given("I have successfully logged in to the API gatekeeper and I am on the Dashboard Page")

      stubRandomDevelopers(11, "individual-paye")

      stubFor(get(urlEqualTo("/developers/all"))
        .willReturn(aResponse().withBody(developerList).withStatus(200)))

      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDeveloperList
      on(DeveloperPage)

      // check if the Previous button is disabled

      //assertLinkIsDisabled("Previous")
      assertNumberOfDevelopersPerPage(10)
      assertResult(getResultEntriesCount)("Showing 1 to 10 of 12 entries")
      DeveloperPage.showNextEntries()
      assertResult(getResultEntriesCount)("Showing 11 to 12 of 12 entries")
      //DeveloperPage.showNextEntries()
      //assertResult(getResultEntriesCount)("Showing 21 to 30 of 30 entries")

      // check if the Next button is disabled
     // assertLinkIsDisabled("Next")
      DeveloperPage.showPreviousEntries()
      assertResult(getResultEntriesCount)("Showing 1 to 10 of 12 entries")

    }
  }

  def stubApplicationList() = {
    stubFor(get(urlEqualTo("/gatekeeper/applications"))
      .willReturn(aResponse().withBody(approvedApplications).withStatus(200)))

    stubFor(get(urlEqualTo(s"/application?subscribesTo=some-context")).willReturn(aResponse()
      .withBody(applicationResponse).withStatus(200)))
  }

  def stubRandomDevelopers(randomDevelopers: Int, apicontext: String) = {
    stubFor(get(urlEqualTo("/gatekeeper/applications"))
      .willReturn(aResponse().withBody(approvedApplications).withStatus(200)))

    stubFor(get(urlEqualTo(s"/application")).willReturn(aResponse()
      .withBody(applicationResponse).withStatus(200)))

    stubFor(get(urlEqualTo(s"/application?subscribesTo=$apicontext")).willReturn(aResponse()
      .withBody(applicationResponse).withStatus(200)))

    stubFor(get(urlEqualTo(s"/api-definition")).willReturn(aResponse().withStatus(200).withBody(apiDefinition)))

//    stubFor(get(urlEqualTo("/developers/all"))
//      .willReturn(aResponse().withBody(developerListJsonGenerator(randomDevelopers).get).withStatus(200)))
  }

   private def assertNumberOfDevelopersPerPage(expected: Int) = {
    webDriver.findElements(By.cssSelector("tbody > tr")).size() shouldBe expected
  }

   private def getResultEntriesCount() : String = {
    val resultEntriesText = webDriver.findElement(By.cssSelector(".grid-layout__column--1-3.text--center")).getText
    return resultEntriesText
  }

  private def assertLinkIsDisabled(link : String) = {
    webDriver.findElement(By.linkText(s"[$link]"))

  }


}

