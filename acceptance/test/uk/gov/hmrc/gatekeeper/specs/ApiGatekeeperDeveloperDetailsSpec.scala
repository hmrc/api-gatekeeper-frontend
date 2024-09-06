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
import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.pages._
import uk.gov.hmrc.gatekeeper.stubs.{ApiPlatformDeskproStub, ThirdPartyApplicationStub, ThirdPartyDeveloperStub, XmlServicesStub}
import uk.gov.hmrc.gatekeeper.testdata._
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

class ApiGatekeeperDeveloperDetailsSpec
    extends ApiGatekeeperBaseSpec
    with ApplicationWithSubscriptionDataTestData
    with ApplicationResponseTestData
    with StateHistoryTestData
    with Assertions
    with CommonTestData
    with ApiDefinitionTestData
    with UrlEncoding
    with XmlServicesStub
    with ThirdPartyDeveloperStub
    with ThirdPartyApplicationStub
    with ApiPlatformDeskproStub {
  val developers = List[RegisteredUser](RegisteredUser("joe.bloggs@example.co.uk".toLaxEmail, UserId.random, "joe", "bloggs", false))

  info("AS A Gatekeeper superuser")
  info("I WANT to be able to view the applications an administrator/developer is on")
  info("SO THAT I can follow the correct process before deleting the administrator/developer")

  Feature("Developer details page") {

    Scenario("Ensure a user can select an individual developer") {

      Given("I have successfully logged in to the API Gatekeeper")
      stubPaginatedApplicationList()

      stubFetchAllApplicationsList()

      stubApplicationForDeveloper(unverifiedUser.userId, defaultApplicationResponse.toSeq.toJsonString)
      stubApplication(applicationWithSubscriptionData.toJsonString, developers, stateHistories.toJsonString, applicationId)
      stubApiDefinition()
      stubDevelopers()
      stubDevelopersSearch()
      stubDeveloper(unverifiedUser)
      stubGetXmlApiForCategories()
      stubGetAllXmlApis()
      stubGetXmlOrganisationsForUser(unverifiedUser.userId)
      stubApplicationSubscription(MockDataSugar.applicationSubscription)
      stubGetOrganisationsForUser(unverifiedUser.email)

      signInGatekeeper(app)
      on(ApplicationsPage)

      When("I select to navigate to the Developers page")
      ApplicationsPage.selectDevelopers()

      Then("I am successfully navigated to the Developers page where I can view all developer list details by default")
      on(DeveloperPage)

      When("I select a developer email")
      DeveloperPage.searchByPartialEmail(unverifiedUser.email.text)
      DeveloperPage.selectByDeveloperEmail(unverifiedUser.email)

      Then("I am successfully navigated to the Developer Details page")
      on(DeveloperDetailsPage)

      And("I can see the developer's details and associated applications")
      assert(DeveloperDetailsPage.firstName() == unverifiedUser.firstName)
      assert(DeveloperDetailsPage.lastName() == unverifiedUser.lastName)
      assert(DeveloperDetailsPage.status() == "not yet verified")
      assert(DeveloperDetailsPage.organisations() == "Deskpro Organisation 1\nDeskpro Organisation 2")
      assert(DeveloperDetailsPage.mfaHeading() == "Multi-factor authentication")

      When("I select an associated application")
      DeveloperDetailsPage.selectByApplicationName("My new app")

      Then("I am successfully navigated to the Automated Test Application page")
      on(ApplicationPage)
    }
  }

}
