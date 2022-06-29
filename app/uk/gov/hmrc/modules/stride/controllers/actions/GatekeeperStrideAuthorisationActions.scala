/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import uk.gov.hmrc.modules.stride.domain.models.GatekeeperStrideRole
import uk.gov.hmrc.modules.stride.domain.models.GatekeeperRoles
import uk.gov.hmrc.modules.stride.domain.models.LoggedInRequest

import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.ActionRefiner
import play.api.mvc.MessagesRequest
import uk.gov.hmrc.modules.stride.domain.models.GatekeeperRole
import uk.gov.hmrc.modules.stride.domain.models.GatekeeperRoles.READ_ONLY
import uk.gov.hmrc.modules.stride.services._

trait ForbiddenHandler {
  def handle(msgResult: MessagesRequest[_]): Result
}

trait GatekeeperStrideAuthorisationActions {
  self: FrontendBaseController =>

  def strideAuthorisationService: StrideAuthorisationService

  implicit def ec: ExecutionContext

  def gatekeeperRoleActionRefiner(minimumRoleRequired: GatekeeperRole): ActionRefiner[MessagesRequest, LoggedInRequest] = 
    new ActionRefiner[MessagesRequest, LoggedInRequest] {
      def executionContext = ec

      def refine[A](msgRequest: MessagesRequest[A]): Future[Either[Result, LoggedInRequest[A]]] = {
        
        val strideRoleRequired: GatekeeperStrideRole = minimumRoleRequired match {
          case READ_ONLY => GatekeeperRoles.USER  // The lowest stride category
          case gsr: GatekeeperStrideRole => gsr
        }

        strideAuthorisationService.createStrideRefiner(strideRoleRequired)(msgRequest)
      }
    }

  private def gatekeeperRoleAction(minimumRoleRequired: GatekeeperRole)(block: LoggedInRequest[_] => Future[Result]): Action[AnyContent] =
    Action.async { implicit request =>
      gatekeeperRoleActionRefiner(minimumRoleRequired).invokeBlock(request, block)
    }

  def anyStrideUserAction(block: LoggedInRequest[_] => Future[Result]): Action[AnyContent] =
    gatekeeperRoleAction(GatekeeperRoles.USER)(block)

  def atLeastSuperUserAction(block: LoggedInRequest[_] => Future[Result]): Action[AnyContent] =
    gatekeeperRoleAction(GatekeeperRoles.SUPERUSER)(block)

  def adminOnlyAction(block: LoggedInRequest[_] => Future[Result]): Action[AnyContent] =
    gatekeeperRoleAction(GatekeeperRoles.ADMIN)(block)
}

trait GatekeeperAuthorisationActions {
  self: FrontendBaseController with GatekeeperStrideAuthorisationActions =>
    
  def ldapAuthorisationService: LdapAuthorisationService
    
  def anyAuthenticatedUserAction(block: LoggedInRequest[_] => Future[Result]): Action[AnyContent] =  {
    Action.async { implicit request => 
      ldapAuthorisationService.refineLdap(request)
      .recover { case _ => Left(request) }
      .flatMap(_ match {
        case Left(_) => anyStrideUserAction(block)(request)
        case Right(lir) => block(lir)
      })
    }
  }
}
