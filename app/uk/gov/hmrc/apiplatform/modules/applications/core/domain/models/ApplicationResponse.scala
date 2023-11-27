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

package uk.gov.hmrc.apiplatform.modules.applications.core.domain.models

import java.time.{LocalDateTime, Period}

import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, JsPath, Json, OFormat, Reads}
import uk.gov.hmrc.play.json.Union

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{Collaborator, RateLimitTier}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId}
import uk.gov.hmrc.gatekeeper.models.{Application, ApprovedApplication}

case class ApplicationResponse(
    id: ApplicationId,
    clientId: ClientId,
    gatewayId: String,
    name: String,
    deployedTo: String,
    description: Option[String] = None,
    collaborators: Set[Collaborator],
    createdOn: LocalDateTime,
    lastAccess: Option[LocalDateTime],
    access: Access,
    state: ApplicationState,
    grantLength: Period,
    rateLimitTier: RateLimitTier = RateLimitTier.BRONZE,
    termsAndConditionsUrl: Option[String] = None,
    privacyPolicyUrl: Option[String] = None,
    checkInformation: Option[CheckInformation] = None,
    blocked: Boolean = false,
    ipAllowlist: IpAllowlist = IpAllowlist(),
    moreApplication: MoreApplication = MoreApplication()
  ) extends Application

object ApplicationResponse {
  import uk.gov.hmrc.apiplatform.modules.common.domain.services.LocalDateTimeFormatter._

  implicit val formatTotpIds = Json.format[TotpIds]

  implicit private val formatStandard   = Json.format[Standard]
  implicit private val formatPrivileged = Json.format[Privileged]
  implicit private val formatRopc       = Json.format[Ropc]

  implicit val formatAccess: OFormat[Access] = Union.from[Access]("accessType")
    .and[Standard](AccessType.STANDARD.toString)
    .and[Privileged](AccessType.PRIVILEGED.toString)
    .and[Ropc](AccessType.ROPC.toString)
    .format
  implicit val formatRole                    = Json.formatEnum(CollaboratorRole)
  implicit val format3                       = Json.formatEnum(State)
  implicit val format4                       = Json.format[ApplicationState]
  implicit val format5                       = Json.format[ApprovedApplication]

  val applicationResponseReads: Reads[ApplicationResponse] = (
    (JsPath \ "id").read[ApplicationId] and
      (JsPath \ "clientId").read[ClientId] and
      (JsPath \ "gatewayId").read[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "deployedTo").read[String] and
      (JsPath \ "description").readNullable[String] and
      (JsPath \ "collaborators").read[Set[Collaborator]] and
      (JsPath \ "createdOn").read[LocalDateTime] and
      (JsPath \ "lastAccess").readNullable[LocalDateTime] and
      (JsPath \ "access").read[Access] and
      (JsPath \ "state").read[ApplicationState] and
      (JsPath \ "grantLength").read[Period] and
      (JsPath \ "rateLimitTier").read[RateLimitTier] and
      (JsPath \ "termsAndConditionsUrl").readNullable[String] and
      (JsPath \ "privacyAndPolicyUrl").readNullable[String] and
      (JsPath \ "checkInformation").readNullable[CheckInformation] and
      ((JsPath \ "blocked").read[Boolean] or Reads.pure(false)) and
      (JsPath \ "ipAllowlist").read[IpAllowlist] and
      ((JsPath \ "moreApplication").read[MoreApplication] or Reads.pure(MoreApplication()))
  )(ApplicationResponse.apply _)

  implicit val formatApplicationResponse = {
    Format(applicationResponseReads, Json.writes[ApplicationResponse])
  }
  implicit val format6                   = Json.format[TermsOfUseAgreement]
}
