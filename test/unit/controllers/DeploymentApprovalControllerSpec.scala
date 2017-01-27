package unit.controllers

import connectors.AuthConnector
import controllers.DeploymentApprovalController
import model._
import org.joda.time.DateTime
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers
import services.DeploymentApprovalService
import uk.gov.hmrc.play.frontend.auth.AuthenticationProvider
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class DeploymentApprovalControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication{

  Helpers.running(fakeApplication){

    trait Setup {
      val mockDeploymentService = mock[DeploymentApprovalService]
      val underTest = new DeploymentApprovalController{
        val deploymentApprovalService: DeploymentApprovalService = mockDeploymentService
        val authProvider = mock[AuthenticationProvider]
        val authConnector = mock[AuthConnector]
      }
      val hc = mock[HeaderCarrier]
    }

    "deploymentApprovalController" when {

      val userName = "userName"
      val successfulAuthentication = SuccessfulAuthentication(BearerToken("bearer-token", DateTime.now().plusMinutes(10)), userName, None)

      "call service with correct parameters when fetching the summary details of a service" in new Setup {

        val serviceNameCaptor = ArgumentCaptor.forClass(classOf[String])

        when(underTest.authConnector.login(any[LoginDetails])(any[HeaderCarrier])).thenReturn(Future.successful(successfulAuthentication))
        when(underTest.authConnector.authorized(any[Role])(any[HeaderCarrier])).thenReturn(Future.successful(true))

        when(underTest.deploymentApprovalService.fetchApiDefinitionSummary(serviceNameCaptor.capture())(any[HeaderCarrier])).thenReturn(Future(APIDefinitionSummary("api-calendar", "My Calendar", "My Calendar API")))

        val result = await(underTest.fetchApiDefinitionSummary("api-calendar")(hc))
        result shouldBe APIDefinitionSummary("api-calendar", "My Calendar", "My Calendar API")
        verify(mockDeploymentService).fetchApiDefinitionSummary(any[String])(any[HeaderCarrier])
        serviceNameCaptor.getValue shouldBe "api-calendar"
      }

    }
  }

}
