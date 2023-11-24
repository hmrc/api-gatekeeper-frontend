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

package uk.gov.hmrc.gatekeeper.models

import java.time.{LocalDateTime, Period, ZoneOffset}

import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.play.json.Union

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.{Access, AccessType, Privileged, Ropc, Standard}
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.State.State
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{CheckInformation, IpAllowlist, MoreApplication, State, TermsOfUseAgreement}
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{Collaborator, Collaborators, RateLimitTier}
import uk.gov.hmrc.apiplatform.modules.applications.submissions.domain.models.TermsOfUseAcceptance
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.gatekeeper.utils.PaginationHelper

trait Application {
  val id: ApplicationId
  val name: String
  val state: ApplicationState
  val collaborators: Set[Collaborator]
  val clientId: ClientId
  val deployedTo: String

  def admins = collaborators.filter(_.isAdministrator)

  def isSoleAdmin(emailAddress: LaxEmailAddress) = admins.map(_.emailAddress).contains(emailAddress) && admins.size == 1

  def isApproved                     = state.isApproved
  def isPendingGatekeeperApproval    = state.isPendingGatekeeperApproval
  def isPendingRequesterVerification = state.isPendingRequesterVerification
}

sealed trait PrivacyPolicyLocation

object PrivacyPolicyLocation {
  case object NoneProvided      extends PrivacyPolicyLocation
  case object InDesktopSoftware extends PrivacyPolicyLocation
  case class Url(value: String) extends PrivacyPolicyLocation

  implicit val noneProvidedFormat      = Json.format[NoneProvided.type]
  implicit val inDesktopSoftwareFormat = Json.format[InDesktopSoftware.type]
  implicit val urlFormat               = Json.format[Url]

  implicit val format = Union.from[PrivacyPolicyLocation]("privacyPolicyType")
    .and[NoneProvided.type]("noneProvided")
    .and[InDesktopSoftware.type]("inDesktop")
    .and[Url]("url")
    .format
}

sealed trait TermsAndConditionsLocation

object TermsAndConditionsLocation {
  case object NoneProvided      extends TermsAndConditionsLocation
  case object InDesktopSoftware extends TermsAndConditionsLocation
  case class Url(value: String) extends TermsAndConditionsLocation

  implicit val noneProvidedFormat      = Json.format[NoneProvided.type]
  implicit val inDesktopSoftwareFormat = Json.format[InDesktopSoftware.type]
  implicit val urlFormat               = Json.format[Url]

  implicit val format = Union.from[TermsAndConditionsLocation]("termsAndConditionsType")
    .and[NoneProvided.type]("noneProvided")
    .and[InDesktopSoftware.type]("inDesktop")
    .and[Url]("url")
    .format
}

case class ResponsibleIndividual(fullName: ResponsibleIndividual.Name, emailAddress: ResponsibleIndividual.EmailAddress)

object ResponsibleIndividual {
  import play.api.libs.json.{Format, Json}

  case class Name(value: String)         extends AnyVal
  case class EmailAddress(value: String) extends AnyVal

  implicit val nameFormat         = Json.valueFormat[Name]
  implicit val emailAddressFormat = Json.valueFormat[EmailAddress]

  implicit val format: Format[ResponsibleIndividual] = Json.format[ResponsibleIndividual]

  def build(name: String, email: String) = ResponsibleIndividual(Name(name), EmailAddress(email))
}

case class ImportantSubmissionData(
    termsAndConditionsLocation: TermsAndConditionsLocation,
    privacyPolicyLocation: PrivacyPolicyLocation,
    termsOfUseAcceptances: List[TermsOfUseAcceptance]
  )

object ImportantSubmissionData {
  implicit val format = Json.format[ImportantSubmissionData]
}

// TODO - Remove Enumeration
object OverrideType extends Enumeration {
  type OverrideType = Value
  val PERSIST_LOGIN_AFTER_GRANT, GRANT_WITHOUT_TAXPAYER_CONSENT, SUPPRESS_IV_FOR_AGENTS, SUPPRESS_IV_FOR_ORGANISATIONS, SUPPRESS_IV_FOR_INDIVIDUALS = Value

  val displayedType: OverrideType => String = {
    case PERSIST_LOGIN_AFTER_GRANT      => "Persist login after grant"
    case GRANT_WITHOUT_TAXPAYER_CONSENT => "Grant without taxpayer consent"
    case SUPPRESS_IV_FOR_AGENTS         => "Suppress IV for agents"
    case SUPPRESS_IV_FOR_ORGANISATIONS  => "Suppress IV for organisations"
    case SUPPRESS_IV_FOR_INDIVIDUALS    => "Suppress IV for individuals"
  }

  implicit val format = Json.formatEnum(OverrideType)
}

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

case class PaginatedApplicationResponse(applications: List[ApplicationResponse], page: Int, pageSize: Int, total: Int, matching: Int) {
  val maxPage = PaginationHelper.maxPage(matching, pageSize)
}

object PaginatedApplicationResponse {
  implicit val format = Json.format[PaginatedApplicationResponse]
}

case class ApplicationWithSubscriptionsResponse(id: ApplicationId, name: String, lastAccess: Option[LocalDateTime], apiIdentifiers: Set[ApiIdentifier])

object ApplicationWithSubscriptionsResponse {
  implicit val format: Format[ApplicationWithSubscriptionsResponse] = Json.format[ApplicationWithSubscriptionsResponse]
}

case class TotpIds(production: String)

case class TotpSecrets(production: String)

case class SubscriptionNameAndVersion(name: String, version: String)

object CollaboratorRole extends Enumeration {
  type CollaboratorRole = Value
  val DEVELOPER, ADMINISTRATOR = Value

  def displayedRole: Collaborator => String = _ match {
    case _: Collaborators.Administrator => "Administrator"
    case _                              => "Developer"
  }

  def from(role: Option[String]) = role match {
    case Some(r) => CollaboratorRole.values.find(e => e.toString == r.toUpperCase)
    case _       => Some(CollaboratorRole.DEVELOPER)
  }

  implicit val format = Json.formatEnum(CollaboratorRole)
}

case class ApplicationState(
    name: State = State.TESTING,
    requestedByEmailAddress: Option[String] = None,
    verificationCode: Option[String] = None,
    updatedOn: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
  ) {
  def isApproved                     = name.isApproved
  def isPendingGatekeeperApproval    = name.isPendingGatekeeperApproval
  def isPendingRequesterVerification = name.isPendingRequesterVerification
  def isDeleted                      = name.isDeleted
}
