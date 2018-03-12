/*
 * Copyright 2018 HM Revenue & Customs
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
import connectors.AuthConnector
import model.LoginDetails.{JsonStringDecryption, JsonStringEncryption}
import model._
import org.joda.time.DateTime
import org.mockito.BDDMockito._
import org.mockito.Matchers._
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import services.{ApiDefinitionService, ApplicationService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthenticationProvider

import scala.concurrent.Future

trait ControllerSetupBase extends MockitoSugar {

  val mockAuthConnector = mock[AuthConnector]
  val mockAuthProvider = mock[AuthenticationProvider]
  val mockApplicationService = mock[ApplicationService]
  val mockApiDefinitionService = mock[ApiDefinitionService]
  val mockConfig = mock[AppConfig]

  implicit val encryptedStringFormats = JsonStringEncryption
  implicit val decryptedStringFormats = JsonStringDecryption
  implicit val format = Json.format[LoginDetails]

  val basicApplication = ApplicationResponse(UUID.randomUUID(), "application1", "PRODUCTION", None,
    Set(Collaborator("sample@email.com", CollaboratorRole.ADMINISTRATOR), Collaborator("someone@email.com", CollaboratorRole.DEVELOPER)),
    DateTime.now(), Standard(), ApplicationState())
  val application = ApplicationWithHistory(basicApplication, Seq.empty)
  val applicationId = application.application.id.toString

  val userName = "userName"
  val superUserName = "superUserName"
  val authToken = GatekeeperSessionKeys.AuthToken -> "some-bearer-token"
  val userToken = GatekeeperSessionKeys.LoggedInUser -> userName
  val superUserToken = GatekeeperSessionKeys.LoggedInUser -> superUserName
  val aLoggedInRequest = FakeRequest().withSession(authToken, userToken)
  val aSuperUserLoggedInRequest = FakeRequest().withSession(authToken, superUserToken)
  val aLoggedOutRequest = FakeRequest().withSession()
  val noUsers = Seq.empty[ApplicationDeveloper]

  def givenAUnsuccessfulLogin(): Unit = {
    givenALogin(userName, false)
  }

  def givenASuccessfulLogin(): Unit = {
    givenALogin(userName, true)
  }

  def givenASuccessfulSuperUserLogin(): Unit = {
    givenALogin(superUserName, true)
  }

  private def givenALogin(userName: String, successful: Boolean): Unit = {
    val successfulAuthentication = SuccessfulAuthentication(BearerToken("bearer-token", DateTime.now().plusMinutes(10)), userName, None)
    given(mockAuthConnector.login(any[LoginDetails])(any[HeaderCarrier])).willReturn(Future.successful(successfulAuthentication))
    given(mockAuthConnector.authorized(any[Role])(any[HeaderCarrier])).willReturn(Future.successful(successful))
  }

  def givenTheAppWillBeReturned(application: ApplicationWithHistory = application) = {
    given(mockApplicationService.fetchApplication(anyString)(any[HeaderCarrier])).willReturn(Future.successful(application))
  }
}
