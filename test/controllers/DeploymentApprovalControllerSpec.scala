/*
 * Copyright 2022 HM Revenue & Customs
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

import model.Environment._
import model._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF.TokenProvider
import utils.WithCSRFAddToken
import views.html.deploymentApproval.{DeploymentApprovalView, DeploymentReviewView}
import views.html.{ErrorTemplate, ForbiddenView}
import uk.gov.hmrc.modules.stride.services.StrideAuthorisationServiceMockModule

import java.net.URLEncoder
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.modules.stride.domain.models.GatekeeperRoles

class DeploymentApprovalControllerSpec extends ControllerBaseSpec with WithCSRFAddToken {
  implicit val materializer = app.materializer

  private lazy val errorTemplateView = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView = app.injector.instanceOf[ForbiddenView]
  private lazy val deploymentApprovalView = app.injector.instanceOf[DeploymentApprovalView]
  private lazy val deploymentReviewView = app.injector.instanceOf[DeploymentReviewView]

  trait Setup extends ControllerSetupBase with StrideAuthorisationServiceMockModule {
    val csrfToken = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken

    override val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken)

    val serviceName = "ServiceName" + UUID.randomUUID()
    val mockedURl = URLEncoder.encode("""http://mock-gatekeeper-frontend/api-gatekeeper/applications""", "UTF-8")

    val underTest = new DeploymentApprovalController(
      forbiddenView,
      mockDeploymentApprovalService,
      mockApiCataloguePublishConnector,
      mcc,
      deploymentApprovalView,
      deploymentReviewView,
      errorTemplateView,
      StrideAuthorisationServiceMock.aMock
    )
  }

  "pendingPage" should {
    "render the deployment approval page for APIs in all environments" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      DeploymentApprovalServiceMock.FetchUnapprovedServices.returns(
        APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(SANDBOX)),
        APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(PRODUCTION))
      )

      val result =  underTest.pendingPage()(aLoggedInRequest)

      status(result) shouldBe OK
      contentAsString(result) should include("API approval")
      contentAsString(result) should include(serviceName)
      contentAsString(result) should include("Production")
      contentAsString(result) should include("Sandbox")

      verify(mockDeploymentApprovalService).fetchUnapprovedServices()(*)
    }

    "redirect to the login page if the user is not logged in" in new Setup {
      StrideAuthorisationServiceMock.Auth.sessionRecordNotFound()

      val result =  underTest.pendingPage()(aLoggedInRequest)

      status(result) shouldBe SEE_OTHER
    }
  }

  "reviewPage" should {
    "render the deployment review page for a sandbox API" in new Setup {
      val environment = SANDBOX
      val approvalSummary = APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(environment))

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      DeploymentApprovalServiceMock.FetchApprovalSummary.returnsForEnv(environment)(approvalSummary)

      val result =  addToken(underTest.reviewPage(serviceName, environment.toString))(aLoggedInRequest)

      status(result) shouldBe OK
      contentAsString(result) should include("API approval")
      contentAsString(result) should include("You must check if the API meets all the necessary requirements before submitting to live.")
      contentAsString(result) should include(serviceName)
      contentAsString(result) should include("Sandbox")

      verify(mockDeploymentApprovalService).fetchApprovalSummary(eqTo(serviceName), eqTo(environment))(*)
    }

    "render the deployment review page for a production API" in new Setup {
      val environment = PRODUCTION
      val approvalSummary = APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(environment))

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      DeploymentApprovalServiceMock.FetchApprovalSummary.returnsForEnv(environment)(approvalSummary)

      val result=  addToken(underTest.reviewPage(serviceName, environment.toString))(aLoggedInRequest)

      status(result) shouldBe OK
      contentAsString(result) should include("API approval")
      contentAsString(result) should include("You must check if the API meets all the necessary requirements before submitting to live.")
      contentAsString(result) should include(serviceName)
      contentAsString(result) should include("Production")

      verify(mockDeploymentApprovalService).fetchApprovalSummary(eqTo(serviceName), eqTo(environment))(*)
    }

    "redirect to the login page if the user is not logged in" in new Setup {
      StrideAuthorisationServiceMock.Auth.sessionRecordNotFound()

      val result =  underTest.handleApproval(serviceName, "PRODUCTION")(aLoggedInRequest)

      status(result) shouldBe SEE_OTHER
    }
  }
  "handleApproval" should {
    "call the approveService and redirect if form contains confirmation for a sandbox API" in new Setup {
      val environment = SANDBOX
      val approvalSummary = APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(environment))

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      DeploymentApprovalServiceMock.FetchApprovalSummary.returnsForEnv(environment)(approvalSummary)
      DeploymentApprovalServiceMock.ApproveService.succeeds()

      val request = aLoggedInRequest.withFormUrlEncodedBody("approval_confirmation" -> "Yes")

      val result =  addToken(underTest.handleApproval(serviceName, environment.toString))(request)

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some("/api-gatekeeper/pending")

      verify(mockDeploymentApprovalService).approveService(eqTo(serviceName), eqTo(environment))(*)
      verifyZeroInteractions(mockApiCataloguePublishConnector)
    }

    "call the approveService and redirect if form contains confirmation for a production API" in new Setup {
      val environment = PRODUCTION
      val approvalSummary = APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(environment))

      ApiCataloguePublishConnectorMock.PublishByServiceName.returnRight()
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      DeploymentApprovalServiceMock.FetchApprovalSummary.returnsForEnv(environment)(approvalSummary)
      DeploymentApprovalServiceMock.ApproveService.succeeds()

      val request = aLoggedInRequest.withFormUrlEncodedBody("approval_confirmation" -> "Yes")

      val result =  addToken(underTest.handleApproval(serviceName, environment.toString))(request)

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some("/api-gatekeeper/pending")

      verify(mockDeploymentApprovalService).approveService(eqTo(serviceName), eqTo(environment))(*)
      verify(mockApiCataloguePublishConnector).publishByServiceName(eqTo(serviceName))(*)
    }

    "return bad request if approval is not confirmed" in new Setup {
      val environment = PRODUCTION

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val request = aLoggedInRequest.withFormUrlEncodedBody("approval_confirmation" -> "No")

      intercept[UnsupportedOperationException](
        await(addToken(underTest.handleApproval(serviceName, environment.toString))(request))
      )
    }

    "return bad request if invalid form" in new Setup {
      val environment = PRODUCTION
      val approvalSummary = APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(environment))

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      DeploymentApprovalServiceMock.FetchApprovalSummary.returnsForEnv(environment)(approvalSummary)

      val request = aLoggedInRequest.withFormUrlEncodedBody("notAValidField" -> "not_used")

      val result =  addToken(underTest.handleApproval(serviceName, environment.toString))(request)

      status(result) shouldBe BAD_REQUEST
    }

    "redirect to the login page if the user is not logged in" in new Setup {
      StrideAuthorisationServiceMock.Auth.sessionRecordNotFound()

      val result =  underTest.handleApproval(serviceName, "PRODUCTION")(aLoggedInRequest)

      status(result) shouldBe SEE_OTHER
    }
  }
}
