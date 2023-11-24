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

package uk.gov.hmrc.gatekeeper.models.applications

import java.time.{LocalDateTime, Period}

import uk.gov.hmrc.play.json.Union

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{CheckInformation, IpAllowlist, MoreApplication}
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{Collaborator, RateLimitTier}
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.gatekeeper.models._

case class NewApplication(
    id: ApplicationId,
    clientId: ClientId,
    gatewayId: String,
    name: String,
    createdOn: LocalDateTime,
    lastAccess: Option[LocalDateTime],
    lastAccessTokenUsage: Option[LocalDateTime] = None,
    deployedTo: Environment,
    description: Option[String] = None,
    collaborators: Set[Collaborator] = Set.empty,
    access: Access = Standard(),
    state: ApplicationState = ApplicationState(),
    rateLimitTier: RateLimitTier,
    blocked: Boolean,
    checkInformation: Option[CheckInformation] = None,
    ipAllowlist: IpAllowlist = IpAllowlist(),
    grantLength: Period,
    moreApplication: MoreApplication = MoreApplication()
  ) {

  lazy val privacyPolicyLocation = access match {
    case Standard(_, _, _, Some(ImportantSubmissionData(_, privacyPolicyLocation, _)), _) => privacyPolicyLocation
    case Standard(_, _, Some(url), _, _)                                                  => PrivacyPolicyLocation.Url(url)
    case _                                                                                => PrivacyPolicyLocation.NoneProvided
  }

  lazy val termsAndConditionsLocation = access match {
    case Standard(_, _, _, Some(ImportantSubmissionData(termsAndConditionsLocation, _, _)), _) => termsAndConditionsLocation
    case Standard(_, Some(url), _, _, _)                                                       => TermsAndConditionsLocation.Url(url)
    case _                                                                                     => TermsAndConditionsLocation.NoneProvided
  }
}

object NewApplication {
  import play.api.libs.json.Json
  import uk.gov.hmrc.apiplatform.modules.common.domain.services.LocalDateTimeFormatter._

  implicit val formatTotpIds = Json.format[TotpIds]

  implicit private val formatStandard   = Json.format[Standard]
  implicit private val formatPrivileged = Json.format[Privileged]
  implicit private val formatRopc       = Json.format[Ropc]

  implicit val formAccessType = Union.from[Access]("accessType")
    .and[Standard](AccessType.STANDARD.toString)
    .and[Privileged](AccessType.PRIVILEGED.toString)
    .and[Ropc](AccessType.ROPC.toString)
    .format

  implicit val formatRole             = Json.formatEnum(CollaboratorRole)
  implicit val formatApplicationState = Json.format[ApplicationState]
  implicit val applicationFormat      = Json.format[NewApplication]

  implicit val ordering: Ordering[NewApplication] = Ordering.by(_.name)
}
