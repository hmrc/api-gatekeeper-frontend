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

import cats.data.NonEmptyList
import com.github.tomakehurst.wiremock.client.WireMock._

import play.api.http.Status._
import play.api.libs.json.Json

import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.{CommandFailure, CommandFailures, DispatchSuccessResult}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, UserId}
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.pages._

class ApiGatekeeperDeleteApplicationSpec extends ApiGatekeeperBaseSpec {

  val developers = List[RegisteredUser](RegisteredUser("joe.bloggs@example.co.uk".toLaxEmail, UserId.random, "joe", "bloggs", false))

  Feature("Delete an application") {

    Scenario("I can delete an application") {
      stubApplicationForDeleteSuccess()

      When("I navigate to the Delete Page for an application")
      navigateThroughDeleteApplication()

      Then("I am successfully navigated to the Delete Application Success page")
      on(DeleteApplicationSuccessPage)
      assert(DeleteApplicationSuccessPage.bodyText.contains("Application deleted"))
    }

    Scenario("I cannot delete an application") {
      stubApplicationForDeleteFailure()

      When("I navigate to the Delete Page for an application")
      navigateThroughDeleteApplication()

      Then("I am successfully navigated to the Delete Application technical difficulties page")
      onTechDifficultiesFor(DeleteApplicationSuccessPage)
      assert(DeleteApplicationSuccessPage.bodyText.contains("Technical difficulties"))
    }
  }

  def navigateThroughDeleteApplication() = {
    Given("I have successfully logged in to the API Gatekeeper")
    stubPaginatedApplicationList()

    stubApiDefinition()

    signInAdvancedUserGatekeeper(app)
    signInSuperUserGatekeeper(app)
    on(ApplicationsPage)

    When("I select to navigate to the Applications page")
    DashboardPage.selectApplications()

    Then("I am successfully navigated to the Applications page where I can view all applications")
    on(ApplicationsPage)

    stubApplication(applicationWithSubscriptionData, developers, stateHistories, applicationId)

    When("I select to navigate to the Automated Test Application page")
    ApplicationsPage.clickApplicationNameLink(applicationName.value)

    Then("I am successfully navigated to the Automated Test Application page")
    on(ApplicationPage)

    stubApplicationToDelete(applicationId)

    When("I click the Delete Application Button")
    ApplicationPage.clickDeleteApplicationButton()

    Then("I am successfully navigated to the Delete Application page")
    on(DeleteApplicationPage)

    stubApplicationToDelete(applicationId)

    When("I fill out the Delete Application Form correctly")
    DeleteApplicationPage.completeForm(applicationName.value)

    And("I click the Delete Application Button")
    DeleteApplicationPage.clickDeleteButton()
  }

  def stubApplicationToDelete(applicationId: ApplicationId) = {
    stubApplicationById(applicationId, defaultApplicationResponse)
  }

  def stubApplicationForDeleteSuccess() = {
    val gkAppResponse = defaultApplication

    val response = DispatchSuccessResult(gkAppResponse)
    stubFor(patch(urlEqualTo(s"/applications/${applicationId.toString()}/dispatch")).willReturn(aResponse().withStatus(OK).withBody(Json.toJson(response).toString())))
  }

  def stubApplicationForDeleteFailure() = {
    import uk.gov.hmrc.apiplatform.modules.common.domain.services.NonEmptyListFormatters._
    val response: NonEmptyList[CommandFailure] = NonEmptyList.one(CommandFailures.ApplicationNotFound)
    stubFor(patch(urlEqualTo(s"/applications/${applicationId.toString()}/dispatch")).willReturn(aResponse().withStatus(BAD_REQUEST).withBody(Json.toJson(response).toString())))
  }
}
