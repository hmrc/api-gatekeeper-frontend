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

import acceptance.pages.{ApprovedPage, DashboardPage, ResendVerificationPage}
import acceptance.ApprovedBaseSpec
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.Tag

class APIGatekeeperApprovedSpec extends ApprovedBaseSpec {

  feature("View approved application details") {

    scenario("View details for an application in production", Tag("NonSandboxTest")) {

      stubApplicationListAndDevelopers
      stubFor(get(urlEqualTo(s"/gatekeeper/application/$approvedApp1"))
        .willReturn(aResponse().withBody(approvedApplication("application description", true)).withStatus(200)))

      signInGatekeeper
      on(DashboardPage)
      clickOnLink(s"data-view-$approvedApp1")
      on(ApprovedPage(approvedApp1, "Application"))

      verifyText("data-status", "Verified")
      clickOnLink("data-status")
      verifyText("data-summary", "The submitter has verified that they still have access to the email address associated with this application.")
      verifyText("data-description", "application description")
      assertApplicationDetails()
     }

    scenario("View details for an application pending verification", Tag("NonSandboxTest")) {

      stubApplicationListAndDevelopers
      stubFor(get(urlEqualTo(s"/gatekeeper/application/$approvedApp1"))
        .willReturn(aResponse().withBody(approvedApplication("application description")).withStatus(200)))
      stubFor(post(urlMatching(s"/application/$approvedApp1/resend-verification"))
        .willReturn(aResponse().withStatus(204)))

      signInGatekeeper
      on(DashboardPage)
      clickOnLink(s"data-view-$approvedApp1")
      on(ApprovedPage(approvedApp1, "Application"))

      verifyText("data-status", "Not Verified")
      verifyLinkPresent("resend-email", s"/gatekeeper/application/$approvedApp1/resend-verification")
      clickOnLink("data-status")
      verifyText("data-summary", "The submitter has not verified that they still have access to the email address associated with this application.")
      verifyText("data-description", "application description")
      assertApplicationDetails()

      clickOnLink("resend-email")
      on(ResendVerificationPage(approvedApp1, "Application"))
      verifyText("success-message", "Verification email has been sent")
      assertApplicationDetails()
    }

    scenario("View details for an application with no description", Tag("NonSandboxTest")){

      stubApplicationListAndDevelopers
      stubFor(get(urlEqualTo(s"/gatekeeper/application/$approvedApp1"))
        .willReturn(aResponse().withBody(approvedApplication()).withStatus(200)))

      signInGatekeeper
      on(DashboardPage)
      clickOnLink(s"data-view-$approvedApp1")
      on(ApprovedPage(approvedApp1, "Application"))

      verifyText("data-description", "No description added")
      assertApplicationDetails()
    }

    scenario("Navigate back to the dashboard page", Tag("NonSandboxTest")) {
      stubApplicationListAndDevelopers
      stubFor(get(urlEqualTo(s"/gatekeeper/application/$approvedApp1"))
        .willReturn(aResponse().withBody(approvedApplication()).withStatus(200)))

      signInGatekeeper
      on(DashboardPage)
      clickOnLink(s"data-view-$approvedApp1")
      on(ApprovedPage(approvedApp1, "Application"))
      clickOnLink("data-back-link")
      on(DashboardPage)
    }
  }

}
