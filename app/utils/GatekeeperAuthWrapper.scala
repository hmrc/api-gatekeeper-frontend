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
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{~, _}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait GatekeeperAuthWrapper {
  self: BaseController =>

  def authConnector: AuthConnector

  implicit def loggedIn(implicit request: LoggedInRequest[_]) = Some(request.name)

  def requiresRole(requiresAtLeastSuperUser: Boolean = false, requiresAdmin: Boolean = false)(body: LoggedInRequest[_] => HeaderCarrier => Future[Result]): Action[AnyContent] = Action.async {
    implicit request =>
      implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

      val predicate = authPredicate(requiresAtLeastSuperUser, requiresAdmin)
      val retrieval = Retrievals.name and Retrievals.authorisedEnrolments

      authConnector.authorise(predicate, retrieval).flatMap {
        case name ~ authorisedEnrolments =>
          body(LoggedInRequest(name.name.get.toString, authorisedEnrolments, request))(hc)
      } recoverWith {
        case _: NoActiveSession =>
          request.secure
          Future.successful(toStrideLogin(hostUri))
        case _: InsufficientEnrolments =>
          Future.successful(Forbidden)
      }
  }

  private def hostUri(implicit request: Request[_]) = { //this is unused
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

  def isAtLeastSuperUser(implicit request: LoggedInRequest[_]): Boolean = {
    request.authorisedEnrolments.getEnrolment(appConfig.superUserRole).isDefined || request.authorisedEnrolments.getEnrolment(appConfig.adminRole).isDefined
  }

  def isAdmin(implicit request: LoggedInRequest[_]): Boolean = {
    request.authorisedEnrolments.getEnrolment(appConfig.adminRole).isDefined
  }

  def authPredicate(requiresAtLeastSuperUser: Boolean = false, requiresAdmin: Boolean = false): Predicate = {

    val adminEnrolment = Enrolment(appConfig.adminRole)
    val superUserEnrolment = Enrolment(appConfig.superUserRole)
    val userEnrolment = Enrolment(appConfig.userRole)

    (requiresAtLeastSuperUser, requiresAdmin) match {
      case (_, true) => adminEnrolment
      case (true, false) => adminEnrolment or superUserEnrolment
      case (false, false) => adminEnrolment or superUserEnrolment or userEnrolment
    }
  }

}

case class LoggedInRequest[A](name: String, authorisedEnrolments: Enrolments, request: Request[A]) extends WrappedRequest(request)
