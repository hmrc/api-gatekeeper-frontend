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

import connectors.{ApplicationConnector, AuthConnector}
import model.Role
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.{GatekeeperAuthProvider, GatekeeperAuthWrapper}
import views.html.dashboard.dashboard
import views.html.review.review
import views.html.approvedApplication.approved

import scala.concurrent.Future

object DashboardController extends DashboardController {
  override def authProvider = GatekeeperAuthProvider

  override def authConnector = AuthConnector

  override val applicationConnector = ApplicationConnector
}

trait DashboardController extends FrontendController with GatekeeperAuthWrapper {

  val applicationConnector: ApplicationConnector

  val dashboardPage: Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
      Future.successful(Ok(dashboard()))
  }

  val reviewPage: Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
      Future.successful(Ok(review()))
  }

  val approvedApplicationPage: Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
      Future.successful(Ok(approved()))
  }

  def approveUplift(appId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
      applicationConnector.approveUplift(appId, loggedIn.get) map {
        ApproveUpliftSuccessful => Ok(dashboard())
      }

  }
}
