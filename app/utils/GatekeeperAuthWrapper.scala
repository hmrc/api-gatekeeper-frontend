/*
 * Copyright 2021 HM Revenue & Customs
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

import config.AppConfig
import connectors.AuthConnector
import model.{GatekeeperRole, LoggedInRequest, LoggedInUser}
import model.GatekeeperRole.GatekeeperRole
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{ ~ }
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.ForbiddenView

import scala.concurrent.{ExecutionContext, Future}

trait GatekeeperAuthWrapper extends I18nSupport{
  self: FrontendBaseController =>
  def authConnector: AuthConnector
  val forbiddenView: ForbiddenView

  implicit def loggedIn(implicit request: LoggedInRequest[_]): LoggedInUser = LoggedInUser(request.name)

  def requiresAtLeast(minimumRoleRequired: GatekeeperRole)(body: LoggedInRequest[_] => Future[Result])
                     (implicit ec: ExecutionContext, appConfig: AppConfig): Action[AnyContent] = Action.async {
    implicit request: Request[AnyContent] =>

      val predicate = authPredicate(minimumRoleRequired)
      val retrieval = Retrievals.name and Retrievals.authorisedEnrolments

      authConnector.authorise(predicate, retrieval) flatMap {
        case Some(name) ~ authorisedEnrolments =>
          body(LoggedInRequest(name.name, authorisedEnrolments, request))
        case None ~ authorisedEnrolments =>
          println("POMEGRANATE")
          Future.successful(Forbidden(forbiddenView()))   // ??? TODO
      } recoverWith {
        case _: NoActiveSession =>
          request.secure
          Future.successful(toStrideLogin)
        case _: InsufficientEnrolments =>
          Future.successful(Forbidden(forbiddenView()))
      }
  }

  private def toStrideLogin(implicit appConfig: AppConfig): Result = {
    Redirect(
      appConfig.strideLoginUrl,
      Map(
        "successURL" -> Seq(appConfig.gatekeeperSuccessUrl),
        "origin" -> Seq(appConfig.appName)
      ))
  }

  def authPredicate(minimumRoleRequired: GatekeeperRole)(implicit appConfig: AppConfig): Predicate = {

    val adminEnrolment = Enrolment(appConfig.adminRole)
    val superUserEnrolment = Enrolment(appConfig.superUserRole)
    val userEnrolment = Enrolment(appConfig.userRole)

    minimumRoleRequired match {
      case GatekeeperRole.ADMIN => adminEnrolment
      case GatekeeperRole.SUPERUSER => adminEnrolment or superUserEnrolment
      case GatekeeperRole.USER => adminEnrolment or superUserEnrolment or userEnrolment
    }
  }

  def isAtLeastSuperUser(implicit request: LoggedInRequest[_], appConfig: AppConfig): Boolean = {
    request.authorisedEnrolments.getEnrolment(appConfig.superUserRole).isDefined || request.authorisedEnrolments.getEnrolment(appConfig.adminRole).isDefined
  }

  def isAdmin(implicit request: LoggedInRequest[_], appConfig: AppConfig): Boolean = {
    request.authorisedEnrolments.getEnrolment(appConfig.adminRole).isDefined
  }
}
