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
import scala.concurrent.Future

import mocks.connectors.ApplicationConnectorMockProvider
import mocks.services.{ApmServiceMockProvider, ApplicationServiceMockProvider}
import org.apache.pekko.stream.Materializer
import org.jsoup.Jsoup

import play.api.mvc.Result
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
import uk.gov.hmrc.gatekeeper.views.html.applications._
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

class RedirectUrisControllerSpec
    extends ControllerBaseSpec
    with WithCSRFAddToken
    with TitleChecker
    with CollaboratorTracker
    with ApplicationWithCollaboratorsFixtures {

  implicit val materializer: Materializer = app.materializer

  private lazy val errorTemplateView     = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView         = app.injector.instanceOf[ForbiddenView]
  private lazy val applicationsView      = app.injector.instanceOf[ApplicationsView]
  private lazy val applicationView       = app.injector.instanceOf[ApplicationView]
  private lazy val manageRedirectUriView = app.injector.instanceOf[ManageRedirectUriView]
  private lazy val errorHandler          = app.injector.instanceOf[ErrorHandler]

  running(app) {

    trait Setup extends ControllerSetupBase
        with ApplicationServiceMockProvider
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
        mcc,
        manageRedirectUriView,
        errorTemplateView,
        mockApmService,
        errorHandler,
        LdapAuthorisationServiceMock.aMock
      )

    }

    "manageRedirectUrisPage" should {
      "return the manage Redirect Uri page for an admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        givenTheAppWillBeReturned()

        val result = underTest.manageRedirectUriPage(applicationId)(anAdminLoggedInRequest)

        status(result) shouldBe OK
        contentAsString(result) should include("Manage Redirect URIs")
      }

      "return the manage Redirect Uri page for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()

        val result = underTest.manageRedirectUriPage(applicationId)(aSuperUserLoggedInRequest)

        status(result) shouldBe OK
        contentAsString(result) should include("Manage Redirect URIs")
      }

      "return the forbidden page for a normal user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        givenTheAppWillBeReturned()

        val result = underTest.manageRedirectUriPage(applicationId)(aLoggedInRequest)

        status(result) shouldBe FORBIDDEN
        contentAsString(result) should include("You do not have permission")
      }
    }

    "manageRedirectUrisAction" should {
      val redirectUriToUpdate = LoginRedirectUri.unsafeApply("http://localhost:8909")

      "manage the Redirect Uri using the app service for an admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        givenTheAppWillBeReturned()
        ApplicationServiceMock.ManageRedirectUris.succeeds()
        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("redirectUri1" -> redirectUriToUpdate.toString)

        val result = underTest.manageRedirectUriAction(applicationId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString}")
        verify(mockApplicationService).manageRedirectUris(eqTo(application.application), eqTo(List(redirectUriToUpdate)), eqTo("Bobby Example"))(*)
      }

      "manage the Redirect Uri using the app service for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()
        ApplicationServiceMock.ManageRedirectUris.succeeds()
        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("redirectUri1" -> redirectUriToUpdate.toString)

        val result = underTest.manageRedirectUriAction(applicationId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString}")
        verify(mockApplicationService).manageRedirectUris(eqTo(application.application), eqTo(List(redirectUriToUpdate)), eqTo("Bobby Example"))(*)
      }

      "manage multiple Redirect Uri using the app service" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()
        ApplicationServiceMock.ManageRedirectUris.succeeds()
        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
          "redirectUri1" -> redirectUriToUpdate.toString,
          "redirectUri2" -> "https://example.com",
          "redirectUri3" -> "https://otherexample.com"
        )

        val result = underTest.manageRedirectUriAction(applicationId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString}")
        verify(mockApplicationService).manageRedirectUris(
          eqTo(application.application),
          eqTo(List(redirectUriToUpdate, LoginRedirectUri.unsafeApply("https://example.com"), LoginRedirectUri.unsafeApply("https://otherexample.com"))),
          eqTo("Bobby Example")
        )(*)
      }

      "manage duplicate Redirect Uri using the app service" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()
        ApplicationServiceMock.ManageRedirectUris.succeeds()
        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
          "redirectUri1" -> redirectUriToUpdate.toString,
          "redirectUri2" -> "https://example.com",
          "redirectUri3" -> "https://example.com"
        )

        val result = underTest.manageRedirectUriAction(applicationId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString}")
        verify(mockApplicationService).manageRedirectUris(
          eqTo(application.application),
          eqTo(List(redirectUriToUpdate, LoginRedirectUri.unsafeApply("https://example.com"))),
          eqTo("Bobby Example")
        )(*)
      }

      "manage gaps in Redirect Uri using the app service" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()
        ApplicationServiceMock.ManageRedirectUris.succeeds()
        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
          "redirectUri1" -> redirectUriToUpdate.toString,
          "redirectUri3" -> "https://example.com",
          "redirectUri5" -> "https://example2.com"
        )

        val result = underTest.manageRedirectUriAction(applicationId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString}")
        verify(mockApplicationService).manageRedirectUris(
          eqTo(application.application),
          eqTo(List(redirectUriToUpdate, LoginRedirectUri.unsafeApply("https://example.com"), LoginRedirectUri.unsafeApply("https://example2.com"))),
          eqTo("Bobby Example")
        )(*)
      }

      "clear the Redirect Uri when redirectUris is empty" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()
        ApplicationServiceMock.ManageRedirectUris.succeeds()
        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody()

        val result = underTest.manageRedirectUriAction(applicationId)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString}")
        verify(mockApplicationService).manageRedirectUris(eqTo(application.application), eqTo(List()), eqTo("Bobby Example"))(*)
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

          val result = underTest.manageRedirectUriAction(applicationId)(request)

          status(result) shouldBe BAD_REQUEST
          verify(mockApplicationService, times(0)).manageRedirectUris(*, *, *)(*)
        }
      }

      "return the forbidden page for a normal user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        givenTheAppWillBeReturned()
        val request = aLoggedInRequest.withFormUrlEncodedBody("redirectUris" -> redirectUriToUpdate.toString)

        val result = underTest.manageRedirectUriAction(applicationId)(request)

        status(result) shouldBe FORBIDDEN
        contentAsString(result) should include("You do not have permission")
      }
    }

    def assertIncludesOneError(result: Future[Result], message: String) = {

      val body = contentAsString(result)

      body should include(message)
      assert(Jsoup.parse(body).getElementsByClass("govuk-form-group--error").size == 1)
    }
  }
}
