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

import java.net.URLEncoder
import java.util.UUID

import model.Environment._
import model._
import org.mockito.BDDMockito._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF.TokenProvider
import utils.WithCSRFAddToken
import views.html.deploymentApproval.{DeploymentApprovalView, DeploymentReviewView}
import views.html.{ErrorTemplate, ForbiddenView}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeploymentApprovalControllerSpec extends ControllerBaseSpec with WithCSRFAddToken {
  implicit val materializer = app.materializer

  private lazy val errorTemplateView = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView = app.injector.instanceOf[ForbiddenView]
  private lazy val deploymentApprovalView = app.injector.instanceOf[DeploymentApprovalView]
  private lazy val deploymentReviewView = app.injector.instanceOf[DeploymentReviewView]

  trait Setup extends ControllerSetupBase {
    val csrfToken = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken

    override val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken)

    val serviceName = "ServiceName" + UUID.randomUUID()
    val redirectLoginUrl = "https://loginUri" +
      s"?successURL=${URLEncoder.encode("http://mock-gatekeeper-frontend/api-gatekeeper/applications", "UTF-8")}" +
      s"&origin=${URLEncoder.encode("api-gatekeeper-frontend", "UTF-8")}"

    val underTest = new DeploymentApprovalController(
      mockAuthConnector,
      forbiddenView,
      mockDeploymentApprovalService,
      mcc,
      deploymentApprovalView,
      deploymentReviewView,
      errorTemplateView)
  }

  "pendingPage" should {
    "render the deployment approval page for APIs in all environments" in new Setup {
      val approvalSummaries = List(
        APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(SANDBOX)),
        APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(PRODUCTION)))

      givenTheGKUserIsAuthorisedAndIsANormalUser()
      given(mockDeploymentApprovalService.fetchUnapprovedServices()(*)).willReturn(Future.successful(approvalSummaries))

      val result = await(underTest.pendingPage()(aLoggedInRequest))

      status(result) shouldBe OK
      bodyOf(result) should include("API approval")
      bodyOf(result) should include(serviceName)
      bodyOf(result) should include("Production")
      bodyOf(result) should include("Sandbox")

      verify(mockDeploymentApprovalService).fetchUnapprovedServices()(*)

      verifyAuthConnectorCalledForUser
    }

    "redirect to the login page if the user is not logged in" in new Setup {
      givenAUnsuccessfulLogin()

      val result = await(underTest.pendingPage()(aLoggedInRequest))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(redirectLoginUrl)
    }
  }

  "reviewPage" should {
    "render the deployment review page for a sandbox API" in new Setup {
      val environment = SANDBOX
      val approvalSummary = APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(environment))

      givenTheGKUserIsAuthorisedAndIsANormalUser()
      given(mockDeploymentApprovalService.fetchApprovalSummary(*, eqTo(environment))(*)).willReturn(Future.successful(approvalSummary))

      val result = await(addToken(underTest.reviewPage(serviceName, environment.toString))(aLoggedInRequest))

      status(result) shouldBe OK
      bodyOf(result) should include("API approval")
      bodyOf(result) should include("You must check if the API meets all the necessary requirements before submitting to live.")
      bodyOf(result) should include(serviceName)
      bodyOf(result) should include("Sandbox")

      verify(mockDeploymentApprovalService).fetchApprovalSummary(eqTo(serviceName), eqTo(environment))(*)

      verifyAuthConnectorCalledForUser
    }

    "render the deployment review page for a production API" in new Setup {
      val environment = PRODUCTION
      val approvalSummary = APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(environment))

      givenTheGKUserIsAuthorisedAndIsANormalUser()
      given(mockDeploymentApprovalService.fetchApprovalSummary(*, eqTo(environment))(*)).willReturn(Future.successful(approvalSummary))

      val result = await(addToken(underTest.reviewPage(serviceName, environment.toString))(aLoggedInRequest))

      status(result) shouldBe OK
      bodyOf(result) should include("API approval")
      bodyOf(result) should include("You must check if the API meets all the necessary requirements before submitting to live.")
      bodyOf(result) should include(serviceName)
      bodyOf(result) should include("Production")

      verify(mockDeploymentApprovalService).fetchApprovalSummary(eqTo(serviceName), eqTo(environment))(*)

      verifyAuthConnectorCalledForUser
    }

    "redirect to the login page if the user is not logged in" in new Setup {
      givenAUnsuccessfulLogin()

      val result = await(underTest.handleApproval(serviceName, "PRODUCTION")(aLoggedInRequest))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(redirectLoginUrl)
    }
  }
  "handleApproval" should {
    "call the approveService and redirect if form contains confirmation for a sandbox API" in new Setup {
      val environment = SANDBOX
      val approvalSummary = APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(environment))

      givenTheGKUserIsAuthorisedAndIsANormalUser()
      given(mockDeploymentApprovalService.fetchApprovalSummary(*, *)(*)).willReturn(Future.successful(approvalSummary))
      given(mockDeploymentApprovalService.approveService(*, *)(*)).willReturn(Future.successful(()))

      val request = aLoggedInRequest.withFormUrlEncodedBody("approval_confirmation" -> "Yes")

      val result = await(addToken(underTest.handleApproval(serviceName, environment.toString))(request))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some("/api-gatekeeper/pending")

      verify(mockDeploymentApprovalService).approveService(eqTo(serviceName), eqTo(environment))(*)
      verifyAuthConnectorCalledForUser
    }

    "call the approveService and redirect if form contains confirmation for a production API" in new Setup {
      val environment = PRODUCTION
      val approvalSummary = APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(environment))

      givenTheGKUserIsAuthorisedAndIsANormalUser()
      given(mockDeploymentApprovalService.fetchApprovalSummary(*, *)(*)).willReturn(Future.successful(approvalSummary))
      given(mockDeploymentApprovalService.approveService(*, *)(*)).willReturn(Future.successful(()))

      val request = aLoggedInRequest.withFormUrlEncodedBody("approval_confirmation" -> "Yes")

      val result = await(addToken(underTest.handleApproval(serviceName, environment.toString))(request))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some("/api-gatekeeper/pending")

      verify(mockDeploymentApprovalService).approveService(eqTo(serviceName), eqTo(environment))(*)
      verifyAuthConnectorCalledForUser
    }

    "return bad request if approval is not confirmed" in new Setup {
      val environment = PRODUCTION

      givenTheGKUserIsAuthorisedAndIsANormalUser()

      val request = aLoggedInRequest.withFormUrlEncodedBody("approval_confirmation" -> "No")

      assertThrows[UnsupportedOperationException](await(addToken(underTest.handleApproval(serviceName, environment.toString))(request)))
    }

    "return bad request if invalid form" in new Setup {
      val environment = PRODUCTION
      val approvalSummary = APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(environment))

      givenTheGKUserIsAuthorisedAndIsANormalUser()
      given(mockDeploymentApprovalService.fetchApprovalSummary(*, *)(*)).willReturn(Future.successful(approvalSummary))

      val request = aLoggedInRequest.withFormUrlEncodedBody("notAValidField" -> "not_used")

      val result = await(addToken(underTest.handleApproval(serviceName, environment.toString))(request))

      status(result) shouldBe BAD_REQUEST
    }

    "redirect to the login page if the user is not logged in" in new Setup {
      givenAUnsuccessfulLogin()

      val result = await(underTest.handleApproval(serviceName, "PRODUCTION")(aLoggedInRequest))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(redirectLoginUrl)
    }
  }
}
