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
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{ ~ }
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import uk.gov.hmrc.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.modules.stride.domain.models.GatekeeperStrideRole
import uk.gov.hmrc.modules.stride.domain.models.GatekeeperRoles
import uk.gov.hmrc.modules.stride.domain.models.LoggedInRequest

import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.ActionRefiner
import play.api.mvc.MessagesRequest
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.modules.stride.config.StrideAuthConfig
import cats.data.OptionT
import uk.gov.hmrc.internalauth.client.FrontendAuthComponents
import uk.gov.hmrc.internalauth.client.Retrieval
import scala.concurrent.Future.successful
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.modules.stride.domain.models.GatekeeperRole
import uk.gov.hmrc.modules.stride.domain.models.GatekeeperRoles.READ_ONLY
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.modules.stride.services.StrideAuthorisationService

trait ForbiddenHandler {
  def handle(msgResult: MessagesRequest[_]): Result
}

trait GatekeeperAuthorisationActions {
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

  def anyAuthenticatedUserAction(block: LoggedInRequest[_] => Future[Result]): Action[AnyContent] = 
    gatekeeperRoleAction(GatekeeperRoles.READ_ONLY)(block)

}
