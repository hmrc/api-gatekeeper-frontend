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

import mocks.services.ApiPlatformDeskproServiceMockProvider
import org.apache.pekko.stream.Materializer

import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF.TokenProvider

import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.gatekeeper.config.ErrorHandler
import uk.gov.hmrc.gatekeeper.connectors.ApiPlatformDeskproConnector.DeskproTicket
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.utils.{CollaboratorTracker, TitleChecker, WithCSRFAddToken}
import uk.gov.hmrc.gatekeeper.views.html.ErrorTemplate

class DeskproControllerSpec
    extends ControllerBaseSpec
    with WithCSRFAddToken
    with TitleChecker
    with CollaboratorTracker
    with FixedClock {

  implicit val materializer: Materializer = app.materializer

  private lazy val errorTemplateView = app.injector.instanceOf[ErrorTemplate]
  private lazy val errorHandler      = app.injector.instanceOf[ErrorHandler]

  running(app) {

    trait Setup extends ControllerSetupBase
        with ApiPlatformDeskproServiceMockProvider {

      val csrfToken                          = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken
      override val aLoggedInRequest          = FakeRequest().withSession(csrfToken, authToken, userToken).withCSRFToken
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, superUserToken).withCSRFToken
      override val anAdminLoggedInRequest    = FakeRequest().withSession(csrfToken, authToken, adminToken).withCSRFToken

      LdapAuthorisationServiceMock.Auth.notAuthorised

      val underTest = new DeskproController(
        StrideAuthorisationServiceMock.aMock,
        mockApplicationService,
        mockQueryService,
        mcc,
        errorTemplateView,
        mockApmService,
        errorHandler,
        LdapAuthorisationServiceMock.aMock,
        mockApiPlatformDeskproService,
        mockDeveloperService
      )

      val ticketId: Int = 123
      val personId: Int = 16
      val userEmail     = developerOne.emailAddress
      val ticket        = DeskproTicket(ticketId, "ref1", personId, userEmail, "awaiting_user", instant, instant, Some(instant), "subject 1")
    }

    "developerPage" should {
      "on request get developer for Deskpro ticket" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        ApiPlatformDeskproServiceMock.GetDeskproTicket.returns(ticketId)(ticket)
        DeveloperServiceMock.FetchUser.returnsFor(userEmail)

        val eventualResult: Future[Result] = underTest.developerPage(ticketId)(aLoggedInRequest)

        status(eventualResult) shouldBe SEE_OTHER
        redirectLocation(eventualResult).map(x => x.contains("/developer") shouldBe true)
      }

      "on ticket not found show not found page" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        ApiPlatformDeskproServiceMock.GetDeskproTicket.returnsNotFound()

        val eventualResult: Future[Result] = underTest.developerPage(ticketId)(aLoggedInRequest)
        status(eventualResult) shouldBe NOT_FOUND
        contentAsString(eventualResult).contains(s"Failed to find Deskpro ticket $ticketId") shouldBe true
      }

      "on developer not found show not found page" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        ApiPlatformDeskproServiceMock.GetDeskproTicket.returns(ticketId)(ticket)
        DeveloperServiceMock.FetchUser.returnsNotFound()

        val eventualResult: Future[Result] = underTest.developerPage(ticketId)(aLoggedInRequest)
        status(eventualResult) shouldBe NOT_FOUND
        contentAsString(eventualResult).contains(s"Failed to find developer for Deskpro ticket $ticketId") shouldBe true
      }
    }
  }
}
