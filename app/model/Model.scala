/*
 * Copyright 2017 HM Revenue & Customs
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

import model.RateLimitTier.RateLimitTier
import model.State.State
import org.joda.time.DateTime
import play.api.libs.json.Json
import uk.gov.hmrc.crypto.json.{JsonDecryptor, JsonEncryptor}
import uk.gov.hmrc.crypto.{ApplicationCrypto, Protected}
import uk.gov.hmrc.http.SessionKeys


case class LoginDetails(userName: String, password: Protected[String])

object LoginDetails {
  implicit val crypto = ApplicationCrypto.JsonCrypto

  object JsonStringEncryption extends JsonEncryptor[String]

  object JsonStringDecryption extends JsonDecryptor[String]

  implicit val encryptedStringFormats = JsonStringEncryption
  implicit val decryptedStringFormats = JsonStringDecryption

  implicit val formats = Json.format[LoginDetails]

  def make(userName: String, password: String) = LoginDetails(userName, Protected(password))

  def unmake(user: LoginDetails) = Some((user.userName, user.password.decryptedValue))
}


case class Role(scope: String, name: String)

object Role {
  implicit val format = Json.format[Role]
  val APIGatekeeper = Role("api", "gatekeeper")
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

case class UpdateRateLimitTierRequest(rateLimitTier: String)

object UpdateRateLimitTierRequest {
  implicit val format = Json.format[UpdateRateLimitTierRequest]
}

sealed trait UpdateApplicationRateLimitTierSuccessful

case object UpdateApplicationRateLimitTierSuccessful extends UpdateApplicationRateLimitTierSuccessful


case class ApplicationWithHistory(application: ApplicationResponse, history: Seq[StateHistory])

object ApplicationWithHistory {
  implicit val format1 = Json.format[APIIdentifier]
  implicit val formatRole = EnumJson.enumFormat(CollaboratorRole)
  implicit val format2 = Json.format[Collaborator]
  implicit val format3 = Json.format[ApplicationState]
  implicit val format4 = EnumJson.enumFormat(State)
  implicit val formatRateLimitTier = EnumJson.enumFormat(RateLimitTier)
  implicit val format5 = Json.format[ApplicationResponse]
  implicit val format6 = Json.format[ApplicationWithHistory]
}

case class ApplicationWithUpliftRequest(id: UUID, name: String, submittedOn: DateTime, state: State)


object ApplicationWithUpliftRequest {

  implicit val formatState = EnumJson.enumFormat(State)
  implicit val format = Json.format[ApplicationWithUpliftRequest]

  val compareBySubmittedOn = (a: ApplicationWithUpliftRequest, b: ApplicationWithUpliftRequest) => a.submittedOn.isBefore(b.submittedOn)
}

class PreconditionFailed extends Throwable

class FetchApplicationsFailed extends Throwable

class InconsistentDataState(message: String) extends RuntimeException(message)

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

case class ApplicationDetails(id: String, name: String, description: String, rateLimitTier: Option[RateLimitTier], submission: SubmissionDetails)

case class ApprovedApplication(details: ApplicationDetails, admins: Seq[User], approvedBy: String, approvedOn: DateTime, verified: Boolean)

case class CategorisedApplications(pendingApproval: Seq[ApplicationWithUpliftRequest], approved: Seq[ApplicationWithUpliftRequest])
