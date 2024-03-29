/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.specs

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.pages._
import uk.gov.hmrc.gatekeeper.stubs.ThirdPartyApplicationStub
import uk.gov.hmrc.gatekeeper.testdata.{ApplicationResponseTestData, ApplicationWithStateHistoryTestData, ApplicationWithSubscriptionDataTestData, StateHistoryTestData}
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

class ApiGatekeeperApplicationReviewSpec
    extends ApiGatekeeperBaseSpec
    with StateHistoryTestData
    with ApplicationWithSubscriptionDataTestData
    with ApplicationResponseTestData
    with ApplicationWithStateHistoryTestData
    with UrlEncoding
    with ThirdPartyApplicationStub {

  val developers = List[RegisteredUser](RegisteredUser("holly.golightly@example.com".toLaxEmail, UserId.random, "holly", "golightly", false))

  Feature("Approve a request to uplift an application") {
    Scenario("I see the review page and I am able to approve the uplift request") {

      Given("I have successfully logged in to the API Gatekeeper")
      stubPaginatedApplicationList()

      stubApiDefinition()
      signInSuperUserGatekeeper(app)

      on(ApplicationsPage)
      stubApplication(pendingApprovalApplicationWithSubscriptionData.toJsonString, developers, pendingApprovalStateHistory.toJsonString, pendingApprovalApplicationId)

      When("I select to navigate to the Automated Test Application page")
      ApplicationsPage.clickApplicationNameLink(pendingApprovalApplicationName)

      Then("I am successfully navigated to the Automated Test Application page")
      on(ApplicationToReviewPage)

      verifyText("data-submitted-on", "22 August 2019")
      verifyText("data-submitted-by-email", "admin@example.com")
      verifyText("data-submission-contact-name", "Holly Golightly")
      verifyText("data-submission-contact-email", "holly.golightly@example.com")
      verifyText("data-submission-contact-telephone", "020 1122 3344")

      stubApplicationToReview(pendingApprovalApplicationId)
      ApplicationsPage.clickOnReview()
      on(ReviewPage(pendingApprovalApplicationId, "Application requiring approval"))
      ReviewPage(pendingApprovalApplicationId, "Application requiring approval").clickApprove()

      stubApplicationApproveUplift(pendingApprovalApplicationId, superUserGatekeeperId)

      ApplicationsPage.clickSubmit()

      on(ApplicationToReviewPage)
    }
  }

  Feature("Reject a request to uplift an application when no action was selected") {
    Scenario("I see the review page and I cannot submit without choosing an action") {
      Given("I have successfully logged in to the API Gatekeeper")
      stubPaginatedApplicationList()

      stubApiDefinition()
      signInSuperUserGatekeeper(app)

      on(ApplicationsPage)
      stubApplication(pendingApprovalApplicationWithSubscriptionData.toJsonString, developers, pendingApprovalStateHistory.toJsonString, pendingApprovalApplicationId)

      When("I select to navigate to the Automated Test Application page")
      ApplicationsPage.clickApplicationNameLink(pendingApprovalApplicationName)

      Then("I am successfully navigated to the Automated Test Application page")
      on(ApplicationToReviewPage)

      verifyText("data-submitted-on", "22 August 2019")
      verifyText("data-submitted-by-email", "admin@example.com")
      verifyText("data-submission-contact-name", "Holly Golightly")
      verifyText("data-submission-contact-email", "holly.golightly@example.com")
      verifyText("data-submission-contact-telephone", "020 1122 3344")

      stubApplicationToReview(pendingApprovalApplicationId)
      ApplicationToReviewPage.clickOnReview()

      on(ReviewPage(pendingApprovalApplicationId, "Application requiring approval"))
      ReviewPage(pendingApprovalApplicationId, "Application requiring approval").clickSubmit()

      on(ReviewPage(pendingApprovalApplicationId, "Application requiring approval"))
      verifyText("data-global-error", "Review the application")
    }
  }

  Feature("Reject a request to uplift an application") {
    Scenario("I see the review page and I am able to reject the uplift request with a reason") {
      Given("I have successfully logged in to the API Gatekeeper")
      stubPaginatedApplicationList()

      stubApiDefinition()
      signInSuperUserGatekeeper(app)

      on(ApplicationsPage)
      stubApplication(pendingApprovalApplicationWithSubscriptionData.toJsonString, developers, pendingApprovalStateHistory.toJsonString, pendingApprovalApplicationId)

      When("I select to navigate to the Automated Test Application page")
      ApplicationsPage.clickApplicationNameLink(pendingApprovalApplicationName)

      Then("I am successfully navigated to the Automated Test Application page")
      on(ApplicationToReviewPage)

      verifyText("data-submitted-on", "22 August 2019")
      verifyText("data-submitted-by-email", "admin@example.com")
      verifyText("data-submission-contact-name", "Holly Golightly")
      verifyText("data-submission-contact-email", "holly.golightly@example.com")
      verifyText("data-submission-contact-telephone", "020 1122 3344")

      stubApplicationToReview(pendingApprovalApplicationId)
      ApplicationToReviewPage.clickOnReview()

      on(ReviewPage(pendingApprovalApplicationId, "Application requiring approval"))
      ReviewPage(pendingApprovalApplicationId, "Application requiring approval").rejectApplication()

      stubApplicationRejectUplift(pendingApprovalApplicationId, gatekeeperId)

      ReviewPage(pendingApprovalApplicationId, "Application requiring approval").clickSubmit()

      on(ReviewPage(pendingApprovalApplicationId, "Application requiring approval"))
      verifyText("data-global-error", "This field is required")
    }
  }

}
