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

import java.time.Instant

import uk.gov.hmrc.apiplatform.modules.applications.common.domain.models.FullName
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{CheckInformation, ContactDetails, TermsOfUseAgreement}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.gatekeeper.models.RegisteredUser
import uk.gov.hmrc.gatekeeper.pages.{ApplicationPage, ApplicationsPage, DeveloperDetailsPage}
import uk.gov.hmrc.gatekeeper.stubs.{ApiPlatformDeskproStub, ThirdPartyApplicationStub, ThirdPartyDeveloperStub, XmlServicesStub}
import uk.gov.hmrc.gatekeeper.testdata.{ApplicationResponseTestData, ApplicationWithSubscriptionDataTestData, MockDataSugar, StateHistoryTestData}

class ApiGatekeeperApplicationSpec
    extends ApiGatekeeperBaseSpec
    with StateHistoryTestData
    with ApplicationWithSubscriptionDataTestData
    with ApplicationResponseTestData
    with XmlServicesStub
    with ThirdPartyDeveloperStub
    with ThirdPartyApplicationStub
    with ApiPlatformDeskproStub {

  val developers = List[RegisteredUser](RegisteredUser(unverifiedUser.email, unverifiedUser.userId, unverifiedUser.firstName, unverifiedUser.lastName, unverifiedUser.verified))

  Feature("Application List for Search Functionality") {
    info("AS A Product Owner")
    info("I WANT The SDST (Software Developer Support Team) to be able to search for applications")
    info("SO THAT The SDST can review the status of the applications")

    Scenario("Ensure a user can view a list of Applications") {
      Given("I have successfully logged in to the API Gatekeeper")
      stubPaginatedApplicationList()
      stubApiDefinition()
      signInGatekeeper(app)
      Then("I am successfully navigated to the Applications page where I can view all developer list details by default")
      on(ApplicationsPage)
    }
  }

  Feature("Show applications information") {
    Scenario("View a specific application") {
      Given("I have successfully logged in to the API Gatekeeper")
      stubPaginatedApplicationList()
      stubApiDefinition()
      signInGatekeeper(app)

      on(ApplicationsPage)

      val defaultCheckInformation: CheckInformation =
        CheckInformation(
          contactDetails = Some(
            ContactDetails(
              fullname = FullName("Holly Golightly"),
              email = LaxEmailAddress("holly.golightly@example.com"),
              telephoneNumber = "020 1122 3344"
            )
          ),
          confirmedName = true,
          providedPrivacyPolicyURL = true,
          providedTermsAndConditionsURL = true,
          applicationDetails = Some(""),
          termsOfUseAgreements = List(
            TermsOfUseAgreement(
              emailAddress = LaxEmailAddress("test@example.com"),
              timeStamp = Instant.ofEpochSecond(1459868573962L),
              version = "1.0"
            )
          )
        )

      val testApp = applicationWithSubscriptionData.modify(_.copy(checkInformation = Some(defaultCheckInformation)))

      stubApplication(testApp.toJsonString, developers, stateHistories.toJsonString, applicationId)

      When("I select to navigate to the Automated Test Application page")
      ApplicationsPage.clickApplicationNameLink("My new app")

      Then("I am successfully navigated to the Automated Test Application page")
      on(ApplicationPage)
      verifyText("data-environment", "Production")
      verifyText("data-app-id", applicationId.value.toString)
      verifyText("data-status", "Active")
      verifyText("data-rate-limit", "Bronze")
      verifyText("data-description-private", applicationDescription)
      verifyText("data-description-public", "")
      ApplicationPage.getDataPrivacyUrl() shouldBe "http://localhost:22222/privacy"
      ApplicationPage.getDataTermsUrl() shouldBe "http://localhost:22222/terms"
      verifyText("data-access-type", "Standard")
      verifyText("data-subscriptions", "API Simulator 1.0 (Stable)\nHello World 1.0 (Stable)")
      verifyText("data-collaborator-email", "admin@example.com", 0)
      verifyText("data-collaborator-role", "Admin", 0)
      verifyText("data-collaborator-email", "purnima.fakename@example.com", 1)
      verifyText("data-collaborator-role", "Developer", 1)
      verifyText("data-collaborator-email", "dixie.fakename@example.com", 2)
      verifyText("data-collaborator-role", "Developer", 2)
      verifyText("data-submitted-on", "22 August 2019")
      verifyText("data-submitted-by-email", "admin@example.com")
      verifyText("data-submission-contact-name", "Holly Golightly")
      verifyText("data-submission-contact-email", "holly.golightly@example.com")
      verifyText("data-submission-contact-telephone", "020 1122 3344")
      verifyText("data-checked-on", "22 July 2020")
      verifyText("data-checked-by", "gatekeeper.username")
      verifyText("data-application-protected-from-deletion", "No")

      And("I can see the Copy buttons")
      verifyText("data-clip-text", "Copy all team member email addresses", 0)
      verifyText("data-clip-text", "Copy admin email addresses", 1)
    }
  }

  Feature("Show an applications developer information") {
    Scenario("View a specific developer on an application") {
      Given("I have successfully logged in to the API Gatekeeper")
      stubPaginatedApplicationList()
      stubApiDefinition()
      signInGatekeeper(app)

      on(ApplicationsPage)
      stubApplication(applicationWithSubscriptionData.toJsonString, developers, stateHistories.toJsonString, applicationId)

      When("I select to navigate to the Automated Test Application page")
      ApplicationsPage.clickApplicationNameLink(applicationName.value)

      Then("I am successfully navigated to the Automated Test Application page")
      on(ApplicationPage)

      stubDeveloperGetUnverified()
      stubGetAllXmlApis()
      stubGetXmlApiForCategories()
      stubGetXmlOrganisationsForUser(unverifiedUser.userId)
      stubApplicationForDeveloper(unverifiedUser.userId, MockDataSugar.applicationForDeveloperResponse)
      stubGetOrganisationsForUser(unverifiedUser.email)

      When("I select to navigate to a collaborator")
      ApplicationsPage.selectDeveloperByEmail(unverifiedUser.email)

      Then("I am successfully navigated to the developer details page")
      on(DeveloperDetailsPage)
    }
  }

}
