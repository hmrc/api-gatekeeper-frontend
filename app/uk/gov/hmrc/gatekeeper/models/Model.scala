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

import java.time.{Instant, LocalDateTime}
import scala.collection.immutable.ListSet

import play.api.data.Form
import play.api.libs.json._
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.http.SessionKeys

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiDefinition
import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access.Privileged
import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.{AccessType, OverrideType, _}
import uk.gov.hmrc.apiplatform.modules.applications.common.domain.models.FullName
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.subscriptionfields.domain.models._
import uk.gov.hmrc.gatekeeper.models.EmailOptionChoice.EmailOptionChoice
import uk.gov.hmrc.gatekeeper.models.EmailPreferencesChoice.EmailPreferencesChoice
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields.SubscriptionFieldDefinition

case class BearerToken(authToken: String, expiry: LocalDateTime) {
  override val toString = authToken
}

object BearerToken {
  implicit val format: OFormat[BearerToken] = Json.format[BearerToken]
}

object GatekeeperSessionKeys {
  val LoggedInUser = "LoggedInUser"
  val AuthToken    = SessionKeys.authToken
}

case class ApplicationAndSubscribedFieldDefinitionsWithHistory(application: ApplicationWithHistory, subscriptionsWithFieldDefinitions: List[Subscription])

case class ApplicationAndSubscriptionVersion(application: ApplicationWithHistory, subscription: Subscription, version: VersionSubscription)

case class ApplicationAndSubscriptionsWithHistory(application: ApplicationWithHistory, subscriptions: List[Subscription])

case class ApplicationWithHistory(application: ApplicationWithCollaborators, history: List[StateHistory])

case class ApplicationWithSubscriptionDataAndStateHistory(applicationWithSubscriptionData: ApplicationWithSubscriptionFields, stateHistory: List[StateHistory])

object ApiDefinitionFields {
  type Alias = Map[ApiContext, Map[ApiVersionNbr, Map[FieldName, SubscriptionFieldDefinition]]]
}

case class ApplicationWithSubscriptionDataAndFieldDefinitions(
    applicationWithSubscriptionData: ApplicationWithSubscriptionFields,
    apiDefinitionFields: ApiDefinitionFields.Alias,
    allPossibleSubs: List[ApiDefinition]
  )

object ApplicationWithHistory {
  implicit val format4: OFormat[ApplicationState]         = Json.format[ApplicationState]
  implicit val format5: OFormat[SubmissionDetails]        = Json.format[SubmissionDetails]
  implicit val format6: OFormat[ApplicationReviewDetails] = Json.format[ApplicationReviewDetails]
  implicit val format7: OFormat[ApprovedApplication]      = Json.format[ApprovedApplication]
  implicit val format9: OFormat[ApplicationWithHistory]   = Json.format[ApplicationWithHistory]
}

case class ApplicationWithUpliftRequest(id: ApplicationId, name: String, submittedOn: LocalDateTime, state: State)

object ApplicationWithUpliftRequest {

  implicit val format: OFormat[ApplicationWithUpliftRequest] = Json.format[ApplicationWithUpliftRequest]

  val compareBySubmittedOn = (a: ApplicationWithUpliftRequest, b: ApplicationWithUpliftRequest) => a.submittedOn.isBefore(b.submittedOn)
}

object PreconditionFailedException extends Throwable

class FetchApplicationsFailed(cause: Throwable) extends Throwable(cause)

class InconsistentDataState(message: String) extends RuntimeException(message)

case class ApproveUpliftRequest(gatekeeperUserId: String)

object ApproveUpliftRequest {
  implicit val format: OFormat[ApproveUpliftRequest] = Json.format[ApproveUpliftRequest]
}

sealed trait ApproveUpliftSuccessful

case object ApproveUpliftSuccessful extends ApproveUpliftSuccessful

case class RejectUpliftRequest(gatekeeperUserId: String, reason: String)

object RejectUpliftRequest {
  implicit val format: OFormat[RejectUpliftRequest] = Json.format[RejectUpliftRequest]
}

sealed trait RejectUpliftSuccessful

case object RejectUpliftSuccessful extends RejectUpliftSuccessful

// TODO - Remove Enumeration
object UpliftAction extends Enumeration {
  type UpliftAction = Value
  val APPROVE, REJECT = Value

  def from(action: String): Option[Value] = UpliftAction.values.find(e => e.toString == action.toUpperCase)

  implicit val format: Format[UpliftAction] = Json.formatEnum(UpliftAction)
}

case class SubmissionDetails(submitterName: String, submitterEmail: String, submittedOn: Instant)

case class ApprovalDetails(submittedOn: LocalDateTime, approvedBy: String, approvedOn: LocalDateTime)

object SubmissionDetails {
  implicit val format: OFormat[SubmissionDetails] = Json.format[SubmissionDetails]
}

case class ApplicationReviewDetails(
    id: ApplicationId,
    name: String,
    description: String,
    rateLimitTier: Option[RateLimitTier],
    submission: SubmissionDetails,
    reviewContactName: Option[FullName],
    reviewContactEmail: Option[LaxEmailAddress],
    reviewContactTelephone: Option[String],
    applicationDetails: Option[String],
    termsAndConditionsUrl: Option[String] = None,
    privacyPolicyUrl: Option[String] = None
  )

object ApplicationReviewDetails {
  import SubmissionDetails.format
  implicit val format2: OFormat[ApplicationReviewDetails] = Json.format[ApplicationReviewDetails]
}

case class ApprovedApplication(details: ApplicationReviewDetails, admins: List[RegisteredUser], approvedBy: String, approvedOn: Instant, verified: Boolean)

object ApprovedApplication {
  implicit val format1: OFormat[ApplicationReviewDetails] = Json.format[ApplicationReviewDetails]
  implicit val format2: OFormat[ApprovedApplication]      = Json.format[ApprovedApplication]
}
case class CategorisedApplications(pendingApproval: List[ApplicationWithUpliftRequest], approved: List[ApplicationWithUpliftRequest])

case class OverrideRequest(overrideType: OverrideType, scopes: Set[String] = Set.empty)

object OverrideRequest {
  implicit val format: OFormat[OverrideRequest] = Json.format[OverrideRequest]
}

case class UpdateOverridesRequest(overrides: Set[OverrideFlag])

object UpdateOverridesRequest {
  implicit val format: OFormat[UpdateOverridesRequest] = Json.format[UpdateOverridesRequest]
}

sealed trait UpdateOverridesResult

case object UpdateOverridesSuccessResult                                                   extends UpdateOverridesResult
case class UpdateOverridesFailureResult(overrideFlagErrors: Set[OverrideFlag] = Set.empty) extends UpdateOverridesResult

sealed trait UpdateScopesResult
case object UpdateScopesSuccessResult       extends UpdateScopesResult
case object UpdateScopesInvalidScopesResult extends UpdateScopesResult

case class ValidateApplicationNameRequest(applicationName: String, selfApplicationId: Option[ApplicationId])

object ValidateApplicationNameRequest {
  implicit val format: OFormat[ValidateApplicationNameRequest] = Json.format[ValidateApplicationNameRequest]
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

sealed trait EmailPreferencesDeleteResult
case object EmailPreferencesDeleteSuccessResult extends EmailPreferencesDeleteResult
case object EmailPreferencesDeleteFailureResult extends EmailPreferencesDeleteResult

sealed trait CreatePrivAppResult

case class AppAccess(accessType: AccessType, scopes: List[String])

case class CreatePrivAppSuccessResult(id: ApplicationId, name: ApplicationName, deployedTo: Environment, clientId: ClientId, totp: Option[TotpSecrets], access: AppAccess)
    extends CreatePrivAppResult

object CreatePrivAppSuccessResult {
  implicit val rds1: Reads[TotpSecrets] = Json.reads[TotpSecrets]
  implicit val rds4: Reads[AppAccess]   = Json.reads[AppAccess]

  private def asAppAccess(access: Access): AppAccess = access match {
    case Privileged(_, scopes) => AppAccess(access.accessType, scopes.toList)
    case _                     => throw new IllegalStateException("Should only be here with a Priviledged app")
  }

  private def unpack: (CoreApplication, Option[TotpSecrets]) => CreatePrivAppSuccessResult = (app, totp) => {
    CreatePrivAppSuccessResult(app.id, app.name, app.deployedTo, app.clientId, totp, asAppAccess(app.access))
  }

  import play.api.libs.functional.syntax._

  private val newLayoutReads: Reads[CreatePrivAppSuccessResult] = (
    (JsPath \ "details").read[CoreApplication] and
      (JsPath \ "totp").readNullable[String].map(_.map(TotpSecrets(_)))
  )(unpack)

  implicit val reads: Reads[CreatePrivAppSuccessResult] = newLayoutReads.orElse(Json.reads[CreatePrivAppSuccessResult])
}

case object CreatePrivAppFailureResult extends CreatePrivAppResult

case class ApiScope(key: String, name: String, description: String, confidenceLevel: Option[ConfidenceLevel] = None)

object ApiScope {
  implicit val formats: OFormat[ApiScope] = Json.format[ApiScope]
}

final case class DeleteApplicationForm(applicationNameConfirmation: String, collaboratorEmail: Option[String])

object DeleteApplicationForm {
  implicit val format: OFormat[DeleteApplicationForm] = Json.format[DeleteApplicationForm]
}

final case class DeleteApplicationRequest(gatekeeperUserId: String, requestedByEmailAddress: String)

object DeleteApplicationRequest {
  implicit val format: OFormat[DeleteApplicationRequest] = Json.format[DeleteApplicationRequest]
}

final case class BlockApplicationForm(applicationNameConfirmation: String)

object BlockApplicationForm {
  implicit val format: OFormat[BlockApplicationForm] = Json.format[BlockApplicationForm]
}

final case class UnblockApplicationForm(applicationNameConfirmation: String)

object UnblockApplicationForm {
  implicit val format: OFormat[UnblockApplicationForm] = Json.format[UnblockApplicationForm]
}

case class DeleteCollaboratorRequest(
    email: String,
    adminsToEmail: Set[String],
    notifyCollaborator: Boolean
  )

object DeleteCollaboratorRequest {
  implicit val writesDeleteCollaboratorRequest: OWrites[DeleteCollaboratorRequest] = Json.writes[DeleteCollaboratorRequest]
}

final case class DeleteDeveloperRequest(gatekeeperUserId: String, emailAddress: String)

object DeleteDeveloperRequest {
  implicit val format: OFormat[DeleteDeveloperRequest] = Json.format[DeleteDeveloperRequest]
}

final case class CreatePrivAppForm(
    environment: Environment = Environment.SANDBOX,
    applicationName: String = "",
    applicationDescription: String = "",
    adminEmail: String = ""
  )

object CreatePrivAppForm {

  def invalidAppName(form: Form[CreatePrivAppForm]) = {
    form.withError("applicationName", "application.name.already.exists")
  }

  def adminMustBeRegisteredUser(form: Form[CreatePrivAppForm]) = {
    form.withError("adminEmail", "admin.email.is.not.registered")
  }

  def adminMustBeVerifiedEmailAddress(form: Form[CreatePrivAppForm]) = {
    form.withError("adminEmail", "admin.email.is.not.verified")
  }

  def adminMustHaveMfaEnabled(form: Form[CreatePrivAppForm]) = {
    form.withError("adminEmail", "admin.email.is.not.mfa.enabled")
  }
}

sealed trait FieldsDeleteResult
case object FieldsDeleteSuccessResult extends FieldsDeleteResult
case object FieldsDeleteFailureResult extends FieldsDeleteResult

final case class AddTeamMemberRequest(email: String, role: Collaborator.Role, requestingEmail: Option[String])

object AddTeamMemberRequest {
  implicit val format3: OFormat[AddTeamMemberRequest] = Json.format[AddTeamMemberRequest]
}

final case class AddTeamMemberResponse(registeredUser: Boolean)

object AddTeamMemberResponse {
  implicit val format: OFormat[AddTeamMemberResponse] = Json.format[AddTeamMemberResponse]
}

case class ApiApprovalState(
    actor: Actor,
    changedAt: Instant,
    status: Option[ApprovalStatus] = None,
    notes: Option[String] = None
  )

object ApiApprovalState {
  implicit val stateFormat: Format[ApiApprovalState] = Json.format[ApiApprovalState]

}

case class APIApprovalSummary(
    serviceName: String,
    name: String,
    description: Option[String],
    environment: Option[Environment],
    status: ApprovalStatus = ApprovalStatus.NEW,
    createdOn: Option[Instant] = Some(Instant.now()),
    lastUpdated: Option[Instant] = None,
    stateHistory: Seq[ApiApprovalState] = Seq.empty
  ) {
  lazy val env = environment.get.toString.toLowerCase.capitalize
}

object APIApprovalSummary {
  implicit val format: OFormat[APIApprovalSummary] = Json.using[Json.WithDefaultValues].format[APIApprovalSummary]
}

sealed trait ApprovalStatus

object ApprovalStatus {
  case object NEW         extends ApprovalStatus
  case object APPROVED    extends ApprovalStatus
  case object FAILED      extends ApprovalStatus
  case object RESUBMITTED extends ApprovalStatus

  /* The order of the following declarations is important since it defines the ordering of the enumeration.
   * Be very careful when changing this, code may be relying on certain values being larger/smaller than others. */
  val values = ListSet(NEW, APPROVED, FAILED, RESUBMITTED)

  def apply(text: String): Option[ApprovalStatus] = ApprovalStatus.values.find(_.toString.toUpperCase == text.toUpperCase())

  def unsafeApply(text: String): ApprovalStatus = apply(text).getOrElse(throw new RuntimeException(s"$text is not a valid ApprovalStatus"))

  import play.api.libs.json.Format
  import uk.gov.hmrc.apiplatform.modules.common.domain.services.SealedTraitJsonFormatting
  implicit val format: Format[ApprovalStatus] = SealedTraitJsonFormatting.createFormatFor[ApprovalStatus]("ApprovalStatus", apply)
}

case class ApiApprovalRequest(serviceName: String, actor: Actors.GatekeeperUser, notes: Option[String] = None)

object ApiApprovalRequest {
  implicit val format: OFormat[ApiApprovalRequest] = Json.format[ApiApprovalRequest]
}

class UpdateApiDefinitionsFailed extends Throwable

case class SendEmailChoice(sendEmailChoice: EmailOptionChoice)
case class SendEmailPreferencesChoice(sendEmailPreferences: EmailPreferencesChoice)

case class RemoveAllCollaboratorsForUserIdRequest(userId: UserId, gatekeeperUserId: String)

object RemoveAllCollaboratorsForUserIdRequest {
  implicit val formatRemoveAllCollaboratorsForUserIdRequest: OFormat[RemoveAllCollaboratorsForUserIdRequest] = Json.format[RemoveAllCollaboratorsForUserIdRequest]
}

sealed trait RemoveAllCollaboratorsForUserIdResult
case object RemoveAllCollaboratorsForUserIdSuccessResult extends RemoveAllCollaboratorsForUserIdResult
case object RemoveAllCollaboratorsForUserIdFailureResult extends RemoveAllCollaboratorsForUserIdResult
