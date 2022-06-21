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

import testdata.{ApplicationResponseTestData, ApplicationWithHistoryTestData, ApplicationWithSubscriptionDataTestData, StateHistoryTestData}
import pages._
import com.github.tomakehurst.wiremock.client.WireMock._
import common.CookieHelper
import play.api.http.Status._
import model._

class ApiGatekeeperApplicationReviewSpec
    extends ApiGatekeeperBaseSpec 
    with StateHistoryTestData 
    with ApplicationWithSubscriptionDataTestData 
    with ApplicationResponseTestData 
    with ApplicationWithHistoryTestData
    with utils.UrlEncoding
    with CookieHelper {



  val developers = List[RegisteredUser](RegisteredUser("holly.golightly@example.com", UserId.random, "holly", "golightly", false))

  val approveRequest =
    s"""
       |{
       |  "gatekeeperUserId":"$superUserGatekeeperId"
       |}
     """.stripMargin

  val rejectRequest =
    s"""
       |{
       |  "gatekeeperUserId":"$gatekeeperId",
       |  "reason":"A similar name is already taken by another application"
       |}
     """.stripMargin


  Feature("Approve a request to uplift an application") {
    Scenario("I see the review page and I am able to approve the uplift request") {

      Given("I have successfully logged in to the API Gatekeeper")
      stubPaginatedApplicationList()

      stubApiDefinition()
      signInSuperUserGatekeeper(app, stubPort)

      on(ApplicationsPage)
      stubApplication(pendingApprovalApplicationWithSubscriptionData.toJsonString, developers, pendingApprovalStateHistory.toJsonString, pendingApprovalApplicationId)

      When("I select to navigate to the Automated Test Application page")
      ApplicationsPage.selectByApplicationName(pendingApprovalApplicationName)

      Then("I am successfully navigated to the Automated Test Application page")
      on(ApplicationToReviewPage)

      verifyText("data-submitted-on", "22 August 2019")
      verifyText("data-submitted-by-email", "admin@example.com")
      verifyText("data-submission-contact-name", "Holly Golightly")
      verifyText("data-submission-contact-email", "holly.golightly@example.com")
      verifyText("data-submission-contact-telephone", "020 1122 3344")

      stubApplicationToReview()
      clickOnReview("review")
      on(ReviewPage(pendingApprovalApplicationId, "Application requiring approval"))
      clickOnElement("approve-app")

      stubFor(
        post(
          urlMatching(s"/application/$pendingApprovalApplicationId/approve-uplift")
        )
        .withRequestBody(equalToJson(approveRequest))
        .willReturn(aResponse().withStatus(OK))
      )

      clickOnSubmit()

      on(ApplicationToReviewPage)
    }
  }

  Feature("Reject a request to uplift an application when no action was selected") {
    Scenario("I see the review page and I cannot submit without choosing an action") {
      Given("I have successfully logged in to the API Gatekeeper")
      stubPaginatedApplicationList()

      stubApiDefinition()
      signInSuperUserGatekeeper(app, stubPort)

      on(ApplicationsPage)
      stubApplication(pendingApprovalApplicationWithSubscriptionData.toJsonString, developers, pendingApprovalStateHistory.toJsonString, pendingApprovalApplicationId)

      When("I select to navigate to the Automated Test Application page")
      ApplicationsPage.selectByApplicationName(pendingApprovalApplicationName)

      Then("I am successfully navigated to the Automated Test Application page")
      on(ApplicationToReviewPage)

      verifyText("data-submitted-on", "22 August 2019")
      verifyText("data-submitted-by-email", "admin@example.com")
      verifyText("data-submission-contact-name", "Holly Golightly")
      verifyText("data-submission-contact-email", "holly.golightly@example.com")
      verifyText("data-submission-contact-telephone", "020 1122 3344")

      stubApplicationToReview()
      clickOnReview("review")

      on(ReviewPage(pendingApprovalApplicationId, "Application requiring approval"))
      clickOnSubmit()

      on(ReviewPage(pendingApprovalApplicationId, "Application requiring approval"))
      verifyText("data-global-error", "Review the application")
    }
  }

  Feature("Reject a request to uplift an application") {
    Scenario("I see the review page and I am able to reject the uplift request with a reason") {
      Given("I have successfully logged in to the API Gatekeeper")
      stubPaginatedApplicationList()

      stubApiDefinition()
      signInSuperUserGatekeeper(app, stubPort)

      on(ApplicationsPage)
      stubApplication(pendingApprovalApplicationWithSubscriptionData.toJsonString, developers, pendingApprovalStateHistory.toJsonString, pendingApprovalApplicationId)

      When("I select to navigate to the Automated Test Application page")
      ApplicationsPage.selectByApplicationName(pendingApprovalApplicationName)

      Then("I am successfully navigated to the Automated Test Application page")
      on(ApplicationToReviewPage)

      verifyText("data-submitted-on", "22 August 2019")
      verifyText("data-submitted-by-email", "admin@example.com")
      verifyText("data-submission-contact-name", "Holly Golightly")
      verifyText("data-submission-contact-email", "holly.golightly@example.com")
      verifyText("data-submission-contact-telephone", "020 1122 3344")

      stubApplicationToReview()
      clickOnReview("review")

      on(ReviewPage(pendingApprovalApplicationId, "Application requiring approval"))
      clickOnElement("reject-app")

      stubFor(post(urlMatching(s"/application/$pendingApprovalApplicationId/reject-uplift"))
        .withRequestBody(equalToJson(rejectRequest))
        .willReturn(aResponse().withStatus(200)))
      clickOnSubmit()

      on(ReviewPage(pendingApprovalApplicationId, "Application requiring approval"))
      verifyText("data-global-error", "This field is required")
    }
  }

  def stubApplicationToReview() = {
    stubFor(get(urlEqualTo(s"/gatekeeper/application/$pendingApprovalApplicationId")).willReturn(aResponse().withBody(pendingApprovalApplicationWithHistory.toJsonString).withStatus(OK))) 
  }
}
