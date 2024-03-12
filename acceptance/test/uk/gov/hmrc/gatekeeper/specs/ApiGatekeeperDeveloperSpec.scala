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

import org.scalatest.Assertions

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.pages.DeveloperPage.APIFilter
import uk.gov.hmrc.gatekeeper.pages.{ApplicationsPage, DeveloperPage}
import uk.gov.hmrc.gatekeeper.stubs.{ThirdPartyApplicationStub, ThirdPartyDeveloperStub}
import uk.gov.hmrc.gatekeeper.testdata.MockDataSugar

class ApiGatekeeperDeveloperSpec
    extends ApiGatekeeperBaseSpec
    with Assertions
    with ThirdPartyApplicationStub
    with ThirdPartyDeveloperStub {

  import MockDataSugar._

  info("AS A Product Owner")
  info("I WANT The SDST (Software Developer Support Team) to be able to select developers with an interest in a particular API")
  info("SO THAT The SDST can create and send email communications to selected developers")

  Feature("API Filter for Email Recipients") {

    Scenario("Ensure a user can view the list of registered developers") {

      val developers = List(
        RegisteredUser(
          email = developer4.toLaxEmail,
          userId = UserId.random,
          firstName = dev4FirstName,
          lastName = dev4LastName,
          verified = true
        ),
        RegisteredUser(
          email = developer5.toLaxEmail,
          userId = UserId.random,
          firstName = dev5FirstName,
          lastName = dev5LastName,
          verified = false
        )
      )

      Given("I have successfully logged in to the API Gatekeeper")
      stubApplicationList(approvedApplications)
      stubApplication(applicationResponse)
      stubApplicationSubscription(applicationSubscription)
      stubPaginatedApplicationList()
      stubApplicationsCollaborators(developers)
      stubApiDefinition()
      stubRandomDevelopers(100)

      stubGetDevelopersByEmails(developers)

      stubDevelopersSearch("partialEmail", developers)

      signInGatekeeper(app)
      on(ApplicationsPage)

      When("I select to navigate to the Developers page")
      ApplicationsPage.selectDevelopers()

      Then("I am successfully navigated to the Developers page")
      on(DeveloperPage)

      When("I enter a partial email to filter by")
      DeveloperPage.writeInSearchBox("partialEmail")

      And("I pick a an API definition")
      DeveloperPage.selectBySubscription(APIFilter.EMPLOYERSPAYE)

      And("I pick an environment")
      DeveloperPage.selectByEnvironment(Environment.PRODUCTION)

      And("I pick a Developer Status")
      DeveloperPage.selectByDeveloperStatus("VERIFIED")

      And("I submit my search")
      DeveloperPage.clickSubmit()

      Then("I see a list of filtered developers")

      val expectedDeveloper: Seq[(String, String, String, String)] = List(
        (dev4FirstName, dev4LastName, developer4, statusVerified)
      )

      val allDevs: Seq[((String, String, String, String), Int)] = expectedDeveloper.zipWithIndex

      for ((dev, index) <- allDevs) {
        DeveloperPage.getDeveloperFirstNameByIndex(index) shouldBe dev._1
        DeveloperPage.getDeveloperSurnameByIndex(index) shouldBe dev._2
        DeveloperPage.getDeveloperEmailByIndex(index) shouldBe dev._3
        DeveloperPage.getDeveloperStatusByIndex(index) shouldBe dev._4
      }

      assertThereAreNoMoreThanNDevelopers(1)
    }
  }

  private def assertThereAreNoMoreThanNDevelopers(count: Int) = assertDeveloperAtRowDoesNotExist(count)

  private def assertDeveloperAtRowDoesNotExist(rowIndex: Int) = {
    DeveloperPage.developerRowExists(rowIndex) shouldBe false
  }
}
