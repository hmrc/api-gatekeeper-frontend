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

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.TokenProvider

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Standard
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationResponse, ApplicationState}
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{Collaborator, Collaborators, GrantLength}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.gatekeeper.config.ErrorHandler
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.utils.WithCSRFAddToken
import uk.gov.hmrc.gatekeeper.views.html.developers._
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

class DeveloperControllerSpec extends ControllerBaseSpec with WithCSRFAddToken {

  implicit val materializer                   = app.materializer
  private lazy val errorTemplateView          = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView              = app.injector.instanceOf[ForbiddenView]
  private lazy val developerDetailsView       = app.injector.instanceOf[DeveloperDetailsView]
  private lazy val removeMfaView              = app.injector.instanceOf[RemoveMfaView]
  private lazy val removeMfaSuccessView       = app.injector.instanceOf[RemoveMfaSuccessView]
  private lazy val deleteDeveloperView        = app.injector.instanceOf[DeleteDeveloperView]
  private lazy val deleteDeveloperSuccessView = app.injector.instanceOf[DeleteDeveloperSuccessView]
  private lazy val errorHandler               = app.injector.instanceOf[ErrorHandler]

  Helpers.running(app) {

    def anApplication(collaborators: Set[Collaborator]) = {
      val grantLength = GrantLength.EIGHTEEN_MONTHS.days
      ApplicationResponse(
        ApplicationId.random,
        ClientId.random,
        "gatewayId",
        "application",
        Environment.PRODUCTION,
        None,
        collaborators,
        LocalDateTime.now(),
        Some(LocalDateTime.now()),
        Standard(),
        ApplicationState(),
        grantLength
      )
    }

    trait Setup extends ControllerSetupBase {

      val emailAddress = "someone@example.com".toLaxEmail
      val user         = RegisteredUser(emailAddress, UserId.random, "Firstname", "Lastname", true)
      val developerId  = UuidIdentifier(user.userId)

      val apps      = List(anApplication(Set(
        Collaborators.Administrator(UserId.random, emailAddress),
        Collaborators.Developer(UserId.random, "someoneelse@example.com".toLaxEmail)
      )))
      val developer = Developer(user, apps)

      val csrfToken                          = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken
      val loggedInSuperUser                  = "superUserName"
      val loggedInUser                       = "Bobby Example"
      override val aLoggedInRequest          = FakeRequest().withSession(csrfToken, authToken, userToken).withCSRFToken
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, superUserToken).withCSRFToken

      val developersController = new DeveloperController(
        mockDeveloperService,
        mockApplicationService,
        forbiddenView,
        mockApiDefinitionService,
        mcc,
        developerDetailsView,
        removeMfaView,
        removeMfaSuccessView,
        deleteDeveloperView,
        deleteDeveloperSuccessView,
        errorTemplateView,
        mockApmService,
        errorHandler,
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

      DeveloperServiceMock.RemoveMfa.returns(user)
    }

    "removeMfaPage" should {
      "not allow a user with insufficient enrolments to access the page" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        DeveloperServiceMock.FetchDeveloper.handles(developer)

        val result = developersController.removeMfaPage(developerId)(aLoggedInRequest)
        status(result) shouldBe FORBIDDEN
      }

      "allow a normal user to access the page" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        DeveloperServiceMock.FetchDeveloper.handles(developer)

        val result = addToken(developersController.removeMfaPage(developerId))(aLoggedInRequest)

        status(result) shouldBe OK
      }
    }

    "removeMfaAction" should {

      "not allow a user with insufficient enrolments to access the page" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        val result = developersController.removeMfaAction(developerId)(aLoggedInRequest)
        status(result) shouldBe FORBIDDEN
      }

      "allow a normal user to access the page and confirm removal" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        DeveloperServiceMock.RemoveMfa.returns(user)

        val request = aLoggedInRequest.withFormUrlEncodedBody(("confirm", "yes"))
        val result  = developersController.removeMfaAction(developerId)(request)

        status(result) shouldBe OK
        verify(mockDeveloperService).removeMfa(eqTo(developerId), eqTo(loggedInUser))(*)
      }

      "allow a normal user to access the page and not confirm removal" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

        val request = aLoggedInRequest.withFormUrlEncodedBody(("confirm", "no"))
        val result  = developersController.removeMfaAction(developerId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/developer?developerId=${developerId.userId.value}")
        verify(mockDeveloperService, times(0)).removeMfa(eqTo(developerId), eqTo(loggedInUser))(*)
      }

      "return an internal server error when no radio buttons are selected" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

        val result = developersController.removeMfaAction(developerId)(aLoggedInRequest)

        status(result) shouldBe BAD_REQUEST
      }

      "return an internal server error when it fails to remove MFA" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        DeveloperServiceMock.RemoveMfa.throws(new RuntimeException("Failed to remove MFA"))

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("confirm", "yes"))
        val result  = developersController.removeMfaAction(developerId)(request)

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "deleteDeveloperPage" should {

      "not allow a user with insifficient enrolments to access the page" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        val result = developersController.deleteDeveloperPage(developerId)(aLoggedInRequest)
        status(result) shouldBe FORBIDDEN
      }

      "allow a super user to access the page" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        DeveloperServiceMock.FetchDeveloper.handles(developer)
        val result = addToken(developersController.deleteDeveloperPage(developerId))(aSuperUserLoggedInRequest)
        status(result) shouldBe OK
        verify(mockDeveloperService).fetchDeveloper(eqTo(developerId), eqTo(FetchDeletedApplications.Exclude))(*)
      }
    }

    "deleteDeveloperAction" should {

      "not allow an unauthorised user to access the page" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments
        val result = developersController.deleteDeveloperAction(developerId)(aLoggedInRequest)
        status(result) shouldBe FORBIDDEN
      }

      "allow a super user to access the page" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        DeveloperServiceMock.DeleteDeveloper.returnsFor(developer, DeveloperDeleteSuccessResult)
        val result = developersController.deleteDeveloperAction(developerId)(aSuperUserLoggedInRequest)
        status(result) shouldBe OK
        verify(mockDeveloperService).deleteDeveloper(eqTo(developerId), eqTo("Bobby Example"))(*)
      }

      "return an internal server error when the delete fails" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        DeveloperServiceMock.DeleteDeveloper.returnsFor(developer, DeveloperDeleteFailureResult)
        val result = developersController.deleteDeveloperAction(developerId)(aSuperUserLoggedInRequest)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
