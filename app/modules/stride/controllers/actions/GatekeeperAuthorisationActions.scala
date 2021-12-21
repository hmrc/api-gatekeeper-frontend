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

package uk.gov.hmrc.modules.stride.controllers.actions

import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{ ~ }
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import uk.gov.hmrc.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.modules.stride.domain.models.GatekeeperRole
import uk.gov.hmrc.modules.stride.domain.models.GatekeeperRole.GatekeeperRole
import uk.gov.hmrc.modules.stride.controllers.models.LoggedInRequest

import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.ActionRefiner
import play.api.mvc.MessagesRequest
import uk.gov.hmrc.modules.stride.config.StrideAuthConfig


trait ForbiddenHandler {
  def handle(msgResult: MessagesRequest[_]): Result
}

trait GatekeeperAuthorisationActions {
  self: FrontendBaseController =>

  def authConnector: AuthConnector
  
  def forbiddenHandler: ForbiddenHandler

  def strideAuthConfig: StrideAuthConfig

  implicit def ec: ExecutionContext

  def gatekeeperRoleActionRefiner(minimumRoleRequired: GatekeeperRole): ActionRefiner[MessagesRequest, LoggedInRequest] = 
    new ActionRefiner[MessagesRequest, LoggedInRequest] {
      def executionContext = ec
      def refine[A](msgRequest: MessagesRequest[A]): Future[Either[Result, LoggedInRequest[A]]] = {

        lazy val loginRedirect = 
          Redirect(
            strideAuthConfig.strideLoginUrl, 
            Map("successURL" -> Seq(strideAuthConfig.successUrl), "origin" -> Seq(strideAuthConfig.origin))
          )

        implicit val request = msgRequest

        val predicate = authPredicate(minimumRoleRequired)
        val retrieval = Retrievals.name and Retrievals.authorisedEnrolments

        authConnector.authorise(predicate, retrieval) map {
          case Some(name) ~ authorisedEnrolments => Right(new LoggedInRequest(name.name, authorisedEnrolments, request))
          case None ~ authorisedEnrolments       => Left(forbiddenHandler.handle(msgRequest))
        } recover {
          case _: NoActiveSession                => Left(loginRedirect)
          case _: InsufficientEnrolments         => Left(forbiddenHandler.handle(msgRequest))
        }
      }
    }   

  private def authPredicate(minimumRoleRequired: GatekeeperRole): Predicate = {
    val adminEnrolment = Enrolment(strideAuthConfig.adminRole)
    val superUserEnrolment = Enrolment(strideAuthConfig.superUserRole)
    val userEnrolment = Enrolment(strideAuthConfig.userRole)

    minimumRoleRequired match {
      case GatekeeperRole.ADMIN => adminEnrolment
      case GatekeeperRole.SUPERUSER => adminEnrolment or superUserEnrolment
      case GatekeeperRole.USER => adminEnrolment or superUserEnrolment or userEnrolment
    }
  }

  private def gatekeeperRoleAction(minimumRoleRequired: GatekeeperRole)(block: LoggedInRequest[_] => Future[Result]): Action[AnyContent] =
    Action.async { implicit request =>
      gatekeeperRoleActionRefiner(minimumRoleRequired).invokeBlock(request, block)
    }

  def anyStrideUserAction(block: LoggedInRequest[_] => Future[Result]): Action[AnyContent] =
    gatekeeperRoleAction(GatekeeperRole.USER)(block)

  def atLeastSuperUserAction(block: LoggedInRequest[_] => Future[Result]): Action[AnyContent] =
    gatekeeperRoleAction(GatekeeperRole.SUPERUSER)(block)

  def adminOnlyAction(block: LoggedInRequest[_] => Future[Result]): Action[AnyContent] =
    gatekeeperRoleAction(GatekeeperRole.ADMIN)(block)
}
