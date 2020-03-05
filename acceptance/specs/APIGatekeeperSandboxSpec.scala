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

import _root_.BaseSpec
import _root_.SignInSugar
import com.github.tomakehurst.wiremock.client.WireMock._
import component.matchers.CustomMatchers
import org.openqa.selenium.By
import org.scalatest.{GivenWhenThen, Matchers, Tag, TestData}
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Mode}

import scala.io.Source

class APIGatekeeperSandboxSpec extends BaseSpec
  with SignInSugar with Matchers with CustomMatchers with MockDataSugar with GivenWhenThen {

  override def newAppForTest(testData: TestData): Application = {
    GuiceApplicationBuilder()
      .configure("run.mode" -> "Stub", "isExternalTestEnvironment" -> true)
      .in(Mode.Prod)
      .build()
  }

  feature("Strategic Sandbox - Gatekeeper - understand I am on the External Test Service") {

    info("As an admin (SDST)")
    info("I can tell that I am on the External Test Service version of Gatekeeper")
    info("So that I don't get confused about where I am")

    scenario("Ensure developer is on Gatekeeper in ET and they know it", Tag("SandboxTest")) {

      stubApplicationList()
      stubFor(get(urlEqualTo(s"/gatekeeper/application/$approvedApp1"))
        .willReturn(aResponse().withBody(approvedApplication("application description", true)).withStatus(OK)))
      val paginatedApplications = Source.fromURL(getClass.getResource("/paginated-applications.json")).mkString.replaceAll("\n", "")

      stubFor(get(urlMatching("/applications.*")).willReturn(aResponse()
        .withBody(paginatedApplications).withStatus(OK)))
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
        .willReturn(aResponse().withBody(authBody).withStatus(OK)))

      stubFor(get(urlEqualTo("/auth/authenticate/user/authorise?scope=api&role=gatekeeper"))
        .willReturn(aResponse().withStatus(OK)))

      Given("the developer goes to the Gatekeeper home page")

      signInGatekeeper

      val actualApplicationName = webDriver.findElement(By.className("header__menu__proposition-name")).getText
      var actualApplicationTitle = webDriver.getTitle

      Then("the application name is HMRC API Gatekeeper - Developer Sandbox")
      actualApplicationName shouldBe "HMRC API Gatekeeper - Developer Sandbox"

      And("the browser window title is HMRC API Gatekeeper - Developer Sandbox - Applications")
      actualApplicationTitle = webDriver.getTitle
      actualApplicationTitle shouldBe "HMRC API Gatekeeper - Developer Sandbox - Applications"
    }
  }

  def stubApplicationList() = {
    stubFor(get(urlEqualTo("/gatekeeper/applications"))
      .willReturn(aResponse().withBody(approvedApplications).withStatus(OK)))

    stubFor(get(urlEqualTo(s"/application")).willReturn(aResponse()
      .withBody(applications).withStatus(200)))
  }

  def stubApplicationSubscription() = {
    stubFor(get(urlEqualTo("/application/subscriptions"))
      .willReturn(aResponse().withBody(applicationSubscription).withStatus(OK)))
  }

  def stubApiDefinition() = {
    stubFor(get(urlEqualTo(s"/api-definition"))
      .willReturn(aResponse().withStatus(200).withBody(apiDefinition)))

    stubFor(get(urlEqualTo(s"/api-definition?type=private"))
      .willReturn(aResponse().withStatus(200).withBody(apiDefinition)))
  }
}
