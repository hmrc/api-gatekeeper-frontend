/*
 * Copyright 2016 HM Revenue & Customs
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

import connectors.{ApiDefinitionConnector, AuthConnector}
import model._
import model.LoginDetails.{JsonStringDecryption, JsonStringEncryption}
import org.joda.time.DateTime
import org.mockito.BDDMockito._
import org.mockito.Matchers._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import services.ApplicationService
import uk.gov.hmrc.play.frontend.auth.AuthenticationProvider
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.Future

trait ControllerSetupBase extends MockitoSugar {

  val mockAuthConnector = mock[AuthConnector]
  val mockAuthProvider = mock[AuthenticationProvider]
  val mockApplicationService = mock[ApplicationService]
  val mockApiDefinitionConnector = mock[ApiDefinitionConnector]

  implicit val encryptedStringFormats = JsonStringEncryption
  implicit val decryptedStringFormats = JsonStringDecryption
  implicit val format = Json.format[LoginDetails]

  val userName = "userName"
  val authToken = SessionKeys.authToken -> "some-bearer-token"
  val userToken = GatekeeperSessionKeys.LoggedInUser -> "userName"
  val aLoggedInRequest = FakeRequest().withSession(authToken, userToken)
  val aLoggedOutRequest = FakeRequest().withSession()
  val noUsers = Seq.empty[ApplicationDeveloper]

  def givenAUnsuccessfulLogin(): Unit = {
    givenALogin(false)
  }

  def givenASuccessfulLogin(): Unit = {
    givenALogin(true)
  }

  private def givenALogin(successful: Boolean): Unit = {
    val successfulAuthentication = SuccessfulAuthentication(BearerToken("bearer-token", DateTime.now().plusMinutes(10)), userName, None)
    given(mockAuthConnector.login(any[LoginDetails])(any[HeaderCarrier])).willReturn(Future.successful(successfulAuthentication))
    given(mockAuthConnector.authorized(any[Role])(any[HeaderCarrier])).willReturn(Future.successful(successful))
  }
}
