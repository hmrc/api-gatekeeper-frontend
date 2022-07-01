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

package common

import com.github.tomakehurst.wiremock.client.WireMock._
import org.openqa.selenium.WebDriver
import play.api.http.Status.{OK, SEE_OTHER}
import pages.ApplicationsPage
import play.api.Application
import utils.MockCookies

trait SignInSugar extends NavigationSugar {
  val gatekeeperId: String = "joe.test"
  val superUserGatekeeperId: String = "maxpower"
  val adminUserGatekeeperId: String = "supermaxpower"

  val requestJsonForUser =
    """
      |{
      |  "authorise": [
      |    {
      |      "$or": [
      |        {
      |          "identifiers": [],
      |          "state": "Activated",
      |          "enrolment": "admin-role"
      |        },
      |        {
      |          "identifiers": [],
      |          "state": "Activated",
      |          "enrolment": "super-user-role"
      |        },
      |        {
      |          "identifiers": [],
      |          "state": "Activated",
      |          "enrolment": "user-role"
      |        }
      |      ]
      |    }
      |  ],
      |  "retrieve": [
      |    "optionalName",
      |    "authorisedEnrolments"
      |  ]
      |}
    """.stripMargin

  val requestJsonForSuperUser =
    """
      |{
      |  "authorise": [
      |    {
      |      "$or": [
      |        {
      |          "identifiers": [],
      |          "state": "Activated",
      |          "enrolment": "admin-role"
      |        },
      |        {
      |          "identifiers": [],
      |          "state": "Activated",
      |          "enrolment": "super-user-role"
      |        }
      |      ]
      |    }
      |  ],
      |  "retrieve": [
      |    "optionalName",
      |    "authorisedEnrolments"
      |  ]
      |}
    """.stripMargin

  val requestJsonForAdmin =
    """
      |{
      |  "authorise": [
      |     {
      |       "identifiers": [],
      |       "state": "Activated",
      |       "enrolment": "admin-role"
      |     }
      |  ],
      |  "retrieve": [
      |    "optionalName",
      |    "authorisedEnrolments"
      |  ]
      |}
    """.stripMargin


  def signInGatekeeper(app: Application)(implicit webDriver: WebDriver) = {

    val responseJson =
      s"""{
         |  "optionalName": {"name":"$gatekeeperId","lastName":"Smith"},
         |  "authorisedEnrolments": [{"key": "user-role"}]
         |}""".stripMargin

    setupAuthCall(requestJsonForUser, responseJson)
    setupStrideAuthPage(app)

    go(ApplicationsPage)// 1st call will redirect to stride auth but we've stubbed out response to set cookie, still need to hit this route though
    go(ApplicationsPage)// now we have a valid session cookie we should be fine and normal auth stub should work as normal
  }

  def signInSuperUserGatekeeper(app: Application)(implicit webDriver: WebDriver) = {

    val responseJson =
      s"""{
         |  "optionalName": {"name":"$superUserGatekeeperId","lastName":"Smith"},
         |  "authorisedEnrolments": [{"key": "super-user-role"}]
         |}""".stripMargin

    setupAuthCall(requestJsonForUser, responseJson)
    setupAuthCall(requestJsonForSuperUser, responseJson)
    setupStrideAuthPage(app)

    go(ApplicationsPage)// 1st call will redirect to stride auth but we've stubbed out response to set cookie, still need to hit this route though
    go(ApplicationsPage)// now we have a valid session cookie we should be fine and normal auth stub should work as normal

  }

  def signInAdminUserGatekeeper(app: Application)(implicit webDriver: WebDriver) = {

    val responseJson =
      s"""{
         |  "optionalName": {"name":"$adminUserGatekeeperId","lastName":"Smith"},
         |  "authorisedEnrolments": [{"key": "admin-role"}]
         |}""".stripMargin

    setupAuthCall(requestJsonForUser, responseJson)
    setupAuthCall(requestJsonForSuperUser, responseJson)
    setupAuthCall(requestJsonForAdmin, responseJson)
    setupStrideAuthPage(app)

    go(ApplicationsPage)// 1st call will redirect to stride auth but we've stubbed out response to set cookie, still need to hit this route though
    go(ApplicationsPage)// now we have a valid session cookie we should be fine and normal auth stub should work as normal
  }

  private def setupAuthCall(requestJson: String, responseJson: String) = {
    stubFor(post(urlPathEqualTo("/auth/authorise"))
      .withRequestBody(equalToJson(requestJson))
      .willReturn(aResponse()
        .withBody(responseJson)
        .withStatus(OK)))
  }

  def setupStrideAuthPage(app: Application) ={
    stubFor(get(urlPathEqualTo("/stride/sign-in"))
      .willReturn(aResponse()
        .withHeader("LOCATION", s"/api-gatekeeper")
        .withHeader("SET-COOKIE", s"mdtp=${MockCookies.makeCookieValue(app)}; SameSite=Lax; Path=/; HTTPOnly")
        .withStatus(SEE_OTHER)))
  }
}
