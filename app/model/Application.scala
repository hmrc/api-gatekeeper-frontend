/*
 * Copyright 2018 HM Revenue & Customs
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

package model

import java.util.UUID

import model.CollaboratorRole.CollaboratorRole
import model.RateLimitTier.RateLimitTier
import model.State.State
import org.joda.time.DateTime
import play.api.libs.json._
import uk.gov.hmrc.play.json.Union
import uk.gov.hmrc.time.DateTimeUtils

trait Application {
  val id: UUID
  val name: String
  val state: ApplicationState
  val collaborators: Set[Collaborator]

  def admins = collaborators.filter(_.role == CollaboratorRole.ADMINISTRATOR)

  def isSoleAdmin(emailAddress: String) = admins.map(_.emailAddress).contains(emailAddress) && admins.size == 1
}

case class ContactDetails(fullname: String, email: String, telephoneNumber: String)

object ContactDetails {
  implicit val formatContactDetails = Json.format[ContactDetails]
}

case class TermsOfUseAgreement(emailAddress: String, timeStamp: DateTime, version: String)

case class CheckInformation(contactDetails: Option[ContactDetails] = None,
                            confirmedName: Boolean = false,
                            providedPrivacyPolicyURL: Boolean = false,
                            providedTermsAndConditionsURL: Boolean = false,
                            applicationDetails: Option[String] = None,
                            termsOfUseAgreements: Seq[TermsOfUseAgreement] = Seq.empty)

object CheckInformation {
  implicit val formatTermsOfUseAgreement = Json.format[TermsOfUseAgreement]
  implicit val formatApprovalInformation = Json.format[CheckInformation]
}

sealed trait Access {
  val accessType: AccessType.Value
}

sealed trait AccessWithRestrictedScopes extends Access {
  val scopes: Set[String]
}

case class Standard(redirectUris: Seq[String] = Seq.empty,
                    termsAndConditionsUrl: Option[String] = None,
                    privacyPolicyUrl: Option[String] = None,
                    overrides: Set[OverrideFlag] = Set.empty) extends Access {
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
  private implicit val formatGrantWithoutConsent = Json.format[GrantWithoutConsent]
  private implicit val formatPersistLogin = Format[PersistLogin](
    Reads { _ => JsSuccess(PersistLogin()) },
    Writes { _ => Json.obj() })
  private implicit val formatSuppressIvForAgents = Json.format[SuppressIvForAgents]
  private implicit val formatSuppressIvForOrganisations = Json.format[SuppressIvForOrganisations]

  implicit val formatOverride = Union.from[OverrideFlag]("overrideType")
    .and[GrantWithoutConsent](OverrideType.GRANT_WITHOUT_TAXPAYER_CONSENT.toString)
    .and[PersistLogin](OverrideType.PERSIST_LOGIN_AFTER_GRANT.toString)
    .and[SuppressIvForAgents](OverrideType.SUPPRESS_IV_FOR_AGENTS.toString)
    .and[SuppressIvForOrganisations](OverrideType.SUPPRESS_IV_FOR_ORGANISATIONS.toString)
    .format
}

sealed trait OverrideFlagWithScopes extends OverrideFlag {
  val scopes: Set[String]
}

case class PersistLogin() extends OverrideFlag {
  val overrideType = OverrideType.PERSIST_LOGIN_AFTER_GRANT
}

case class SuppressIvForAgents(scopes: Set[String]) extends OverrideFlagWithScopes {
  val overrideType = OverrideType.SUPPRESS_IV_FOR_AGENTS
}

case class SuppressIvForOrganisations(scopes: Set[String]) extends OverrideFlagWithScopes {
  val overrideType = OverrideType.SUPPRESS_IV_FOR_ORGANISATIONS
}

case class GrantWithoutConsent(scopes: Set[String]) extends OverrideFlagWithScopes {
  val overrideType = OverrideType.GRANT_WITHOUT_TAXPAYER_CONSENT
}

object OverrideType extends Enumeration {
  type OverrideType = Value
  val PERSIST_LOGIN_AFTER_GRANT, GRANT_WITHOUT_TAXPAYER_CONSENT, SUPPRESS_IV_FOR_AGENTS, SUPPRESS_IV_FOR_ORGANISATIONS = Value

  val displayedType: (OverrideType) => String = {
    case PERSIST_LOGIN_AFTER_GRANT => "Persist login after grant"
    case GRANT_WITHOUT_TAXPAYER_CONSENT => "Grant without taxpayer consent"
    case SUPPRESS_IV_FOR_AGENTS => "Suppress IV for agents"
    case SUPPRESS_IV_FOR_ORGANISATIONS => "Suppress IV for organisations"
  }

  implicit val format = EnumJson.enumFormat(OverrideType)
}

case class ApplicationResponse(id: UUID,
                               name: String,
                               deployedTo: String,
                               description: Option[String] = None,
                               collaborators: Set[Collaborator],
                               createdOn: DateTime,
                               access: Access,
                               state: ApplicationState,
                               rateLimitTier: RateLimitTier = RateLimitTier.BRONZE,
                               termsAndConditionsUrl: Option[String] = None,
                               privacyPolicyUrl: Option[String] = None,
                               checkInformation: Option[CheckInformation] = None)
                               extends Application {

}

object ApplicationResponse {
  implicit val formatTotpIds = Json.format[TotpIds]

  private implicit val formatStandard = Json.format[Standard]
  private implicit val formatPrivileged = Json.format[Privileged]
  private implicit val formatRopc = Json.format[Ropc]

  implicit val formatAccess = Union.from[Access]("accessType")
    .and[Standard](AccessType.STANDARD.toString)
    .and[Privileged](AccessType.PRIVILEGED.toString)
    .and[Ropc](AccessType.ROPC.toString)
    .format
  implicit val format1 = Json.format[APIIdentifier]
  implicit val formatRole = EnumJson.enumFormat(CollaboratorRole)
  implicit val format2 = Json.format[Collaborator]
  implicit val format3 = EnumJson.enumFormat(State)
  implicit val format4 = Json.format[ApplicationState]
  implicit val formatRateLimitTier = EnumJson.enumFormat(RateLimitTier)
  implicit val format5 = Json.format[ApprovedApplication]
  implicit val applicationResponseFormatter = Json.format[ApplicationResponse]
  implicit val format6 = Json.format[TermsOfUseAgreement]
}

object AccessType extends Enumeration {
  type AccessType = Value
  val STANDARD, PRIVILEGED, ROPC = Value

  val displayedType: (AccessType) => String = {
    case STANDARD => "Standard"
    case PRIVILEGED => "Privileged"
    case ROPC => "ROPC"
  }
}

case class TotpIds(production: String, sandbox: String)

case class SubscriptionNameAndVersion(name: String, version: String)

case class SubscribedApplicationResponse(id: UUID,
                                         name: String,
                                         description: Option[String] = None,
                                         collaborators: Set[Collaborator],
                                         createdOn: DateTime,
                                         state: ApplicationState,
                                         access: Access,
                                         subscriptions: Seq[SubscriptionNameAndVersion],
                                         termsOfUseAgreed: Boolean) extends Application


object SubscribedApplicationResponse {
  implicit val format1 = Json.format[APIIdentifier]
  implicit val formatRole = EnumJson.enumFormat(CollaboratorRole)
  implicit val format2 = Json.format[Collaborator]
  implicit val format3 = EnumJson.enumFormat(State)
  implicit val format4 = Json.format[ApplicationState]
  implicit val formatTotpIds = Json.format[TotpIds]
  private implicit val formatStandard = Json.format[Standard]
  private implicit val formatPrivileged = Json.format[Privileged]
  private implicit val formatRopc = Json.format[Ropc]
  implicit val formatAccess = Union.from[Access]("accessType")
    .and[Standard](AccessType.STANDARD.toString)
    .and[Privileged](AccessType.PRIVILEGED.toString)
    .and[Ropc](AccessType.ROPC.toString)
    .format
  implicit val format5 = Json.format[SubscriptionNameAndVersion]
  implicit val format6 = Json.format[SubscribedApplicationResponse]

  private def isTermsOfUseAccepted(checkInformation: CheckInformation): Boolean = {
    checkInformation.termsOfUseAgreements.exists(tou => tou.version == "1.0")
  }

  def createFrom(appResponse: ApplicationResponse, subscriptions: Seq[SubscriptionNameAndVersion]) =
    SubscribedApplicationResponse(appResponse.id, appResponse.name, appResponse.description,
      appResponse.collaborators, appResponse.createdOn, appResponse.state, appResponse.access, subscriptions, appResponse.checkInformation.exists(isTermsOfUseAccepted))
}

case class DetailedSubscribedApplicationResponse(id: UUID,
                                                 name: String,
                                                 description: Option[String] = None,
                                                 collaborators: Set[Collaborator],
                                                 createdOn: DateTime,
                                                 state: ApplicationState,
                                                 access: Access,
                                                 subscriptions: Seq[SubscriptionDetails],
                                                 termsOfUseAgreed: Boolean) extends Application

case class SubscriptionDetails(name: String, context: String, version: String)


object DetailedSubscribedApplicationResponse {
  implicit val subscriptionsFormat = Json.format[SubscriptionDetails]
  implicit val format1 = Json.format[APIIdentifier]
  implicit val formatRole = EnumJson.enumFormat(CollaboratorRole)
  implicit val format2 = Json.format[Collaborator]
  implicit val format3 = EnumJson.enumFormat(State)
  implicit val format4 = Json.format[ApplicationState]
  implicit val formatTotpIds = Json.format[TotpIds]
  private implicit val formatStandard = Json.format[Standard]
  private implicit val formatPrivileged = Json.format[Privileged]
  private implicit val formatRopc = Json.format[Ropc]
  implicit val formatAccess = Union.from[Access]("accessType")
    .and[Standard](AccessType.STANDARD.toString)
    .and[Privileged](AccessType.PRIVILEGED.toString)
    .and[Ropc](AccessType.ROPC.toString)
    .format
  implicit val format5 = Json.format[DetailedSubscribedApplicationResponse]
}


object State extends Enumeration {
  type State = Value
  val TESTING, PENDING_GATEKEEPER_APPROVAL, PENDING_REQUESTER_VERIFICATION, PRODUCTION = Value
  implicit val format = EnumJson.enumFormat(State)

  val displayedState: (State) => String = {
    case TESTING => "Created"
    case PENDING_GATEKEEPER_APPROVAL => "Pending gatekeeper check"
    case PENDING_REQUESTER_VERIFICATION => "Pending submitter verification"
    case PRODUCTION => "Active"
  }

  val additionalInformation: (State) => String = {
    case TESTING => "A production application that its admin has created but not submitted for checking"
    case PENDING_GATEKEEPER_APPROVAL => "A production application that one of its admins has submitted for checking"
    case PENDING_REQUESTER_VERIFICATION => "A production application that has passed checking in Gatekeeper but the submitter has not completed the email verification process"
    case PRODUCTION => "A production application that has passed checking, been verified and is therefore fully active - or any sandbox application"
  }
}

object CollaboratorRole extends Enumeration {
  type CollaboratorRole = Value
  val DEVELOPER, ADMINISTRATOR = Value
}

case class Collaborator(emailAddress: String, role: CollaboratorRole)

case class ApplicationState(name: State = State.TESTING, requestedByEmailAddress: Option[String] = None,
                            verificationCode: Option[String] = None, updatedOn: DateTime = DateTimeUtils.now)

object RateLimitTier extends Enumeration {
  type RateLimitTier = Value

  val BRONZE, SILVER, GOLD, PLATINUM = Value

  def from(tier: String) = RateLimitTier.values.find(e => e.toString == tier.toUpperCase)

  def displayedTier: (RateLimitTier) => String = {
    case BRONZE => "Bronze"
    case SILVER => "Silver"
    case GOLD => "Gold"
    case PLATINUM => "Platinum"
  }

  implicit val format = EnumJson.enumFormat(RateLimitTier)
}
