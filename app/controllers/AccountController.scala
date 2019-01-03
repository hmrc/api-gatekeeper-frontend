/*
 * Copyright 2019 HM Revenue & Customs
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
import model.Forms._
import model.{GatekeeperSessionKeys, LoginDetails}
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import utils.{GatekeeperAuthProvider, GatekeeperAuthWrapper}
import views.html.login._

import scala.concurrent.Future

object AccountController extends AccountController with WithAppConfig {
  override val authConnector = AuthConnector
  override def authProvider = GatekeeperAuthProvider
}

trait AccountController extends BaseController with GatekeeperAuthWrapper {

  val authConnector: AuthConnector
  val welcomePage = routes.ApplicationController.applicationsPage()

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
    implicit request => Redirect(routes.AccountController.loginPage).removingFromSession(GatekeeperSessionKeys.AuthToken)
  }

  private[controllers] def processLogin(loginDetails: LoginDetails)(implicit request: Request[_]) = {
    authConnector.login(loginDetails).map {
      authExchangeResponse => Redirect(welcomePage).withNewSession.addingToSession(
        GatekeeperSessionKeys.AuthToken -> authExchangeResponse.access_token.authToken,
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
