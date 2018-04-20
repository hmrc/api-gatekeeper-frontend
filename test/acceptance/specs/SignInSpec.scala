/*
 * Copyright 2018 HM Revenue & Customs
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

import acceptance.{BaseSpec, SignInSugar}
import acceptance.pages.{DashboardPage, SignInPage}
import com.github.tomakehurst.wiremock.client.WireMock._
import component.matchers.CustomMatchers
import org.openqa.selenium.By
import org.scalatest.{GivenWhenThen, Matchers, Tag}

import scala.io.Source

class SignInSpec extends BaseSpec with SignInSugar with Matchers with CustomMatchers with MockDataSugar with GivenWhenThen {

  feature("Gatekeeper Sign in") {

    info("In order to manage uplift application requests")
    info("As a gatekeeper")
    info("I would like to sign in")

    scenario("Sign in with valid credentials") {
      val body =
        """
          |{
          | "access_token": {
          |     "authToken":"Bearer fggjmiJzyVZrR6/e39TimjqHyla3x8kmlTd",
          |     "expiry":1459365831061
          |     },
          |     "expires_in":14400,
          |     "roles":[{"scope":"api","name":"gatekeeper"}],
          |     "authority_uri":"/auth/oid/joe.test",
          |     "token_type":"Bearer"
          |}
        """.stripMargin
      stubFor(post(urlEqualTo("/auth/authenticate/user"))
        .willReturn(aResponse().withBody(body).withStatus(200)))

      stubFor(get(urlEqualTo("/auth/authenticate/user/authorise?scope=api&role=gatekeeper"))
        .willReturn(aResponse().withStatus(200)))

      stubFor(get(urlEqualTo("/gatekeeper/applications"))
        .willReturn(aResponse().withBody("[]").withStatus(200)))

      goOn(SignInPage)

      SignInPage.signIn("joe.test", "password")

      on(DashboardPage)
    }


    scenario("Sign in with invalid credentials"){
        stubFor(post(urlEqualTo("/auth/authenticate/user"))
          .willReturn(aResponse().withStatus(401)))

        goOn(SignInPage)

        SignInPage.signIn("joe.test", "password")

        on(SignInPage)
        SignInPage.isError shouldBe true
    }

    scenario("Sign in with unauthorised credentials")  {
      val body =
        """
          |{
          | "access_token": {
          |     "authToken":"Bearer fggjmiJzyVZrR6/e39TimjqHyla3x8kmlTd",
          |     "expiry":1459365831061
          |     },
          |     "expires_in":14400,
          |     "roles":[{"scope":"something","name":"gatekeeper"}],
          |     "authority_uri":"/auth/oid/joe.test",
          |     "token_type":"Bearer"
          |}
        """.stripMargin
      stubFor(post(urlEqualTo("/auth/authenticate/user"))
        .willReturn(aResponse().withBody(body).withStatus(200)))

      stubFor(get(urlEqualTo("/auth/authenticate/user/authorise?scope=api&role=gatekeeper"))
        .willReturn(aResponse().withStatus(401)))

      goOn(SignInPage)

      SignInPage.signIn("joe.test", "password")
      on(DashboardPage)
      DashboardPage.isUnauthorised shouldBe true
    }

    scenario("Ensure developer is on Gatekeeper in Prod and they know it", Tag("NonSandboxTest")) {

      stubApplicationList()
      stubFor(get(urlEqualTo(s"/gatekeeper/application/$approvedApp1"))
        .willReturn(aResponse().withBody(approvedApplication("application description", true)).withStatus(200)))
      val applicationsList = Source.fromURL(getClass.getResource("/resources/applications.json")).mkString.replaceAll("\n", "")

      stubFor(get(urlEqualTo(s"/application")).willReturn(aResponse()
        .withBody(applicationsList).withStatus(200)))

      stubApplicationSubscription
      stubApiDefinition

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
        .willReturn(aResponse().withBody(authBody).withStatus(200)))

      stubFor(get(urlEqualTo("/auth/authenticate/user/authorise?scope=api&role=gatekeeper"))
        .willReturn(aResponse().withStatus(200)))

      Given("The developer goes to the Gatekeeper home page")
      goOn(SignInPage)
      on(SignInPage)

      Then("The application name is HMRC API Gatekeeper")
      val actualApplicationName = webDriver.findElement(By.className("header__menu__proposition-name")).getText
      actualApplicationName shouldBe "HMRC API Gatekeeper"

      And("the browser window title is API Gatekeeper - Login")
      var actualApplicationTitle = webDriver.getTitle
      actualApplicationTitle shouldBe "HMRC API Gatekeeper - Login"

//      And("The application header colour is rgba(0, 94, 165, 1)")
//      val actualHeaderColour = webDriver.findElement(By.cssSelector("#wrapper div.service-info")).getCssValue("border-top-color")
//      actualHeaderColour.replace(" ", "") should include("rgba(0, 94, 165, 1)".replace(" ", ""))

      When("the users signs in")
      SignInPage.signIn("joe.test", "password")
      on(DashboardPage)

      Then("the application name is API Gatekeeper")
      actualApplicationName shouldBe "HMRC API Gatekeeper"

      And("the browser window title is HMRC API Gatekeeper - Dashboard")
      actualApplicationTitle = webDriver.getTitle
      actualApplicationTitle shouldBe "HMRC API Gatekeeper - Dashboard"

//      And("the application header colour is rgba(0, 94, 165, 1)")
//      actualHeaderColour.replace(" ", "") should include("rgba(0, 94, 165, 1)".replace(" ", ""))
    }

    scenario("Cookie banner is displayed on the top of the page when user visits the website first time", Tag("NonSandboxTest")) {

      Given("The developer goes to the Gatekeeper home page")
      goOn(SignInPage)
      on(SignInPage)

      Then("the cookie banner is displayed at the ")
      val cookieBanner = webDriver.findElement(By.id("global-cookie-message")).getLocation.toString
      cookieBanner shouldBe "(0, 0)"
    }
  }

  def stubApplicationList() = {
    stubFor(get(urlEqualTo("/gatekeeper/applications"))
      .willReturn(aResponse().withBody(approvedApplications).withStatus(200)))

    stubFor(get(urlEqualTo(s"/application")).willReturn(aResponse()
      .withBody(applications).withStatus(200)))
  }

  def stubApplicationSubscription() = {
    stubFor(get(urlEqualTo("/application/subscriptions"))
      .willReturn(aResponse().withBody(applicationSubscription).withStatus(200)))
  }

  def stubApiDefinition() = {
    stubFor(get(urlEqualTo(s"/api-definition"))
      .willReturn(aResponse().withStatus(200).withBody(apiDefinition)))

    stubFor(get(urlEqualTo(s"/api-definition?type=private"))
      .willReturn(aResponse().withStatus(200).withBody(apiDefinition)))
  }
}
