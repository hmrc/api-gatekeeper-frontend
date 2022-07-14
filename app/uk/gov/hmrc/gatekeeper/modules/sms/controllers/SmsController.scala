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

import uk.gov.hmrc.apiplatform.modules.sms.connectors.ThirdPartyDeveloperConnector.SendSmsResponse
import uk.gov.hmrc.apiplatform.modules.sms.model.Forms.SendSmsForm
import play.api.data.Form
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.sms.connectors.ThirdPartyDeveloperConnector
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.utils.ErrorHelper
import uk.gov.hmrc.gatekeeper.views.html.ErrorTemplate

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful

@Singleton
class SmsController @Inject()(thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector,
                              strideAuthorisationService: StrideAuthorisationService,
                              sendSmsView: SendSmsView,
                              sendSmsSuccessView: SendSmsSuccessView,
                              override val errorTemplate: ErrorTemplate,
                              mcc: MessagesControllerComponents)(implicit val appConfig: AppConfig, override val ec: ExecutionContext)
  extends GatekeeperBaseController(strideAuthorisationService, mcc)
    with ErrorHelper {

  def sendSmsPage() = anyStrideUserAction { implicit request =>
    successful(Ok(sendSmsView(SendSmsForm.form)))

    }

  def sendSmsAction() = anyStrideUserAction { implicit request =>

    def handleValidForm(form: SendSmsForm) = {
          thirdPartyDeveloperConnector.sendSms(form.phoneNumber) map {
            case Right(s: SendSmsResponse) => Ok(sendSmsSuccessView(s.message))
            case Left(_: Throwable) =>  technicalDifficulties
          }
    }

    def handleInvalidForm(formWithErrors: Form[SendSmsForm]) =
      successful(BadRequest(sendSmsView(formWithErrors)))

    SendSmsForm.form.bindFromRequest.fold(handleInvalidForm, handleValidForm)

  }

}
