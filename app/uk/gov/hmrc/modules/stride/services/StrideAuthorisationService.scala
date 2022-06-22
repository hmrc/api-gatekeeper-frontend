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

package uk.gov.hmrc.modules.stride.services

import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{ ~ }

import uk.gov.hmrc.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.modules.stride.domain.models.GatekeeperStrideRole
import uk.gov.hmrc.modules.stride.domain.models.GatekeeperRoles
import uk.gov.hmrc.modules.stride.domain.models.LoggedInRequest

import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.MessagesRequest
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.modules.stride.domain.models.GatekeeperRole
import uk.gov.hmrc.modules.stride.controllers.actions.ForbiddenHandler
import play.api.mvc.Result

@Singleton
class StrideAuthorisationService @Inject() (
  authConnector: AuthConnector,
  forbiddenHandler: ForbiddenHandler,
  strideAuthConfig: StrideAuthConfig
)(implicit val ec: ExecutionContext) {

  def createStrideRefiner[A](strideRoleRequired: GatekeeperStrideRole): (MessagesRequest[A]) => Future[Either[Result, LoggedInRequest[A]]] = (msgRequest) => {
    implicit val hc = HeaderCarrierConverter.fromRequestAndSession(msgRequest, msgRequest.session)

    lazy val loginRedirect = 
      Redirect(
        strideAuthConfig.strideLoginUrl, 
        Map("successURL" -> Seq(strideAuthConfig.successUrl), "origin" -> Seq(strideAuthConfig.origin))
      )

    implicit val request = msgRequest

    val predicate = authPredicate(strideRoleRequired)
    val retrieval = Retrievals.name and Retrievals.authorisedEnrolments

    authConnector.authorise(predicate, retrieval) map {
      case Some(name) ~ authorisedEnrolments => 
        def applyRole(role: GatekeeperRole): Either[Result, LoggedInRequest[A]] = Right(new LoggedInRequest(name.name, role, request))

        println(strideRoleRequired)
        
        ( authorisedEnrolments.getEnrolment(strideAuthConfig.adminRole).isDefined, authorisedEnrolments.getEnrolment(strideAuthConfig.superUserRole).isDefined, authorisedEnrolments.getEnrolment(strideAuthConfig.userRole).isDefined ) match {
          case (true, _, _) => applyRole(GatekeeperRoles.ADMIN)
          case (_, true, _) => applyRole(GatekeeperRoles.SUPERUSER)
          case (_, _, true) => applyRole(GatekeeperRoles.USER)
          case _            => Left(forbiddenHandler.handle(msgRequest))
        }

      case None ~ authorisedEnrolments       => Left(forbiddenHandler.handle(msgRequest))
    } recover {
      case _: NoActiveSession                => Left(loginRedirect)
      case _: InsufficientEnrolments         => Left(forbiddenHandler.handle(msgRequest))
    }
  }

  private def authPredicate(strideRoleRequired: GatekeeperStrideRole): Predicate = {
    val adminEnrolment = Enrolment(strideAuthConfig.adminRole)
    val superUserEnrolment = Enrolment(strideAuthConfig.superUserRole)
    val userEnrolment = Enrolment(strideAuthConfig.userRole)

    strideRoleRequired match {
      case GatekeeperRoles.ADMIN => adminEnrolment
      case GatekeeperRoles.SUPERUSER => adminEnrolment or superUserEnrolment
      case GatekeeperRoles.USER => adminEnrolment or superUserEnrolment or userEnrolment
    }
  }
}
