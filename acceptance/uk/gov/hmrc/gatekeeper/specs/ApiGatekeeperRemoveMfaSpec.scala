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

import org.scalatest.{Assertions, Tag}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.gatekeeper.models.MfaType
import uk.gov.hmrc.gatekeeper.pages._
import uk.gov.hmrc.gatekeeper.stubs.{ThirdPartyDeveloperStub, XmlServicesStub}
import uk.gov.hmrc.gatekeeper.testdata.{CommonTestData, MockDataSugar}

class ApiGatekeeperRemoveMfaSpec
  extends ApiGatekeeperBaseSpec
    with Assertions
    with CommonTestData
    with ThirdPartyDeveloperStub
    with XmlServicesStub {

  import MockDataSugar._

  info("As a Gatekeeper superuser")
  info("I WANT to be able to remove MFA for a developer")
  info("SO THAT they can reset MFA if they lost their secret")

  Feature("Remove MFA") {

    Scenario("Ensure a super user can remove MFA from a developer", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper")
      initStubs()
      signInSuperUserGatekeeper(app)
      on(ApplicationsPage)

      When("I navigate to the Developer Details page")
      navigateToDeveloperDetails()

      Then("I can see the MFA heading")
      assert(DeveloperDetailsPage.mfaHeading() == "Multi-factor authentication")

      Then("I can see the Link to remove MFA")
      assert(DeveloperDetailsPage.removeMfaLinkText() == "Remove multi-factor authentication")

      When("I click on remove MFA")
      DeveloperDetailsPage.removeMfa()

      Then("I am successfully navigated to the Remove MFA page")
      on(RemoveMfaPage)

      When("I select the 'Yes' option")
      RemoveMfaPage.selectRadioButton("yes")

      When("I confirm I want to remove MFA")
      RemoveMfaPage.removeMfa()

      Then("I am successfully navigated to the Remove MFA Success page")
      on(RemoveMfaSuccessPage)

      When("I click on Back to developer details")
      RemoveMfaSuccessPage.backToDeveloperDetails()
      
      Then("I am successfully navigated to the Developer page")
      on(DeveloperDetailsPage)
    }

    Scenario("Ensure a non-super user CAN remove MFA from a developer", Tag("NonSandboxTest")) {

      Given("I have successfully logged in to the API Gatekeeper")
      initStubs()
      signInGatekeeper(app)
      on(ApplicationsPage)

      When("I navigate to the Developer Details page")
      navigateToDeveloperDetails()

      Then("I can see the MFA detail types and names")
      assert(DeveloperDetailsPage.authAppMfaType() == MfaType.AUTHENTICATOR_APP.asText)
      assert(DeveloperDetailsPage.authAppMfaName() == "On (Google Auth App)")
      assert(DeveloperDetailsPage.smsMfaType() == MfaType.SMS.asText)
      assert(DeveloperDetailsPage.smsMfaName() == "On (****6789)")

      Then("I can see the link to remove MFA")
      assert(DeveloperDetailsPage.removeMfaLinkText() == "Remove multi-factor authentication")
      assert(DeveloperDetailsPage.removeMfaLinkIsDisabled() == false)

      When("I click on remove MFA")
      DeveloperDetailsPage.removeMfa()

      Then("I am successfully navigated to the Remove MFA page")
      on(RemoveMfaPage)

      When("I select the 'Yes' option")
      RemoveMfaPage.selectRadioButton("yes")

      When("I confirm I want to remove MFA")
      RemoveMfaPage.removeMfa()

      Then("I am successfully navigated to the Remove MFA Success page")
      on(RemoveMfaSuccessPage)

      When("I click on Back to developer details")
      RemoveMfaSuccessPage.backToDeveloperDetails()

      Then("I am successfully navigated to the Developer Details page")
      on(DeveloperDetailsPage)
    }
  }

  def initStubs(): Unit = {
    stubFetchAllApplicationsList()
    stubPaginatedApplicationList()
    stubApplicationForDeveloper(developer8Id.toString(), applicationResponseForEmail)
    stubApplicationExcludingDeletedForDeveloper()
    stubApiDefinition()
    stubDevelopers()
    stubDevelopersSearch()
    stubDeveloper()
    stubGetAllXmlApis()
    stubGetXmlApiForCategories()
    stubGetXmlOrganisationsForUser(UserId(developer8Id))
    stubApplicationSubscription(MockDataSugar.applicationSubscription)
    stubRemoveMfa()
  }

  def navigateToDeveloperDetails(): Unit = {
    When("I select to navigate to the Developers page")
    ApplicationsPage.selectDevelopers()

    Then("I am successfully navigated to the Developers page")
    on(DeveloperPage)

    When("I select a developer email")
    DeveloperPage.searchByPartialEmail(developer8)
    DeveloperPage.selectByDeveloperEmail(developer8.toLaxEmail)

    Then("I am successfully navigated to the Developer Details page")
    on(DeveloperDetailsPage)
  }

}
