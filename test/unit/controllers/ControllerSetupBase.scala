/*
 * Copyright 2019 HM Revenue & Customs
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

import java.util.UUID

import config.AppConfig
import connectors.{ApplicationConnector, AuthConnector, DeveloperConnector}
import model._
import org.apache.http.auth.InvalidCredentialsException
import org.joda.time.DateTime
import org.mockito.BDDMockito._
import org.mockito.Matchers._
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import services.{ApiDefinitionService, ApplicationService}
import uk.gov.hmrc.auth.core.retrieve.{Name, Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import play.api.mvc.Results.Forbidden
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments, InsufficientEnrolments, InvalidBearerToken}

import scala.concurrent.ExecutionContext
//import model.LoginDetails._

import scala.concurrent.Future

trait ControllerSetupBase extends MockitoSugar {

  val mockAuthConnector = mock[AuthConnector]
  val mockApplicationService = mock[ApplicationService]
  val mockApiDefinitionService = mock[ApiDefinitionService]
  val mockConfig = mock[AppConfig]
  val mockApplicationConnector = mock[ApplicationConnector]
  val mockDeveloperConnector = mock[DeveloperConnector]

  val basicApplication = ApplicationResponse(UUID.randomUUID(), "clientid1", "application1", "PRODUCTION", None,
    Set(Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR), Collaborator("someone@example.com", CollaboratorRole.DEVELOPER)),
    DateTime.now(), Standard(), ApplicationState())
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


    given(mockConfig.userRole).willReturn(userRole)

    val response = Future.successful(new ~(Name(Some(userName), None), Enrolments(Set(Enrolment(userRole)))))

    given(mockAuthConnector.authorise(any(), any[Retrieval[Any]])(any[HeaderCarrier], any[ExecutionContext]))
      .willReturn(response)
  }

  def givenTheUserHasInsufficientEnrolments(): Unit = {
    given(mockAuthConnector.authorise(any(), any[Retrieval[Any]])(any[HeaderCarrier], any[ExecutionContext]))
      .willReturn(Future.failed(new InsufficientEnrolments))
  }

  def givenTheUserIsAuthorisedAndIsASuperUser(): Unit = { //TODO - change this for "the user is just plain authorised"


    given(mockConfig.superUserRole).willReturn(superUserRole)

    val response = Future.successful(new ~(Name(Some(superUserName), None), Enrolments(Set(Enrolment(superUserRole)))))

    given(mockAuthConnector.authorise(any(), any[Retrieval[Any]])(any[HeaderCarrier], any[ExecutionContext]))
      .willReturn(response)
  }

  def givenTheUserIsAuthorisedAndIsAnAdmin(): Unit = { //TODO - change this for "the user is just plain authorised"
    given(mockConfig.adminRole).willReturn(adminRole)

    val response = Future.successful(new ~(Name(Some(adminName), None), Enrolments(Set(Enrolment(adminRole)))))

    given(mockAuthConnector.authorise(any(), any[Retrieval[Any]])(any[HeaderCarrier], any[ExecutionContext]))
      .willReturn(response)
  }

  def givenTheAppWillBeReturned(application: ApplicationWithHistory = application) = {
    given(mockApplicationService.fetchApplication(anyString)(any[HeaderCarrier])).willReturn(Future.successful(application))
  }
}
