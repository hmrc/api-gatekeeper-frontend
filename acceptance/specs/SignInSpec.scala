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

package specs

import matchers.CustomMatchers
import pages.ApplicationsPage
import common.SignInSugar
import com.github.tomakehurst.wiremock.client.WireMock._
import org.openqa.selenium.By
import org.scalatest.{GivenWhenThen, Tag}
import play.api.http.Status._
import org.scalatest.matchers.should.Matchers

class SignInSpec extends ApiGatekeeperBaseSpec with SignInSugar with Matchers with CustomMatchers with GivenWhenThen {

  import MockDataSugar._

  Feature("Gatekeeper Sign in") {

    info("In order to manage uplift application requests")
    info("As a gatekeeper")
    info("I would like to sign in")

    Scenario("Sign in with invalid auth token") {
      stubPaginatedApplicationList()

      stubFor(post(urlPathEqualTo("/auth/authorise"))
        .willReturn(aResponse()
          .withHeader("WWW-Authenticate", "MDTP detail=\"InsufficientEnrolments\"")
          .withStatus(UNAUTHORIZED)))
      setupStrideAuthPage(app,stubPort)
      go(ApplicationsPage)
      goOn(ApplicationsPage)

      ApplicationsPage.isForbidden shouldBe true
    }

    Scenario("Ensure developer is on Gatekeeper in Prod and they know it", Tag("NonSandboxTest")) {
      stubPaginatedApplicationList()

      stubFor(get(urlEqualTo(s"/gatekeeper/application/$approvedApp1"))
        .willReturn(aResponse().withBody(approvedApplication("application description", true)).withStatus(OK)))
      stubApiDefinition()

      val authBody =
        s"""
           |{
           | "access_token": {
           |     "authToken":"Bearer fggjmiJzyVZrR6/e39TimjqHyla3x8kmlTd",
           |     "expiry":1459365831061
           |     },
           |     "expires_in":14400,
           |     "roles":[{"scope":"api","name":"gatekeeper"}],
           |     "authority_uri":"/auth/oid/$gatekeeperId",
           |     "token_type":"Bearer"
           |}
      """.stripMargin

      stubFor(post(urlEqualTo("/auth/authenticate/user"))
        .willReturn(aResponse().withBody(authBody).withStatus(OK)))

      stubFor(get(urlEqualTo("/auth/authenticate/user/authorise?scope=api&role=gatekeeper"))
        .willReturn(aResponse().withStatus(OK)))

      Given("The developer goes to the Gatekeeper home page")

      stubApiDefinition()
      signInGatekeeper(app, stubPort)

      val actualApplicationName = webDriver.findElement(By.className("hmrc-header__service-name")).getText
      var actualApplicationTitle = webDriver.getTitle
      on(ApplicationsPage)

      Then("the application name is API Gatekeeper")
      actualApplicationName shouldBe "HMRC API Gatekeeper"

      And("the browser window title is HMRC API Gatekeeper - Applications")
      actualApplicationTitle = webDriver.getTitle
      actualApplicationTitle shouldBe "HMRC API Gatekeeper - Applications"
    }

    ignore("Cookie banner is displayed on the top of the page when user visits the website first time", Tag("NonSandboxTest")) {
      stubPaginatedApplicationList()

      stubApiDefinition()

      Given("The developer goes to the Gatekeeper home page")
      signInGatekeeper(app, stubPort)

      on(ApplicationsPage)

      Then("the cookie banner is displayed at the ")
      val cookieBanner = webDriver.findElement(By.id("global-cookie-message")).getLocation.toString
      cookieBanner should include("(0, ")
    }
  }

  def stubApplicationSubscription() = {
    stubFor(get(urlEqualTo("/application/subscriptions"))
      .willReturn(aResponse().withBody(applicationSubscription).withStatus(OK)))
  }
}
