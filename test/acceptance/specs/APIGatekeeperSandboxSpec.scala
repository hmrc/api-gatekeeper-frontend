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

import acceptance.pages.{ApplicationPage, SignInPage}
import acceptance.{SandboxBaseSpec, SignInSugar}
import com.github.tomakehurst.wiremock.client.WireMock._
import component.matchers.CustomMatchers
import org.openqa.selenium.By
import org.scalatest.{GivenWhenThen, Matchers}
import org.scalatest.Tag

import scala.io.Source

class APIGatekeeperSandboxSpec extends SandboxBaseSpec with SignInSugar with Matchers with CustomMatchers with MockDataSugar with GivenWhenThen {

  feature("Strategic Sandbox - Gatekeeper - understand I am on the Enhanced Test Service") {

    info("As an admin (SDST)")
    info("I can tell that I am on the Enhanced Test Service version of Gatekeeper")
    info("So that I don't get confused about where I am")

    scenario("Ensure developer is on Gatekeeper in ET and they know it", Tag("SandboxTest")) {

      Given("the developer goes to the Gatekeeper home page")
      goOn(SignInPage)
      on(SignInPage)

      Then("the application name is HMRC API Gatekeeper - Developer Sandbox")
      val actualApplicationName = webDriver.findElement(By.className("header__menu__proposition-name")).getText
      actualApplicationName shouldBe "HMRC API Gatekeeper - Developer Sandbox"

      And("the browser window title is HMRC API Gatekeeper - Developer Sandbox - Login")
      var actualApplicationTitle = webDriver.getTitle
      actualApplicationTitle shouldBe "HMRC API Gatekeeper - Developer Sandbox - Login"

      And("the application header colour is rgba(40, 161, 151)")
      val actualHeaderColour = webDriver.findElement(By.cssSelector("#wrapper div.service-info")).getCssValue("border-top-color")
      actualHeaderColour.replace(" ", "") should include("rgba(40, 161, 151, 1)".replace(" ", ""))

      When("the users signs in")
      SignInPage.signIn("joe.test", "password")
      on(ApplicationPage)

      Then("the application name is HMRC API Gatekeeper - Developer Sandbox")
      actualApplicationName shouldBe "HMRC API Gatekeeper - Developer Sandbox"

      And("the browser window title is HMRC API Gatekeeper - Developer Sandbox - Applications")
      actualApplicationTitle = webDriver.getTitle
      actualApplicationTitle shouldBe "HMRC API Gatekeeper - Developer Sandbox - Applications"

      And("the application header colour is rgba(40, 161, 151)")
      actualHeaderColour.replace(" ", "") should include("rgba(40, 161, 151, 1)".replace(" ", ""))
    }

    scenario("Cookie banner is displayed on the top of the page when user first visits the website", Tag("SandboxTest")) {

      Given("The developer goes to the Gatekeeper home page")
      goOn(SignInPage)
      on(SignInPage)

      Then("the cookie banner is displayed at the very top of the page")
      val cookieBanner = webDriver.findElement(By.id("global-cookie-message")).getLocation.toString
      cookieBanner shouldBe "(0, 0)"
    }
  }
}
