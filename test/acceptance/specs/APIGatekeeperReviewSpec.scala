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

import acceptance.pages.{DashboardPage, ReviewPage}
import acceptance.{BaseSpec, SignInSugar}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.common.Json
import component.matchers.CustomMatchers
import org.openqa.selenium.By
import org.scalatest.{Matchers, Tag}
import utils.MessClient

class APIGatekeeperReviewSpec  extends BaseSpec with SignInSugar with Matchers with CustomMatchers with MessClient {

  val approveRequest =
    s"""
       |{
       |  "gatekeeperUserId":"$gatekeeperId"
       |}
     """.stripMargin

  val rejectRequest =
    s"""
       |{
       |  "gatekeeperUserId":"$gatekeeperId",
       |  "reason":"A similar name is already taken by another application"
       |}
     """.stripMargin

  val appPendingApprovalId1 = "d11fd0de-0ce7-4990-b33a-63a4c4dd4f2c"

  feature("Approve a request to uplift an application") {

    scenario("I see the review page and I am able to approve the uplift request", Tag("NonSandboxTest")) {
      signInGatekeeper
      on(DashboardPage)
      clickOnLink(s"data-review-$appPendingApprovalId1")
      on(ReviewPage(appPendingApprovalId1, "Friendly Taxman1"))
      clickOnSubmit()
      on(DashboardPage)
      webDriver.findElement(By.linkText("Sign out")).click()
    }

    scenario("I see the dashboard page when the request to uplift the application fails with a 412", Tag("NonSandboxTest")) {
      val stubMapping = post(urlMatching(s"/application/$appPendingApprovalId1/approve-uplift")).atPriority(1)
        .withRequestBody(equalToJson(approveRequest))
        .willReturn(aResponse().withStatus(412)).build()

      val failedUpliftRequest = Json.write(stubMapping)

      val stubFailedUpliftRequest = postRequest("http://localhost:9999/__admin/mappings", failedUpliftRequest)

      import play.api.libs.json.Json

      val id = (Json.parse(stubFailedUpliftRequest.body) \ "id").asOpt[String].get

      signInGatekeeper
      on(DashboardPage)
      clickOnLink(s"data-review-$appPendingApprovalId1")
      on(ReviewPage(appPendingApprovalId1, "Friendly Taxman1"))
      clickOnSubmit()
      on(DashboardPage)
      webDriver.findElement(By.linkText("Sign out")).click()

      val deleteStub = deleteRequest(s"http://localhost:9999/__admin/mappings/${id}")
      deleteStub.code shouldBe 200
    }
  }

  feature("Reject a request to uplift an application") {

    scenario("I see the review page and I am able to reject the uplift request with a reason", Tag("NonSandboxTest")) {
      signInGatekeeper
      on(DashboardPage)
      clickOnLink(s"data-review-$appPendingApprovalId1")
      on(ReviewPage(appPendingApprovalId1, "Friendly Taxman1"))
      clickOnElement("reject-app")
      verifyLinkPresent("data-naming-guidelines", "/api-documentation/docs/using-the-hub/name-guidelines")
      clickOnSubmit()
      on(ReviewPage(s"data-review-$appPendingApprovalId1", "Friendly Taxman1"))
      verifyText("data-global-error","This field is required")
      webDriver.findElement(By.linkText("Sign out")).click()
    }
  }
}
