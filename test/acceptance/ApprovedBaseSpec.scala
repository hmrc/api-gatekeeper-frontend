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

package acceptance

import java.net.URLEncoder

import acceptance.specs.MockDataSugar
import com.github.tomakehurst.wiremock.client.WireMock._
import component.matchers.CustomMatchers
import org.openqa.selenium.{By, NoSuchElementException, WebDriver}
import org.scalatest._

trait ApprovedBaseSpec extends BaseSpec
  with SignInSugar with Matchers with CustomMatchers with MockDataSugar {

  protected def stubApplicationListAndDevelopers() = {
    val encodedEmail = URLEncoder.encode(adminEmail, "UTF-8")
    val encodedAdminEmails = URLEncoder.encode(s"$adminEmail,$admin2Email", "UTF-8")
    val expectedAdmins = s"""[${administrator()},${administrator(admin2Email, "Admin", "McAdmin")}]""".stripMargin

    stubFor(get(urlEqualTo("/gatekeeper/applications"))
      .willReturn(aResponse().withBody(approvedApplications).withStatus(200)))

    stubFor(get(urlEqualTo(s"/developer?email=$encodedEmail"))
      .willReturn(aResponse().withBody(administrator()).withStatus(200)))

    stubFor(get(urlEqualTo(s"/developers?emails=$encodedAdminEmails"))
      .willReturn(aResponse().withBody(expectedAdmins).withStatus(200)))
  }

  private def assertApplicationRateLimitTier(isSuperUser: Boolean)(implicit webDriver: WebDriver) = {
    if (isSuperUser) {
      id("rate-limit-tier").element.text shouldBe "Rate limit tier: BRONZE"
      id("rate-limit-tier-table").element.text should containInOrder(List("BRONZE", "SILVER", "GOLD", "Save new rate limit tier"))
    } else {
      intercept[NoSuchElementException] {
        webDriver.findElement(By.id("rate-limit-tier"))
      }
      intercept[NoSuchElementException] {
        webDriver.findElement(By.id("rate-limit-tier-table"))
      }
    }
  }

  protected def assertApplicationDetails(isSuperUser: Boolean = false): Unit = {
    verifyText("data-submitter-name", s"$firstName $lastName")
    verifyText("data-submitter-email", adminEmail)
    assertApplicationRateLimitTier(isSuperUser)
    id("admins").element.text should containInOrder(List(s"$firstName $lastName", adminEmail, "Admin McAdmin", admin2Email))
    verifyText("data-submitted-on", "Submitted: 22 March 2016")
    verifyText("data-approved-on", "Approved: 05 April 2016")
    verifyText("data-approved-by", "Approved by: gatekeeper.username")
  }

}
