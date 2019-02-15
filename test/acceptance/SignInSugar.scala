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

  def signInGatekeeper()(implicit webDriver: WebDriver) = {
    signInUser(gatekeeperId)
  }

  def signInSuperUserGatekeeper()(implicit webDriver: WebDriver) = {
    signInUser(superUserGatekeeperId)
  }

  def signInUser(id: String)(implicit webDriver: WebDriver) = {

    val requestJson =
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

    // TODO: I had to pass the id passed as the response name (well name) as this is what is expected / needed for other calls (as the gatekeeper id)
    // Check with Gurpreet. How does this work. Is it ok. Some explritory testing around this
    val responseJson =
      s"""{
        |  "name": {"name":"$id","lastName":"Smith"},
        |  "authorisedEnrolments": []
        |}""".stripMargin


    stubFor(post(urlPathEqualTo("/auth/authorise"))
      //.withRequestBody(equalTo(requestJson.toString)) // TODO - Should probably check this.
      .willReturn(aResponse()
      .withBody(responseJson)
      .withStatus(OK)))

    go(ApplicationsPage)
  }
}
