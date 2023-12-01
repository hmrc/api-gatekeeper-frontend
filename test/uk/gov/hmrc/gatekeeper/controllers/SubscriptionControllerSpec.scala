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

import mocks.services._

import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF.TokenProvider

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.OverrideFlag.PersistLogin
import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.{Privileged, Ropc, Standard}
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationResponse
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.gatekeeper.builder.{ApiBuilder, ApplicationBuilder}
import uk.gov.hmrc.gatekeeper.config.ErrorHandler
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.applications.ApplicationWithSubscriptionData
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.utils.{TitleChecker, WithCSRFAddToken}
import uk.gov.hmrc.gatekeeper.views.html._
import uk.gov.hmrc.gatekeeper.views.html.applications.ManageSubscriptionsView

class SubscriptionControllerSpec
    extends ControllerBaseSpec
    with WithCSRFAddToken
    with TitleChecker {

  implicit val materializer = app.materializer

  private lazy val errorTemplateView       = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView           = app.injector.instanceOf[ForbiddenView]
  private lazy val manageSubscriptionsView = app.injector.instanceOf[ManageSubscriptionsView]
  private lazy val errorHandler            = app.injector.instanceOf[ErrorHandler]

  val gatekeeperUser = Actors.GatekeeperUser("Bobby Example")

  running(app) {

    trait Setup extends ControllerSetupBase with SubscriptionsServiceMockModule {

      val csrfToken                          = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken
      override val aLoggedInRequest          = FakeRequest().withSession(csrfToken, authToken, userToken).withCSRFToken
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, superUserToken).withCSRFToken
      override val anAdminLoggedInRequest    = FakeRequest().withSession(csrfToken, authToken, adminToken).withCSRFToken

      val applicationWithOverrides = ApplicationWithHistory(
        basicApplication.copy(access = Standard(overrides = Set(PersistLogin))),
        List.empty
      )

      val privilegedApplication = ApplicationWithHistory(
        basicApplication.copy(access = Privileged(scopes = Set("openid", "email"))),
        List.empty
      )

      val ropcApplication = ApplicationWithHistory(
        basicApplication.copy(access = Ropc(scopes = Set("openid", "email"))),
        List.empty
      )

      def aPaginatedApplicationResponse(applications: List[ApplicationResponse]): PaginatedApplicationResponse = {
        val page     = 1
        val pageSize = 10
        PaginatedApplicationResponse(applications, page, pageSize, total = applications.size, matching = applications.size)
      }

      val underTest = new SubscriptionController(
        manageSubscriptionsView,
        mcc,
        forbiddenView,
        errorTemplateView,
        mockApplicationService,
        SubscriptionsServiceMock.aMock,
        mockApmService,
        errorHandler,
        StrideAuthorisationServiceMock.aMock
      )

      def givenThePaginatedApplicationsWillBeReturned = {
        ApplicationServiceMock.SearchApplications.returns()
        FetchAllApiDefinitions.inAny.returns()
      }
    }

    "subscribeToApi" should {
      val apiContext = ApiContext.random

      "call the service to subscribe to the API when submitted for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()

        SubscriptionsServiceMock.SubscribeToApi.succeeds()

        val result = addToken(underTest.subscribeToApi(applicationId, apiContext, ApiVersionNbr("1.0")))(aSuperUserLoggedInRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString()}/subscriptions")

        SubscriptionsServiceMock.SubscribeToApi.verifyCalledWith(basicApplication, ApiIdentifier(apiContext, ApiVersionNbr("1.0")), gatekeeperUser)
      }

      "return forbidden when submitted for a non-super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        givenTheAppWillBeReturned()

        val result = addToken(underTest.subscribeToApi(applicationId, apiContext, ApiVersionNbr.random))(aLoggedInRequest)

        status(result) shouldBe FORBIDDEN

        SubscriptionsServiceMock.SubscribeToApi.verifyNotCalled()
      }
    }

    "unsubscribeFromApi" should {
      val apiContext = ApiContext.random

      "call the service to unsubscribe from the API when submitted for a super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()

        SubscriptionsServiceMock.UnsubscribeFromApi.succeeds()

        val result = addToken(underTest.unsubscribeFromApi(applicationId, apiContext, ApiVersionNbr("1.0")))(aSuperUserLoggedInRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString()}/subscriptions")

        SubscriptionsServiceMock.UnsubscribeFromApi.verifyCalledWith(basicApplication, ApiIdentifier(apiContext, ApiVersionNbr("1.0")), gatekeeperUser)
      }

      "return forbidden when submitted for a non-super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        givenTheAppWillBeReturned()

        val result = addToken(underTest.unsubscribeFromApi(applicationId, apiContext, ApiVersionNbr.random))(aLoggedInRequest)

        status(result) shouldBe FORBIDDEN

        SubscriptionsServiceMock.UnsubscribeFromApi.verifyNotCalled()
      }
    }

    "manageSubscription" when {
      "the user is a superuser" should {
        "fetch the subscriptions with the fields" in new Setup with ApplicationBuilder with ApiBuilder {

          val newApplication                  = DefaultApplication
          val applicationWithSubscriptionData = ApplicationWithSubscriptionData(newApplication, Set.empty, Map.empty)
          val apiDefinition                   = DefaultApiDefinition.withName("API NAme").addVersion(VersionOne, DefaultVersionData)
          val possibleSubs                    = List(apiDefinition)

          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
          ApmServiceMock.FetchApplicationById.returns(applicationWithSubscriptionData)
          ApmServiceMock.fetchAllPossibleSubscriptionsReturns(possibleSubs)

          val result = addToken(underTest.manageSubscription(applicationId))(aSuperUserLoggedInRequest)

          status(result) shouldBe OK
        }
      }

      "the user is not a superuser" should {
        "show 403 forbidden" in new Setup {
          StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

          val result = addToken(underTest.manageSubscription(applicationId))(aLoggedInRequest)

          status(result) shouldBe FORBIDDEN
        }
      }
    }
  }
}
