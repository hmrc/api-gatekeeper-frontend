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

import java.util.UUID
import uk.gov.hmrc.gatekeeper.models.CollaboratorRole.CollaboratorRole
import uk.gov.hmrc.gatekeeper.models.RateLimitTier.RateLimitTier
import uk.gov.hmrc.gatekeeper.models.State.State
import uk.gov.hmrc.gatekeeper.utils.PaginationHelper
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.play.json.Union
import play.api.libs.json.JodaReads._
import play.api.libs.json.JodaWrites._

import java.time.Period
import java.time.LocalDateTime
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId

case class ClientId(value: String) extends AnyVal

object ClientId {
  import play.api.libs.json.Json
  implicit val clientIdFormat = Json.valueFormat[ClientId]

  def empty: ClientId  = ClientId("")
  def random: ClientId = ClientId(UUID.randomUUID().toString)
}

trait Application {
  val id: ApplicationId
  val name: String
  val state: ApplicationState
  val collaborators: Set[Collaborator]
  val clientId: ClientId
  val deployedTo: String

  def admins = collaborators.filter(_.role == CollaboratorRole.ADMINISTRATOR)

  def isSoleAdmin(emailAddress: String) = admins.map(_.emailAddress).contains(emailAddress) && admins.size == 1

  def isApproved                     = state.isApproved
  def isPendingGatekeeperApproval    = state.isPendingGatekeeperApproval
  def isPendingRequesterVerification = state.isPendingRequesterVerification
}

case class ContactDetails(fullname: String, email: String, telephoneNumber: String)

object ContactDetails {
  implicit val formatContactDetails = Json.format[ContactDetails]
}

case class TermsOfUseAgreement(emailAddress: String, timeStamp: DateTime, version: String)

case class CheckInformation(
    contactDetails: Option[ContactDetails] = None,
    confirmedName: Boolean = false,
    providedPrivacyPolicyURL: Boolean = false,
    providedTermsAndConditionsURL: Boolean = false,
    applicationDetails: Option[String] = None,
    termsOfUseAgreements: List[TermsOfUseAgreement] = List.empty
  ) {

  def latestTOUAgreement: Option[TermsOfUseAgreement] = {
    implicit val dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)

    termsOfUseAgreements match {
      case Nil        => None
      case agreements => Option(agreements.maxBy(_.timeStamp))
    }
  }
}

object CheckInformation {

  implicit val formatTermsOfUseAgreement = Json.format[TermsOfUseAgreement]
  implicit val formatApprovalInformation = Json.format[CheckInformation]
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

trait LocalDateTimeFormatters extends EnvReads with EnvWrites {

  implicit val dateFormat: Format[LocalDateTime] = Format(DefaultLocalDateTimeReads, DefaultLocalDateTimeWrites)
}

case class TermsOfUseAcceptance(responsibleIndividual: ResponsibleIndividual, dateTime: LocalDateTime)

object TermsOfUseAcceptance extends LocalDateTimeFormatters {
  implicit val format = Json.format[TermsOfUseAcceptance]
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

sealed trait Access {
  val accessType: AccessType.Value
}

sealed trait AccessWithRestrictedScopes extends Access {
  val scopes: Set[String]
}

case class Standard(
    redirectUris: List[String] = List.empty,
    termsAndConditionsUrl: Option[String] = None,
    privacyPolicyUrl: Option[String] = None,
    importantSubmissionData: Option[ImportantSubmissionData] = None,
    overrides: Set[OverrideFlag] = Set.empty
  ) extends Access {
  override val accessType = AccessType.STANDARD
}

case class Privileged(totpIds: Option[TotpIds] = None, scopes: Set[String] = Set.empty) extends AccessWithRestrictedScopes {
  override val accessType = AccessType.PRIVILEGED
}

case class Ropc(scopes: Set[String] = Set.empty) extends AccessWithRestrictedScopes {
  override val accessType = AccessType.ROPC
}

sealed trait OverrideFlag {
  val overrideType: OverrideType.Value
}

object OverrideFlag {
  private implicit val formatGrantWithoutConsent: OFormat[GrantWithoutConsent] = Json.format[GrantWithoutConsent]

  private implicit val formatPersistLogin: OFormat[PersistLogin.type] = OFormat[PersistLogin.type](
    Reads { _ => JsSuccess(PersistLogin) },
    OWrites[PersistLogin.type] { _ => Json.obj() }
  )

  private implicit val formatSuppressIvForAgents: OFormat[SuppressIvForAgents]               = Json.format[SuppressIvForAgents]
  private implicit val formatSuppressIvForOrganisations: OFormat[SuppressIvForOrganisations] = Json.format[SuppressIvForOrganisations]
  private implicit val formatSuppressIvForIndividuals: OFormat[SuppressIvForIndividuals]     = Json.format[SuppressIvForIndividuals]

  implicit val formatOverride = Union.from[OverrideFlag]("overrideType")
    .and[GrantWithoutConsent](OverrideType.GRANT_WITHOUT_TAXPAYER_CONSENT.toString)
    .and[PersistLogin.type](OverrideType.PERSIST_LOGIN_AFTER_GRANT.toString)
    .and[SuppressIvForAgents](OverrideType.SUPPRESS_IV_FOR_AGENTS.toString)
    .and[SuppressIvForOrganisations](OverrideType.SUPPRESS_IV_FOR_ORGANISATIONS.toString)
    .and[SuppressIvForIndividuals](OverrideType.SUPPRESS_IV_FOR_INDIVIDUALS.toString)
    .format
}

sealed trait OverrideFlagWithScopes extends OverrideFlag {
  val scopes: Set[String]
}

case object PersistLogin extends OverrideFlag {
  val overrideType = OverrideType.PERSIST_LOGIN_AFTER_GRANT
}

case class SuppressIvForAgents(scopes: Set[String]) extends OverrideFlagWithScopes {
  val overrideType = OverrideType.SUPPRESS_IV_FOR_AGENTS
}

case class SuppressIvForOrganisations(scopes: Set[String]) extends OverrideFlagWithScopes {
  val overrideType = OverrideType.SUPPRESS_IV_FOR_ORGANISATIONS
}

case class SuppressIvForIndividuals(scopes: Set[String]) extends OverrideFlagWithScopes {
  val overrideType = OverrideType.SUPPRESS_IV_FOR_INDIVIDUALS
}

case class GrantWithoutConsent(scopes: Set[String]) extends OverrideFlagWithScopes {
  val overrideType = OverrideType.GRANT_WITHOUT_TAXPAYER_CONSENT
}

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
    createdOn: DateTime,
    lastAccess: Option[DateTime],
    access: Access,
    state: ApplicationState,
    grantLength: Period,
    rateLimitTier: RateLimitTier = RateLimitTier.BRONZE,
    termsAndConditionsUrl: Option[String] = None,
    privacyPolicyUrl: Option[String] = None,
    checkInformation: Option[CheckInformation] = None,
    blocked: Boolean = false,
    ipAllowlist: IpAllowlist = IpAllowlist()
  ) extends Application

object ApplicationResponse {
  import play.api.libs.json.JodaReads._
  import play.api.libs.json.JodaWrites._

  implicit val formatTotpIds = Json.format[TotpIds]

  private implicit val formatStandard   = Json.format[Standard]
  private implicit val formatPrivileged = Json.format[Privileged]
  private implicit val formatRopc       = Json.format[Ropc]

  implicit val formatAccess        = Union.from[Access]("accessType")
    .and[Standard](AccessType.STANDARD.toString)
    .and[Privileged](AccessType.PRIVILEGED.toString)
    .and[Ropc](AccessType.ROPC.toString)
    .format
  implicit val formatRole          = Json.formatEnum(CollaboratorRole)
  implicit val format2             = Json.format[Collaborator]
  implicit val format3             = Json.formatEnum(State)
  implicit val format4             = Json.format[ApplicationState]
  implicit val formatRateLimitTier = Json.formatEnum(RateLimitTier)
  implicit val format5             = Json.format[ApprovedApplication]

  val applicationResponseReads: Reads[ApplicationResponse] = (
    (JsPath \ "id").read[ApplicationId] and
      (JsPath \ "clientId").read[ClientId] and
      (JsPath \ "gatewayId").read[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "deployedTo").read[String] and
      (JsPath \ "description").readNullable[String] and
      (JsPath \ "collaborators").read[Set[Collaborator]] and
      (JsPath \ "createdOn").read[DateTime] and
      (JsPath \ "lastAccess").readNullable[DateTime] and
      (JsPath \ "access").read[Access] and
      (JsPath \ "state").read[ApplicationState] and
      (JsPath \ "grantLength").read[Period] and
      (JsPath \ "rateLimitTier").read[RateLimitTier] and
      (JsPath \ "termsAndConditionsUrl").readNullable[String] and
      (JsPath \ "privacyAndPolicyUrl").readNullable[String] and
      (JsPath \ "checkInformation").readNullable[CheckInformation] and
      ((JsPath \ "blocked").read[Boolean] or Reads.pure(false)) and
      (JsPath \ "ipAllowlist").read[IpAllowlist]
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

object AccessType extends Enumeration {
  type AccessType = Value
  val STANDARD, PRIVILEGED, ROPC = Value

  val displayedType: AccessType => String = {
    case STANDARD   => "Standard"
    case PRIVILEGED => "Privileged"
    case ROPC       => "ROPC"
  }

  def from(accessType: String) = {
    AccessType.values.find(e => e.toString == accessType.toUpperCase)
  }
}

case class TotpIds(production: String)

case class TotpSecrets(production: String)

case class SubscriptionNameAndVersion(name: String, version: String)

object State extends Enumeration {
  type State = Value
  val TESTING, PENDING_RESPONSIBLE_INDIVIDUAL_VERIFICATION, PENDING_GATEKEEPER_APPROVAL, PENDING_REQUESTER_VERIFICATION, PRE_PRODUCTION, PRODUCTION, DELETED = Value
  implicit val format                                                                                                                                        = Json.formatEnum(State)

  val displayedState: State => String = {
    case TESTING                                     => "Created"
    case PENDING_RESPONSIBLE_INDIVIDUAL_VERIFICATION => "Pending Responsible Individual Verification"
    case PENDING_GATEKEEPER_APPROVAL                 => "Pending gatekeeper check"
    case PENDING_REQUESTER_VERIFICATION              => "Pending submitter verification"
    case PRE_PRODUCTION                              => "Active"
    case PRODUCTION                                  => "Active"
    case DELETED                                     => "Deleted"
  }

  val additionalInformation: State => String = {
    case TESTING                                     =>
      "A production application that its admin has created but not submitted for checking"
    case PENDING_RESPONSIBLE_INDIVIDUAL_VERIFICATION =>
      "A production application that has been submitted for checking, but the responsible individual has not completed the email verification process"
    case PENDING_GATEKEEPER_APPROVAL                 =>
      "A production application that one of its admins has submitted for checking"
    case PENDING_REQUESTER_VERIFICATION              =>
      "A production application that has passed checking in Gatekeeper but the submitter has not completed the email verification process"
    case PRE_PRODUCTION                              =>
      "A production application that has passed checking, been verified, and is waiting for the user to confirm that they have carried out some initial setup"
    case PRODUCTION                                  =>
      "A production application that has passed checking, been verified and set up, and is therefore fully active - or any sandbox application"
    case DELETED                                     =>
      "An application that has been deleted and is no longer active"
  }

  implicit class StateHelpers(state: State) {
    def isApproved                     = state == State.PRE_PRODUCTION || state == State.PRODUCTION
    def isPendingGatekeeperApproval    = state == State.PENDING_GATEKEEPER_APPROVAL
    def isPendingRequesterVerification = state == State.PENDING_REQUESTER_VERIFICATION
    def isDeleted                      = state == State.DELETED
  }
}

object Environment extends Enumeration {
  type Environment = Value
  val SANDBOX, PRODUCTION = Value
  implicit val format     = Json.formatEnum(Environment)

  implicit class Display(e: Environment) {

    def asDisplayed() = e match {
      case SANDBOX    => "Sandbox"
      case PRODUCTION => "Production"
    }
  }
}

object CollaboratorRole extends Enumeration {
  type CollaboratorRole = Value
  val DEVELOPER, ADMINISTRATOR = Value

  def displayedRole: CollaboratorRole => String = _.toString.toLowerCase.capitalize

  def from(role: Option[String]) = role match {
    case Some(r) => CollaboratorRole.values.find(e => e.toString == r.toUpperCase)
    case _       => Some(CollaboratorRole.DEVELOPER)
  }

  implicit val format = Json.formatEnum(CollaboratorRole)
}

case class Collaborator(emailAddress: String, role: CollaboratorRole, userId: UserId)

case class ApplicationState(name: State = State.TESTING, requestedByEmailAddress: Option[String] = None, verificationCode: Option[String] = None, updatedOn: DateTime = DateTime.now()) {
  def isApproved                     = name.isApproved
  def isPendingGatekeeperApproval    = name.isPendingGatekeeperApproval
  def isPendingRequesterVerification = name.isPendingRequesterVerification
  def isDeleted                      = name.isDeleted
}

object RateLimitTier extends Enumeration {
  type RateLimitTier = Value

  val BRONZE, SILVER, GOLD, PLATINUM, RHODIUM = Value

  def from(tier: String) = RateLimitTier.values.find(e => e.toString == tier.toUpperCase)

  def displayedTier: RateLimitTier => String = {
    case BRONZE   => "Bronze"
    case SILVER   => "Silver"
    case GOLD     => "Gold"
    case PLATINUM => "Platinum"
    case RHODIUM  => "Rhodium"
  }

  lazy val asOrderedList: List[RateLimitTier] = RateLimitTier.values.toList.sorted

  implicit val format = Json.formatEnum(RateLimitTier)
}

object GrantLength extends Enumeration {
  type GrantLength = Value
  val thirtyDays                               = 30
  val ninetyDays                               = 90
  val oneHundredAndEightyDays                  = 180
  val threeSixtyFiveDays                       = 365
  val fiveHundredAndFortySevenDays             = 547
  val oneThousandNinetyFiveDays                = 1095
  val oneThousandEightHundredAndTwentyFiveDays = 1825
  val threeThousandSixHundredAndFiftyDays      = 3650
  val thirtySixThousandFiveHundredDays         = 36500

  val ONE_MONTH         = Value(thirtyDays)
  val THREE_MONTHS      = Value(ninetyDays)
  val SIX_MONTHS        = Value(oneHundredAndEightyDays)
  val ONE_YEAR          = Value(threeSixtyFiveDays)
  val EIGHTEEN_MONTHS   = Value(fiveHundredAndFortySevenDays)
  val THREE_YEARS       = Value(oneThousandNinetyFiveDays)
  val FIVE_YEARS        = Value(oneThousandEightHundredAndTwentyFiveDays)
  val TEN_YEARS         = Value(threeThousandSixHundredAndFiftyDays)
  val ONE_HUNDRED_YEARS = Value(thirtySixThousandFiveHundredDays)

  def from(grantLength: Int) = {
    lazy val errorMsg: String = "It should only be one of ('1 month', '3 months', '6 months', '1 year', '18 months', " +
      "'3 years', '5 years', '10 years', '100 years') represented in days"
    GrantLength.values.find(e => e.id == grantLength).getOrElse(throw new IllegalStateException(s"$grantLength is not an expected value. $errorMsg"))
  }

  def displayedGrantLength(grantLength: Int): String = {
    GrantLength.from(grantLength) match {
      case ONE_MONTH         => "1 month"
      case THREE_MONTHS      => "3 months"
      case SIX_MONTHS        => "6 months"
      case ONE_YEAR          => "1 year"
      case EIGHTEEN_MONTHS   => "18 months"
      case THREE_YEARS       => "3 years"
      case FIVE_YEARS        => "5 years"
      case TEN_YEARS         => "10 years"
      case ONE_HUNDRED_YEARS => "100 years"
    }
  }

  lazy val asOrderedIdList: List[Int] = GrantLength.values.map(value => value.id).toList.sorted

  implicit val format = Json.formatEnum(GrantLength)
}
