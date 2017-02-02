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

package utils

import connectors.AuthConnector
import controllers.routes
import model.{GatekeeperSessionKeys, Role}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result, _}
import uk.gov.hmrc.play.frontend.auth.AuthenticationProvider
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait GatekeeperAuthWrapper {
  self: Results =>

  def authProvider: AuthenticationProvider

  def authConnector: AuthConnector

  implicit def loggedIn(implicit request: Request[_]) = request.session.get(GatekeeperSessionKeys.LoggedInUser)

  private def validatedAsyncAction(f: Request[_] => Future[Result]) =
    SessionTimeoutValidation(authProvider)(Action.async(f))


  def requiresLogin()(body: Request[_] => HeaderCarrier => Future[Result]): Action[AnyContent] = {
    validatedAsyncAction { implicit request =>
      val hc = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))
      (request.session.get(SessionKeys.authToken)) match {
        case Some(_) => body(request)(hc)
        case _ => authProvider.redirectToLogin
      }
    }
  }

  def requiresRole(requiredRole: Role)(body: Request[_] => HeaderCarrier => Future[Result]): Action[AnyContent] = {
    validatedAsyncAction { implicit request =>
      val hc = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))
      request.session.get(SessionKeys.authToken)
        .map { _ =>
          authConnector.authorized(requiredRole)(hc).flatMap {
            case true => body(request)(hc)
            case false => Future.successful(Unauthorized(views.html.unauthorized()))
          }
        }
        .getOrElse(Future.successful(Redirect(routes.AccountController.loginPage)))
    }
  }

  def redirectIfLoggedIn(redirectTo: play.api.mvc.Call)(body: Request[_] => HeaderCarrier => Future[Result]): Action[AnyContent] = {
    validatedAsyncAction { implicit request =>
      val hc = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))
      (request.session.get(SessionKeys.authToken)) match {
        case Some(_) => Future.successful(Redirect(redirectTo))
        case _ => body(request)(hc)
      }
    }
  }


}
