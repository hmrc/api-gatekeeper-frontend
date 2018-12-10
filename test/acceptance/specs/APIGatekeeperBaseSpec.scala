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


import acceptance.pages.{ApplicationsPage, BlockedApplicationPage, DashboardPage}
import acceptance.{BaseSpec, SignInSugar, WebPage}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo, urlMatching}
import component.matchers.CustomMatchers
import model.User
import org.scalatest.{GivenWhenThen, Matchers}
import play.api.http.Status._
import play.api.libs.json.Json

import scala.io.Source

class APIGatekeeperBaseSpec extends BaseSpec with SignInSugar with Matchers with CustomMatchers with MockDataSugar with GivenWhenThen {
  def stubApplication(application: String, developers: List[User]) = {
    stubFor(get(urlEqualTo("/gatekeeper/application/fa38d130-7c8e-47d8-abc0-0374c7f73216")).willReturn(aResponse().withBody(application).withStatus(OK)))
    stubFor(get(urlEqualTo("/application/fa38d130-7c8e-47d8-abc0-0374c7f73216")).willReturn(aResponse().withBody(application).withStatus(OK)))
    stubFor(get(urlEqualTo("/gatekeeper/application/fa38d130-7c8e-47d8-abc0-0374c7f73216/subscription")).willReturn(aResponse().withBody("[]").withStatus(OK)))
    stubFor(get(urlEqualTo("/application/fa38d130-7c8e-47d8-abc0-0374c7f73216/subscription")).willReturn(aResponse().withBody("[]").withStatus(OK)))
    stubFor(get(urlMatching(s"/developers")).willReturn(aResponse().withBody(Json.toJson(developers).toString())))
  }

  def stubBlockedApplication(application: String, developers: List[User]) = {
    stubFor(get(urlEqualTo("/gatekeeper/application/fa38d130-7c8e-47d8-abc0-0374c7f73217")).willReturn(aResponse().withBody(application).withStatus(OK)))
    stubFor(get(urlEqualTo("/application/fa38d130-7c8e-47d8-abc0-0374c7f73217")).willReturn(aResponse().withBody(application).withStatus(OK)))
    stubFor(get(urlEqualTo("/gatekeeper/application/fa38d130-7c8e-47d8-abc0-0374c7f73217/subscription")).willReturn(aResponse().withBody("[]").withStatus(OK)))
    stubFor(get(urlEqualTo("/application/fa38d130-7c8e-47d8-abc0-0374c7f73217/subscription")).willReturn(aResponse().withBody("[]").withStatus(OK)))
  }

  def stubApplicationList() = {
    stubFor(get(urlEqualTo("/gatekeeper/applications")).willReturn(aResponse().withBody(approvedApplications).withStatus(OK)))
    stubFor(get(urlEqualTo("/application")).willReturn(aResponse().withBody(applications).withStatus(OK)))
  }

  def stubApiDefinition() = {
    stubFor(get(urlEqualTo("/api-definition")).willReturn(aResponse().withStatus(OK).withBody(apiDefinition)))
    stubFor(get(urlEqualTo("/api-definition?type=private")).willReturn(aResponse().withStatus(OK).withBody(apiDefinition)))
  }

  def stubApplicationSubscription(developers: List[User]) = {
    stubFor(get(urlEqualTo("/application/subscriptions")).willReturn(aResponse().withBody(applicationSubscription).withStatus(OK)))
    stubFor(get(urlEqualTo("/application/df0c32b6-bbb7-46eb-ba50-e6e5459162ff/subscription")).willReturn(aResponse().withBody(applicationSubscriptions).withStatus(OK)))
    stubFor(get(urlMatching(s"/developers.*")).willReturn(aResponse().withBody(Json.toJson(developers).toString())))
  }

  def navigateToApplicationPageFor(applicationName: String, page: WebPage, developers: List[User]) = {
    Given("I have successfully logged in to the API Gatekeeper")
    stubApplicationList()

    val applicationsList = Source.fromURL(getClass.getResource("/resources/applications.json")).mkString.replaceAll("\n", "")

    stubFor(get(urlEqualTo("/application")).willReturn(aResponse().withBody(applicationsList).withStatus(OK)))

    stubApplicationSubscription(developers)
    stubApiDefinition()

    signInSuperUserGatekeeper
    on(ApplicationsPage)

    When("I select to navigate to the Applications page")
    DashboardPage.selectApplications()

    Then("I am successfully navigated to the Applications page where I can view all applications")
    on(ApplicationsPage)

    When(s"I select to navigate to the application named $applicationName")
    ApplicationsPage.selectByApplicationName(applicationName)

    Then(s"I am successfully navigated to the application named $applicationName")
    on(page)
  }
}
