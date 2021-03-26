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
import org.joda.time.DateTime
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.TokenProvider
import utils.WithCSRFAddToken
import views.html.developers._
import views.html.{ErrorTemplate, ForbiddenView}

import scala.concurrent.ExecutionContext.Implicits.global

class DevelopersControllerSpec extends ControllerBaseSpec with WithCSRFAddToken {

  implicit val materializer = app.materializer
  private lazy val errorTemplateView = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView = app.injector.instanceOf[ForbiddenView]
  private lazy val developersView = app.injector.instanceOf[DevelopersView]
  private lazy val developerDetailsView = app.injector.instanceOf[DeveloperDetailsView]
  private lazy val removeMfaView = app.injector.instanceOf[RemoveMfaView]
  private lazy val removeMfaSuccessView = app.injector.instanceOf[RemoveMfaSuccessView]
  private lazy val deleteDeveloperView = app.injector.instanceOf[DeleteDeveloperView]
  private lazy val deleteDeveloperSuccessView = app.injector.instanceOf[DeleteDeveloperSuccessView]

  Helpers.running(app) {

    def anApplication(collaborators: Set[Collaborator]) = {
      ApplicationResponse(
        ApplicationId.random, ClientId.random, "gatewayId", "application", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState())
    }

    trait Setup extends ControllerSetupBase {

      val emailAddress = "someone@example.com"
      val user = RegisteredUser(emailAddress, UserId.random, "Firstname", "Lastname", true)
      val developerId = UuidIdentifier(user.userId)

      val apps = List(anApplication(Set(Collaborator(emailAddress, CollaboratorRole.ADMINISTRATOR, UserId.random),
        Collaborator("someoneelse@example.com", CollaboratorRole.ADMINISTRATOR, UserId.random))))
      val developer = Developer( user, apps )

      val csrfToken = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken
      val loggedInSuperUser = "superUserName"
      val loggedInUser = "userName"
      override val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken)
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, superUserToken)

      val developersController = new DevelopersController(
        mockDeveloperService,
        mockApplicationService,
        forbiddenView,
        mockApiDefinitionService,
        mockAuthConnector,
        mcc,
        developersView,
        developerDetailsView,
        removeMfaView,
        removeMfaSuccessView,
        deleteDeveloperView,
        deleteDeveloperSuccessView,
        errorTemplateView,
        mockApmService
      )

      def givenNoDataSuppliedDelegateServices(): Unit = {
        givenDelegateServicesSupply(List.empty[ApplicationResponse], noDevs)
      }

      def givenDelegateServicesSupply(apps: List[ApplicationResponse], developers: List[Developer]): Unit = {
        val apiFilter = ApiFilter(Some(""))
        val environmentFilter = ApiSubscriptionInEnvironmentFilter(Some(""))
        val statusFilter = StatusFilter(None)
        val users = developers.map(developer => RegisteredUser(developer.email, UserId.random, developer.firstName, developer.lastName, developer.verified, developer.organisation))
        ApplicationServiceMock.FetchApplications.returnsFor(apiFilter, environmentFilter, apps: _*)
        FetchAllApiDefinitions.inAny.returns()
        DeveloperServiceMock.FilterUsersBy.returnsFor(apiFilter, apps:_*)(developers:_*)
        DeveloperServiceMock.FilterUsersBy.returnsFor(statusFilter)(developers:_*)
        DeveloperServiceMock.GetDevelopersWithApps.returnsFor(apps:_*)(users:_*)(developers:_*)
        DeveloperServiceMock.FetchUsers.returns(users:_*)
      }

      DeveloperServiceMock.RemoveMfa.returns(user)
    }

    "developersPage" should {

      "default to page 1 with 100 items in table" in new Setup {
        givenTheGKUserIsAuthorisedAndIsANormalUser()
        givenNoDataSuppliedDelegateServices()
        val result = developersController.developersPage(None, None, None)(aLoggedInRequest)
        contentAsString(result) should include("data-page-length=\"100\"")
        verifyAuthConnectorCalledForUser
      }

      "do something else if user is not authenticated" in new Setup {
        givenTheGKUserHasInsufficientEnrolments()
        val result = developersController.developersPage(None, None, None)(aLoggedOutRequest)
        status(result) shouldBe FORBIDDEN
      }

      "load successfully if user is authenticated and authorised" in new Setup {
        givenTheGKUserIsAuthorisedAndIsANormalUser()
        givenNoDataSuppliedDelegateServices()
        val result = developersController.developersPage(None, None, None)(aLoggedInRequest)
        status(result) shouldBe OK
        contentAsString(result) should include("<h1>Developers Old</h1>")
        contentAsString(result) should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/applications\">Applications</a>")
        contentAsString(result) should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/developers2\">Developers</a>")
        verifyAuthConnectorCalledForUser
      }


      "load successfully if user is authenticated and authorised, but not show dashboard tab if external test" in new Setup {
        givenTheGKUserIsAuthorisedAndIsANormalUser()
        givenNoDataSuppliedDelegateServices()
        val result = developersController.developersPage(None, None, None)(aLoggedInRequest)
        status(result) shouldBe OK
        contentAsString(result) should include("<h1>Developers Old</h1>")
        contentAsString(result) shouldNot include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/dashboard\">Dashboard</a>")
        contentAsString(result) should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/applications\">Applications</a>")
        contentAsString(result) should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/developers2\">Developers</a>")
        verifyAuthConnectorCalledForUser
      }

      "go to unauthorised page if user is not authorised" in new Setup {
        givenAUnsuccessfulLogin()
        val result = developersController.developersPage(None, None, None)(aLoggedInRequest)
        status(result) shouldBe SEE_OTHER
      }

      "list all developers when filtering off" in new Setup {
        val users = List(
          RegisteredUser("sample@example.com", UserId.random, "Sample", "Email", false),
          RegisteredUser("another@example.com", UserId.random, "Sample2", "Email", true),
          RegisteredUser("someone@example.com", UserId.random, "Sample3", "Email", true)
        )
        val collaborators = Set(
          Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR, UserId.random), Collaborator("someone@example.com", CollaboratorRole.DEVELOPER, UserId.random))
        val applications = List(ApplicationResponse(
          ApplicationId.random, ClientId.random, "gatewayId", "application", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState()))
        val devs = users.map(Developer(_, applications))
        givenTheGKUserIsAuthorisedAndIsANormalUser()
        givenDelegateServicesSupply(applications, devs)
        val result = developersController.developersPage(None, None, None)(aLoggedInRequest)
        status(result) shouldBe OK
        collaborators.foreach(c => contentAsString(result) should include(c.emailAddress))
        verifyAuthConnectorCalledForUser
      }

      "display message if no developers found by filter" in new Setup {
        val collaborators = Set[Collaborator]()
        val applications = List(ApplicationResponse(
          ApplicationId.random, ClientId.random, "gatewayId", "application", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState()))
        givenTheGKUserIsAuthorisedAndIsANormalUser()
        givenDelegateServicesSupply(applications, noDevs)
        val result = developersController.developersPage(None, None, None)(aLoggedInRequest)
        status(result) shouldBe OK
        contentAsString(result) should include("No developers for your selected filter")
        verifyAuthConnectorCalledForUser
      }
    }

    "removeMfaPage" should {
      "not allow a user with insufficient enrolments to access the page" in new Setup {
        givenTheGKUserHasInsufficientEnrolments()
        DeveloperServiceMock.FetchDeveloper.handles(developer)

        val result = developersController.removeMfaPage(developerId)(aLoggedInRequest)
        status(result) shouldBe FORBIDDEN
      }

      "allow a normal user to access the page" in new Setup {
        givenTheGKUserIsAuthorisedAndIsANormalUser()
        DeveloperServiceMock.FetchDeveloper.handles(developer)

        val result = addToken(developersController.removeMfaPage(developerId))(aLoggedInRequest)

        status(result) shouldBe OK
        verifyAuthConnectorCalledForUser
      }
    }

    "removeMfaAction" should {

      "not allow a user with insufficient enrolments to access the page" in new Setup {
        givenTheGKUserHasInsufficientEnrolments()
        val result = developersController.removeMfaAction(developerId)(aLoggedInRequest)
        status(result) shouldBe FORBIDDEN
      }

      "allow a normal user to access the page" in new Setup {
        givenTheGKUserIsAuthorisedAndIsANormalUser()
        DeveloperServiceMock.RemoveMfa.returns(user)

        val result = developersController.removeMfaAction(developerId)(aLoggedInRequest)

        status(result) shouldBe OK
        verify(mockDeveloperService).removeMfa(eqTo(developerId), eqTo(loggedInUser))(*)
        verifyAuthConnectorCalledForUser
      }

      "return an internal server error when it fails to remove MFA" in new Setup {
        givenTheGKUserIsAuthorisedAndIsASuperUser()
        DeveloperServiceMock.RemoveMfa.throws(new RuntimeException("Failed to remove MFA"))

        val result = developersController.removeMfaAction(developerId)(aSuperUserLoggedInRequest)

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "deleteDeveloperPage" should {

      "not allow a user with insifficient enrolments to access the page" in new Setup {
        givenTheGKUserHasInsufficientEnrolments()
        val result = developersController.deleteDeveloperPage(developerId)(aLoggedInRequest)
        status(result) shouldBe FORBIDDEN
      }

      "allow a super user to access the page" in new Setup {
        givenTheGKUserIsAuthorisedAndIsASuperUser()
        DeveloperServiceMock.FetchDeveloper.handles(developer)
        val result = addToken(developersController.deleteDeveloperPage(developerId))(aSuperUserLoggedInRequest)
        status(result) shouldBe OK
        verify(mockDeveloperService).fetchDeveloper(eqTo(developerId))(*)
        verifyAuthConnectorCalledForSuperUser
      }
    }

    "deleteDeveloperAction" should {

      "not allow an unauthorised user to access the page" in new Setup {
        givenTheGKUserHasInsufficientEnrolments()
        val result = developersController.deleteDeveloperAction(developerId)(aLoggedInRequest)
        status(result) shouldBe FORBIDDEN
      }

      "allow a super user to access the page" in new Setup {
        givenTheGKUserIsAuthorisedAndIsASuperUser()
        DeveloperServiceMock.DeleteDeveloper.returnsFor(developer, DeveloperDeleteSuccessResult)
        val result = developersController.deleteDeveloperAction(developerId)(aSuperUserLoggedInRequest)
        status(result) shouldBe OK
        verify(mockDeveloperService).deleteDeveloper(eqTo(developerId), eqTo(superUserName))(*)
        verifyAuthConnectorCalledForSuperUser
      }

      "return an internal server error when the delete fails" in new Setup {
        givenTheGKUserIsAuthorisedAndIsASuperUser()
        DeveloperServiceMock.DeleteDeveloper.returnsFor(developer, DeveloperDeleteFailureResult)
        val result = developersController.deleteDeveloperAction(developerId)(aSuperUserLoggedInRequest)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
