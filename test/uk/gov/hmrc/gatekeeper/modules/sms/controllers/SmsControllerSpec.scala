/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatform.modules.sms.controllers

import controllers.{ControllerBaseSpec, ControllerSetupBase}
import uk.gov.hmrc.apiplatform.modules.sms.connectors.ThirdPartyDeveloperConnector.SendSmsResponse
import modules.sms.views.html.{SendSmsSuccessView, SendSmsView}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.TokenProvider
import uk.gov.hmrc.apiplatform.modules.sms.controllers.SmsController
import uk.gov.hmrc.apiplatform.modules.sms.mocks.ThirdPartyDeveloperConnectorMockProvider
import views.html.ErrorTemplate

import scala.concurrent.ExecutionContext.Implicits.global

class SmsControllerSpec extends ControllerBaseSpec with ThirdPartyDeveloperConnectorMockProvider with WithCSRFAddToken {

  implicit val materializer = app.materializer
  private lazy val errorTemplateView: ErrorTemplate = app.injector.instanceOf[ErrorTemplate]

  Helpers.running(app) {

    trait Setup extends ControllerSetupBase {

      val csrfToken = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken
      override val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken).withCSRFToken
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, superUserToken).withCSRFToken
      val sendSmsView = app.injector.instanceOf[SendSmsView]
      val sendSmsSuccessView = app.injector.instanceOf[SendSmsSuccessView]

      val smsController = new SmsController(
        mockThirdPartyDeveloperConnector,
        strideAuthConfig,
        mockAuthConnector,
        forbiddenHandler,
        sendSmsView,
        sendSmsSuccessView,
        errorTemplateView,
        mcc
      )

      val testNumber = "0123456789"
      val sendSmsResponse = SendSmsResponse("SMS sent")

    }

    "sendSmsAction" should {

      "show success message when connector returns Right" in new Setup {
        givenTheGKUserIsAuthorisedAndIsANormalUser()
        ThirdPartyDeveloperConnectorMock.SendSms.returnsSendSmsResponse(sendSmsResponse)
        val request = aLoggedInRequest.withFormUrlEncodedBody(("phoneNumber", testNumber))
        val result = addToken(smsController.sendSmsAction())(request)

        contentAsString(result) should include("SMS sent")

        verifyAuthConnectorCalledForUser
      }

      "show validation errors when form field is empty" in new Setup {
        givenTheGKUserIsAuthorisedAndIsANormalUser()
        ThirdPartyDeveloperConnectorMock.SendSms.returnsSendSmsResponse(sendSmsResponse)
        val request = aLoggedInRequest.withFormUrlEncodedBody(("phoneNumber", ""))
        val result = addToken(smsController.sendSmsAction())(request)

        contentAsString(result) should include("Phone number is required")

        verifyAuthConnectorCalledForUser
      }

      "show technical difficulties page when connector returns Left" in new Setup {
        givenTheGKUserIsAuthorisedAndIsANormalUser()
        ThirdPartyDeveloperConnectorMock.SendSms.returnsError
        val request = aLoggedInRequest.withFormUrlEncodedBody(("phoneNumber", testNumber))
        val result = addToken(smsController.sendSmsAction())(request)

        contentAsString(result) should include("technical difficulties")

        verifyAuthConnectorCalledForUser
      }

    }
  }
}
