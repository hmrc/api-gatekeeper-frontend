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

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

import cats.data.NonEmptyList
import com.google.inject.{Inject, Singleton}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.domain.models.Collaborator
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{Actors, LaxEmailAddress}
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.gatekeeper.connectors.{CommandConnector, DeveloperConnector, ProductionCommandConnector, SandboxCommandConnector}
import uk.gov.hmrc.gatekeeper.models.Application

@Singleton
class TeamMemberService @Inject() (
    sandboxCommandConnector: SandboxCommandConnector,
    productionCommandConnector: ProductionCommandConnector,
    developerConnector: DeveloperConnector
  )(implicit ec: ExecutionContext
  ) extends ApplicationLogger {

  def addTeamMember(app: Application, collaborator: Collaborator, gatekeeperUserName: String)(implicit hc: HeaderCarrier): Future[Either[NonEmptyList[CommandFailure], Unit]] = {
    val commandConnector = commandConnectorFor(app)

    for {
      adminsToEmail <- getAdminsToEmail(app.collaborators, excludes = Set.empty)
      cmd            = AddCollaborator(Actors.GatekeeperUser(gatekeeperUserName), collaborator, LocalDateTime.now())
      response      <- commandConnector.dispatch(app.id, cmd, adminsToEmail)
    } yield response.map(_ => ())
  }

  def removeTeamMember(
      app: Application,
      teamMemberToRemove: LaxEmailAddress,
      gatekeeperUserName: String
    )(implicit hc: HeaderCarrier
    ): Future[Either[NonEmptyList[CommandFailure], Unit]] = {

    val commandConnector = commandConnectorFor(app)
    val collaborator     = app.collaborators.find(_.emailAddress equalsIgnoreCase (teamMemberToRemove)).get // Safe to do here.

    for {
      adminsToEmail <- getAdminsToEmail(app.collaborators, excludes = Set(teamMemberToRemove))
      cmd            = RemoveCollaborator(Actors.GatekeeperUser(gatekeeperUserName), collaborator, LocalDateTime.now())
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

  def commandConnectorFor(application: Application): CommandConnector =
    if (application.deployedTo == "PRODUCTION") productionCommandConnector else sandboxCommandConnector

}
