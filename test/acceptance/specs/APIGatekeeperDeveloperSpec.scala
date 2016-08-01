/*
 * Copyright 2016 HM Revenue & Customs
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

import java.net.URLEncoder

import acceptance.pages.{ApprovedPage, DashboardPage, DeveloperPage}
import acceptance.{BaseSpec, SignInSugar}
import com.github.tomakehurst.wiremock.client.WireMock._
import component.matchers.CustomMatchers
import org.scalatest.Matchers

class APIGatekeeperDeveloperSpec extends BaseSpec with SignInSugar with Matchers with CustomMatchers with MockDataSugar {

  feature("View Developer List") {

    scenario("View details of the developer") {

      stubApplicationListAndDevelopers
      //stubFor(get(urlEqualTo(s"/gatekeeper/application/$approvedApp1"))
       // .willReturn(aResponse().withBody(approvedApplication("application description", true)).withStatus(200)))

      signInGatekeeper
      on(DashboardPage)
      DashboardPage.selectDeveloperList
      on(DeveloperPage)
      DeveloperPage.bodyText should containInOrder(List(s"$devFirstName $devLastName $verifiedUser1",
                                                        s"$dev2FirstName$dev2LastName $verifiedUser2",
                                                        s"$dev3FirstName$dev3LastName $verifiedUser3"))
   }
  }

  def stubApplicationListAndDevelopers() = {
    val encodedEmail = URLEncoder.encode(adminEmail, "UTF-8")
    val encodedAdminEmails = URLEncoder.encode(s"$adminEmail,$admin2Email", "UTF-8")
    val expectedAdmins = s"""[${administrator()},${administrator(admin2Email, "Admin", "McAdmin")}]""".stripMargin

    stubFor(get(urlEqualTo("/gatekeeper/applications"))
      .willReturn(aResponse().withBody(approvedApplications).withStatus(200)))

    stubFor(get(urlEqualTo("/developers/all"))
      .willReturn(aResponse().withBody(verifiedUsers).withStatus(200)))

    stubFor(get(urlEqualTo(s"/developer?email=$encodedEmail"))
      .willReturn(aResponse().withBody(administrator()).withStatus(200)))

    stubFor(get(urlEqualTo(s"/developers?emails=$encodedAdminEmails"))
      .willReturn(aResponse().withBody(expectedAdmins).withStatus(200)))
  }


}

