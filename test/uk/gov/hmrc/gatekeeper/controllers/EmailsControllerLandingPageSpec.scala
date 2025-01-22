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

import org.apache.pekko.stream.Materializer

import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF.TokenProvider

import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.utils.{TitleChecker, WithCSRFAddToken}
import uk.gov.hmrc.gatekeeper.views.html.emails._
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

class EmailsControllerLandingPageSpec extends ControllerBaseSpec with WithCSRFAddToken with TitleChecker {

  implicit val materializer: Materializer = app.materializer

  private lazy val errorTemplateView = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView     = app.injector.instanceOf[ForbiddenView]

  running(app) {

    trait Setup extends ControllerSetupBase {
      val mockEmailLandingView = mock[EmailLandingView]
      when(mockEmailLandingView.apply()(*, *, *)).thenReturn(play.twirl.api.HtmlFormat.empty)

      val csrfToken: (String, String)                                             = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken
      override val aLoggedInRequest: FakeRequest[AnyContentAsEmpty.type]          = FakeRequest().withSession(csrfToken, authToken, userToken).withCSRFToken
      override val aSuperUserLoggedInRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest().withSession(csrfToken, authToken, superUserToken).withCSRFToken
      override val anAdminLoggedInRequest: FakeRequest[AnyContentAsEmpty.type]    = FakeRequest().withSession(csrfToken, authToken, adminToken).withCSRFToken

      def createGetRequest(path: String) = {
        FakeRequest("GET", path)
          .withSession(csrfToken, authToken, userToken)
          .withCSRFToken
      }

      val underTest = new EmailsController(
        mockDeveloperService,
        mockApiDefinitionService,
        mockEmailLandingView,
        mock[EmailInformationView],
        mock[EmailAllUsersView],
        mock[EmailApiSubscriptionsView],
        mock[EmailPreferencesChoiceView],
        mock[EmailPreferencesTopicView],
        mock[EmailPreferencesApiCategoryView],
        mock[EmailPreferencesSpecificApiView],
        mock[EmailPreferencesSelectApiView],
        mock[EmailPreferencesSelectTopicView],
        mockApplicationService,
        forbiddenView,
        mcc,
        errorTemplateView,
        mockApmService,
        StrideAuthorisationServiceMock.aMock
      )
    }

    "email landing page" should {
      "on initial request with logged in user should display disabled options and checked email all options" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        val result: Future[Result] = underTest.landing()(aLoggedInRequest)
        status(result) shouldBe OK

        verify(mockEmailLandingView).apply()(*, *, *)
      }

      "on initial request with user not logged in should returned forbidden" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments
        val result: Future[Result] = underTest.landing()(aLoggedInRequest)
        status(result) shouldBe FORBIDDEN

        verifyZeroInteractions(mockEmailLandingView)
      }
    }
  }

}
