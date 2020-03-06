/*
 * Copyright 2020 HM Revenue & Customs
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
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito.verify
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF.TokenProvider
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import utils.WithCSRFAddToken

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeploymentApprovalControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication with WithCSRFAddToken {

  implicit val materializer = fakeApplication.materializer

  trait Setup extends ControllerSetupBase {

    val csrfToken = "csrfToken" -> fakeApplication.injector.instanceOf[TokenProvider].generateToken

    override val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken)

    val serviceName = "ServiceName" + UUID.randomUUID()
    val redirectLoginUrl = s"https://loginUri" +
      s"?successURL=${URLEncoder.encode("http://mock-gatekeeper-frontend/api-gatekeeper/applications", "UTF-8")}" +
      s"&origin=${URLEncoder.encode("Gatekeeper app name", "UTF-8")}"

    given(mockConfig.strideLoginUrl).willReturn("https://loginUri")
    given(mockConfig.appName).willReturn("Gatekeeper app name")
    given(mockConfig.gatekeeperSuccessUrl).willReturn("http://mock-gatekeeper-frontend/api-gatekeeper/applications")

    val underTest = new DeploymentApprovalController(mockAuthConnector, mockDeploymentApprovalService)(mockConfig, global)
  }

  "pendingPage" should {
    "render the deployment approval page for APIs in all environments" in new Setup {
      val approvalSummaries = Seq(
        APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(SANDBOX)),
        APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(PRODUCTION)))

      givenTheUserIsAuthorisedAndIsANormalUser()
      given(mockDeploymentApprovalService.fetchUnapprovedServices()(any[HeaderCarrier])).willReturn(Future.successful(approvalSummaries))

      val result = await(underTest.pendingPage()(aLoggedInRequest))

      status(result) shouldBe OK
      bodyOf(result) should include("API approval")
      bodyOf(result) should include(serviceName)
      bodyOf(result) should include("Production")
      bodyOf(result) should include("Sandbox")

      verify(mockDeploymentApprovalService).fetchUnapprovedServices()(any[HeaderCarrier])

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

      givenTheUserIsAuthorisedAndIsANormalUser()
      given(mockDeploymentApprovalService.fetchApprovalSummary(any(), eqTo(environment))(any[HeaderCarrier])).willReturn(Future.successful(approvalSummary))

      val result = await(addToken(underTest.reviewPage(serviceName, environment.toString))(aLoggedInRequest))

      status(result) shouldBe OK
      bodyOf(result) should include("API approval")
      bodyOf(result) should include("You must check if the API meets all the necessary requirements before submitting to live.")
      bodyOf(result) should include(serviceName)
      bodyOf(result) should include("Sandbox")

      verify(mockDeploymentApprovalService).fetchApprovalSummary(eqTo(serviceName), eqTo(environment))(any[HeaderCarrier])

      verifyAuthConnectorCalledForUser
    }

    "render the deployment review page for a production API" in new Setup {
      val environment = PRODUCTION
      val approvalSummary = APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(environment))

      givenTheUserIsAuthorisedAndIsANormalUser()
      given(mockDeploymentApprovalService.fetchApprovalSummary(any(), eqTo(environment))(any[HeaderCarrier])).willReturn(Future.successful(approvalSummary))

      val result = await(addToken(underTest.reviewPage(serviceName, environment.toString))(aLoggedInRequest))

      status(result) shouldBe OK
      bodyOf(result) should include("API approval")
      bodyOf(result) should include("You must check if the API meets all the necessary requirements before submitting to live.")
      bodyOf(result) should include(serviceName)
      bodyOf(result) should include("Production")

      verify(mockDeploymentApprovalService).fetchApprovalSummary(eqTo(serviceName), eqTo(environment))(any[HeaderCarrier])

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

      givenTheUserIsAuthorisedAndIsANormalUser()
      given(mockDeploymentApprovalService.fetchApprovalSummary(any(), any())(any[HeaderCarrier])).willReturn(Future.successful(approvalSummary))
      given(mockDeploymentApprovalService.approveService(any(), any())(any[HeaderCarrier])).willReturn(Future.successful(()))

      val request = aLoggedInRequest.withFormUrlEncodedBody("approval_confirmation" -> "Yes")

      val result = await(addToken(underTest.handleApproval(serviceName, environment.toString))(request))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some("/api-gatekeeper/pending")

      verify(mockDeploymentApprovalService).approveService(eqTo(serviceName), eqTo(environment))(any[HeaderCarrier])
      verifyAuthConnectorCalledForUser
    }

    "call the approveService and redirect if form contains confirmation for a production API" in new Setup {
      val environment = PRODUCTION
      val approvalSummary = APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(environment))

      givenTheUserIsAuthorisedAndIsANormalUser()
      given(mockDeploymentApprovalService.fetchApprovalSummary(any(), any())(any[HeaderCarrier])).willReturn(Future.successful(approvalSummary))
      given(mockDeploymentApprovalService.approveService(any(), any())(any[HeaderCarrier])).willReturn(Future.successful(()))

      val request = aLoggedInRequest.withFormUrlEncodedBody("approval_confirmation" -> "Yes")

      val result = await(addToken(underTest.handleApproval(serviceName, environment.toString))(request))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some("/api-gatekeeper/pending")

      verify(mockDeploymentApprovalService).approveService(eqTo(serviceName), eqTo(environment))(any[HeaderCarrier])
      verifyAuthConnectorCalledForUser
    }

    "return bad request if approval is not confirmed" in new Setup {
      val environment = PRODUCTION
      val approvalSummary = APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(environment))

      givenTheUserIsAuthorisedAndIsANormalUser()

      val request = aLoggedInRequest.withFormUrlEncodedBody("approval_confirmation" -> "No")

      assertThrows[UnsupportedOperationException](await(addToken(underTest.handleApproval(serviceName, environment.toString))(request)))
    }

    "return bad request if invalid form" in new Setup {
      val environment = PRODUCTION
      val approvalSummary = APIApprovalSummary(serviceName, "aName", Option("aDescription"), Some(environment))

      givenTheUserIsAuthorisedAndIsANormalUser()
      given(mockDeploymentApprovalService.fetchApprovalSummary(any(), any())(any[HeaderCarrier])).willReturn(Future.successful(approvalSummary))

      val request = aLoggedInRequest.withFormUrlEncodedBody("notAValidField" -> "not_used")

      var result = await(addToken(underTest.handleApproval(serviceName, environment.toString))(request))

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
