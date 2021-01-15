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


import connectors.{ApplicationConnector, AuthConnector, DeveloperConnector}
import mocks.service.{ApplicationServiceMock, ApmServiceMock}
import mocks.TestRoles._
import model._
import org.joda.time.DateTime
import org.mockito.BDDMockito._
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import play.api.test.FakeRequest
import services.{ApiDefinitionService, DeploymentApprovalService}
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments, InsufficientEnrolments, InvalidBearerToken}
import uk.gov.hmrc.auth.core.retrieve.{~, Name, Retrieval}

import scala.concurrent.Future

trait ControllerSetupBase extends MockitoSugar with ApplicationServiceMock with ApmServiceMock with ArgumentMatchersSugar {

  val mockAuthConnector = mock[AuthConnector]
  val mockApiDefinitionService = mock[ApiDefinitionService]
  val mockApplicationConnector = mock[ApplicationConnector]
  val mockDeveloperConnector = mock[DeveloperConnector]
  val mockDeploymentApprovalService = mock[DeploymentApprovalService]

  val basicApplication = ApplicationResponse(
    ApplicationId.random,
    ClientId.random,
    "gatewayId1",
    "application1",
    "PRODUCTION",
    None,
    Set(Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR), Collaborator("someone@example.com", CollaboratorRole.DEVELOPER)),
    DateTime.now(),
    DateTime.now(),
    Standard(),
    ApplicationState())
  val application = ApplicationWithHistory(basicApplication, List.empty)
  val applicationId = application.application.id

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
  val noDevs = List.empty[NewModel.Developer]

  def givenAUnsuccessfulLogin(): Unit = {
    given(mockAuthConnector.authorise(*, *)(*, *))
      .willReturn(Future.failed(new InvalidBearerToken))
  }

  def givenTheUserIsAuthorisedAndIsANormalUser(): Unit = {
    val response = Future.successful(new ~(Name(Some(userName), None), Enrolments(Set(Enrolment(userRole)))))

    given(mockAuthConnector.authorise(*, *[Retrieval[Any]])(*, *))
      .willReturn(response)
  }

  def givenTheUserHasInsufficientEnrolments(): Unit = {
    given(mockAuthConnector.authorise(*, *[Retrieval[Any]])(*, *))
      .willReturn(Future.failed(new InsufficientEnrolments))
  }

  def givenTheUserIsAuthorisedAndIsASuperUser(): Unit = {
    val response = Future.successful(new ~(Name(Some(superUserName), None), Enrolments(Set(Enrolment(superUserRole)))))

    given(mockAuthConnector.authorise(*, *[Retrieval[Any]])(*, *))
      .willReturn(response)
  }

  def givenTheUserIsAuthorisedAndIsAnAdmin(): Unit = {
    val response = Future.successful(new ~(Name(Some(adminName), None), Enrolments(Set(Enrolment(adminRole)))))

    given(mockAuthConnector.authorise(*, *[Retrieval[Any]])(*, *))
      .willReturn(response)
  }

  def givenTheAppWillBeReturned(application: ApplicationWithHistory = application) = {
    given(mockApplicationService.fetchApplication(*[ApplicationId])(*)).willReturn(Future.successful(application))
  }

  def verifyAuthConnectorCalledForUser = {
    verify(mockAuthConnector)
      .authorise(eqTo(Enrolment(adminRole) or Enrolment(superUserRole) or Enrolment(userRole)), *[Retrieval[Any]])(*, *)
  }

  def verifyAuthConnectorCalledForSuperUser = {
    verify(mockAuthConnector)
      .authorise(eqTo(Enrolment(adminRole) or Enrolment(superUserRole)), *[Retrieval[Any]])(*, *)
  }

  def verifyAuthConnectorCalledForAdmin = {
    verify(mockAuthConnector)
      .authorise(eqTo(Enrolment(adminRole)), *[Retrieval[Any]])(*, *)
  }
}
