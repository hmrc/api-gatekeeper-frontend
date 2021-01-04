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

package model.applications

import model.{Access, AccessType, ApplicationId, ApplicationState, CheckInformation, ClientId, Collaborator, CollaboratorRole, IpAllowlist, Privileged, Ropc, Standard, TotpIds}
import org.joda.time.DateTime
import model.Environment.Environment
import uk.gov.hmrc.play.json.Union
import model.RateLimitTier.RateLimitTier

case class NewApplication(
    id: ApplicationId,
    clientId: ClientId,
    gatewayId: String,
    name: String,
    createdOn: DateTime,
    lastAccess: DateTime,
    lastAccessTokenUsage: Option[DateTime] = None,
    deployedTo: Environment,
    description: Option[String] = None,
    collaborators: Set[Collaborator] = Set.empty,
    access: Access = Standard(),
    state: ApplicationState = ApplicationState(),
    rateLimitTier: RateLimitTier,
    blocked: Boolean,
    checkInformation: Option[CheckInformation] = None,
    ipAllowlist: IpAllowlist = IpAllowlist()
)

object NewApplication {
  import play.api.libs.json.Json
  import play.api.libs.json.JodaReads._
  import play.api.libs.json.JodaWrites._

  implicit val formatTotpIds = Json.format[TotpIds]

  private implicit val formatStandard = Json.format[Standard]
  private implicit val formatPrivileged = Json.format[Privileged]
  private implicit val formatRopc = Json.format[Ropc]
  implicit val formAccessType = Union.from[Access]("accessType")
    .and[Standard](AccessType.STANDARD.toString)
    .and[Privileged](AccessType.PRIVILEGED.toString)
    .and[Ropc](AccessType.ROPC.toString)
    .format

  implicit val formatRole = Json.formatEnum(CollaboratorRole)
  implicit val formatCollaborator = Json.format[Collaborator]
  implicit val formatApplicationState = Json.format[ApplicationState]
  implicit val formatRateLimitTier = Json.formatEnum(model.RateLimitTier)
  implicit val applicationFormat = Json.format[NewApplication]

  implicit val ordering: Ordering[NewApplication] = Ordering.by(_.name)
}
