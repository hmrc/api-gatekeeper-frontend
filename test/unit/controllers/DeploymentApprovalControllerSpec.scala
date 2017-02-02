/*
 * Copyright 2017 HM Revenue & Customs
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

    trait Setup extends ControllerSetupBase {
      val mockDeploymentService = mock[DeploymentApprovalService]
      val underTest = new DeploymentApprovalController{
        val appConfig = mockConfig
        val deploymentApprovalService: DeploymentApprovalService = mockDeploymentService
        val authProvider = mockAuthProvider
        val authConnector = mockAuthConnector
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
