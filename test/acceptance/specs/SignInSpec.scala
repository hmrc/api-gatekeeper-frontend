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

import acceptance.{BaseSpec, SignInSugar}
import acceptance.pages.{DashboardPage, SignInPage}
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.common.Json
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import component.matchers.CustomMatchers
import org.openqa.selenium.By
import org.scalatest.{GivenWhenThen, Matchers, Tag}
import utils.MessClient

class SignInSpec extends BaseSpec with SignInSugar with Matchers with CustomMatchers with GivenWhenThen with MessClient {

  feature("Gatekeeper Sign in") {

    info("In order to manage uplift application requests")
    info("As a gatekeeper")
    info("I would like to sign in")

    scenario("Sign in with valid credentials", Tag("NonSandboxTest")) {
      goOn(SignInPage)
      SignInPage.signIn("joe.test", "password123")
      on(DashboardPage)
    }

    scenario("Sign in with invalid credentials", Tag("NonSandboxTest")){
      val stubMapping: StubMapping = post(urlEqualTo("/auth/authenticate/user")).atPriority(1)
          .willReturn(aResponse().withStatus(401)).build()

      val invalidCredentials: String = Json.write(stubMapping)

      val stubInvalidCredentials = postRequest("http://localhost:9999/__admin/mappings", invalidCredentials)

      import play.api.libs.json.Json

      val id = (Json.parse(stubInvalidCredentials.body) \ "id").asOpt[String].get
      goOn(SignInPage)
      SignInPage.signIn("joe.test", "password")
      on(SignInPage)
      SignInPage.isError shouldBe true

      val deleteStub = deleteRequest(s"http://localhost:9999/__admin/mappings/${id}")
      deleteStub.code shouldBe 200
    }

    scenario("Ensure developer is on Gatekeeper in Prod and they know it", Tag("NonSandboxTest")) {
      Given("The developer goes to the Gatekeeper home page")
      goOn(SignInPage)
      on(SignInPage)

      Then("The application name is HMRC API Gatekeeper")
      val actualApplicationName = webDriver.findElement(By.className("header__menu__proposition-name")).getText
      actualApplicationName shouldBe "HMRC API Gatekeeper"

      And("the browser window title is API Gatekeeper - Login")
      var actualApplicationTitle = webDriver.getTitle
      actualApplicationTitle shouldBe "HMRC API Gatekeeper - Login"

      And("The application header colour is rgba(0, 94, 165, 1)")
      val actualHeaderColour = webDriver.findElement(By.cssSelector("#wrapper div.service-info")).getCssValue("border-top-color")
      actualHeaderColour.replace(" ", "") should include("rgba(0, 94, 165, 1)".replace(" ", ""))

      When("the users signs in")
      SignInPage.signIn("joe.test", "password123")
      on(DashboardPage)

      Then("the application name is API Gatekeeper")
      actualApplicationName shouldBe "HMRC API Gatekeeper"

      And("the browser window title is HMRC API Gatekeeper - Dashboard")
      actualApplicationTitle = webDriver.getTitle
      actualApplicationTitle shouldBe "HMRC API Gatekeeper - Dashboard"

      And("the application header colour is rgba(0, 94, 165, 1)")
      actualHeaderColour.replace(" ", "") should include("rgba(0, 94, 165, 1)".replace(" ", ""))
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
}
