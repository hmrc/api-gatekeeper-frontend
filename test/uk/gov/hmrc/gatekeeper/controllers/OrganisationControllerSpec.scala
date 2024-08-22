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

import mocks.connectors.ApplicationConnectorMockProvider
import mocks.services.{ApmServiceMockProvider, ApplicationServiceMockProvider}
import org.apache.pekko.stream.Materializer
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.TokenProvider
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationServiceMockModule, StrideAuthorisationServiceMockModule}
import uk.gov.hmrc.gatekeeper.config.ErrorHandler
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.utils.{CollaboratorTracker, TitleChecker, WithCSRFAddToken}
import uk.gov.hmrc.gatekeeper.views.html.applications._
import uk.gov.hmrc.gatekeeper.views.html.ErrorTemplate

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OrganisationControllerSpec
    extends ControllerBaseSpec
    with WithCSRFAddToken
    with TitleChecker
    with CollaboratorTracker
    with FixedClock {

  implicit val materializer: Materializer = app.materializer

  private lazy val errorTemplateView             = app.injector.instanceOf[ErrorTemplate]
  private lazy val organisationView              = app.injector.instanceOf[OrganisationView]
  private lazy val errorHandler                  = app.injector.instanceOf[ErrorHandler]

  running(app) {

    trait Setup extends ControllerSetupBase
        with ApplicationServiceMockProvider
        with ApplicationConnectorMockProvider
        with ApmServiceMockProvider
        with StrideAuthorisationServiceMockModule
        with LdapAuthorisationServiceMockModule {

      val csrfToken                          = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken
      override val aLoggedInRequest          = FakeRequest().withSession(csrfToken, authToken, userToken).withCSRFToken
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, superUserToken).withCSRFToken
      override val anAdminLoggedInRequest    = FakeRequest().withSession(csrfToken, authToken, adminToken).withCSRFToken

      LdapAuthorisationServiceMock.Auth.notAuthorised

      val underTest = new OrganisationController(
        StrideAuthorisationServiceMock.aMock,
        mockApplicationService,
        mcc,
        organisationView,
        errorTemplateView,
        mockApmService,
        errorHandler,
        LdapAuthorisationServiceMock.aMock
      )

      def givenThePaginatedApplicationsWillBeReturned = {
        ApplicationServiceMock.SearchApplications.returns()
        FetchAllApiDefinitions.inAny.returns()
        ApmServiceMock.FetchNonOpenApiDefinitions.returns()
      }

    }

    "applicationsPage" should {
      "on request with no specified environment all sandbox applications supplied" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenThePaginatedApplicationsWillBeReturned

        val eventualResult: Future[Result] = underTest.organisationPage(ApplicationId.random)(aLoggedInRequest)

        status(eventualResult) shouldBe OK
        titleOf(eventualResult) shouldBe "Unit Test Title - Applications"
        val responseBody = Helpers.contentAsString(eventualResult)
        responseBody should include("<h1 class=\"govuk-heading-l\" id=\"applications-title\">Applications</h1>")

//        verify(mockApplicationService).searchApplications(eqTo(Some(Environment.SANDBOX)), *)(*)
//        verify(mockApmService).fetchNonOpenApis(eqTo(SANDBOX))(*)
      }
    }
  }
}
