/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers

import model._
import model.DeveloperStatusFilter.VerifiedStatus
import play.api.mvc.Result
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.TokenProvider
import services.DeveloperService
import utils.WithCSRFAddToken
import views.html.{ErrorTemplate, ForbiddenView}
import views.html.developers.Developers2View

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.successful

class Developers2ControllerSpec extends ControllerBaseSpec with WithCSRFAddToken {

  implicit val materializer = app.materializer
  private lazy val errorTemplateView: ErrorTemplate = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView = app.injector.instanceOf[ForbiddenView]
  private lazy val developersView = app.injector.instanceOf[Developers2View]


  Helpers.running(app) {

    def aUser(email: String) = RegisteredUser(email, UserId.random, "first", "last", verified = false)
    val apiVersion1 = ApiVersion("1.0")
    val apiVersion2 = ApiVersion("2.0")

    trait Setup extends ControllerSetupBase {

      val csrfToken = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken
      override val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken)
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, superUserToken)

      val mockDeveloperService = mock[DeveloperService]

      val developersController = new Developers2Controller(
        mockAuthConnector,
        forbiddenView,
        mockDeveloperService,
        mockApiDefinitionService,
        mcc,
        developersView,
        errorTemplateView
      )

      def givenNoDataSuppliedDelegateServices(): Unit = {
        givenDelegateServicesSupply(Seq.empty[ApplicationResponse], noDevs)
      }

      def givenDelegateServicesSupply(apps: Seq[ApplicationResponse], developers: List[Developer]): Unit = {
        val apiFilter = ApiFilter(Some(""))
        val environmentFilter = ApiSubscriptionInEnvironmentFilter(Some(""))
        val statusFilter = StatusFilter(None)
        val users = developers.map(developer => RegisteredUser(developer.email, UserId.random, developer.firstName, developer.lastName, developer.verified, developer.organisation))
        when(mockApplicationService.fetchApplications(eqTo(apiFilter), eqTo(environmentFilter))(*)).thenReturn(successful(apps))
        when(mockApiDefinitionService.fetchAllApiDefinitions(*)(*)).thenReturn(Seq.empty[ApiDefinition])
        when(mockDeveloperService.filterUsersBy(apiFilter, apps)(developers)).thenReturn(developers)
        when(mockDeveloperService.filterUsersBy(statusFilter)(developers)).thenReturn(developers)
        when(mockDeveloperService.getDevelopersWithApps(eqTo(apps), eqTo(users))).thenReturn(developers)
        when(mockDeveloperService.fetchUsers(*)).thenReturn(successful(users))
      }

      def givenFetchDeveloperReturns(developer: Developer) = {
        when(mockDeveloperService.fetchDeveloper(eqTo(developer.email))(*)).thenReturn(successful(developer))
      }

      def givenDeleteDeveloperReturns(result: DeveloperDeleteResult) = {
        when(mockDeveloperService.deleteDeveloper(*, *)(*)).thenReturn(successful(result))
      }

      def givenRemoveMfaReturns(user: Future[RegisteredUser]) = {
        when(mockDeveloperService.removeMfa(*, *)(*)).thenReturn(user)
      }
    }

    "developersPage" should {
      "show no results when initially opened" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenNoDataSuppliedDelegateServices()

        val result = await(developersController.developersPage()(aLoggedInRequest))

        bodyOf(result) should include("Developers")

        verifyAuthConnectorCalledForUser
      }

      "allow searching by email or partial email" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenNoDataSuppliedDelegateServices()

        private val emailAddress = "developer@example.com"
        private val partialEmailAddress = "example"
        private val user = aUser(emailAddress)

        // Note: Developers is both users and collaborators
        when(mockDeveloperService.searchDevelopers(*)(*)).thenReturn(List(user))

        val result: Result = await(developersController.developersPage(Some(partialEmailAddress))(aLoggedInRequest))

        bodyOf(result) should include(emailAddress)

        val expectedFilter = Developers2Filter(Some(partialEmailAddress))
        verify(mockDeveloperService).searchDevelopers(eqTo(expectedFilter))(*)
      }

      "search by empty filters values doesn't filter by them" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenNoDataSuppliedDelegateServices()

        private val emailFilter = ""
        private val apiVersionFilter = ""

        when(mockDeveloperService.searchDevelopers(*)(*)).thenReturn(List())

        await(developersController.developersPage(Some(emailFilter), Some(apiVersionFilter))(aLoggedInRequest))

        val expectedEmptyFilter = Developers2Filter()
        verify(mockDeveloperService).searchDevelopers(eqTo(expectedEmptyFilter))(*)
      }

      "remember the search filter text on submit" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenNoDataSuppliedDelegateServices()

        private val searchFilter = "aFilter"

        when(mockDeveloperService.searchDevelopers(*)(*)).thenReturn(successful(Seq.empty))

        implicit val request = FakeRequest("GET", s"/developers2?emailFilter=$searchFilter").withSession(csrfToken, authToken, userToken)

        val result: Result = await(developersController.developersPage(Some(searchFilter))(request))

        bodyOf(result) should include(s"""value="$searchFilter"""")
      }

      "allow me to copy all the email addresses" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenNoDataSuppliedDelegateServices()

        private val email1 = "a@example.com"
        private val email2 = "b@example.com"
        val users = List(aUser(email1), aUser(email2))

        when(mockDeveloperService.searchDevelopers(*)(*)).thenReturn(users)

        implicit val request = FakeRequest("GET", s"/developers2?emailFilter=").withSession(csrfToken, authToken, userToken)

        val result: Result = await(developersController.developersPage(Some(""))(request))

        bodyOf(result) should include(s"$email1; $email2")
      }

      "show an api version filter dropdown with correct display text" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenNoDataSuppliedDelegateServices()

        val apiVersions = List(ApiVersionDefinition(apiVersion1, ApiStatus.ALPHA), ApiVersionDefinition(apiVersion2, ApiStatus.STABLE))
        val apiDefinition = ApiDefinition("", "", name = "MyApi", "", ApiContext.random, apiVersions, None, None)
        when(mockApiDefinitionService.fetchAllApiDefinitions(*)(*)).thenReturn(List(apiDefinition))

        val result = await(developersController.developersPage()(aLoggedInRequest))

        bodyOf(result) should include(s"MyApi (${apiVersion1.value}) (Alpha)")
        bodyOf(result) should include(s"MyApi (${apiVersion2.value}) (Stable)")

        verifyAuthConnectorCalledForUser
      }

      "show an api version filter dropdown with correct values for form submit with context and version" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenNoDataSuppliedDelegateServices()

        val apiContext = ApiContext.random

        val apiVersions = List(ApiVersionDefinition(apiVersion1, ApiStatus.STABLE), ApiVersionDefinition(apiVersion2, ApiStatus.STABLE))
        val apiDefinition = ApiDefinition("", "", name = "", "", apiContext, apiVersions, None, None)
        when(mockApiDefinitionService.fetchAllApiDefinitions(*)(*)).thenReturn(List(apiDefinition))

        val result = await(developersController.developersPage()(aLoggedInRequest))

        bodyOf(result) should include(s"${apiContext.value}__${apiVersion1.value}")
        bodyOf(result) should include(s"${apiContext.value}__${apiVersion2.value}")

        verifyAuthConnectorCalledForUser
      }

      "search by api version" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenNoDataSuppliedDelegateServices()

        private val emailAddress = "developer@example.com"
        private val user = aUser(emailAddress)
        private val apiDefinitionValueFromDropDown = "api-definition__1.0"

        // Note: Developers is both users and collaborators
        when(mockDeveloperService.searchDevelopers(*)(*)).thenReturn(List(user))

        val result: Result = await(developersController.developersPage(maybeApiVersionFilter = Some(apiDefinitionValueFromDropDown))(aLoggedInRequest))

        bodyOf(result) should include(emailAddress)

        val filter = ApiContextVersion(ApiContext("api-definition"), apiVersion1)
        val expectedFilter = Developers2Filter(maybeApiFilter = Some(filter))
        verify(mockDeveloperService).searchDevelopers(eqTo(expectedFilter))(*)
      }

      "show an api version filter dropdown without duplicates" in new Setup {
        val apiContext = ApiContext.random

        val apiVersionDefinition = ApiVersionDefinition(apiVersion1, ApiStatus.ALPHA)

        val apiVersionDefinitions = List(apiVersionDefinition, apiVersionDefinition)
        val apiDefinition = Seq(ApiDefinition("", "", name = "MyApi", "", apiContext, apiVersionDefinitions, None, None))

        val result = developersController.getApiVersionsDropDownValues(apiDefinition)

        result.size shouldBe 1
        result.head.value shouldBe s"${apiContext.value}__${apiVersion1.value}"
      }

      "show number of entries" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenNoDataSuppliedDelegateServices()

        private val email1 = "a@example.com"
        private val email2 = "b@example.com"

        val users = List(aUser(email1), aUser(email2))

        when(mockDeveloperService.searchDevelopers(*)(*)).thenReturn(users)

        implicit val request = FakeRequest("GET", s"/developers2?emailFilter=").withSession(csrfToken, authToken, userToken)

        val result: Result = await(developersController.developersPage(Some(""))(request))

        bodyOf(result) should include("Showing 2 entries")
      }

      "allow searching by developerStatusFilter" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenNoDataSuppliedDelegateServices()

        private val emailAddress = "developer@example.com"
        private val statusFilter = "VERIFIED"
        private val user = aUser(emailAddress)

        // Note: Developers is both users and collaborators
        when(mockDeveloperService.searchDevelopers(*)(*)).thenReturn(List(user))

        val result: Result = await(developersController.developersPage(maybeDeveloperStatusFilter = Some(statusFilter))(aLoggedInRequest))

        bodyOf(result) should include(emailAddress)

        val expectedFilter = Developers2Filter(developerStatusFilter = VerifiedStatus)
        verify(mockDeveloperService).searchDevelopers(eqTo(expectedFilter))(*)
      }

      "allow searching by environmentFilter" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenNoDataSuppliedDelegateServices()

        private val emailAddress = "developer@example.com"
        private val user = aUser(emailAddress)
        private val environmentFilter = "PRODUCTION"

        // Note: Developers is both users and collaborators
        when(mockDeveloperService.searchDevelopers(*)(*)).thenReturn(List(user))

        val result: Result = await(developersController.developersPage(maybeEnvironmentFilter = Some(environmentFilter))(aLoggedInRequest))

        bodyOf(result) should include(emailAddress)

        val expectedFilter = Developers2Filter(environmentFilter = ProductionEnvironment)
        verify(mockDeveloperService).searchDevelopers(eqTo(expectedFilter))(*)
      }
    }
  }
}
