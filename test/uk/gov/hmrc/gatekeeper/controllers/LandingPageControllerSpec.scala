/*
 * Copyright 2026 HM Revenue & Customs
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

import mocks.services._
import org.apache.pekko.stream.Materializer

import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF.TokenProvider

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.gatekeeper.config.ErrorHandler
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.utils.{CollaboratorTracker, TitleChecker, WithCSRFAddToken}
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, _}

class LandingPageControllerSpec
    extends ControllerBaseSpec
    with WithCSRFAddToken
    with TitleChecker
    with CollaboratorTracker
    with ApplicationWithCollaboratorsFixtures {

  implicit val materializer: Materializer = app.materializer

  private lazy val landingPageView = app.injector.instanceOf[LandingPageView]
  private lazy val errorHandler    = app.injector.instanceOf[ErrorHandler]
  private lazy val errorTemplate   = app.injector.instanceOf[ErrorTemplate]

  running(app) {

    trait Setup extends ControllerSetupBase
        with ApmServiceMockProvider {

      val csrfToken                 = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken
      override val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken).withCSRFToken

      LdapAuthorisationServiceMock.Auth.notAuthorised

      val underTest = new LandingPageController(
        StrideAuthorisationServiceMock.aMock,
        ApplicationQueryServiceMock.aMock,
        mcc,
        landingPageView,
        errorTemplate,
        mockApmService,
        errorHandler,
        LdapAuthorisationServiceMock.aMock
      )
    }

    "landingPage" should {
      "show page" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

        val result: Future[Result] = underTest.landingPage()(aLoggedInRequest)

        status(result) shouldBe OK
        val content = contentAsString(result)
        content should include("APIs")
        content should include("API approvals")
        content should include("Applications")
        content should include("Developers")
        content should include("XML vendors")
        content should include("Send emails based on preferences")
        content should include("Send emails based on API subscription")
        content should include("Send emails to all users")
      }

      "redirect to the login page if the user is not logged in" in new Setup {
        StrideAuthorisationServiceMock.Auth.sessionRecordNotFound

        val result = underTest.landingPage()(aLoggedInRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("http://example.com")
      }
    }
  }
}
