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

import java.net.URLEncoder

import acceptance.pages.{ApprovedPage, DashboardPage, DeveloperPage}
import acceptance.{BaseSpec, SignInSugar}
import com.github.tomakehurst.wiremock.client.WireMock._
import component.matchers.CustomMatchers
import org.openqa.selenium.By
import org.scalatest.Matchers

class APIGatekeeperDeveloperSpec extends BaseSpec with SignInSugar with Matchers with CustomMatchers with MockDataSugar {

  feature("View Developer List") {

    scenario("View details of the developer") {

      stubApplicationListAndDevelopers

      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDeveloperList
      on(DeveloperPage)
      DeveloperPage.bodyText should containInOrder(List(s"$devFirstName $devLastName $verifiedUser1",
                                                        s"$dev2FirstName$dev2LastName $verifiedUser2"))
                                                      //  s"$dev3FirstName$dev3LastName $verifiedUser3"))

      DeveloperPage.bodyText should include("to open your external email client and create a new email with all emails as bcc.")

   }

    scenario("Filter developer by Self Assessment API") {

      stubApplicationListAndDevelopers
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDeveloperList
      on(DeveloperPage)

      DeveloperPage.bodyText should containInOrder(List(s"$devFirstName $devLastName $verifiedUser1",
                                                        s"$dev2FirstName$dev2LastName $verifiedUser2"))
                                                       // s"$dev3FirstName$dev3LastName $verifiedUser3"))
    }
  }

  feature("View Page returns x number of users"){

    scenario("View 10, 50 and 100 number of developers in the Developer Page"){

      stubApplicationListAndDevelopers
      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDeveloperList
      on(DeveloperPage)

      // stub 100 users in the page

      DeveloperPage.selectNoofRows("10")

      //assert the no of users in the page and the entry status shown in the page
      assertNumberOfDevelopersDisplayedInthePage(10)
      DeveloperPage.selectNoofRows("50")
      assertNumberOfDevelopersDisplayedInthePage(50)
      DeveloperPage.selectNoofRows("100")
      assertNumberOfDevelopersDisplayedInthePage(100)

    }
  }

  feature("Pagination of email list") {

    info("AS A Product Owner")
    info("I WANT any list of email recipients that is too large to fit on one page to be paginated")
    info("SO THAT The view of recipients is displayed in an easy to read way")

    scenario("Number of pages and Page in view (e.g. Page 1 of XX)"){


    }

    scenario(""){

    }


  }

  def stubApplicationListAndDevelopers() = {
    val encodedEmail = URLEncoder.encode(adminEmail, "UTF-8")
    val encodedAdminEmails = URLEncoder.encode(s"$adminEmail,$admin2Email", "UTF-8")
    val expectedAdmins = s"""[${administrator()},${administrator(admin2Email, "Admin", "McAdmin")}]""".stripMargin

    stubFor(get(urlEqualTo("/gatekeeper/applications"))
      .willReturn(aResponse().withBody(approvedApplications).withStatus(200)))

    stubFor(get(urlEqualTo("/developers/all"))
      .willReturn(aResponse().withBody(verifiedUsers).withStatus(200)))

    stubFor(get(urlEqualTo(s"/developer?email=$encodedEmail"))
      .willReturn(aResponse().withBody(administrator()).withStatus(200)))

    stubFor(get(urlEqualTo(s"/developers?emails=$encodedAdminEmails"))
      .willReturn(aResponse().withBody(expectedAdmins).withStatus(200)))
  }

  def assertNumberOfDevelopersDisplayedInthePage(expected: Int) = {
    webDriver.findElements(By.cssSelector("tbody > tr")).size() shouldBe expected
  }

  def assertEntriesStatusInthePage(expected: String) = {
    webDriver.findElement(By.cssSelector("#content > .grid-layout__column.grid-layout__column--1-3.entries_status")).getText shouldBe expected
  }


}

