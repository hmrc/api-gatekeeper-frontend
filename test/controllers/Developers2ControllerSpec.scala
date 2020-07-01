/*
 * Copyright 2020 HM Revenue & Customs
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

import mocks.config.AppConfigMock
import model._
import model.DeveloperStatusFilter.VerifiedStatus
import org.mockito.BDDMockito._
import org.mockito.Matchers.{any, anyString, eq => meq}
import org.mockito.Mockito.verify
import play.api.mvc.Result
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.TokenProvider
import services.DeveloperService
import uk.gov.hmrc.http.HeaderCarrier
import utils.WithCSRFAddToken
import views.html.{ErrorTemplate, Forbidden}
import views.html.developers.Developers2View

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.successful

class Developers2ControllerSpec extends ControllerBaseSpec with WithCSRFAddToken {

  implicit val materializer = fakeApplication.materializer
  private lazy val errorTemplateView: ErrorTemplate = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView = app.injector.instanceOf[Forbidden]
  private lazy val developersView = app.injector.instanceOf[Developers2View]

  Helpers.running(fakeApplication) {

    def aUser(email: String) = User(email, "first", "last", verified = Some(false))

    trait Setup extends ControllerSetupBase with AppConfigMock {

      val csrfToken = "csrfToken" -> fakeApplication.injector.instanceOf[TokenProvider].generateToken
      override val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken)
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, superUserToken)

      val mockDeveloperService = mock[DeveloperService]

      val developersController = new Developers2Controller(
        mockAuthConnector,
        mockDeveloperService,
        mockApiDefinitionService,
        mcc,
        developersView,
        forbiddenView,
        errorTemplateView
      )

      def givenNoDataSuppliedDelegateServices(): Unit = {
        givenDelegateServicesSupply(Seq.empty[ApplicationResponse], noDevs)
      }

      def givenDelegateServicesSupply(apps: Seq[ApplicationResponse], developers: Seq[ApplicationDeveloper]): Unit = {
        val apiFilter = ApiFilter(Some(""))
        val environmentFilter = ApiSubscriptionInEnvironmentFilter(Some(""))
        val statusFilter = StatusFilter(None)
        val users = developers.map(developer => User(developer.email, developer.firstName, developer.lastName, developer.verified, developer.organisation))
        given(mockApplicationService.fetchApplications(meq(apiFilter), meq(environmentFilter))(any[HeaderCarrier])).willReturn(successful(apps))
        given(mockApiDefinitionService.fetchAllApiDefinitions(any())(any[HeaderCarrier])).willReturn(Seq.empty[APIDefinition])
        given(mockDeveloperService.filterUsersBy(apiFilter, apps)(developers)).willReturn(developers)
        given(mockDeveloperService.filterUsersBy(statusFilter)(developers)).willReturn(developers)
        given(mockDeveloperService.getDevelopersWithApps(meq(apps), meq(users))(any[HeaderCarrier])).willReturn(developers)
        given(mockDeveloperService.fetchUsers(any[HeaderCarrier])).willReturn(successful(users))
      }

      def givenFetchDeveloperReturns(developer: ApplicationDeveloper) = {
        given(mockDeveloperService.fetchDeveloper(meq(developer.email))(any[HeaderCarrier])).willReturn(successful(developer))
      }

      def givenDeleteDeveloperReturns(result: DeveloperDeleteResult) = {
        given(mockDeveloperService.deleteDeveloper(anyString, anyString)(any[HeaderCarrier])).willReturn(successful(result))
      }

      def givenRemoveMfaReturns(user: Future[User]): BDDMyOngoingStubbing[Future[User]] = {
        given(mockDeveloperService.removeMfa(anyString, anyString)(any[HeaderCarrier])).willReturn(user)
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
        given(mockDeveloperService.searchDevelopers(any())(any[HeaderCarrier])).willReturn(List(user))

        val result: Result = await(developersController.developersPage(Some(partialEmailAddress))(aLoggedInRequest))

        bodyOf(result) should include(emailAddress)

        val expectedFilter = Developers2Filter(Some(partialEmailAddress))
        verify(mockDeveloperService).searchDevelopers(meq(expectedFilter))(any[HeaderCarrier])
      }

      "search by empty filters values doesn't filter by them" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenNoDataSuppliedDelegateServices()

        private val emailFilter = ""
        private val apiVersionFilter = ""

        given(mockDeveloperService.searchDevelopers(any())(any[HeaderCarrier])).willReturn(List())

        val result: Result = await(developersController.developersPage(Some(emailFilter), Some(apiVersionFilter))(aLoggedInRequest))

        val expectedEmptyFilter = Developers2Filter()
        verify(mockDeveloperService).searchDevelopers(meq(expectedEmptyFilter))(any[HeaderCarrier])
      }

      "remember the search filter text on submit" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenNoDataSuppliedDelegateServices()

        private val searchFilter = "aFilter"

        given(mockDeveloperService.searchDevelopers(any())(any[HeaderCarrier])).willReturn(List.empty)

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

        given(mockDeveloperService.searchDevelopers(any())(any[HeaderCarrier])).willReturn(users)

        implicit val request = FakeRequest("GET", s"/developers2?emailFilter=").withSession(csrfToken, authToken, userToken)

        val result: Result = await(developersController.developersPage(Some(""))(request))

        bodyOf(result) should include(s"$email1; $email2")
      }

      "show an api version filter dropdown with correct display text" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenNoDataSuppliedDelegateServices()

        val apiVersions = List(APIVersion("1.0", APIStatus.ALPHA), APIVersion("2.0", APIStatus.STABLE))
        val apiDefinition = APIDefinition("", "", name = "MyApi", "", "", apiVersions, None)
        given(mockApiDefinitionService.fetchAllApiDefinitions(any())(any[HeaderCarrier])).willReturn(List(apiDefinition))

        val result = await(developersController.developersPage()(aLoggedInRequest))

        bodyOf(result) should include("MyApi (1.0) (Alpha)")
        bodyOf(result) should include("MyApi (2.0) (Stable)")

        verifyAuthConnectorCalledForUser
      }

      "show an api version filter dropdown with correct values for form submit with context and version" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenNoDataSuppliedDelegateServices()

        val apiVersions = List(APIVersion("1.0", APIStatus.STABLE), APIVersion("2.0", APIStatus.STABLE))
        val apiDefinition = APIDefinition("", "", name = "", "", context = "my-api-context", apiVersions, None)
        given(mockApiDefinitionService.fetchAllApiDefinitions(any())(any[HeaderCarrier])).willReturn(List(apiDefinition))

        val result = await(developersController.developersPage()(aLoggedInRequest))

        bodyOf(result) should include("my-api-context__1.0")
        bodyOf(result) should include("my-api-context__2.0")

        verifyAuthConnectorCalledForUser
      }

      "search by api version" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenNoDataSuppliedDelegateServices()

        private val emailAddress = "developer@example.com"
        private val user = aUser(emailAddress)
        private val apiDefinitionValueFromDropDown = "api-definition__1.0"

        // Note: Developers is both users and collaborators
        given(mockDeveloperService.searchDevelopers(any())(any[HeaderCarrier])).willReturn(List(user))

        val result: Result = await(developersController.developersPage(maybeApiVersionFilter = Some(apiDefinitionValueFromDropDown))(aLoggedInRequest))

        bodyOf(result) should include(emailAddress)

        val filter = ApiContextVersion("api-definition", "1.0")
        val expectedFilter = Developers2Filter(maybeApiFilter = Some(filter))
        verify(mockDeveloperService).searchDevelopers(meq(expectedFilter))(any[HeaderCarrier])
      }

      "show an api version filter dropdown without duplicates" in new Setup {

        val apiVersion = APIVersion("1.0", APIStatus.ALPHA)

        val apiVersions = List(apiVersion, apiVersion)
        val apiDefinition = Seq(APIDefinition("", "", name = "MyApi", "", "myApiContext", apiVersions, None))

        val result = developersController.getApiVersionsDropDownValues(apiDefinition)


        result.size shouldBe 1
        result.head.value shouldBe "myApiContext__1.0"
      }

      "show number of entries" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenNoDataSuppliedDelegateServices()

        private val email1 = "a@example.com"
        private val email2 = "b@example.com"

        val users = List(aUser(email1), aUser(email2))

        given(mockDeveloperService.searchDevelopers(any())(any[HeaderCarrier])).willReturn(users)

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
        given(mockDeveloperService.searchDevelopers(any())(any[HeaderCarrier])).willReturn(List(user))

        val result: Result = await(developersController.developersPage(maybeDeveloperStatusFilter = Some(statusFilter))(aLoggedInRequest))

        bodyOf(result) should include(emailAddress)

        val expectedFilter = Developers2Filter(developerStatusFilter = VerifiedStatus)
        verify(mockDeveloperService).searchDevelopers(meq(expectedFilter))(any[HeaderCarrier])
      }

      "allow searching by environmentFilter" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenNoDataSuppliedDelegateServices()

        private val emailAddress = "developer@example.com"
        private val user = aUser(emailAddress)
        private val environmentFilter = "PRODUCTION"

        // Note: Developers is both users and collaborators
        given(mockDeveloperService.searchDevelopers(any())(any[HeaderCarrier])).willReturn(List(user))

        val result: Result = await(developersController.developersPage(maybeEnvironmentFilter = Some(environmentFilter))(aLoggedInRequest))

        bodyOf(result) should include(emailAddress)

        val expectedFilter = Developers2Filter(environmentFilter = ProductionEnvironment)
        verify(mockDeveloperService).searchDevelopers(meq(expectedFilter))(any[HeaderCarrier])
      }
    }
  }
}
