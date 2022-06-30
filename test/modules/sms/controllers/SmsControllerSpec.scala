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

package modules.sms.controllers

import controllers.{ControllerBaseSpec, ControllerSetupBase}
import modules.sms.connectors.{SendSmsResponse, ThirdPartyDeveloperConnector}
import modules.sms.mocks.ThirdPartyDeveloperConnectorMockProvider
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import utils.FakeRequestCSRFSupport._
import views.html.ErrorTemplate

import scala.concurrent.ExecutionContext.Implicits.global

class SmsControllerSpec extends ControllerBaseSpec with ThirdPartyDeveloperConnectorMockProvider {

  implicit val materializer = app.materializer
  private lazy val errorTemplateView: ErrorTemplate = app.injector.instanceOf[ErrorTemplate]

  Helpers.running(app) {

    trait Setup extends ControllerSetupBase {

      override val aLoggedInRequest = FakeRequest().withSession(authToken, userToken)
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(authToken, superUserToken)

      val smsController = new SmsController(
        mockThirdPartyDeveloperConnector,
        strideAuthConfig,
        mockAuthConnector,
        forbiddenHandler,
        errorTemplateView,
        mcc
      )

      val sendSmsResponse = SendSmsResponse("SMS sent")

    }

    "sendSms" should {

      "show success message when connector returns Right" in new Setup {
        givenTheGKUserIsAuthorisedAndIsANormalUser()
        ThirdPartyDeveloperConnectorMock.SendSms.returnsSendSmsResponse(sendSmsResponse)
        val result = smsController.sendSms()(aLoggedInRequest)

        contentAsString(result) should include("SMS sent")

        verifyAuthConnectorCalledForUser
      }

      "show technical difficulties page when connector returns Left" in new Setup {
        givenTheGKUserIsAuthorisedAndIsANormalUser()
        ThirdPartyDeveloperConnectorMock.SendSms.returnsError
        val result = smsController.sendSms()(aLoggedInRequest)

        contentAsString(result) should include("technical difficulties")

        verifyAuthConnectorCalledForUser
      }

    }
  }
}
