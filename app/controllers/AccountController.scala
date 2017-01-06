/*
 * Copyright 2017 HM Revenue & Customs
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

import config.AppConfig
import connectors.AuthConnector
import model.Forms._
import model.{GatekeeperSessionKeys, LoginDetails}
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.SessionKeys
import utils.{GatekeeperAuthProvider, GatekeeperAuthWrapper}
import views.html.login._

import scala.concurrent.Future

object AccountController extends AccountController {
  override val authConnector = AuthConnector
  override val appConfig = AppConfig
  override def authProvider = GatekeeperAuthProvider
}

trait AccountController extends FrontendController with GatekeeperAuthWrapper {

  val authConnector: AuthConnector
  val welcomePage = routes.DashboardController.dashboardPage
  implicit val appConfig: AppConfig

  val loginPage: Action[AnyContent] = redirectIfLoggedIn(welcomePage) {
    implicit request => implicit hc => Future.successful(Ok(login(loginForm)))
  }

  val authenticate = Action.async { implicit request =>
    loginForm.bindFromRequest().fold(
      errors => Future.successful(BadRequest(login(loginForm))),
      loginDetails => processLogin(loginDetails)
    )
  }

  def logout = Action {
    implicit request => Redirect(routes.AccountController.loginPage).removingFromSession(SessionKeys.authToken)
  }

  private[controllers] def processLogin(loginDetails: LoginDetails)(implicit request: Request[_]) = {
    authConnector.login(loginDetails).map {
      authExchangeResponse => Redirect(welcomePage).withNewSession.addingToSession(
        SessionKeys.authToken -> authExchangeResponse.access_token.authToken,
        GatekeeperSessionKeys.LoggedInUser -> authExchangeResponse.userName
      )
    }.recover {
      case t: Throwable => {
        Logger.error(s"Got exception while logging in user: $t")
        Unauthorized(login(loginForm.withGlobalError(Messages("invalid.username.or.password"))))
      }
    }
  }

}
