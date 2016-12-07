package unit.controllers

import connectors.AuthConnector
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
