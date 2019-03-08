/*
 * Copyright 2019 HM Revenue & Customs
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

import model.AccessType.AccessType
import model.OverrideType.OverrideType
import model.RateLimitTier._
import model.Environment._
import model.State.State
import org.joda.time.DateTime
import play.api.data.Form
import play.api.libs.json._
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.json.Union

case class Role(scope: String, name: String)

object Role {
  implicit val format = Json.format[Role]
  val APIGatekeeper = Role("api", "gatekeeper")
}

object GatekeeperRole extends Enumeration {
  type GatekeeperRole = Value
  val USER,SUPERUSER,ADMIN = Value
}


case class BearerToken(authToken: String, expiry: DateTime) {
  override val toString = authToken
}

object BearerToken {
  implicit val format = Json.format[BearerToken]
}

case class SuccessfulAuthentication(access_token: BearerToken, userName: String, roles: Option[Set[Role]])

object GatekeeperSessionKeys {
  val LoggedInUser = "LoggedInUser"
  val AuthToken = SessionKeys.authToken
}

case class ApplicationWithHistory(application: ApplicationResponse, history: Seq[StateHistory])

object ApplicationWithHistory {
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
  implicit val format3 = Json.format[ApplicationState]
  implicit val format4 = EnumJson.enumFormat(State)
  implicit val format5 = Json.format[SubmissionDetails]
  implicit val format6 = Json.format[ApplicationReviewDetails]
  implicit val format7 = Json.format[ApprovedApplication]
  implicit val format8 = Json.format[ApplicationResponse]
  implicit val format9 = Json.format[ApplicationWithHistory]
}

case class UpdateRateLimitTierRequest(rateLimitTier: RateLimitTier)

object UpdateRateLimitTierRequest {
  implicit val format = Json.format[UpdateRateLimitTierRequest]
}

case class ApplicationWithUpliftRequest(id: UUID, name: String, submittedOn: DateTime, state: State)


object ApplicationWithUpliftRequest {

  implicit val formatState = EnumJson.enumFormat(State)
  implicit val format = Json.format[ApplicationWithUpliftRequest]

  val compareBySubmittedOn = (a: ApplicationWithUpliftRequest, b: ApplicationWithUpliftRequest) => a.submittedOn.isBefore(b.submittedOn)
}

class PreconditionFailed extends Throwable

class FetchApplicationsFailed extends Throwable

class FetchApplicationSubscriptionsFailed extends Throwable

class InconsistentDataState(message: String) extends RuntimeException(message)

class TeamMemberAlreadyExists extends Throwable

class TeamMemberLastAdmin extends Throwable

class ApplicationNotFound extends Throwable

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

object UpliftAction extends Enumeration {
  type UpliftAction = Value
  val APPROVE, REJECT = Value

  def from(action: String) = UpliftAction.values.find(e => e.toString == action.toUpperCase)

  implicit val format = EnumJson.enumFormat(UpliftAction)
}

case class SubmissionDetails(submitterName: String, submitterEmail: String, submittedOn: DateTime)

case class ApprovalDetails(submittedOn: DateTime, approvedBy: String, approvedOn: DateTime)

object SubmissionDetails {
  implicit val format = Json.format[SubmissionDetails]
}

case class ApplicationReviewDetails(id: String,
                                    name: String,
                                    description: String,
                                    rateLimitTier: Option[RateLimitTier],
                                    submission: SubmissionDetails,
                                    reviewContactName: Option[String],
                                    reviewContactEmail: Option[String],
                                    reviewContactTelephone: Option[String],
                                    applicationDetails: Option[String],
                                    termsAndConditionsUrl: Option[String] = None,
                                    privacyPolicyUrl: Option[String] = None)

object ApplicationReviewDetails {
  implicit val format1 = Json.format[SubmissionDetails]
  implicit val format2 = Json.format[ApplicationReviewDetails]
}

case class ApprovedApplication(details: ApplicationReviewDetails, admins: Seq[User], approvedBy: String, approvedOn: DateTime, verified: Boolean)

object  ApprovedApplication {
  implicit val format1 = Json.format[ApplicationReviewDetails]
  implicit val format2 = Json.format[ApprovedApplication]
}
case class CategorisedApplications(pendingApproval: Seq[ApplicationWithUpliftRequest], approved: Seq[ApplicationWithUpliftRequest])

case class OverrideRequest(overrideType: OverrideType, scopes: Set[String] = Set.empty)

object OverrideRequest {
  implicit val format = Json.format[OverrideRequest]
}

case class UpdateOverridesRequest(overrides: Set[OverrideFlag])

object UpdateOverridesRequest {
  implicit val format = Json.format[UpdateOverridesRequest]
}

sealed trait UpdateOverridesResult

case object UpdateOverridesSuccessResult extends UpdateOverridesResult
case class UpdateOverridesFailureResult(overrideFlagErrors: Set[OverrideFlag] = Set.empty) extends UpdateOverridesResult

case class UpdateScopesRequest(scopes: Set[String])

object UpdateScopesRequest {
  implicit val format = Json.format[UpdateScopesRequest]
}

sealed trait UpdateScopesResult
case object UpdateScopesSuccessResult extends UpdateScopesResult
case object UpdateScopesFailureResult extends UpdateScopesResult
case object UpdateScopesInvalidScopesResult extends UpdateScopesResult

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

case class CreatePrivOrROPCAppSuccessResult(id: String, name: String, deployedTo: String, clientId: String, totp: Option[TotpSecrets], access: AppAccess) extends CreatePrivOrROPCAppResult
object CreatePrivOrROPCAppSuccessResult {
  implicit val rds1 = Json.reads[TotpSecrets]
  implicit val rds2 = EnumJson.enumReads(AccessType)
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

final case class DeleteDeveloperRequest(gatekeeperUserId: String, emailAddress: String)
object DeleteDeveloperRequest {
  implicit val format = Json.format[DeleteDeveloperRequest]
}

final case class CreatePrivOrROPCAppForm(environment: Environment = SANDBOX, accessType: Option[String] = None, applicationName: String = "", applicationDescription: String = "", adminEmail: String = "")
object CreatePrivOrROPCAppForm {

  def invalidAppName(form: Form[CreatePrivOrROPCAppForm]) = {
    form.withError("applicationName", "application.name.already.exists")
  }
}

sealed trait FieldsDeleteResult
case object FieldsDeleteSuccessResult extends FieldsDeleteResult
case object FieldsDeleteFailureResult extends FieldsDeleteResult


final case class CreatePrivOrROPCAppRequest(environment: String, name: String, description: String, collaborators: Seq[Collaborator], access: AppAccess)
object CreatePrivOrROPCAppRequest {
  implicit val format1 = EnumJson.enumFormat(AccessType)
  implicit val format2 = EnumJson.enumFormat(CollaboratorRole)
  implicit val format3 = Json.format[Collaborator]
  implicit val format4 = Json.format[TotpSecrets]
  implicit val format6 = Json.format[AppAccess]
  implicit val format7 = Json.format[CreatePrivOrROPCAppRequest]
}

case class AppAccess(accessType: AccessType, scopes: Seq[String])

final case class ClientSecret(secret: String)
final case class ClientCredentials(clientSecrets: Seq[ClientSecret])
final case class GetClientCredentialsResult(production: ClientCredentials)

object GetClientCredentialsResult {
  implicit val reads1 = Json.reads[ClientSecret]
  implicit val reads2 = Json.reads[ClientCredentials]
  implicit val reads3 = Json.reads[GetClientCredentialsResult]
}

final case class AddTeamMemberRequest(adminEmail: String, collaborator: Collaborator, isRegistered: Boolean, adminsToEmail: Set[String])

object AddTeamMemberRequest {
  implicit val format1 = EnumJson.enumFormat(CollaboratorRole)
  implicit val format2 = Json.format[Collaborator]
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
  implicit val formatApiDefinitionSummary = Json.format[APIApprovalSummary]
}

case class ApproveServiceRequest(serviceName: String)

object ApproveServiceRequest {
  implicit val format = Json.format[ApproveServiceRequest]
}

class UpdateApiDefinitionsFailed extends Throwable
