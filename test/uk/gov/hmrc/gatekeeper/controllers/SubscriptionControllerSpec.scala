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

import java.time.{Clock, Instant, LocalDateTime, ZoneOffset}
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF.TokenProvider

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
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
  private lazy val fixedClock              = Clock.fixed(Instant.now(), ZoneOffset.UTC)

  running(app) {

    trait Setup extends ControllerSetupBase {

      val csrfToken                          = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken
      override val aLoggedInRequest          = FakeRequest().withSession(csrfToken, authToken, userToken).withCSRFToken
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, superUserToken).withCSRFToken
      override val anAdminLoggedInRequest    = FakeRequest().withSession(csrfToken, authToken, adminToken).withCSRFToken

      val underTest = new SubscriptionController(
        manageSubscriptionsView,
        mcc,
        forbiddenView,
        errorTemplateView,
        mockApplicationService,
        mockApmService,
        errorHandler,
        StrideAuthorisationServiceMock.aMock,
        fixedClock
      )
    }

    "subscribeToApi" should {
      val apiIdentifier = ApiIdentifier.random

      "call the service to subscribe to the API when submitted for a super user" in new Setup {
        val subscribeToApi = SubscribeToApi(GatekeeperActor(userToken._2), apiIdentifier, LocalDateTime.now(fixedClock))
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()

        ApmServiceMock.SubscribeToApi.succeeds()

        val result = addToken(underTest.subscribeToApi(applicationId, apiIdentifier.context, apiIdentifier.version))(aSuperUserLoggedInRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString()}/subscriptions")

        verify(mockApmService).subscribeToApi(eqTo(applicationId), eqTo(subscribeToApi))(*)
      }

      "return forbidden when submitted for a non-super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        givenTheAppWillBeReturned()

        val result = addToken(underTest.subscribeToApi(applicationId, apiIdentifier.context, apiIdentifier.version))(aLoggedInRequest)

        status(result) shouldBe FORBIDDEN

        verify(mockApmService, never).subscribeToApi(eqTo(applicationId), *)(*)
      }
    }

    "unsubscribeFromApi" should {
      val apiIdentifier = ApiIdentifier.random

      "call the service to unsubscribe from the API when submitted for a super user" in new Setup {
        val unsubscribeFromApi = UnsubscribeFromApi(GatekeeperActor(userToken._2), apiIdentifier, LocalDateTime.now(fixedClock))
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenTheAppWillBeReturned()

        ApmServiceMock.UnsubscribeFromApi.succeeds()

        val result = addToken(underTest.unsubscribeFromApi(applicationId, apiIdentifier.context, apiIdentifier.version))(aSuperUserLoggedInRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString()}/subscriptions")

        verify(mockApmService).unsubscribeFromApi(eqTo(applicationId), eqTo(unsubscribeFromApi))(*)
      }

      "return forbidden when submitted for a non-super user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        givenTheAppWillBeReturned()

        val result = addToken(underTest.unsubscribeFromApi(applicationId, apiIdentifier.context, apiIdentifier.version))(aLoggedInRequest)

        status(result) shouldBe FORBIDDEN

        verify(mockApmService, never).unsubscribeFromApi(eqTo(applicationId), *)(*)
      }
    }

    "manageSubscription" when {
      "the user is a superuser" should {
        "fetch the subscriptions with the fields" in new Setup with ApplicationBuilder with ApiBuilder {

          val newApplication                  = buildApplication()
          val applicationWithSubscriptionData = ApplicationWithSubscriptionData(newApplication, Set.empty, Map.empty)
          val apiData                         = DefaultApiData.withName("API NAme").addVersion(VersionOne, DefaultVersionData)
          val apiContext                      = ApiContext("Api Context")
          val apiContextAndApiData            = Map(apiContext -> apiData)

          StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
          ApmServiceMock.FetchApplicationById.returns(applicationWithSubscriptionData)
          ApmServiceMock.fetchAllPossibleSubscriptionsReturns(apiContextAndApiData)

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
