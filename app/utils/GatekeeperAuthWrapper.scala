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
import play.api.Mode
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.retrieve.{~, _}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait GatekeeperAuthWrapper {
  self: BaseController =>

  def authConnector: AuthConnector

  implicit def loggedIn(implicit request: LoggedInRequest[_]) = Some(request.name)

  def requiresRole(requiresSuperUser: Boolean = false)(body: LoggedInRequest[_] => HeaderCarrier => Future[Result]): Action[AnyContent] = Action.async {
    implicit request =>
      val superUserEnrolment = Enrolment(appConfig.superUserRole) or Enrolment(appConfig.adminRole)
      val anyGatekeeperEnrolment = superUserEnrolment or Enrolment(appConfig.userRole)
      val enrolment = if (requiresSuperUser) superUserEnrolment else anyGatekeeperEnrolment

      implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

      val predicate = enrolment and AuthProviders(PrivilegedApplication)
      val retrieval: Retrieval[~[Name, Enrolments]] = Retrievals.name and Retrievals.authorisedEnrolments

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

  private def toStrideLogin(successUrl: String, failureUrl: Option[String] = None): Result = //this is never used
    Redirect(
      appConfig.strideLoginUrl,
      Map(
        "successURL" -> Seq(successUrl),
        "origin" -> Seq(appConfig.appName)
      ) ++ failureUrl.map(f => Map("failureURL" -> Seq(f))).getOrElse(Map()))

  def isSuperUser(implicit request: LoggedInRequest[_]): Boolean = {
    request.authorisedEnrolments.getEnrolment(appConfig.superUserRole).isDefined || request.authorisedEnrolments.getEnrolment(appConfig.adminRole).isDefined
  }

}

case class LoggedInRequest[A](name: String, authorisedEnrolments: Enrolments, request: Request[A]) extends WrappedRequest(request)
