/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.controllers

import uk.gov.hmrc.gatekeeper.models._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import play.api.test.Helpers._
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}
import uk.gov.hmrc.gatekeeper.views.html.developers.Developers2View
import uk.gov.hmrc.modules.gkauth.services.StrideAuthorisationServiceMockModule

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.modules.gkauth.domain.models.GatekeeperRoles

class Developers2ControllerSpec extends ControllerBaseSpec {

  implicit val materializer = app.materializer
  private lazy val errorTemplateView: ErrorTemplate = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView = app.injector.instanceOf[ForbiddenView]
  private lazy val developersView = app.injector.instanceOf[Developers2View]


  Helpers.running(app) {

    val apiVersion1 = ApiVersion("1.0")
    val apiVersion2 = ApiVersion("2.0")

    trait Setup extends ControllerSetupBase with StrideAuthorisationServiceMockModule {

      override val aLoggedInRequest = FakeRequest().withSession(authToken, userToken).withCSRFToken
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(authToken, superUserToken).withCSRFToken

      val developersController = new Developers2Controller(
        forbiddenView,
        mockDeveloperService,
        mockApiDefinitionService,
        mcc,
        developersView,
        errorTemplateView,
       StrideAuthorisationServiceMock.aMock
      )

      def givenNoDataSuppliedDelegateServices(): Unit = {
        givenDelegateServicesSupply(List.empty[ApplicationResponse], noDevs)
      }

      def givenDelegateServicesSupply(apps: List[ApplicationResponse], developers: List[Developer]): Unit = {
        val apiFilter = ApiFilter(Some(""))
        val environmentFilter = ApiSubscriptionInEnvironmentFilter(Some(""))
        val statusFilter = StatusFilter(None)
        val users = developers.map(developer => RegisteredUser(developer.email, UserId.random, developer.firstName, developer.lastName, developer.verified, developer.organisation))
        ApplicationServiceMock.FetchApplications.returnsFor(apiFilter,environmentFilter, apps:_*)
        FetchAllApiDefinitions.inAny.returns()
        DeveloperServiceMock.FilterUsersBy.returnsFor(apiFilter,apps:_*)(developers:_*)
        DeveloperServiceMock.FilterUsersBy.returnsFor(statusFilter)(developers:_*)
        DeveloperServiceMock.GetDevelopersWithApps.returnsFor(apps:_*)(users:_*)(developers:_*)
        DeveloperServiceMock.FetchUsers.returns(users:_*)
      }

    }

    "blankDevelopersPage" should {
      "show no results when initially opened" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        val result = developersController.blankDevelopersPage()(aLoggedInRequest)

        contentAsString(result) should include("Developers")

      }
    }
    
    "developersPage" should {

      "show no results when initially opened" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        val result = developersController.developersPage()(aLoggedInRequest.withFormUrlEncodedBody())

        contentAsString(result) should include("Developers")

      }

      "searching with all empty filters does not trigger a query" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        private val EMPTY = ""

        DeveloperServiceMock.SearchDevelopers.returns()

        val request = aLoggedInRequest.withFormUrlEncodedBody(
                          "emailFilter"-> EMPTY,
                          "apiVersionFilter" -> EMPTY,
                          "environmentFilter" -> EMPTY,
                          "developerStatusFilter" -> EMPTY
                      )
        val result = developersController.developersPage()(request)

        await(result)

        verify(mockDeveloperService, never).searchDevelopers(*)(*)
      }

      "allow searching by email or partial email" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        private val emailAddress = "developer@example.com"
        private val partialEmailAddress = "example"
        private val user = aUser(emailAddress)

        // Note: Developers is both users and collaborators
        DeveloperServiceMock.SearchDevelopers.returns(user)

        val request = aLoggedInRequest.withFormUrlEncodedBody("emailFilter"-> partialEmailAddress)
        val result = developersController.developersPage()(request)

        contentAsString(result) should include(emailAddress)

        val expectedFilter = Developers2Filter(Some(partialEmailAddress))
        verify(mockDeveloperService).searchDevelopers(eqTo(expectedFilter))(*)
      }

      "remember the search filter text on submit" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        private val searchFilter = "anEmailFilterCriteria"

        DeveloperServiceMock.SearchDevelopers.returns()

        val request = aLoggedInRequest.withFormUrlEncodedBody("emailFilter"-> searchFilter)
        val result = developersController.developersPage()(request)

        contentAsString(result) should include(s"""value="$searchFilter"""")
      }

      "allow me to copy all the email addresses for verified users" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        private val email1 = "a@example.com"
        private val email2 = "b@example.com"
        private val email3 = "c@example.com"

        DeveloperServiceMock.SearchDevelopers.returns(aUser(email1,true), aUser(email2,true), aUser(email3))

        implicit val request = aLoggedInRequest.withFormUrlEncodedBody("developerStatusFilter" -> "ALL")
        val result = developersController.developersPage()(request)

        contentAsString(result) should include(s"$email1; $email2")
      }
      
      "search by api version" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        private val emailAddress = "developer@example.com"
        private val user = aUser(emailAddress)
        private val apiDefinitionValueFromDropDown = "api-definition__1.0"

        // Note: Developers is both users and collaborators
        DeveloperServiceMock.SearchDevelopers.returns(user)

        val request = aLoggedInRequest.withFormUrlEncodedBody("apiVersionFilter" -> apiDefinitionValueFromDropDown)
        val result = developersController.developersPage()(request)

        contentAsString(result) should include(emailAddress)

        val filter = ApiContextVersion(ApiContext("api-definition"), apiVersion1)
        val expectedFilter = Developers2Filter(maybeApiFilter = Some(filter))
        verify(mockDeveloperService).searchDevelopers(eqTo(expectedFilter))(*)
      }


      "show an api version filter dropdown with correct display text" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        val apiVersions = List(ApiVersionDefinition(apiVersion1, ApiStatus.ALPHA), ApiVersionDefinition(apiVersion2, ApiStatus.STABLE))
        val apiDefinition = ApiDefinition("", "", name = "MyApi", "", ApiContext.random, apiVersions, None, None)
        FetchAllApiDefinitions.inAny.returns(apiDefinition)

        val result = developersController.developersPage()(aLoggedInRequest)

        contentAsString(result) should include(s"MyApi (${apiVersion1.value}) (Alpha)")
        contentAsString(result) should include(s"MyApi (${apiVersion2.value}) (Stable)")

      }

      "show an api version filter dropdown with correct values for form submit with context and version" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        val apiContext = ApiContext.random

        val apiVersions = List(ApiVersionDefinition(apiVersion1, ApiStatus.STABLE), ApiVersionDefinition(apiVersion2, ApiStatus.STABLE))
        val apiDefinition = ApiDefinition("", "", name = "", "", apiContext, apiVersions, None, None)
        FetchAllApiDefinitions.inAny.returns(apiDefinition)

        val result = developersController.developersPage()(aLoggedInRequest)

        contentAsString(result) should include(s"${apiContext.value}__${apiVersion1.value}")
        contentAsString(result) should include(s"${apiContext.value}__${apiVersion2.value}")

      }

      "show an api version filter dropdown without duplicates" in new Setup {
        val apiContext = ApiContext.random

        val apiVersionDefinition = ApiVersionDefinition(apiVersion1, ApiStatus.ALPHA)

        val apiVersionDefinitions = List(apiVersionDefinition, apiVersionDefinition)
        val apiDefinition = List(ApiDefinition("", "", name = "MyApi", "", apiContext, apiVersionDefinitions, None, None))

        val result = developersController.getApiVersionsDropDownValues(apiDefinition)

        result.size shouldBe 1
        result.head.value shouldBe s"${apiContext.value}__${apiVersion1.value}"
      }

      "show number of entries" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        private val email1 = "a@example.com"
        private val email2 = "b@example.com"

        DeveloperServiceMock.SearchDevelopers.returns(aUser(email1), aUser(email2))

        implicit val request = aLoggedInRequest.withFormUrlEncodedBody("emailFilter" -> "not relevant")
        val result = developersController.developersPage()(request)

        contentAsString(result) should include("Showing 2 entries")
      }

      "allow searching by developerStatusFilter" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        private val emailAddress = "developer@example.com"
        private val statusFilter = "VERIFIED"
        private val user = aUser(emailAddress)

        // Note: Developers is both users and collaborators
        DeveloperServiceMock.SearchDevelopers.returns(user)

        val request = aLoggedInRequest.withFormUrlEncodedBody("developerStatusFilter" -> statusFilter)
        val result = developersController.developersPage()(request)

        contentAsString(result) should include(emailAddress)

        val expectedFilter = Developers2Filter(developerStatusFilter = DeveloperStatusFilter.VerifiedStatus)
        verify(mockDeveloperService).searchDevelopers(eqTo(expectedFilter))(*)
      }

      "allow searching by environmentFilter" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        private val emailAddress = "developer@example.com"
        private val user = aUser(emailAddress)
        private val environmentFilter = "PRODUCTION"

        // Note: Developers is both users and collaborators
        DeveloperServiceMock.SearchDevelopers.returns(user)

        val request = aLoggedInRequest.withFormUrlEncodedBody("environmentFilter" -> environmentFilter)
        val result = developersController.developersPage()(request)

        contentAsString(result) should include(emailAddress)

        val expectedFilter = Developers2Filter(environmentFilter = ProductionEnvironment)
        verify(mockDeveloperService).searchDevelopers(eqTo(expectedFilter))(*)
      }
    }
  }
}
