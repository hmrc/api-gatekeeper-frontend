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

import config.AppConfig
import modules.sms.connectors.{SendSmsResponse, ThirdPartyDeveloperConnector}
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.modules.stride.controllers.GatekeeperBaseController
import uk.gov.hmrc.modules.stride.controllers.actions.ForbiddenHandler
import utils.ErrorHelper
import views.html.ErrorTemplate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SmsController @Inject()(thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector,
                              strideAuthConfig: StrideAuthConfig,
                              authConnector: AuthConnector,
                              forbiddenHandler: ForbiddenHandler,
                              override val errorTemplate: ErrorTemplate,
                              mcc: MessagesControllerComponents)(implicit val appConfig: AppConfig, override val ec: ExecutionContext)
  extends GatekeeperBaseController(strideAuthConfig, authConnector, forbiddenHandler, mcc)
    with ErrorHelper {

  def sendSms() = anyStrideUserAction { implicit request =>
    thirdPartyDeveloperConnector.sendSms() map {
      case Right(s: SendSmsResponse) => Ok(s.message)
      case Left(_: Throwable) =>  technicalDifficulties
    }
  }

}
