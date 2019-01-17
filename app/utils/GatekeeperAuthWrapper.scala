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

  // TODO: take from saved value on request/session from Retrieval
//  implicit def loggedIn(implicit request: Request[_]) = Some("ME!")// request.session.get(GatekeeperSessionKeys.LoggedInUser) <- None.get :(
  implicit def loggedIn(implicit request: LoggedInRequest[_]) = Some(request.name)

  case class LoggedInRequest[A](name: String, request: Request[A]) extends WrappedRequest(request)

  def requiresRole(requiredRole: Role, requiresSuperUser: Boolean = false)(body: LoggedInRequest[_] => HeaderCarrier => Future[Result]): Action[AnyContent] = Action.async {
     implicit request =>
       val enrolment = if (requiresSuperUser) "superuserrole" else "otherrole"

      implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

      val predicate = Enrolment(enrolment) and AuthProviders(PrivilegedApplication) // TODO: correct enrolment
       val retrieval = Retrievals.allEnrolments and Retrievals.authorisedEnrolments and Retrievals.internalId and Retrievals.name // TODO: correct retrievals
       authConnector.authorise(predicate, retrieval).flatMap {
         case allEnrolments ~ authorisedEnrolments ~ internalId ~ name =>
           request.session.data + ("name" -> name.toString)
           println(s"NAME!!! ${name.toString}") // Name is currently Name(None, None)
           request.session + ("allEnrolments" -> allEnrolments.toString)
           request.session + ("internalId" -> internalId.toString)
           //body(request.withTag("name", name.name.getOrElse("BLEG")))(hc)
           body(LoggedInRequest(name.toString, request))(hc)
       } recoverWith {
         case _: NoActiveSession =>
           Future.successful(toStrideLogin(request.uri))
         case _: InsufficientEnrolments =>
           Future.successful(Forbidden)
       }

    }

  private def toStrideLogin(successUrl: String, failureUrl: Option[String] = None): Result =
    Redirect(
      appConfig.strideLoginUrl,
      Map(
        "successURL" -> Seq(successUrl),
        "origin"     -> Seq(appConfig.appName)
      ) ++ failureUrl.map(f => Map("failureURL" -> Seq(f))).getOrElse(Map()))

  def isSuperUser(implicit request: LoggedInRequest[_]): Boolean = {
    appConfig.superUsers.contains(loggedIn) // TODO: take from enrolments (from either allEnrolments or authorisedEnrolments) on the request/session
  }

}
