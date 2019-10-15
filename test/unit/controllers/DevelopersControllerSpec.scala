/*
 * Copyright 2019 HM Revenue & Customs
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

package unit.controllers

import java.util.UUID

import controllers.DevelopersController
import model._
import org.joda.time.DateTime
import org.mockito.BDDMockito._
import org.mockito.Matchers.{any, anyString, eq => meq}
import org.mockito.Mockito.verify
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.TokenProvider
import services.DeveloperService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import unit.utils.WithCSRFAddToken

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class DevelopersControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication with WithCSRFAddToken {

  implicit val materializer = fakeApplication.materializer

  Helpers.running(fakeApplication) {

    def anApplication(collaborators: Set[Collaborator]) = {
      ApplicationResponse(
        UUID.randomUUID(), "clientid", "gatewayId", "application", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState())
    }

    trait Setup extends ControllerSetupBase {

      implicit val appConfig = mockConfig
      val csrfToken = "csrfToken" -> fakeApplication.injector.instanceOf[TokenProvider].generateToken
      val loggedInSuperUser = "superUserName"
      override val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken)
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, superUserToken)
      given(appConfig.superUsers).willReturn(Seq(loggedInSuperUser))
      given(appConfig.strideLoginUrl).willReturn("https://loginUri")
      given(appConfig.appName).willReturn("Gatekeeper app name")
      given(appConfig.gatekeeperSuccessUrl).willReturn("successUrl_not_checked")

      val mockDeveloperService = mock[DeveloperService]

      val developersController = new DevelopersController(mockDeveloperService, mockApplicationService, mockApiDefinitionService, mockAuthConnector) {
        override val appConfig = mockConfig
      }

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

      "default to page 1 with 100 items in table" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenNoDataSuppliedDelegateServices()
        val result = await(developersController.developersPage(None, None, None)(aLoggedInRequest))
        bodyOf(result) should include("data-page-length=\"100\"")
        verifyAuthConnectorCalledForUser
      }

      "do something else if user is not authenticated" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        val result = await(developersController.developersPage(None, None, None)(aLoggedOutRequest))
        status(result) shouldBe FORBIDDEN
      }

      "load successfully if user is authenticated and authorised" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenNoDataSuppliedDelegateServices()
        val result = await(developersController.developersPage(None, None, None)(aLoggedInRequest))
        status(result) shouldBe OK
        bodyOf(result) should include("<h1>Developers Old</h1>")
        bodyOf(result) should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/applications\">Applications</a>")
        bodyOf(result) should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/developers2\">Developers</a>")
        verifyAuthConnectorCalledForUser
      }


      "load successfully if user is authenticated and authorised, but not show dashboard tab if external test" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenNoDataSuppliedDelegateServices()
        given(developersController.appConfig.isExternalTestEnvironment).willReturn(true)
        val result = await(developersController.developersPage(None, None, None)(aLoggedInRequest))
        status(result) shouldBe OK
        bodyOf(result) should include("<h1>Developers Old</h1>")
        bodyOf(result) shouldNot include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/dashboard\">Dashboard</a>")
        bodyOf(result) should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/applications\">Applications</a>")
        bodyOf(result) should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/developers2\">Developers</a>")
        verifyAuthConnectorCalledForUser
      }

      "go to unauthorised page if user is not authorised" in new Setup {
        givenAUnsuccessfulLogin()
        val result = await(developersController.developersPage(None, None, None)(aLoggedInRequest))
        status(result) shouldBe SEE_OTHER
      }

      "list all developers when filtering off" in new Setup {
        val users = Seq(
          User("sample@example.com", "Sample", "Email", Some(false)),
          User("another@example.com", "Sample2", "Email", Some(true)),
          User("someone@example.com", "Sample3", "Email", Some(true)))
        val collaborators = Set(
          Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR), Collaborator("someone@example.com", CollaboratorRole.DEVELOPER))
        val applications = Seq(ApplicationResponse(
          UUID.randomUUID(), "clientid", "gatewayId", "application", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState()))
        val devs = users.map(Developer.createFromUser(_, applications))
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenDelegateServicesSupply(applications, devs)
        val result = await(developersController.developersPage(None, None, None)(aLoggedInRequest))
        status(result) shouldBe OK
        collaborators.foreach(c => bodyOf(result) should include(c.emailAddress))
        verifyAuthConnectorCalledForUser
      }

      "display message if no developers found by filter" in new Setup {
        val collaborators = Set[Collaborator]()
        val applications = Seq(ApplicationResponse(
          UUID.randomUUID(), "clientid", "gatewayId", "application", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState()))
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenDelegateServicesSupply(applications, noDevs)
        val result = await(developersController.developersPage(None, None, None)(aLoggedInRequest))
        status(result) shouldBe OK
        bodyOf(result) should include("No developers for your selected filter")
        verifyAuthConnectorCalledForUser
      }
    }

    "removeMfaPage" should {
      val emailAddress = "someone@example.com"

      "not allow a user with insufficient enrolments to access the page" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        val result: Result = await(developersController.removeMfaPage(emailAddress)(aLoggedInRequest))
        status(result) shouldBe FORBIDDEN
      }

      "allow a super user to access the page" in new Setup {
        val apps = Seq(anApplication(Set(Collaborator(emailAddress, CollaboratorRole.ADMINISTRATOR),
          Collaborator("someoneelse@example.com", CollaboratorRole.ADMINISTRATOR))))
        val developer: Developer = User(emailAddress, "Firstname", "Lastname", Some(true)).toDeveloper(apps)
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenFetchDeveloperReturns(developer)

        val result: Result = await(addToken(developersController.removeMfaPage(emailAddress))(aSuperUserLoggedInRequest))

        status(result) shouldBe OK
        verify(mockDeveloperService).fetchDeveloper(meq(emailAddress))(any[HeaderCarrier])
        verifyAuthConnectorCalledForSuperUser
      }
    }

    "removeMfaAction" should {
      val emailAddress = "someone@example.com"

      "not allow a user with insufficient enrolments to access the page" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        val result: Result = await(developersController.removeMfaAction(emailAddress)(aLoggedInRequest))
        status(result) shouldBe FORBIDDEN
      }

      "allow a super user to access the page" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenRemoveMfaReturns(successful(User(emailAddress, "Firstname", "Lastname", Some(true))))

        val result: Result = await(developersController.removeMfaAction(emailAddress)(aSuperUserLoggedInRequest))

        status(result) shouldBe OK
        verify(mockDeveloperService).removeMfa(meq(emailAddress), meq(loggedInSuperUser))(any[HeaderCarrier])
        verifyAuthConnectorCalledForSuperUser
      }

      "return an internal server error when it fails to remove MFA" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenRemoveMfaReturns(failed(new RuntimeException("Failed to remove MFA")))

        val result: Result = await(developersController.removeMfaAction(emailAddress)(aSuperUserLoggedInRequest))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "deleteDeveloperPage" should {
      val emailAddress = "someone@example.com"
      val apps = Seq(anApplication(Set(Collaborator(emailAddress, CollaboratorRole.ADMINISTRATOR),
        Collaborator("someoneelse@example.com", CollaboratorRole.ADMINISTRATOR))))
      val developer = User(emailAddress, "Firstname", "Lastname", Some(true)).toDeveloper(apps)

      "not allow a user with insifficient enrolments to access the page" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        val result = await(developersController.deleteDeveloperPage(emailAddress)(aLoggedInRequest))
        status(result) shouldBe FORBIDDEN
      }

      "allow a super user to access the page" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenFetchDeveloperReturns(developer)
        val result = await(addToken(developersController.deleteDeveloperPage(emailAddress))(aSuperUserLoggedInRequest))
        status(result) shouldBe OK
        verify(mockDeveloperService).fetchDeveloper(meq(emailAddress))(any[HeaderCarrier])
        verifyAuthConnectorCalledForSuperUser
      }
    }

    "deleteDeveloperAction" should {
      val emailAddress = "someone@example.com"

      "not allow an unauthorised user to access the page" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        val result = await(developersController.deleteDeveloperAction(emailAddress)(aLoggedInRequest))
        status(result) shouldBe FORBIDDEN
      }

      "allow a super user to access the page" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenDeleteDeveloperReturns(DeveloperDeleteSuccessResult)
        val result = await(developersController.deleteDeveloperAction(emailAddress)(aSuperUserLoggedInRequest))
        status(result) shouldBe OK
        verify(mockDeveloperService).deleteDeveloper(meq(emailAddress), meq(superUserName))(any[HeaderCarrier])
        verifyAuthConnectorCalledForSuperUser
      }

      "return an internal server error when the delete fails" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenDeleteDeveloperReturns(DeveloperDeleteFailureResult)
        val result = await(developersController.deleteDeveloperAction(emailAddress)(aSuperUserLoggedInRequest))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
