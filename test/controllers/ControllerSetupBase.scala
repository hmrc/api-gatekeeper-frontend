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

import java.util.UUID

import connectors.{ApplicationConnector, AuthConnector, DeveloperConnector}
import mocks.service.ApplicationServiceMock
import model._
import org.joda.time.DateTime
import org.mockito.BDDMockito._
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito.verify
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import services.{ApiDefinitionService, DeploymentApprovalService}
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments, InsufficientEnrolments, InvalidBearerToken}
import uk.gov.hmrc.auth.core.retrieve.{~, Name, Retrieval}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait ControllerSetupBase extends MockitoSugar with ApplicationServiceMock {

  val mockAuthConnector = mock[AuthConnector]
  val mockApiDefinitionService = mock[ApiDefinitionService]
  val mockApplicationConnector = mock[ApplicationConnector]
  val mockDeveloperConnector = mock[DeveloperConnector]
  val mockDeploymentApprovalService = mock[DeploymentApprovalService]

  val basicApplication = ApplicationResponse(
    UUID.randomUUID(),
    "clientId1",
    "gatewayId1",
    "application1",
    "PRODUCTION",
    None,
    Set(Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR), Collaborator("someone@example.com", CollaboratorRole.DEVELOPER)),
    DateTime.now(),
    DateTime.now(),
    Standard(),
    ApplicationState())
  val application = ApplicationWithHistory(basicApplication, Seq.empty)
  val applicationId = application.application.id.toString

  val userName = "userName"
  val superUserName = "superUserName"
  val adminName = "adminName"
  val authToken = GatekeeperSessionKeys.AuthToken -> "some-bearer-token"
  val userToken = GatekeeperSessionKeys.LoggedInUser -> userName
  val superUserToken = GatekeeperSessionKeys.LoggedInUser -> superUserName
  val adminToken = GatekeeperSessionKeys.LoggedInUser -> adminName
  val aLoggedInRequest = FakeRequest().withSession(authToken, userToken)
  val aSuperUserLoggedInRequest = FakeRequest().withSession(authToken, superUserToken)
  val anAdminLoggedInRequest = FakeRequest().withSession(authToken, adminToken)
  val aLoggedOutRequest = FakeRequest().withSession()
  val noDevs = Seq.empty[ApplicationDeveloper]

  val adminRole = "adminRole" + UUID.randomUUID
  val superUserRole = "superUserRole" + UUID.randomUUID
  val userRole = "userRole" + UUID.randomUUID

  def givenAUnsuccessfulLogin(): Unit = {
    given(mockAuthConnector.authorise(any(), any[Retrieval[Any]])(any[HeaderCarrier], any[ExecutionContext]))
      .willReturn(Future.failed(new InvalidBearerToken))
  }

  def givenTheUserIsAuthorisedAndIsANormalUser(): Unit = {

    val response = Future.successful(new ~(Name(Some(userName), None), Enrolments(Set(Enrolment(userRole)))))

    given(mockAuthConnector.authorise(any(), any[Retrieval[Any]])(any[HeaderCarrier], any[ExecutionContext]))
      .willReturn(response)
  }

  def givenTheUserHasInsufficientEnrolments(): Unit = {
    given(mockAuthConnector.authorise(any(), any[Retrieval[Any]])(any[HeaderCarrier], any[ExecutionContext]))
      .willReturn(Future.failed(new InsufficientEnrolments))
  }

  def givenTheUserIsAuthorisedAndIsASuperUser(): Unit = {

    val response = Future.successful(new ~(Name(Some(superUserName), None), Enrolments(Set(Enrolment(superUserRole)))))

    given(mockAuthConnector.authorise(any(), any[Retrieval[Any]])(any[HeaderCarrier], any[ExecutionContext]))
      .willReturn(response)
  }

  def givenTheUserIsAuthorisedAndIsAnAdmin(): Unit = {

    val response = Future.successful(new ~(Name(Some(adminName), None), Enrolments(Set(Enrolment(adminRole)))))

    given(mockAuthConnector.authorise(any(), any[Retrieval[Any]])(any[HeaderCarrier], any[ExecutionContext]))
      .willReturn(response)
  }

  def givenTheAppWillBeReturned(application: ApplicationWithHistory = application) = {
    given(mockApplicationService.fetchApplication(anyString)(any[HeaderCarrier])).willReturn(Future.successful(application))
  }

  def verifyAuthConnectorCalledForUser = {
    verify(mockAuthConnector)
      .authorise(eqTo(Enrolment(adminRole) or Enrolment(superUserRole) or Enrolment(userRole)), any[Retrieval[Any]])(any[HeaderCarrier], any[ExecutionContext])
  }

  def verifyAuthConnectorCalledForSuperUser = {
    verify(mockAuthConnector)
      .authorise(eqTo(Enrolment(adminRole) or Enrolment(superUserRole)), any[Retrieval[Any]])(any[HeaderCarrier], any[ExecutionContext])
  }

  def verifyAuthConnectorCalledForAdmin = {
    verify(mockAuthConnector)
      .authorise(eqTo(Enrolment(adminRole)), any[Retrieval[Any]])(any[HeaderCarrier], any[ExecutionContext])
  }
}
