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

import java.net.URLEncoder

import acceptance.pages._
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status._

import scala.io.Source

class APIGatekeeperApplicationReviewSpec extends APIGatekeeperBaseSpec {

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


  feature("Approve a request to uplift an application") {
    scenario("I see the review page and I am able to approve the uplift request") {
      Given("I have successfully logged in to the API Gatekeeper")
      stubApplicationList()

      val applicationsList = Source.fromURL(getClass.getResource("/resources/applications.json")).mkString.replaceAll("\n", "")

      stubFor(get(urlEqualTo("/application")).willReturn(aResponse().withBody(applicationsList).withStatus(OK)))

      stubApplicationSubscription()
      stubApiDefinition()
      signInSuperUserGatekeeper()
      on(ApplicationsPage)
      stubApplicationToReview()

      When("I select to navigate to the Automated Test Application page")
      ApplicationsPage.selectByApplicationName("Application requiring approval")

      Then("I am successfully navigated to the Automated Test Application page")
      on(ApplicationToReviewPage)

      verifyText("data-submitted-on", "22 March 2016")
      verifyText("data-submitted-by-email", "admin@example.com")
      verifyText("data-submission-contact-name", "Holly Golightly")
      verifyText("data-submission-contact-email", "holly.golightly@example.com")
      verifyText("data-submission-contact-telephone", "020 1122 3344")
      verifyText("data-checked-on", "05 April 2016")
      verifyText("data-checked-by", "gatekeeper.username")

      stubApplicationToReview()
      clickOnReview("review")
      on(ReviewPage(appPendingApprovalId1, "First Application"))
      clickOnElement("approve-app")
      stubFor(post(urlMatching(s"/application/$appPendingApprovalId1/approve-uplift"))
        .withRequestBody(equalToJson(approveRequest))
        .willReturn(aResponse().withStatus(OK)))
      clickOnSubmit()
      stubApplicationToReview()
      on(ApplicationToReviewPage)
    }
  }

  feature("Reject a request to uplift an application when no action was selected") {
    scenario("I see the review page and I cannot submit without choosing an action") {
      Given("I have successfully logged in to the API Gatekeeper")
      stubApplicationList()

      val applicationsList = Source.fromURL(getClass.getResource("/resources/applications.json")).mkString.replaceAll("\n", "")

      stubFor(get(urlEqualTo("/application")).willReturn(aResponse().withBody(applicationsList).withStatus(OK)))

      stubApplicationSubscription()
      stubApiDefinition()
      signInSuperUserGatekeeper()
      on(ApplicationsPage)
      stubApplicationToReview()

      When("I select to navigate to the Automated Test Application page")
      ApplicationsPage.selectByApplicationName("Application requiring approval")

      Then("I am successfully navigated to the Automated Test Application page")
      on(ApplicationToReviewPage)

      verifyText("data-submitted-on", "22 March 2016")
      verifyText("data-submitted-by-email", "admin@example.com")
      verifyText("data-submission-contact-name", "Holly Golightly")
      verifyText("data-submission-contact-email", "holly.golightly@example.com")
      verifyText("data-submission-contact-telephone", "020 1122 3344")
      verifyText("data-checked-on", "05 April 2016")
      verifyText("data-checked-by", "gatekeeper.username")

      stubApplicationToReview()
      clickOnReview("review")
      on(ReviewPage(appPendingApprovalId1, "First Application"))
      clickOnSubmit()
      on(ReviewPage(appPendingApprovalId1, "First Application"))
      verifyText("data-global-error", "Review the application")

    }
  }

  feature("Reject a request to uplift an application") {
    scenario("I see the review page and I am able to reject the uplift request with a reason") {
      Given("I have successfully logged in to the API Gatekeeper")
      stubApplicationList()

      val applicationsList = Source.fromURL(getClass.getResource("/resources/applications.json")).mkString.replaceAll("\n", "")

      stubFor(get(urlEqualTo("/application")).willReturn(aResponse().withBody(applicationsList).withStatus(OK)))

      stubApplicationSubscription()
      stubApiDefinition()
      signInSuperUserGatekeeper()
      on(ApplicationsPage)
      stubApplicationToReview()

      When("I select to navigate to the Automated Test Application page")
      ApplicationsPage.selectByApplicationName("Application requiring approval")

      Then("I am successfully navigated to the Automated Test Application page")
      on(ApplicationToReviewPage)

      verifyText("data-submitted-on", "22 March 2016")
      verifyText("data-submitted-by-email", "admin@example.com")
      verifyText("data-submission-contact-name", "Holly Golightly")
      verifyText("data-submission-contact-email", "holly.golightly@example.com")
      verifyText("data-submission-contact-telephone", "020 1122 3344")
      verifyText("data-checked-on", "05 April 2016")
      verifyText("data-checked-by", "gatekeeper.username")

      stubApplicationToReview()
      clickOnReview("review")
      on(ReviewPage(appPendingApprovalId1, "First Application"))
      clickOnElement("reject-app")
      stubFor(post(urlMatching(s"/application/$appPendingApprovalId1/reject-uplift"))
        .withRequestBody(equalToJson(rejectRequest))
        .willReturn(aResponse().withStatus(200)))
      clickOnSubmit()
      on(ReviewPage(appPendingApprovalId1, "First Application"))
      verifyText("data-global-error", "This field is required")


    }
  }

  def stubApplicationToReview() = {
    stubFor(get(urlEqualTo("/gatekeeper/application/df0c32b6-bbb7-46eb-ba50-e6e5459162ff")).willReturn(aResponse().withBody(applicationToReview).withStatus(OK)))
    stubFor(get(urlEqualTo("/application/df0c32b6-bbb7-46eb-ba50-e6e5459162ff")).willReturn(aResponse().withBody(applicationToReview).withStatus(OK)))
    stubFor(get(urlEqualTo("/gatekeeper/application/df0c32b6-bbb7-46eb-ba50-e6e5459162ff/subscription")).willReturn(aResponse().withBody("[]").withStatus(OK)))
    stubFor(get(urlEqualTo("/application/df0c32b6-bbb7-46eb-ba50-e6e5459162ff/subscription")).willReturn(aResponse().withBody("[]").withStatus(OK)))
  }

  def stubApplicationListWithNoSubs() = {
    stubFor(get(urlEqualTo("/gatekeeper/resources/applications")).willReturn(aResponse().withBody(approvedApplications).withStatus(OK)))
    stubFor(get(urlEqualTo("/application")).willReturn(aResponse().withBody(applicationWithNoSubscription).withStatus(OK)))
  }

  def stubDeveloper() = {
    val encodedEmail = URLEncoder.encode(developer8, "UTF-8")

    stubFor(get(urlEqualTo(s"""/developer?email=$encodedEmail"""))
      .willReturn(aResponse().withStatus(OK).withBody(user)))
  }

  def stubApplicationForEmail() = {
    val encodedEmail = URLEncoder.encode(developer8, "UTF-8")

    stubFor(get(urlPathEqualTo("/developer/resources/applications")).withQueryParam("emailAddress", equalTo(encodedEmail))
      .willReturn(aResponse().withBody(applicationResponseForEmail).withStatus(OK)))
  }

}
