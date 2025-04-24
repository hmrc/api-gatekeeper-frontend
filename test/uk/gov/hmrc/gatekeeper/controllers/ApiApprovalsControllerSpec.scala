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

import java.net.URLEncoder
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

import org.apache.pekko.stream.Materializer

import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF.TokenProvider

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationServiceMockModule, StrideAuthorisationServiceMockModule}
import uk.gov.hmrc.gatekeeper.connectors.ApiCataloguePublishConnector
import uk.gov.hmrc.gatekeeper.models.ApprovalStatus.APPROVED
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.utils.WithCSRFAddToken
import uk.gov.hmrc.gatekeeper.views.html.ErrorTemplate
import uk.gov.hmrc.gatekeeper.views.html.apiapprovals.{
  ApiApprovalsApprovedSuccessView,
  ApiApprovalsDeclinedSuccessView,
  ApiApprovalsFilterView,
  ApiApprovalsHistoryView,
  ApiApprovalsReviewView
}

class ApiApprovalsControllerSpec extends ControllerBaseSpec with WithCSRFAddToken {
  implicit val materializer: Materializer = app.materializer

  private lazy val errorTemplateView               = app.injector.instanceOf[ErrorTemplate]
  private lazy val apiApprovalsFilterView          = app.injector.instanceOf[ApiApprovalsFilterView]
  private lazy val apiApprovalsHistoryView         = app.injector.instanceOf[ApiApprovalsHistoryView]
  private lazy val apiApprovalsReviewView          = app.injector.instanceOf[ApiApprovalsReviewView]
  private lazy val apiApprovalsApprovedSuccessView = app.injector.instanceOf[ApiApprovalsApprovedSuccessView]
  private lazy val apiApprovalsDeclinedSuccessView = app.injector.instanceOf[ApiApprovalsDeclinedSuccessView]
  private lazy val apiCataloguePublishConnector    = app.injector.instanceOf[ApiCataloguePublishConnector]

  trait Setup extends ControllerSetupBase with StrideAuthorisationServiceMockModule with LdapAuthorisationServiceMockModule {
    val csrfToken = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken

    override val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken)

    val serviceName    = "ServiceName" + UUID.randomUUID()
    val mockedURl      = URLEncoder.encode("""http://mock-gatekeeper-frontend/api-gatekeeper/applications""", "UTF-8")
    val gatekeeperUser = Actors.GatekeeperUser("Bobby Example")

    val underTest = new ApiApprovalsController(
      mockDeploymentApprovalService,
      mcc,
      apiCataloguePublishConnector,
      apiApprovalsFilterView,
      apiApprovalsHistoryView,
      apiApprovalsReviewView,
      apiApprovalsApprovedSuccessView,
      apiApprovalsDeclinedSuccessView,
      errorTemplateView,
      StrideAuthorisationServiceMock.aMock,
      LdapAuthorisationServiceMock.aMock
    )
  }

  "filterPage" should {
    "render the API Approval page for APIs in all environments" in new Setup {
      LdapAuthorisationServiceMock.Auth.notAuthorised
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      DeploymentApprovalServiceMock.SearchServices.thenReturn(
        APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(Environment.SANDBOX)),
        APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(Environment.PRODUCTION), status = APPROVED)
      )

      val result = underTest.filterPage()(aLoggedInRequest.withCSRFToken)

      status(result) shouldBe OK
      contentAsString(result) should include("API approval")
      contentAsString(result) should include(serviceName)
      contentAsString(result) should include("Production")
      contentAsString(result) should include("Sandbox")
      contentAsString(result) should include("New")
      contentAsString(result) should include("Approved")

      DeploymentApprovalServiceMock.SearchServices.verifyCalled(List.empty)
    }

    "render the API Approval page with passed in status filter" in new Setup {
      LdapAuthorisationServiceMock.Auth.notAuthorised
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      DeploymentApprovalServiceMock.SearchServices.thenReturn()

      val request = aLoggedInRequest.withCSRFToken.withFormUrlEncodedBody("newStatus" -> "true", "resubmittedStatus" -> "true")

      val result = underTest.filterPage()(request)

      status(result) shouldBe OK
      DeploymentApprovalServiceMock.SearchServices.verifyCalled(Seq("status" -> "NEW", "status" -> "RESUBMITTED"))
    }

    "redirect to the login page if the user is not logged in" in new Setup {
      LdapAuthorisationServiceMock.Auth.notAuthorised
      StrideAuthorisationServiceMock.Auth.sessionRecordNotFound

      val result = underTest.filterPage()(aLoggedInRequest)

      status(result) shouldBe SEE_OTHER
    }
  }

  "historyPage" should {
    "render the history page for an API in Production" in new Setup {
      LdapAuthorisationServiceMock.Auth.notAuthorised
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      DeploymentApprovalServiceMock.FetchApprovalSummary.returnsForEnv(Environment.PRODUCTION)(
        APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(Environment.PRODUCTION), status = APPROVED)
      )

      val result = underTest.historyPage(serviceName, Environment.PRODUCTION.displayText)(aLoggedInRequest.withCSRFToken)

      status(result) shouldBe OK
      contentAsString(result) should include("API approvals")
      contentAsString(result) should not include ("New")
      contentAsString(result) should include("Approved")
      contentAsString(result) should include("aName")
      contentAsString(result) should include("aDescription")
      contentAsString(result) should include(serviceName)
      contentAsString(result) should not include ("Sandbox")
      contentAsString(result) should include("Production")
      contentAsString(result) should include("Last update")

      DeploymentApprovalServiceMock.FetchApprovalSummary.verifyCalled(Environment.PRODUCTION)
    }

    "redirect to the login page if the user is not logged in" in new Setup {
      LdapAuthorisationServiceMock.Auth.notAuthorised
      StrideAuthorisationServiceMock.Auth.sessionRecordNotFound

      val result = underTest.historyPage(serviceName, Environment.PRODUCTION.displayText)(aLoggedInRequest)

      status(result) shouldBe SEE_OTHER
    }
  }

  "reviewPage" should {
    "render the review page for an API in Production" in new Setup {
      LdapAuthorisationServiceMock.Auth.notAuthorised
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      DeploymentApprovalServiceMock.FetchApprovalSummary.returnsForEnv(Environment.PRODUCTION)(
        APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(Environment.PRODUCTION), status = APPROVED)
      )

      val result = underTest.reviewPage(serviceName, Environment.PRODUCTION.displayText)(aLoggedInRequest.withCSRFToken)

      status(result) shouldBe OK
      contentAsString(result) should include("API approvals")
      contentAsString(result) should include("aName")
      contentAsString(result) should include("aDescription")
      contentAsString(result) should include(serviceName)
      contentAsString(result) should not include ("Sandbox")
      contentAsString(result) should include("Production")

      DeploymentApprovalServiceMock.FetchApprovalSummary.verifyCalled(Environment.PRODUCTION)
    }

    "redirect to the login page if the user is not logged in" in new Setup {
      LdapAuthorisationServiceMock.Auth.notAuthorised
      StrideAuthorisationServiceMock.Auth.sessionRecordNotFound

      val result = underTest.reviewPage(serviceName, Environment.PRODUCTION.displayText)(aLoggedInRequest)

      status(result) shouldBe SEE_OTHER
    }
  }

  "reviewAction" should {
    "call approveService if approve is selected on the review page and show the approved success page" in new Setup {
      val approveNote = "Service approved"
      LdapAuthorisationServiceMock.Auth.notAuthorised
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      DeploymentApprovalServiceMock.ApproveService.succeeds()
      ApiCataloguePublishConnectorMock.PublishByServiceName.returnRight()

      val request = aLoggedInRequest.withFormUrlEncodedBody("approve" -> "true", "approveDetail" -> approveNote)

      val result = underTest.reviewAction(serviceName, Environment.SANDBOX.displayText)(request.withCSRFToken)

      status(result) shouldBe OK
      contentAsString(result) should include(s"The $serviceName has been approved")

      verify(mockDeploymentApprovalService).approveService(eqTo(serviceName), eqTo(Environment.SANDBOX), eqTo(gatekeeperUser), eqTo(Some(approveNote)))(*)
    }

    "call declineService if decline is selected on the review page and show the declined success page" in new Setup {
      val declineNote = "Service declined"
      LdapAuthorisationServiceMock.Auth.notAuthorised
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      DeploymentApprovalServiceMock.DeclineService.succeeds()

      val request = aLoggedInRequest.withFormUrlEncodedBody("approve" -> "false", "declineDetail" -> declineNote)

      val result = underTest.reviewAction(serviceName, Environment.SANDBOX.displayText)(request.withCSRFToken)

      status(result) shouldBe OK
      contentAsString(result) should include(s"The $serviceName has been declined")

      verify(mockDeploymentApprovalService).declineService(eqTo(serviceName), eqTo(Environment.SANDBOX), eqTo(gatekeeperUser), eqTo(Some(declineNote)))(*)
      verifyZeroInteractions(mockApiCataloguePublishConnector)
    }

    "redirect to the login page if the user is not logged in" in new Setup {
      LdapAuthorisationServiceMock.Auth.notAuthorised
      StrideAuthorisationServiceMock.Auth.sessionRecordNotFound

      val result = underTest.reviewAction(serviceName, Environment.PRODUCTION.displayText)(aLoggedInRequest)

      status(result) shouldBe SEE_OTHER
    }
  }

}
