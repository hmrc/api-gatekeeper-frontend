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

import java.time.LocalDateTime
import java.util.UUID

import play.api.data.Form
import play.api.libs.json._
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.json.Union

import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{Collaborator, RateLimitTier}
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.gatekeeper.models.EmailOptionChoice.EmailOptionChoice
import uk.gov.hmrc.gatekeeper.models.EmailPreferencesChoice.EmailPreferencesChoice
import uk.gov.hmrc.gatekeeper.models.OverrideType.OverrideType
import uk.gov.hmrc.gatekeeper.models.State.State
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields.SubscriptionFieldDefinition
import uk.gov.hmrc.gatekeeper.models.applications.ApplicationWithSubscriptionData
import uk.gov.hmrc.gatekeeper.models.subscriptions.ApiData

case class BearerToken(authToken: String, expiry: LocalDateTime) {
  override val toString = authToken
}

object BearerToken {
  implicit val format = Json.format[BearerToken]
}

object GatekeeperSessionKeys {
  val LoggedInUser = "LoggedInUser"
  val AuthToken    = SessionKeys.authToken
}

case class ApplicationAndSubscribedFieldDefinitionsWithHistory(application: ApplicationWithHistory, subscriptionsWithFieldDefinitions: List[Subscription])

case class ApplicationAndSubscriptionVersion(application: ApplicationWithHistory, subscription: Subscription, version: VersionSubscription)

case class ApplicationAndSubscriptionsWithHistory(application: ApplicationWithHistory, subscriptions: List[Subscription])

case class ApplicationWithHistory(application: ApplicationResponse, history: List[StateHistory])

case class ApplicationWithSubscriptionDataAndStateHistory(applicationWithSubscriptionData: ApplicationWithSubscriptionData, stateHistory: List[StateHistory])

object ApiDefinitions {
  type Alias = Map[ApiContext, Map[ApiVersionNbr, Map[FieldName, SubscriptionFieldDefinition]]]
}

case class ApplicationWithSubscriptionDataAndFieldDefinitions(
    applicationWithSubscriptionData: ApplicationWithSubscriptionData,
    apiDefinitions: ApiDefinitions.Alias,
    allPossibleSubs: Map[ApiContext, ApiData]
  )

object ApplicationWithHistory {
  implicit val formatTotpIds = Json.format[TotpIds]

  implicit private val formatStandard   = Json.format[Standard]
  implicit private val formatPrivileged = Json.format[Privileged]
  implicit private val formatRopc       = Json.format[Ropc]

  implicit val formatAccess = Union.from[Access]("accessType")
    .and[Standard](AccessType.STANDARD.toString)
    .and[Privileged](AccessType.PRIVILEGED.toString)
    .and[Ropc](AccessType.ROPC.toString)
    .format
  implicit val formatRole   = Json.formatEnum(CollaboratorRole)
  implicit val format3      = Json.format[ApplicationState]
  implicit val format4      = Json.formatEnum(State)
  implicit val format5      = Json.format[SubmissionDetails]
  implicit val format6      = Json.format[ApplicationReviewDetails]
  implicit val format7      = Json.format[ApprovedApplication]
  implicit val format8      = Json.format[ApplicationResponse]
  implicit val format9      = Json.format[ApplicationWithHistory]
}

case class ApplicationWithUpliftRequest(id: UUID, name: String, submittedOn: LocalDateTime, state: State)

object ApplicationWithUpliftRequest {

  implicit val formatState = Json.formatEnum(State)
  implicit val format      = Json.format[ApplicationWithUpliftRequest]

  val compareBySubmittedOn = (a: ApplicationWithUpliftRequest, b: ApplicationWithUpliftRequest) => a.submittedOn.isBefore(b.submittedOn)
}

object PreconditionFailedException extends Throwable

class FetchApplicationsFailed(cause: Throwable) extends Throwable(cause)

object FetchApplicationSubscriptionsFailed extends Throwable

class InconsistentDataState(message: String) extends RuntimeException(message)

object TeamMemberAlreadyExists extends Throwable

object TeamMemberLastAdmin extends Throwable

object ApplicationNotFound extends Throwable

case class ApproveUpliftRequest(gatekeeperUserId: String)

object ApproveUpliftRequest {
  implicit val format = Json.format[ApproveUpliftRequest]
}

sealed trait ApproveUpliftSuccessful

case object ApproveUpliftSuccessful extends ApproveUpliftSuccessful

case class RejectUpliftRequest(gatekeeperUserId: String, reason: String)

object RejectUpliftRequest {
  implicit val format = Json.format[RejectUpliftRequest]
}

sealed trait RejectUpliftSuccessful

case object RejectUpliftSuccessful extends RejectUpliftSuccessful

case class ResendVerificationRequest(gatekeeperUserId: String)

object ResendVerificationRequest {
  implicit val format = Json.format[ResendVerificationRequest]
}

sealed trait ResendVerificationSuccessful

case object ResendVerificationSuccessful extends ResendVerificationSuccessful

// TODO - Remove Enumeration
object UpliftAction extends Enumeration {
  type UpliftAction = Value
  val APPROVE, REJECT = Value

  def from(action: String): Option[Value] = UpliftAction.values.find(e => e.toString == action.toUpperCase)

  implicit val format = Json.formatEnum(UpliftAction)
}

case class SubmissionDetails(submitterName: String, submitterEmail: String, submittedOn: LocalDateTime)

case class ApprovalDetails(submittedOn: LocalDateTime, approvedBy: String, approvedOn: LocalDateTime)

object SubmissionDetails {
  implicit val format = Json.format[SubmissionDetails]
}

case class ApplicationReviewDetails(
    id: ApplicationId,
    name: String,
    description: String,
    rateLimitTier: Option[RateLimitTier],
    submission: SubmissionDetails,
    reviewContactName: Option[String],
    reviewContactEmail: Option[String],
    reviewContactTelephone: Option[String],
    applicationDetails: Option[String],
    termsAndConditionsUrl: Option[String] = None,
    privacyPolicyUrl: Option[String] = None
  )

object ApplicationReviewDetails {
  import SubmissionDetails.format
  implicit val format2 = Json.format[ApplicationReviewDetails]
}

case class ApprovedApplication(details: ApplicationReviewDetails, admins: List[RegisteredUser], approvedBy: String, approvedOn: LocalDateTime, verified: Boolean)

object ApprovedApplication {
  implicit val format1 = Json.format[ApplicationReviewDetails]
  implicit val format2 = Json.format[ApprovedApplication]
}
case class CategorisedApplications(pendingApproval: List[ApplicationWithUpliftRequest], approved: List[ApplicationWithUpliftRequest])

case class OverrideRequest(overrideType: OverrideType, scopes: Set[String] = Set.empty)

object OverrideRequest {
  implicit val format = Json.format[OverrideRequest]
}

case class UpdateOverridesRequest(overrides: Set[OverrideFlag])

object UpdateOverridesRequest {
  implicit val format = Json.format[UpdateOverridesRequest]
}

sealed trait UpdateOverridesResult

case object UpdateOverridesSuccessResult                                                   extends UpdateOverridesResult
case class UpdateOverridesFailureResult(overrideFlagErrors: Set[OverrideFlag] = Set.empty) extends UpdateOverridesResult

case class UpdateScopesRequest(scopes: Set[String])

object UpdateScopesRequest {
  implicit val format = Json.format[UpdateScopesRequest]
}

sealed trait UpdateScopesResult
case object UpdateScopesSuccessResult       extends UpdateScopesResult
case object UpdateScopesInvalidScopesResult extends UpdateScopesResult

case class UpdateIpAllowlistRequest(required: Boolean, allowlist: Set[String])

object UpdateIpAllowlistRequest {
  implicit val format = Json.format[UpdateIpAllowlistRequest]
}

sealed trait UpdateIpAllowlistResult
case object UpdateIpAllowlistSuccessResult extends UpdateIpAllowlistResult

case class ValidateApplicationNameRequest(applicationName: String, selfApplicationId: ApplicationId)

object ValidateApplicationNameRequest {
  implicit val format = Json.format[ValidateApplicationNameRequest]
}

trait ApplicationUpdate {
  def timestamp: LocalDateTime
}

trait GatekeeperApplicationUpdate extends ApplicationUpdate {
  def gatekeeperUser: String
}

case class ChangeProductionApplicationName(instigator: UserId, timestamp: LocalDateTime, gatekeeperUser: String, newName: String)            extends ApplicationUpdate
case class DeleteApplicationByGatekeeper(gatekeeperUser: String, requestedByEmailAddress: String, reasons: String, timestamp: LocalDateTime) extends ApplicationUpdate

trait ApplicationUpdateFormatters {

  implicit val changeNameFormatter = Json.writes[ChangeProductionApplicationName]
    .transform((obj: JsObject) => obj + ("updateType" -> JsString("changeProductionApplicationName")))

  implicit val deleteApplicationByGatekeeperFormatter = Json.writes[DeleteApplicationByGatekeeper]
    .transform((obj: JsObject) => obj + ("updateType" -> JsString("deleteApplicationByGatekeeper")))
}

sealed trait UpdateApplicationNameResult
case object UpdateApplicationNameSuccessResult          extends UpdateApplicationNameResult
case object UpdateApplicationNameFailureInvalidResult   extends UpdateApplicationNameResult
case object UpdateApplicationNameFailureDuplicateResult extends UpdateApplicationNameResult

sealed trait ValidateApplicationNameResult
case object ValidateApplicationNameSuccessResult          extends ValidateApplicationNameResult
sealed trait ValidateApplicationNameFailureResult         extends ValidateApplicationNameResult
case object ValidateApplicationNameFailureInvalidResult   extends ValidateApplicationNameFailureResult
case object ValidateApplicationNameFailureDuplicateResult extends ValidateApplicationNameFailureResult

sealed trait ApplicationUpdateResult
case object ApplicationUpdateSuccessResult extends ApplicationUpdateResult
case object ApplicationUpdateFailureResult extends ApplicationUpdateResult

sealed trait ApplicationDeleteResult
case object ApplicationDeleteSuccessResult extends ApplicationDeleteResult
case object ApplicationDeleteFailureResult extends ApplicationDeleteResult

sealed trait ApplicationBlockResult
case object ApplicationBlockSuccessResult extends ApplicationBlockResult
case object ApplicationBlockFailureResult extends ApplicationBlockResult

sealed trait ApplicationUnblockResult
case object ApplicationUnblockSuccessResult extends ApplicationUnblockResult
case object ApplicationUnblockFailureResult extends ApplicationUnblockResult

sealed trait DeveloperDeleteResult
case object DeveloperDeleteSuccessResult extends DeveloperDeleteResult
case object DeveloperDeleteFailureResult extends DeveloperDeleteResult

sealed trait CreatePrivOrROPCAppResult

case class CreatePrivOrROPCAppSuccessResult(id: ApplicationId, name: String, deployedTo: String, clientId: ClientId, totp: Option[TotpSecrets], access: AppAccess)
    extends CreatePrivOrROPCAppResult

object CreatePrivOrROPCAppSuccessResult {
  implicit val rds1 = Json.reads[TotpSecrets]
  implicit val rds2 = Json.formatEnum(AccessType)
  implicit val rds4 = Json.reads[AppAccess]
  implicit val rds5 = Json.reads[CreatePrivOrROPCAppSuccessResult]

  implicit val format1 = Json.format[TotpSecrets]
  implicit val format2 = Json.format[AppAccess]
  implicit val format3 = Json.format[CreatePrivOrROPCAppSuccessResult]
}
case object CreatePrivOrROPCAppFailureResult extends CreatePrivOrROPCAppResult

case class ApiScope(key: String, name: String, description: String, confidenceLevel: Option[ConfidenceLevel] = None)

object ApiScope {
  implicit val formats = Json.format[ApiScope]
}

final case class DeleteApplicationForm(applicationNameConfirmation: String, collaboratorEmail: Option[String])

object DeleteApplicationForm {
  implicit val format = Json.format[DeleteApplicationForm]
}

final case class DeleteApplicationRequest(gatekeeperUserId: String, requestedByEmailAddress: String)

object DeleteApplicationRequest {
  implicit val format = Json.format[DeleteApplicationRequest]
}

final case class BlockApplicationForm(applicationNameConfirmation: String)

object BlockApplicationForm {
  implicit val format = Json.format[BlockApplicationForm]
}

final case class UnblockApplicationForm(applicationNameConfirmation: String)

object UnblockApplicationForm {
  implicit val format = Json.format[UnblockApplicationForm]
}

final case class BlockApplicationRequest(gatekeeperUserId: String)

object BlockApplicationRequest {
  implicit val format = Json.format[BlockApplicationRequest]
}

final case class UnblockApplicationRequest(gatekeeperUserId: String)

object UnblockApplicationRequest {
  implicit val format = Json.format[UnblockApplicationRequest]
}

case class DeleteCollaboratorRequest(
    email: String,
    adminsToEmail: Set[String],
    notifyCollaborator: Boolean
  )

object DeleteCollaboratorRequest {
  implicit val writesDeleteCollaboratorRequest = Json.writes[DeleteCollaboratorRequest]
}

final case class DeleteDeveloperRequest(gatekeeperUserId: String, emailAddress: String)

object DeleteDeveloperRequest {
  implicit val format = Json.format[DeleteDeveloperRequest]
}

final case class CreatePrivOrROPCAppForm(
    environment: Environment = Environment.SANDBOX,
    accessType: Option[String] = None,
    applicationName: String = "",
    applicationDescription: String = "",
    adminEmail: String = ""
  )

object CreatePrivOrROPCAppForm {

  def invalidAppName(form: Form[CreatePrivOrROPCAppForm]) = {
    form.withError("applicationName", "application.name.already.exists")
  }

  def adminMustBeRegisteredUser(form: Form[CreatePrivOrROPCAppForm]) = {
    form.withError("adminEmail", "admin.email.is.not.registered")
  }

  def adminMustBeVerifiedEmailAddress(form: Form[CreatePrivOrROPCAppForm]) = {
    form.withError("adminEmail", "admin.email.is.not.verified")
  }

  def adminMustHaveMfaEnabled(form: Form[CreatePrivOrROPCAppForm]) = {
    form.withError("adminEmail", "admin.email.is.not.mfa.enabled")
  }
}

sealed trait FieldsDeleteResult
case object FieldsDeleteSuccessResult extends FieldsDeleteResult
case object FieldsDeleteFailureResult extends FieldsDeleteResult

final case class CreatePrivOrROPCAppRequest(environment: String, name: String, description: String, collaborators: List[Collaborator], access: AppAccess)

object CreatePrivOrROPCAppRequest {
  implicit val format1 = Json.formatEnum(AccessType)
  implicit val format2 = Json.formatEnum(CollaboratorRole)
  implicit val format4 = Json.format[TotpSecrets]
  implicit val format6 = Json.format[AppAccess]
  implicit val format7 = Json.format[CreatePrivOrROPCAppRequest]
}

case class AppAccess(accessType: AccessType.AccessType, scopes: List[String])

final case class AddTeamMemberRequest(email: String, role: CollaboratorRole.CollaboratorRole, requestingEmail: Option[String])

object AddTeamMemberRequest {
  implicit val format1 = Json.formatEnum(CollaboratorRole)
  implicit val format3 = Json.format[AddTeamMemberRequest]
}

final case class AddTeamMemberResponse(registeredUser: Boolean)

object AddTeamMemberResponse {
  implicit val format = Json.format[AddTeamMemberResponse]
}

case class APIApprovalSummary(serviceName: String, name: String, description: Option[String], environment: Option[Environment]) {
  lazy val env = environment.get.toString.toLowerCase.capitalize
}

object APIApprovalSummary {
  implicit val format = Json.format[APIApprovalSummary]
}

case class ApproveServiceRequest(serviceName: String)

object ApproveServiceRequest {
  implicit val format = Json.format[ApproveServiceRequest]
}

class UpdateApiDefinitionsFailed extends Throwable

case class SendEmailChoice(sendEmailChoice: EmailOptionChoice)
case class SendEmailPreferencesChoice(sendEmailPreferences: EmailPreferencesChoice)
