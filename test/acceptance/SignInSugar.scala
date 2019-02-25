/*
 * Copyright 2019 HM Revenue & Customs
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

import acceptance.pages.ApplicationsPage
import com.github.tomakehurst.wiremock.client.WireMock._
import org.openqa.selenium.WebDriver
import play.api.http.Status.OK

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
      |    "name",
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
      |    "name",
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
      |    "name",
      |    "authorisedEnrolments"
      |  ]
      |}
    """.stripMargin


  def signInGatekeeper()(implicit webDriver: WebDriver) = {

    val responseJson =
      s"""{
         |  "name": {"name":"$gatekeeperId","lastName":"Smith"},
         |  "authorisedEnrolments": [{"key": "user-role"}]
         |}""".stripMargin

    setupAuthCall(requestJsonForUser, responseJson)

    go(ApplicationsPage)

  }

  def signInSuperUserGatekeeper()(implicit webDriver: WebDriver) = {

    val responseJson =
      s"""{
         |  "name": {"name":"$superUserGatekeeperId","lastName":"Smith"},
         |  "authorisedEnrolments": [{"key": "super-user-role"}]
         |}""".stripMargin

    setupAuthCall(requestJsonForUser, responseJson)
    setupAuthCall(requestJsonForSuperUser, responseJson)

    go(ApplicationsPage)

  }

  def signInAdminUserGatekeeper()(implicit webDriver: WebDriver) = {

    val responseJson =
      s"""{
         |  "name": {"name":"$adminUserGatekeeperId","lastName":"Smith"},
         |  "authorisedEnrolments": [{"key": "admin-role"}]
         |}""".stripMargin

    setupAuthCall(requestJsonForUser, responseJson)
    setupAuthCall(requestJsonForSuperUser, responseJson)
    setupAuthCall(requestJsonForAdmin, responseJson)

    go(ApplicationsPage)
  }

  private def setupAuthCall(requestJson: String, responseJson: String)(implicit webDriver: WebDriver) = {
    stubFor(post(urlPathEqualTo("/auth/authorise"))
      .withRequestBody(equalToJson(requestJson.toString))
      .willReturn(aResponse()
        .withBody(responseJson)
        .withStatus(OK)))
  }
}
