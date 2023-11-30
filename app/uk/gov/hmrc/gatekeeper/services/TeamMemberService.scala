/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.services

import java.time.Clock
import scala.concurrent.{ExecutionContext, Future}

import cats.data.NonEmptyList
import com.google.inject.{Inject, Singleton}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationResponse
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.Collaborator
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{Actors, LaxEmailAddress}
import uk.gov.hmrc.apiplatform.modules.common.services.{ApplicationLogger, ClockNow}
import uk.gov.hmrc.gatekeeper.connectors._

@Singleton
class TeamMemberService @Inject() (
    commandConnector: ApplicationCommandConnector,
    developerConnector: DeveloperConnector,
    val clock: Clock
  )(implicit ec: ExecutionContext
  ) extends ApplicationLogger with ClockNow {

  def addTeamMember(
      app: ApplicationResponse,
      collaborator: Collaborator,
      user: Actors.GatekeeperUser
    )(implicit hc: HeaderCarrier
    ): Future[Either[NonEmptyList[CommandFailure], Unit]] = {

    for {
      adminsToEmail <- getAdminsToEmail(app.collaborators, excludes = Set.empty)
      cmd            = ApplicationCommands.AddCollaborator(user, collaborator, now())
      response      <- commandConnector.dispatch(app.id, cmd, adminsToEmail)
    } yield response.map(_ => ())
  }

  def removeTeamMember(
      app: ApplicationResponse,
      teamMemberToRemove: LaxEmailAddress,
      user: Actors.GatekeeperUser
    )(implicit hc: HeaderCarrier
    ): Future[Either[NonEmptyList[CommandFailure], Unit]] = {

    val collaborator = app.collaborators.find(_.emailAddress equalsIgnoreCase (teamMemberToRemove)).get // Safe to do here.

    for {
      adminsToEmail <- getAdminsToEmail(app.collaborators, excludes = Set(teamMemberToRemove))
      cmd            = ApplicationCommands.RemoveCollaborator(user, collaborator, now())
      response      <- commandConnector.dispatch(app.id, cmd, adminsToEmail)
    } yield response.map(_ => ())
  }

  private def getAdminsToEmail(collaborators: Set[Collaborator], excludes: Set[LaxEmailAddress])(implicit hc: HeaderCarrier): Future[Set[LaxEmailAddress]] = {
    val adminEmails = collaborators.filter(_.isAdministrator).map(_.emailAddress).filterNot(excludes.contains(_))

    developerConnector.fetchByEmails(adminEmails)
      .map(registeredUsers =>
        registeredUsers.filter(_.verified)
          .map(_.email)
          .toSet
      )
  }

}
