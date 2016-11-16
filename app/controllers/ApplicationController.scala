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

package controllers

import connectors.AuthConnector
import model.Role
import play.api.mvc.{Action, AnyContent}
import services.ApplicationService
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.{GatekeeperAuthProvider, GatekeeperAuthWrapper}

object ApplicationController extends ApplicationController {
  override val applicationService: ApplicationService = ApplicationService

  override def authConnector = AuthConnector

  override def authProvider = GatekeeperAuthProvider
}

trait ApplicationController extends FrontendController with GatekeeperAuthWrapper {

  val applicationService: ApplicationService

  def resendVerification(appId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
      for {
        _ <- applicationService.resendVerification(appId, loggedIn.get)
      } yield {
        Redirect(controllers.routes.DashboardController.approvedApplicationPage(appId))
          .flashing("success" -> "Verification email has been sent")
      }
  }
}
