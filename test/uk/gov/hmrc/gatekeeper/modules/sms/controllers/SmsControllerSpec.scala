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

package uk.gov.hmrc.gatekeeper.modules.sms.controllers

import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.TokenProvider
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationServiceMockModule
import uk.gov.hmrc.gatekeeper.controllers.{ControllerBaseSpec, ControllerSetupBase}
import uk.gov.hmrc.gatekeeper.modules.sms.connectors.ThirdPartyDeveloperConnector.SendSmsResponse
import uk.gov.hmrc.gatekeeper.modules.sms.mocks.ThirdPartyDeveloperConnectorMockProvider
import uk.gov.hmrc.gatekeeper.modules.sms.views.html.{SendSmsSuccessView, SendSmsView}
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport.CSRFFakeRequest
import uk.gov.hmrc.gatekeeper.utils.WithCSRFAddToken
import uk.gov.hmrc.gatekeeper.views.html.ErrorTemplate

import scala.concurrent.ExecutionContext.Implicits.global

class SmsControllerSpec extends ControllerBaseSpec with ThirdPartyDeveloperConnectorMockProvider with WithCSRFAddToken {

  implicit val materializer = app.materializer
  private lazy val errorTemplateView: ErrorTemplate = app.injector.instanceOf[ErrorTemplate]

  Helpers.running(app) {

    trait Setup extends ControllerSetupBase with StrideAuthorisationServiceMockModule {

      val csrfToken = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken
      override val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken).withCSRFToken
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, superUserToken).withCSRFToken
      val sendSmsView = app.injector.instanceOf[SendSmsView]
      val sendSmsSuccessView = app.injector.instanceOf[SendSmsSuccessView]

      val smsController = new SmsController(
        mockThirdPartyDeveloperConnector,
        StrideAuthorisationServiceMock.aMock,
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
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        ThirdPartyDeveloperConnectorMock.SendSms.returnsSendSmsResponse(sendSmsResponse)
        val request = aLoggedInRequest.withFormUrlEncodedBody(("phoneNumber", testNumber))
        val result = addToken(smsController.sendSmsAction())(request)

        contentAsString(result) should include("SMS sent")
      }

      "show validation errors when form field is empty" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        ThirdPartyDeveloperConnectorMock.SendSms.returnsSendSmsResponse(sendSmsResponse)
        val request = aLoggedInRequest.withFormUrlEncodedBody(("phoneNumber", ""))
        val result = addToken(smsController.sendSmsAction())(request)

        contentAsString(result) should include("Phone number is required")
      }

      "show technical difficulties page when connector returns Left" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        ThirdPartyDeveloperConnectorMock.SendSms.returnsError
        val request = aLoggedInRequest.withFormUrlEncodedBody(("phoneNumber", testNumber))
        val result = addToken(smsController.sendSmsAction())(request)

        contentAsString(result) should include("technical difficulties")
      }

    }
  }
}
