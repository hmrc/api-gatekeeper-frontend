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

import java.net.URLEncoder

import acceptance.pages.{ApprovedPage, DashboardPage, ResendVerificationPage}
import acceptance.{BaseSpec, SignInSugar}
import com.github.tomakehurst.wiremock.client.WireMock._
import component.matchers.CustomMatchers
import org.scalatest.{Matchers, Tag}

class APIGatekeeperApprovedSpec extends BaseSpec with SignInSugar with Matchers with CustomMatchers with MockDataSugar {

  feature("View approved application details") {

    scenario("View details for an application in production", Tag("NonSandboxTest")) {
      signInGatekeeper
      on(DashboardPage)
      clickOnLink(s"data-view-fa9ed720-f0e1-4268-8287-e23e03ae11cd")
      on(ApprovedPage("fa9ed720-f0e1-4268-8287-e23e03ae11cd", "Agents Test"))

      verifyText("data-status", "Verified")
      clickOnLink("data-status")
      verifyText("data-summary", "The submitter has verified that they still have access to the email address associated with this application.")
      verifyText("data-description", "application description")
      assertApplicationDetails("Approved by: temp908@mailinator.com")
     }

    scenario("View details for an application pending verification", Tag("NonSandboxTest")) {
      signInGatekeeper
      on(DashboardPage)
      clickOnLink(s"data-view-0c80c22d-1a59-4923-b2ec-37bf49ef35a8")
      on(ApprovedPage("0c80c22d-1a59-4923-b2ec-37bf49ef35a8", "Freds Tax Calc"))

      verifyText("data-status", "Not Verified")
      verifyLinkPresent("resend-email", s"/gatekeeper/application/0c80c22d-1a59-4923-b2ec-37bf49ef35a8/resend-verification")
      clickOnLink("data-status")
      verifyText("data-summary", "The submitter has not verified that they still have access to the email address associated with this application.")
      verifyText("data-description", "application description")
      assertApplicationDetails("Approved by: temp909@mailinator.com")

      clickOnLink("resend-email")
      on(ResendVerificationPage("0c80c22d-1a59-4923-b2ec-37bf49ef35a8", "Freds Tax Calc"))
      verifyText("success-message", "Verification email has been sent")
      assertApplicationDetails("Approved by: temp909@mailinator.com")
    }

    scenario("View details for an application with no description", Tag("NonSandboxTest")){
      signInGatekeeper
      on(DashboardPage)
      clickOnLink(s"data-view-dd34cc71-bd1d-4ad5-aab6-5a999cd1150b")
      on(ApprovedPage("dd34cc71-bd1d-4ad5-aab6-5a999cd1150b", "Bad Application"))

      verifyText("data-description", "No description added")
      assertApplicationDetails("Approved by: temp908@mailinator.com")
    }

    scenario("Navigate back to the dashboard page", Tag("NonSandboxTest")) {
      signInGatekeeper
      on(DashboardPage)
      clickOnLink(s"data-view-fa9ed720-f0e1-4268-8287-e23e03ae11cd")
      on(ApprovedPage("fa9ed720-f0e1-4268-8287-e23e03ae11cd", "Agents Test"))
      clickOnLink("data-back-link")
      on(DashboardPage)
    }
  }

  def assertApplicationDetails(approvedBy : String) = {
    verifyText("data-submitter-name", "temp 999")
    verifyText("data-submitter-email", "temp907@mailinator.com")
    tagName("tbody").element.text should containInOrder(List(s"admin test", "admin@email.com", "admin test", "admin2@email.com"))
    verifyText("data-submitted-on", "Submitted: 13 March 2017")
    verifyText("data-approved-on", "Approved: 13 March 2017")
    verifyText("data-approved-by", approvedBy)
  }
}
