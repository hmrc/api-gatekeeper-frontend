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

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.utils.{TitleChecker, WithCSRFAddToken}
import uk.gov.hmrc.gatekeeper.views.html.emails._
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

class EmailsControllerApiSubscriptionSpec extends ControllerBaseSpec with WithCSRFAddToken with TitleChecker {

  implicit val materializer: Materializer = app.materializer

  private lazy val errorTemplateView = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView     = app.injector.instanceOf[ForbiddenView]

  running(app) {

    trait Setup extends ControllerSetupBase {
      val mockEmailApiSubscriptionsView = mock[EmailApiSubscriptionsView]

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

      val verifiedUser1  = RegisteredUser("user1@hmrc.com".toLaxEmail, UserId.random, "verifiedUserA", "1", true)
      val verifiedUser2  = RegisteredUser("user2@hmrc.com".toLaxEmail, UserId.random, "verifiedUserB", "2", true)
      val verifiedUser3  = RegisteredUser("user3@hmrc.com".toLaxEmail, UserId.random, "verifiedUserC", "3", true)
      val users          = List(verifiedUser1, verifiedUser2, verifiedUser3)
      val verified2Users = List(verifiedUser1, verifiedUser2)
      val category1      = ApiCategory.EXAMPLE
      val category2      = ApiCategory.VAT

      val apiVersion1 = ApiVersionNbr("1.0")
      val apiVersion3 = ApiVersionNbr("3.0")

      def givenVerifiedDeveloper() = DeveloperServiceMock.FetchUsers.returns(verified2Users: _*)

      def givenViewIsPassedCorrectUsers(users: List[RegisteredUser], emailString: String) = {
        when(mockEmailApiSubscriptionsView.apply(*, eqTo(users), eqTo(emailString), *)(*, *, *)).thenReturn(play.twirl.api.HtmlFormat.empty)
      }

      val api1    = ApiDefinition(
        ServiceName("service1"),
        "/",
        "serviceName",
        "serviceDesc",
        ApiContext("service1"),
        Map(apiVersion1 -> ApiVersion(apiVersion1, ApiStatus.BETA, ApiAccess.PUBLIC, List.empty, false, None, ApiVersionSource.UNKNOWN)),
        false,
        None,
        List(category1)
      )
      val api2    = ApiDefinition(
        ServiceName("service2"),
        "/",
        "service2Name",
        "service2Desc",
        ApiContext("service2"),
        Map(apiVersion3 -> ApiVersion(apiVersion3, ApiStatus.STABLE, ApiAccess.PUBLIC, List.empty, false, None, ApiVersionSource.UNKNOWN)),
        false,
        None,
        List(category2)
      )
      val twoApis = List(api1, api2)

      def givenApiDefinition2Apis() = {
        FetchAllApiDefinitions.inAny.returns(twoApis: _*)
      }

      def given2ApplicationsWithSubscriptions(apiFilter: ApiFilter[String], applications: List[ApplicationWithCollaborators]) = {
        ApplicationServiceMock.FetchApplications.returnsFor(apiFilter, ProductionEnvironment, applications: _*)
        ApplicationServiceMock.FetchApplications.returnsFor(apiFilter, SandboxEnvironment, applications: _*)
      }

      def givenDevelopersByEmail(users: List[RegisteredUser]) = {
        DeveloperServiceMock.FetchDevelopersByEmails.returns(users: _*)
      }

      val underTest = new EmailsController(
        mockDeveloperService,
        mockApiDefinitionService,
        mock[EmailLandingView],
        mock[EmailInformationView],
        mock[EmailAllUsersView],
        mockEmailApiSubscriptionsView,
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

    "email subscribers page" should {
      "render correctly when no filter provided and logged in via Stride as user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenApiDefinition2Apis()
        givenViewIsPassedCorrectUsers(List.empty, "")

        val result: Future[Result] = underTest.emailApiSubscribersPage()(FakeRequest())
        status(result) shouldBe OK

        verify(mockEmailApiSubscriptionsView).apply(eqTo(underTest.getApiVersionsDropDownValues(twoApis)), eqTo(List.empty), eqTo(""), eqTo(Map.empty))(*, *, *)
      }

      "render correctly when no filter provided and not logged in via Stride" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments
        givenApiDefinition2Apis()
        givenViewIsPassedCorrectUsers(List.empty, "")

        val result: Future[Result] = underTest.emailApiSubscribersPage()(FakeRequest())
        status(result) shouldBe FORBIDDEN

        verifyZeroInteractions(mockEmailApiSubscriptionsView)
      }

      "render correctly when api filter provided and logged in via Stride as user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenApiDefinition2Apis()
        given2ApplicationsWithSubscriptions(ApiFilter(Some("service2__3")), List.empty)
        givenDevelopersByEmail(users)

        givenViewIsPassedCorrectUsers(users, users.map(_.email.text).sorted.mkString("; "))

        val result: Future[Result] = underTest.emailApiSubscribersPage(Some("service2__3"))(createGetRequest("/emails/api-subscribers?apiVersionFilter=service2__3"))
        status(result) shouldBe OK

        verify(mockEmailApiSubscriptionsView).apply(
          eqTo(underTest.getApiVersionsDropDownValues(twoApis)),
          eqTo(users),
          eqTo(users.map(_.email.text).sorted.mkString("; ")),
          eqTo(Map("apiVersionFilter" -> "service2__3"))
        )(*, *, *)
      }
    }
  }
}
