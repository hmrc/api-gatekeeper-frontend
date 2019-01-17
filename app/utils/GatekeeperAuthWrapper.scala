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

package utils

import connectors.AuthConnector
import controllers.{BaseController, routes}
import model.{GatekeeperSessionKeys, Role}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait GatekeeperAuthWrapper {
  self: BaseController =>

  def authConnector: AuthConnector

  implicit def loggedIn(implicit request: Request[_]) = request.session.get(GatekeeperSessionKeys.LoggedInUser)

  def requiresLogin(body: Request[_] => HeaderCarrier => Future[Result]): Action[AnyContent] = Action.async { implicit request =>
      implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
      request.session.get(GatekeeperSessionKeys.AuthToken) match {
        case Some(_) => body(request)(hc)
        case _ => Future.successful(Redirect(routes.AccountController.loginPage()))
      }
  }

  def requiresRole(requiredRole: Role, requiresSuperUser: Boolean = false)(body: Request[_] => HeaderCarrier => Future[Result]): Action[AnyContent] = {
    requiresLogin { implicit request => implicit hc =>
      def meetsSuperUserRequirement = if (requiresSuperUser) isSuperUser else true

      def redirectToLogin = Future.successful(Redirect(routes.AccountController.loginPage()))

      val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
      request.session.get(GatekeeperSessionKeys.AuthToken).fold(redirectToLogin){ _ =>
        authConnector.authorized(requiredRole)(hc).flatMap { authorized =>
          if (authorized && meetsSuperUserRequirement) body(request)(hc)
          else Future.successful(Unauthorized(views.html.unauthorized()))
        }
      }
    }
  }

  def redirectIfLoggedIn(redirectTo: play.api.mvc.Call)(body: Request[_] => HeaderCarrier => Future[Result]): Action[AnyContent] = Action.async { implicit request =>
    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    request.session.get(GatekeeperSessionKeys.AuthToken) match {
      case Some(_) => Future.successful(Redirect(redirectTo))
      case _ => body(request)(hc)
    }
  }

  def isSuperUser(implicit request: Request[_]): Boolean = {
    appConfig.superUsers.contains(loggedIn.get)
  }

}
