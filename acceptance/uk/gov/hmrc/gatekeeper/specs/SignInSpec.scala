/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.specs

import org.openqa.selenium.By
import org.scalatest.matchers.should.Matchers
import org.scalatest.{GivenWhenThen, Tag}

import uk.gov.hmrc.selenium.webdriver.Driver

import uk.gov.hmrc.gatekeeper.common.SignInSugar
import uk.gov.hmrc.gatekeeper.matchers.CustomMatchers
import uk.gov.hmrc.gatekeeper.pages.ApplicationsPage
import uk.gov.hmrc.gatekeeper.stubs.AuthStub
import uk.gov.hmrc.gatekeeper.testdata.MockDataSugar

class SignInSpec extends ApiGatekeeperBaseSpec with SignInSugar with Matchers with CustomMatchers with GivenWhenThen with AuthStub {

  import MockDataSugar._

  Feature("Gatekeeper Sign in") {
    info("In order to manage uplift application requests")
    info("As a gatekeeper")
    info("I would like to sign in")

    Scenario("Sign in with invalid auth token") {
      stubPaginatedApplicationList()

    stubAuthorise()
      setupStrideAuthPage(app)
      //TODO -  Sort this out i.e. hitting page twice
      ApplicationsPage.goTo()
      ApplicationsPage.goTo()
      
      ApplicationsPage.isForbidden() shouldBe true
    }

    Scenario("Ensure developer is on Gatekeeper in Prod and they know it", Tag("NonSandboxTest")) {
      stubPaginatedApplicationList()

      stubApplicationResponse(approvedApp1, approvedApplication("application description", true))

      stubApiDefinition()
      stubAuthenticate(gatekeeperId)
      stubAuthenticateAuthorise()

      Given("The developer goes to the Gatekeeper home page")

      stubApiDefinition()
      signInGatekeeper(app)

      on(ApplicationsPage)

      Then("the application name is API Gatekeeper")
      ApplicationsPage.getApplicationName() shouldBe "HMRC API Gatekeeper"

      And("the browser window title is HMRC API Gatekeeper - Applications")
      val actualApplicationTitle = Driver.instance.getTitle
      actualApplicationTitle shouldBe "HMRC API Gatekeeper - Applications"
    }

    ignore("Cookie banner is displayed on the top of the page when user visits the website first time", Tag("NonSandboxTest")) {
      stubPaginatedApplicationList()

      stubApiDefinition()

      Given("The developer goes to the Gatekeeper home page")
      signInGatekeeper(app)

      on(ApplicationsPage)

      Then("the cookie banner is displayed at the ")
      val cookieBanner = Driver.instance.findElement(By.id("global-cookie-message")).getLocation.toString
      cookieBanner should include("(0, ")
    }
  }

}
