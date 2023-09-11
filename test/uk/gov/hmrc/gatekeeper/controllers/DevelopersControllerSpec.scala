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

package uk.gov.hmrc.gatekeeper.controllers

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.views.html.developers.DevelopersView
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

class DevelopersControllerSpec extends ControllerBaseSpec {

  implicit val materializer                         = app.materializer
  private lazy val errorTemplateView: ErrorTemplate = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView                    = app.injector.instanceOf[ForbiddenView]
  private lazy val developersView                   = app.injector.instanceOf[DevelopersView]

  Helpers.running(app) {

    val apiVersion1 = ApiVersion("1.0")
    val apiVersion2 = ApiVersion("2.0")

    trait Setup extends ControllerSetupBase {

      override val aLoggedInRequest          = FakeRequest().withSession(authToken, userToken).withCSRFToken
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(authToken, superUserToken).withCSRFToken

      val developersController = new DevelopersController(
        forbiddenView,
        mockDeveloperService,
        mockApiDefinitionService,
        mcc,
        developersView,
        errorTemplateView,
        StrideAuthorisationServiceMock.aMock,
        LdapAuthorisationServiceMock.aMock
      )

      def givenNoDataSuppliedDelegateServices(): Unit = {
        givenDelegateServicesSupply(List.empty[ApplicationResponse], noDevs)
      }

      def givenDelegateServicesSupply(apps: List[ApplicationResponse], developers: List[Developer]): Unit = {
        val apiFilter         = ApiFilter(Some(""))
        val environmentFilter = ApiSubscriptionInEnvironmentFilter(Some(""))
        val statusFilter      = StatusFilter(None)
        val users             = developers.map(developer => RegisteredUser(developer.email, UserId.random, developer.firstName, developer.lastName, developer.verified, developer.organisation))
        ApplicationServiceMock.FetchApplications.returnsFor(apiFilter, environmentFilter, apps: _*)
        FetchAllApiDefinitions.inAny.returns()
        DeveloperServiceMock.FilterUsersBy.returnsFor(apiFilter, apps: _*)(developers: _*)
        DeveloperServiceMock.FilterUsersBy.returnsFor(statusFilter)(developers: _*)
        DeveloperServiceMock.GetDevelopersWithApps.returnsFor(apps: _*)(users: _*)(developers: _*)
        DeveloperServiceMock.FetchUsers.returns(users: _*)
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
          "emailFilter"           -> EMPTY,
          "apiVersionFilter"      -> EMPTY,
          "environmentFilter"     -> EMPTY,
          "developerStatusFilter" -> EMPTY
        )
        val result  = developersController.developersPage()(request)

        await(result)

        verify(mockDeveloperService, never).searchDevelopers(*)(*)
      }

      "allow searching by email or partial email" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        private val emailAddress        = "developer@example.com"
        private val partialEmailAddress = "example"
        private val user                = aUser(emailAddress.toLaxEmail)

        // Note: Developers is both users and collaborators
        DeveloperServiceMock.SearchDevelopers.returns(user)

        val request = aLoggedInRequest.withFormUrlEncodedBody("emailFilter" -> partialEmailAddress)
        val result  = developersController.developersPage()(request)

        contentAsString(result) should include(emailAddress)

        val expectedFilter = DevelopersSearchFilter(Some(partialEmailAddress))
        verify(mockDeveloperService).searchDevelopers(eqTo(expectedFilter))(*)
      }

      "remember the search filter text on submit" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        private val searchFilter = "anEmailFilterCriteria"

        DeveloperServiceMock.SearchDevelopers.returns()

        val request = aLoggedInRequest.withFormUrlEncodedBody("emailFilter" -> searchFilter)
        val result  = developersController.developersPage()(request)

        contentAsString(result) should include(s"""value="$searchFilter"""")
      }

      "allow me to copy all the email addresses for verified users" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        private val email1 = "a@example.com".toLaxEmail
        private val email2 = "b@example.com".toLaxEmail
        private val email3 = "c@example.com".toLaxEmail

        DeveloperServiceMock.SearchDevelopers.returns(aUser(email1, true), aUser(email2, true), aUser(email3))

        implicit val request = aLoggedInRequest.withFormUrlEncodedBody("developerStatusFilter" -> "ALL")
        val result           = developersController.developersPage()(request)

        contentAsString(result) should include(s"${email1.text}; ${email2.text}")
      }

      "search by api version" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        private val emailAddress                   = "developer@example.com"
        private val user                           = aUser(emailAddress.toLaxEmail)
        private val apiDefinitionValueFromDropDown = "api-definition__1.0"

        // Note: Developers is both users and collaborators
        DeveloperServiceMock.SearchDevelopers.returns(user)

        val request = aLoggedInRequest.withFormUrlEncodedBody("apiVersionFilter" -> apiDefinitionValueFromDropDown)
        val result  = developersController.developersPage()(request)

        contentAsString(result) should include(emailAddress)

        val filter         = ApiContextVersion(ApiContext("api-definition"), apiVersion1)
        val expectedFilter = DevelopersSearchFilter(maybeApiFilter = Some(filter))
        verify(mockDeveloperService).searchDevelopers(eqTo(expectedFilter))(*)
      }

      "show an api version filter dropdown with correct display text" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        val apiVersions   = List(
          ApiVersionGK(apiVersion1, ApiVersionSource.UNKNOWN, ApiStatus.ALPHA),
          ApiVersionGK(apiVersion2, ApiVersionSource.UNKNOWN, ApiStatus.STABLE)
        )
        val apiDefinition = ApiDefinitionGK("", "", name = "MyApi", "", ApiContext.random, apiVersions, None, None)
        FetchAllApiDefinitions.inAny.returns(apiDefinition)

        val result = developersController.developersPage()(aLoggedInRequest)

        contentAsString(result) should include(s"MyApi (${apiVersion1.value}) (Alpha)")
        contentAsString(result) should include(s"MyApi (${apiVersion2.value}) (Stable)")

      }

      "show an api version filter dropdown with correct values for form submit with context and version" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        val apiContext = ApiContext.random

        val apiVersions   = List(
          ApiVersionGK(apiVersion1, ApiVersionSource.UNKNOWN, ApiStatus.STABLE),
          ApiVersionGK(apiVersion2, ApiVersionSource.UNKNOWN, ApiStatus.STABLE)
        )
        val apiDefinition = ApiDefinitionGK("", "", name = "", "", apiContext, apiVersions, None, None)
        FetchAllApiDefinitions.inAny.returns(apiDefinition)

        val result = developersController.developersPage()(aLoggedInRequest)

        contentAsString(result) should include(s"${apiContext.value}__${apiVersion1.value}")
        contentAsString(result) should include(s"${apiContext.value}__${apiVersion2.value}")

      }

      "show an api version filter dropdown without duplicates" in new Setup {
        val apiContext = ApiContext.random

        val apiVersionDefinition = ApiVersionGK(apiVersion1, ApiVersionSource.UNKNOWN, ApiStatus.ALPHA)

        val apiVersionDefinitions = List(apiVersionDefinition, apiVersionDefinition)
        val apiDefinition         = List(ApiDefinitionGK("", "", name = "MyApi", "", apiContext, apiVersionDefinitions, None, None))

        val result = developersController.getApiVersionsDropDownValues(apiDefinition)

        result.size shouldBe 1
        result.head.value shouldBe s"${apiContext.value}__${apiVersion1.value}"
      }

      "show number of entries" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        private val email1 = "a@example.com"
        private val email2 = "b@example.com"

        DeveloperServiceMock.SearchDevelopers.returns(aUser(email1.toLaxEmail), aUser(email2.toLaxEmail))

        implicit val request = aLoggedInRequest.withFormUrlEncodedBody("emailFilter" -> "not relevant")
        val result           = developersController.developersPage()(request)

        contentAsString(result) should include("Showing 2 entries")
      }

      "allow searching by developerStatusFilter" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        private val emailAddress = "developer@example.com"
        private val statusFilter = "VERIFIED"
        private val user         = aUser(emailAddress.toLaxEmail)

        // Note: Developers is both users and collaborators
        DeveloperServiceMock.SearchDevelopers.returns(user)

        val request = aLoggedInRequest.withFormUrlEncodedBody("developerStatusFilter" -> statusFilter)
        val result  = developersController.developersPage()(request)

        contentAsString(result) should include(emailAddress)

        val expectedFilter = DevelopersSearchFilter(developerStatusFilter = DeveloperStatusFilter.VerifiedStatus)
        verify(mockDeveloperService).searchDevelopers(eqTo(expectedFilter))(*)
      }

      "allow searching by environmentFilter" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        private val emailAddress      = "developer@example.com"
        private val user              = aUser(emailAddress.toLaxEmail)
        private val environmentFilter = "PRODUCTION"

        // Note: Developers is both users and collaborators
        DeveloperServiceMock.SearchDevelopers.returns(user)

        val request = aLoggedInRequest.withFormUrlEncodedBody("environmentFilter" -> environmentFilter)
        val result  = developersController.developersPage()(request)

        contentAsString(result) should include(emailAddress)

        val expectedFilter = DevelopersSearchFilter(environmentFilter = ProductionEnvironment)
        verify(mockDeveloperService).searchDevelopers(eqTo(expectedFilter))(*)
      }
    }
  }
}
