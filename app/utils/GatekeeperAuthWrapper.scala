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
import controllers.BaseController
import model.{GatekeeperSessionKeys, Role}
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.{AuthProviders, Enrolment, InsufficientEnrolments, NoActiveSession}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait GatekeeperAuthWrapper {
  self: BaseController =>

  def authConnector: AuthConnector

  implicit def loggedIn(implicit request: LoggedInRequest[_]) = Some(request.name)

  case class LoggedInRequest[A](name: String, request: Request[A]) extends WrappedRequest(request)

  def requiresRole(requiredRole: Role, requiresSuperUser: Boolean = false)(body: LoggedInRequest[_] => HeaderCarrier => Future[Result]): Action[AnyContent] = Action.async {
    implicit request =>
      val enrolment = if (requiresSuperUser) Enrolment(appConfig.superUserRole) else Enrolment(appConfig.superUserRole) or Enrolment(appConfig.userRole)

      implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

      val predicate = enrolment and AuthProviders(PrivilegedApplication)
      authConnector.authorise(predicate, Retrievals.name).flatMap {
        case name =>
          body(LoggedInRequest(name.name.get.toString, request))(hc)
      } recoverWith {
        case _: NoActiveSession =>
          request.secure
          Future.successful(toStrideLogin(hostUri))
        case _: InsufficientEnrolments =>
          Future.successful(Forbidden)
      }

  }

  private def hostUri(implicit request: Request[_]) = {
    val protocol = if (request.secure) "https" else "http"
    s"$protocol://${request.host}${request.uri}"
  }

  private def toStrideLogin(successUrl: String, failureUrl: Option[String] = None): Result =
    Redirect(
      appConfig.strideLoginUrl,
      Map(
        "successURL" -> Seq(successUrl),
        "origin" -> Seq(appConfig.appName)
      ) ++ failureUrl.map(f => Map("failureURL" -> Seq(f))).getOrElse(Map()))

  def isSuperUser(implicit request: LoggedInRequest[_]): Boolean = {
    appConfig.superUsers.contains(loggedIn) // TODO: take from enrolments (from either allEnrolments or authorisedEnrolments) on the request/session
  }

}
