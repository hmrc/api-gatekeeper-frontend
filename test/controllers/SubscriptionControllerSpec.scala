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

import utils.WithCSRFAddToken
import utils.TitleChecker
import model._
import views.html._
import play.filters.csrf.CSRF.TokenProvider
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.FakeRequestCSRFSupport._
import views.html.applications.ManageSubscriptionsView
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.successful
import builder.ApplicationBuilder
import model.applications.ApplicationWithSubscriptionData
import builder.ApiBuilder

class SubscriptionControllerSpec 
    extends ControllerBaseSpec 
    with WithCSRFAddToken
    with TitleChecker {
      
  implicit val materializer = app.materializer

  private lazy val errorTemplateView = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView = app.injector.instanceOf[ForbiddenView]
  private lazy val manageSubscriptionsView = app.injector.instanceOf[ManageSubscriptionsView]

  running(app) {

    trait Setup extends ControllerSetupBase {

      val csrfToken = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken
      override val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken).withCSRFToken
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, superUserToken).withCSRFToken
      override val anAdminLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, adminToken).withCSRFToken

      val applicationWithOverrides = ApplicationWithHistory(
        basicApplication.copy(access = Standard(overrides = Set(PersistLogin()))), List.empty)

      val privilegedApplication = ApplicationWithHistory(
        basicApplication.copy(access = Privileged(scopes = Set("openid", "email"))), List.empty)

      val ropcApplication = ApplicationWithHistory(
        basicApplication.copy(access = Ropc(scopes = Set("openid", "email"))), List.empty)

      def aPaginatedApplicationResponse(applications: List[ApplicationResponse]): PaginatedApplicationResponse = {
      val page = 1
      val pageSize = 10
      PaginatedApplicationResponse(applications, page, pageSize, total = applications.size, matching = applications.size)
    }

      val underTest = new SubscriptionController(
        manageSubscriptionsView,
        mcc,
        forbiddenView,
        mockAuthConnector,
        errorTemplateView,
        mockApplicationService,
        mockApmService
      )

      def givenThePaginatedApplicationsWillBeReturned = {
        ApplicationServiceMock.SearchApplications.returns()
        FetchAllApiDefinitions.inAny.returns()
      }
    }


    "subscribeToApi" should {
      val apiContext = ApiContext.random

      "call the service to subscribe to the API when submitted for a super user" in new Setup {
        givenTheGKUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned()

        ApplicationServiceMock.SubscribeToApi.succeeds()

        val result = addToken(underTest.subscribeToApi(applicationId, apiContext, ApiVersion("1.0")))(aSuperUserLoggedInRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value}/subscriptions")

        verify(mockApplicationService).subscribeToApi(eqTo(basicApplication), eqTo(apiContext), eqTo(ApiVersion("1.0")))(*)
        verifyAuthConnectorCalledForSuperUser
      }

      "return forbidden when submitted for a non-super user" in new Setup {
        givenTheGKUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned()

        val result = addToken(underTest.subscribeToApi(applicationId, apiContext, ApiVersion.random))(aLoggedInRequest)

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).subscribeToApi(eqTo(basicApplication), *[ApiContext], *[ApiVersion])(*)
      }
    }

    "unsubscribeFromApi" should {
      val apiContext = ApiContext.random

      "call the service to unsubscribe from the API when submitted for a super user" in new Setup {
        givenTheGKUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned()

        ApplicationServiceMock.UnsubscribeFromApi.succeeds()

        val result = addToken(underTest.unsubscribeFromApi(applicationId, apiContext, ApiVersion("1.0")))(aSuperUserLoggedInRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value}/subscriptions")

        verify(mockApplicationService).unsubscribeFromApi(eqTo(basicApplication), eqTo(apiContext), eqTo(ApiVersion("1.0")))(*)
        verifyAuthConnectorCalledForSuperUser
      }

      "return forbidden when submitted for a non-super user" in new Setup {
        givenTheGKUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned()

        val result = addToken(underTest.unsubscribeFromApi(applicationId, apiContext, ApiVersion.random))(aLoggedInRequest)

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).unsubscribeFromApi(*, *[ApiContext], *[ApiVersion])(*)
      }
    }

    "manageSubscription" when {
      "the user is a superuser" should {
        "fetch the subscriptions with the fields" in new Setup with ApplicationBuilder with ApiBuilder {

          val newApplication = buildApplication()
          val applicationWithSubscriptionData = ApplicationWithSubscriptionData(newApplication, Set.empty, Map.empty)
          val apiData = DefaultApiData.withName("API NAme").addVersion(VersionOne, DefaultVersionData)
          val apiContext = ApiContext("Api Context")
          val apiContextAndApiData = Map(apiContext -> apiData)

          givenTheGKUserIsAuthorisedAndIsASuperUser()
          ApmServiceMock.FetchApplicationById.returns(applicationWithSubscriptionData)
          ApmServiceMock.fetchAllPossibleSubscriptionsReturns(apiContextAndApiData)

          val result = addToken(underTest.manageSubscription(applicationId))(aSuperUserLoggedInRequest)

          status(result) shouldBe OK
          verifyAuthConnectorCalledForSuperUser
        }
      }

      "the user is not a superuser" should {
        "show 403 forbidden" in new Setup {
          givenTheGKUserHasInsufficientEnrolments()

          val result = addToken(underTest.manageSubscription(applicationId))(aLoggedInRequest)

          status(result) shouldBe FORBIDDEN
        }
      }
    }
  }
}
