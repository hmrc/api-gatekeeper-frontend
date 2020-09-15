/*
 * Copyright 2020 HM Revenue & Customs
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

package model.applications

import model.ClientId

import model.ApplicationId
import model.CheckInformation
import org.joda.time.DateTime
import model.Environment.Environment
import model.Collaborator
import model.Access
import model.Standard
import model.State
import model.State.State
import model.CollaboratorRole
import uk.gov.hmrc.play.json.Union
import model.AccessType
import model.Privileged
import model.Ropc
import model.TotpIds

case class NewApplication(
    id: ApplicationId,
    clientId: ClientId,
    name: String,
    createdOn: DateTime,
    lastAccess: DateTime,
    lastAccessTokenUsage: Option[DateTime] = None,
    deployedTo: Environment,
    description: Option[String] = None,
    collaborators: Set[Collaborator] = Set.empty,
    access: Access = Standard(),
    state: State = State.TESTING,
    checkInformation: Option[CheckInformation] = None,
    ipWhitelist: Set[String] = Set.empty
) {
}

object NewApplication {
  import play.api.libs.json.Json
  import play.api.libs.json.JodaReads._
  import play.api.libs.json.JodaWrites._
  import model.EnumJson

  implicit val formatTotpIds = Json.format[TotpIds]

  private implicit val formatStandard = Json.format[Standard]
  private implicit val formatPrivileged = Json.format[Privileged]
  private implicit val formatRopc = Json.format[Ropc]
  implicit val formAccessType = Union.from[Access]("accessType")
    .and[Standard](AccessType.STANDARD.toString)
    .and[Privileged](AccessType.PRIVILEGED.toString)
    .and[Ropc](AccessType.ROPC.toString)
    .format

  implicit val formatRole = EnumJson.enumFormat(CollaboratorRole)
  implicit val formatCollaborator = Json.format[Collaborator]
  implicit val applicationFormat = Json.format[NewApplication]

  implicit val ordering: Ordering[NewApplication] = Ordering.by(_.name)
}