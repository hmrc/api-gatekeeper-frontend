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

import mocks.connectors.ApplicationConnectorMockProvider
import mocks.services.{ApmServiceMockProvider, ApplicationServiceMockProvider, RedirectUrisServiceMockProvider}
import org.apache.pekko.stream.Materializer

import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF.TokenProvider

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationServiceMockModule, StrideAuthorisationServiceMockModule}
import uk.gov.hmrc.gatekeeper.config.ErrorHandler
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.utils.{CollaboratorTracker, TitleChecker, WithCSRFAddToken}
import uk.gov.hmrc.gatekeeper.views.html.ErrorTemplate
import uk.gov.hmrc.gatekeeper.views.html.applications._

class RedirectUrisControllerSpec
    extends ControllerBaseSpec
    with WithCSRFAddToken
    with TitleChecker
    with CollaboratorTracker
    with ApplicationWithCollaboratorsFixtures {

  implicit val materializer: Materializer = app.materializer

  private lazy val errorTemplateView               = app.injector.instanceOf[ErrorTemplate]
  private lazy val manageLoginRedirectUriView      = app.injector.instanceOf[ManageLoginRedirectUriView]
  private lazy val managePostLogoutRedirectUriView = app.injector.instanceOf[ManagePostLogoutRedirectUriView]
  private lazy val errorHandler                    = app.injector.instanceOf[ErrorHandler]

  running(app) {

    trait Setup extends ControllerSetupBase
        with ApplicationServiceMockProvider
        with RedirectUrisServiceMockProvider
        with ApplicationConnectorMockProvider
        with ApmServiceMockProvider
        with StrideAuthorisationServiceMockModule
        with LdapAuthorisationServiceMockModule {

      val csrfToken                          = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken
      override val aLoggedInRequest          = FakeRequest().withSession(csrfToken, authToken, userToken).withCSRFToken
      val aLoggedInRequestForDeletedApps     = FakeRequest(GET, "?status=ALL").withSession(csrfToken, authToken, userToken).withCSRFToken
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, superUserToken).withCSRFToken
      override val anAdminLoggedInRequest    = FakeRequest().withSession(csrfToken, authToken, adminToken).withCSRFToken

      val applicationWithOverrides = ApplicationWithHistory(
        basicApplication.withAccess(Access.Standard(overrides = Set(OverrideFlag.PersistLogin))),
        List.empty
      )

      val developers = List[RegisteredUser] {
        new RegisteredUser("joe.bloggs@example.co.uk".toLaxEmail, UserId.random, "joe", "bloggs", false)
      }

      val basicAppWithDeleteRestrictionEnabled   = basicApplication.modify(_.copy(deleteRestriction = aDeleteRestriction))
      val basicAppWithDeleteRestrictionEnabledId = basicAppWithDeleteRestrictionEnabled.id
      val appWithDeleteRestrictionEnabled        = ApplicationWithHistory(basicAppWithDeleteRestrictionEnabled, List.empty)

      LdapAuthorisationServiceMock.Auth.notAuthorised

      val underTest = new RedirectUrisController(
        StrideAuthorisationServiceMock.aMock,
        mockApplicationService,
        mockRedirectUrisService,
        mcc,
        manageLoginRedirectUriView,
        managePostLogoutRedirectUriView,
        errorTemplateView,
        mockApmService,
        errorHandler,
        LdapAuthorisationServiceMock.aMock
      )

    }

    "manageLoginRedirectUrisPage" should {
      "return the manage Redirect Uri page for an admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        givenTheAppWillBeReturned()

        val result = underTest.manageLoginRedirectUriPage(applicationId)(anAdminLoggedInRequest)

        status(result) shouldBe OK
        contentAsString(result) should include("Manage Login Redirect URIs")
      }

      "return the manage Redirect Uri page for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()

        val result = underTest.manageLoginRedirectUriPage(applicationId)(aSuperUserLoggedInRequest)

        status(result) shouldBe OK
        contentAsString(result) should include("Manage Login Redirect URIs")
      }

      "return the forbidden page for a normal user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        givenTheAppWillBeReturned()

        val result = underTest.manageLoginRedirectUriPage(applicationId)(aLoggedInRequest)

        status(result) shouldBe FORBIDDEN
        contentAsString(result) should include("You do not have permission")
      }
    }

    "manageLoginRedirectUrisAction" should {
      val redirectUriToUpdate = LoginRedirectUri.unsafeApply("http://localhost:8909")

      "manage the Redirect Uri using the app service for an admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        givenTheAppWillBeReturned()
        RedirectUrisServiceMock.ManageLoginRedirectUris.succeeds()
        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("redirectUri1" -> redirectUriToUpdate.toString)

        val result = underTest.manageLoginRedirectUriAction(applicationId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString}")
        verify(mockRedirectUrisService).manageLoginRedirectUris(eqTo(application.application), eqTo(List(redirectUriToUpdate)), eqTo("Bobby Example"))(*)
      }

      "manage the Redirect Uri using the app service for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()
        RedirectUrisServiceMock.ManageLoginRedirectUris.succeeds()
        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("redirectUri1" -> redirectUriToUpdate.toString)

        val result = underTest.manageLoginRedirectUriAction(applicationId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString}")
        verify(mockRedirectUrisService).manageLoginRedirectUris(eqTo(application.application), eqTo(List(redirectUriToUpdate)), eqTo("Bobby Example"))(*)
      }

      "manage multiple Redirect Uri using the app service" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()
        RedirectUrisServiceMock.ManageLoginRedirectUris.succeeds()
        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
          "redirectUri1" -> redirectUriToUpdate.toString,
          "redirectUri2" -> "https://example.com",
          "redirectUri3" -> "https://otherexample.com"
        )

        val result = underTest.manageLoginRedirectUriAction(applicationId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString}")
        verify(mockRedirectUrisService).manageLoginRedirectUris(
          eqTo(application.application),
          eqTo(List(redirectUriToUpdate, LoginRedirectUri.unsafeApply("https://example.com"), LoginRedirectUri.unsafeApply("https://otherexample.com"))),
          eqTo("Bobby Example")
        )(*)
      }

      "manage duplicate Redirect Uri using the app service" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()
        RedirectUrisServiceMock.ManageLoginRedirectUris.succeeds()
        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
          "redirectUri1" -> redirectUriToUpdate.toString,
          "redirectUri2" -> "https://example.com",
          "redirectUri3" -> "https://example.com"
        )

        val result = underTest.manageLoginRedirectUriAction(applicationId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString}")
        verify(mockRedirectUrisService).manageLoginRedirectUris(
          eqTo(application.application),
          eqTo(List(redirectUriToUpdate, LoginRedirectUri.unsafeApply("https://example.com"))),
          eqTo("Bobby Example")
        )(*)
      }

      "manage gaps in Redirect Uri using the app service" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()
        RedirectUrisServiceMock.ManageLoginRedirectUris.succeeds()
        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
          "redirectUri1" -> redirectUriToUpdate.toString,
          "redirectUri3" -> "https://example.com",
          "redirectUri5" -> "https://example2.com"
        )

        val result = underTest.manageLoginRedirectUriAction(applicationId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString}")
        verify(mockRedirectUrisService).manageLoginRedirectUris(
          eqTo(application.application),
          eqTo(List(redirectUriToUpdate, LoginRedirectUri.unsafeApply("https://example.com"), LoginRedirectUri.unsafeApply("https://example2.com"))),
          eqTo("Bobby Example")
        )(*)
      }

      "clear the Redirect Uri when redirectUris is empty" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()
        RedirectUrisServiceMock.ManageLoginRedirectUris.succeeds()
        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody()

        val result = underTest.manageLoginRedirectUriAction(applicationId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString}")
        verify(mockRedirectUrisService).manageLoginRedirectUris(eqTo(application.application), eqTo(List()), eqTo("Bobby Example"))(*)
      }

      "return bad request for invalid values" in new Setup {
        val invalidRedirectUris: Seq[String] = Seq(
          "http://example.com/post",
          "abcdef"
        )

        invalidRedirectUris.foreach { invalidRedirectUri =>
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
          givenTheAppWillBeReturned()
          val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("redirectUri1" -> invalidRedirectUri)

          val result = underTest.manageLoginRedirectUriAction(applicationId)(request)

          status(result) shouldBe BAD_REQUEST
          verify(mockRedirectUrisService, times(0)).manageLoginRedirectUris(*, *, *)(*)
        }
      }

      "return the forbidden page for a normal user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        givenTheAppWillBeReturned()
        val request = aLoggedInRequest.withFormUrlEncodedBody("redirectUris" -> redirectUriToUpdate.toString)

        val result = underTest.manageLoginRedirectUriAction(applicationId)(request)

        status(result) shouldBe FORBIDDEN
        contentAsString(result) should include("You do not have permission")
      }
    }

    "managePostLogoutRedirectUrisPage" should {
      "return the manage post logout redirect uris page for an admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        givenTheAppWillBeReturned()

        val result = underTest.managePostLogoutRedirectUriPage(applicationId)(anAdminLoggedInRequest)

        status(result) shouldBe OK
        contentAsString(result) should include("Manage Post Logout Redirect URIs")
      }

      "return the manage post logout redirect uris page for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()

        val result = underTest.managePostLogoutRedirectUriPage(applicationId)(aSuperUserLoggedInRequest)

        status(result) shouldBe OK
        contentAsString(result) should include("Manage Post Logout Redirect URIs")
      }

      "return the forbidden page for a normal user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        givenTheAppWillBeReturned()

        val result = underTest.managePostLogoutRedirectUriPage(applicationId)(aLoggedInRequest)

        status(result) shouldBe FORBIDDEN
        contentAsString(result) should include("You do not have permission")
      }
    }

    "managePostLogoutRedirectUrisAction" should {
      val redirectUriToUpdate = PostLogoutRedirectUri.unsafeApply("http://localhost:8909")

      "manage the post logout redirect rri using the app service for an admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        givenTheAppWillBeReturned()
        RedirectUrisServiceMock.ManagePostLogoutRedirectUris.succeeds()
        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("redirectUri1" -> redirectUriToUpdate.toString)

        val result = underTest.managePostLogoutRedirectUriAction(applicationId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/$applicationId")
        verify(mockRedirectUrisService).managePostLogoutRedirectUris(eqTo(application.application), eqTo(List(redirectUriToUpdate)), eqTo("Bobby Example"))(*)
      }

      "manage the post logout redirect uri using the app service for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)

        givenTheAppWillBeReturned()
        RedirectUrisServiceMock.ManagePostLogoutRedirectUris.succeeds()
        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("redirectUri1" -> redirectUriToUpdate.toString)

        val result = underTest.managePostLogoutRedirectUriAction(applicationId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/$applicationId")
        verify(mockRedirectUrisService).managePostLogoutRedirectUris(eqTo(application.application), eqTo(List(redirectUriToUpdate)), eqTo("Bobby Example"))(*)
      }

      "manage multiple post logout redirect uris using the app service" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()
        RedirectUrisServiceMock.ManagePostLogoutRedirectUris.succeeds()
        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
          "redirectUri1" -> redirectUriToUpdate.toString,
          "redirectUri2" -> "https://example.com",
          "redirectUri3" -> "https://otherexample.com"
        )

        val result = underTest.managePostLogoutRedirectUriAction(applicationId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/$applicationId")
        verify(mockRedirectUrisService).managePostLogoutRedirectUris(
          eqTo(application.application),
          eqTo(List(redirectUriToUpdate, PostLogoutRedirectUri.unsafeApply("https://example.com"), PostLogoutRedirectUri.unsafeApply("https://otherexample.com"))),
          eqTo("Bobby Example")
        )(*)
      }

      "manage duplicate post logout redirect uris using the app service" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()
        RedirectUrisServiceMock.ManagePostLogoutRedirectUris.succeeds()
        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
          "redirectUri1" -> redirectUriToUpdate.toString,
          "redirectUri2" -> "https://example.com",
          "redirectUri3" -> "https://example.com"
        )

        val result = underTest.managePostLogoutRedirectUriAction(applicationId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/$applicationId")
        verify(mockRedirectUrisService).managePostLogoutRedirectUris(
          eqTo(application.application),
          eqTo(List(redirectUriToUpdate, PostLogoutRedirectUri.unsafeApply("https://example.com"))),
          eqTo("Bobby Example")
        )(*)
      }

      "manage gaps in post logout redirect uris using the app service" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()
        RedirectUrisServiceMock.ManagePostLogoutRedirectUris.succeeds()
        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
          "redirectUri1" -> redirectUriToUpdate.toString,
          "redirectUri3" -> "https://example.com",
          "redirectUri5" -> "https://example2.com"
        )

        val result = underTest.managePostLogoutRedirectUriAction(applicationId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/$applicationId")
        verify(mockRedirectUrisService).managePostLogoutRedirectUris(
          eqTo(application.application),
          eqTo(List(redirectUriToUpdate, PostLogoutRedirectUri.unsafeApply("https://example.com"), PostLogoutRedirectUri.unsafeApply("https://example2.com"))),
          eqTo("Bobby Example")
        )(*)
      }

      "clear the post logout redirect uri when redirectUris is empty" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()
        RedirectUrisServiceMock.ManagePostLogoutRedirectUris.succeeds()
        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody()

        val result = underTest.managePostLogoutRedirectUriAction(applicationId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/$applicationId")
        verify(mockRedirectUrisService).managePostLogoutRedirectUris(eqTo(application.application), eqTo(List()), eqTo("Bobby Example"))(*)
      }

      "return bad request for invalid values" in new Setup {
        val invalidRedirectUris: Seq[String] = Seq(
          "http://example.com/post",
          "abcdef"
        )

        invalidRedirectUris.foreach { invalidRedirectUri =>
          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
          givenTheAppWillBeReturned()
          val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("redirectUri1" -> invalidRedirectUri)

          val result = underTest.managePostLogoutRedirectUriAction(applicationId)(request)

          status(result) shouldBe BAD_REQUEST
          verify(mockRedirectUrisService, times(0)).managePostLogoutRedirectUris(*, *, *)(*)
        }
      }

      "return the forbidden page for a normal user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        givenTheAppWillBeReturned()
        val request = aLoggedInRequest.withFormUrlEncodedBody("redirectUris" -> redirectUriToUpdate.toString)

        val result = underTest.managePostLogoutRedirectUriAction(applicationId)(request)

        status(result) shouldBe FORBIDDEN
        contentAsString(result) should include("You do not have permission")
      }
    }
  }
}
