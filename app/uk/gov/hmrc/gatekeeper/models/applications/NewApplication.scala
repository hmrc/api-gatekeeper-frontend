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

package uk.gov.hmrc.gatekeeper.models.applications

import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.Environment.Environment
import uk.gov.hmrc.gatekeeper.models.RateLimitTier.RateLimitTier
import org.joda.time.DateTime
import uk.gov.hmrc.play.json.Union

import java.time.Period

case class NewApplication(
    id: ApplicationId,
    clientId: ClientId,
    gatewayId: String,
    name: String,
    createdOn: DateTime,
    lastAccess: Option[DateTime],
    lastAccessTokenUsage: Option[DateTime] = None,
    deployedTo: Environment,
    description: Option[String] = None,
    collaborators: Set[Collaborator] = Set.empty,
    access: Access = Standard(),
    state: ApplicationState = ApplicationState(),
    rateLimitTier: RateLimitTier,
    blocked: Boolean,
    checkInformation: Option[CheckInformation] = None,
    ipAllowlist: IpAllowlist = IpAllowlist(),
    grantLength: Period
) {
  lazy val privacyPolicyLocation = access match {
    case Standard(_, _, _, Some(ImportantSubmissionData(_, privacyPolicyLocation, _)), _) => privacyPolicyLocation
    case Standard(_, _, Some(url), _, _) => PrivacyPolicyLocation.Url(url)
    case _ => PrivacyPolicyLocation.NoneProvided
  }
  lazy val termsAndConditionsLocation = access match {
    case Standard(_, _, _, Some(ImportantSubmissionData(termsAndConditionsLocation, _, _)), _) => termsAndConditionsLocation
    case Standard(_, Some(url), _, _, _) => TermsAndConditionsLocation.Url(url)
    case _ => TermsAndConditionsLocation.NoneProvided
  }
}

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
  implicit val formatRateLimitTier = Json.formatEnum(RateLimitTier)
  implicit val formatGrantLength = Json.formatEnum(GrantLength)
  implicit val applicationFormat = Json.format[NewApplication]

  implicit val ordering: Ordering[NewApplication] = Ordering.by(_.name)
}
