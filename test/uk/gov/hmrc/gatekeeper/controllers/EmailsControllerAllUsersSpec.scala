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

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.utils.{TitleChecker, WithCSRFAddToken}
import uk.gov.hmrc.gatekeeper.views.html.emails._
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

class EmailsControllerAllUsersSpec extends ControllerBaseSpec with WithCSRFAddToken with TitleChecker {
  implicit val materializer: Materializer = app.materializer

  private lazy val errorTemplateView = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView     = app.injector.instanceOf[ForbiddenView]

  running(app) {

    trait Setup extends ControllerSetupBase {
      val mockEmailAllUsersView = mock[EmailAllUsersView]

      when(mockEmailAllUsersView.apply(*, *)(*, *, *)).thenReturn(play.twirl.api.HtmlFormat.empty)

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

      val verifiedUser1             = RegisteredUser("user1@hmrc.com".toLaxEmail, UserId.random, "verifiedUserA", "1", true)
      val verifiedUser2             = RegisteredUser("user2@hmrc.com".toLaxEmail, UserId.random, "verifiedUserB", "2", true)
      val verifiedUser3             = RegisteredUser("user3@hmrc.com".toLaxEmail, UserId.random, "verifiedUserC", "3", true)
      val unVerifiedUser1           = RegisteredUser("user1@somecompany.com".toLaxEmail, UserId.random, "unVerifiedUserA", "1", false)
      val users                     = List(verifiedUser1, verifiedUser2, verifiedUser3)
      val users3Verified1Unverified = users ++ List(unVerifiedUser1)
      val verified2Users            = List(verifiedUser1, verifiedUser2)

      def givenVerifiedDeveloper() = DeveloperServiceMock.FetchUsers.returns(verified2Users: _*)

      def given3VerifiedDevelopers1Unverified() = DeveloperServiceMock.FetchUsers.returns(users3Verified1Unverified: _*)

      def givenNoVerifiedDevelopers() = DeveloperServiceMock.FetchUsers.returns(unVerifiedUser1)

      val underTest = new EmailsController(
        mockDeveloperService,
        mockApiDefinitionService,
        mock[EmailLandingView],
        mock[EmailInformationView],
        mockEmailAllUsersView,
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

    "email all Users page" should {
      "on request should render correctly when 3 verified users are retrieved from developer service " in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        given3VerifiedDevelopers1Unverified()
        val result: Future[Result] = underTest.emailAllUsersPage()(aLoggedInRequest)

        status(result) shouldBe OK
        val filteredUsers       = users3Verified1Unverified.filter(_.verified)
        val expectedEmailString = filteredUsers.map(_.email.text).mkString("; ")
        verify(mockEmailAllUsersView).apply(eqTo(filteredUsers), eqTo(expectedEmailString))(*, *, *)
      }

      "on request should render correctly when 2 users are retrieved from the developer service" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenVerifiedDeveloper()
        val result: Future[Result] = underTest.emailAllUsersPage()(aLoggedInRequest)

        status(result) shouldBe OK
        val expectedEmailString = verified2Users.map(_.email.text).mkString("; ")
        verify(mockEmailAllUsersView).apply(eqTo(verified2Users), eqTo(expectedEmailString))(*, *, *)
      }

      "on request should return forbidden when not logged in to Stride Auth" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        val result: Future[Result] = underTest.emailAllUsersPage()(aLoggedInRequest)

        status(result) shouldBe FORBIDDEN

        verifyZeroInteractions(mockEmailAllUsersView)
      }

      "on request should render correctly when no verified users are retrieved from the developer service" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoVerifiedDevelopers()
        val result: Future[Result] = underTest.emailAllUsersPage()(aLoggedInRequest)

        status(result) shouldBe OK
        verify(mockEmailAllUsersView).apply(eqTo(List.empty), eqTo(""))(*, *, *)
      }
    }
  }

}
