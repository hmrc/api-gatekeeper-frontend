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

import java.time.Instant

import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, _}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{LaxEmailAddress, UserId}
import uk.gov.hmrc.apiplatform.modules.tpd.core.domain.models.User.{DefaultInstantReads, DefaultInstantWrites}
import uk.gov.hmrc.apiplatform.modules.tpd.emailpreferences.domain.models.EmailPreferences
import uk.gov.hmrc.apiplatform.modules.tpd.mfa.domain.models._
import uk.gov.hmrc.gatekeeper.models.organisations.DeskproOrganisation
import uk.gov.hmrc.gatekeeper.models.xml.XmlOrganisation
import uk.gov.hmrc.gatekeeper.utils.MfaDetailHelper

case class CoreUserDetails(email: LaxEmailAddress, id: UserId)

sealed trait AbstractUser {
  def email: LaxEmailAddress
  def userId: UserId
  def firstName: String
  def lastName: String
  lazy val sortField = AbstractUser.asSortField(lastName, firstName)
  lazy val fullName  = s"${firstName} ${lastName}"
}

object AbstractUser {
  def asSortField(lastName: String, firstName: String): String = s"${lastName.trim().toLowerCase()} ${firstName.trim().toLowerCase()}"

  def status(user: AbstractUser): StatusFilter = user match {
    case u: RegisteredUser if (u.verified) => VerifiedStatus
    case u: RegisteredUser                 => UnverifiedStatus
    case _: UnregisteredUser               => UnregisteredStatus
  }
}

case class RegisteredUser(
    email: LaxEmailAddress,
    userId: UserId,
    firstName: String,
    lastName: String,
    verified: Boolean,
    mfaDetails: List[MfaDetail] = List.empty,
    emailPreferences: EmailPreferences = EmailPreferences.noPreferences,
    registrationTime: Option[Instant] = None,
    failedLogins: Int = 0,
    lastLogin: Option[Instant] = None
  ) extends AbstractUser {}

object RegisteredUser {

  val registeredUserReads: Reads[RegisteredUser] = (
    (JsPath \ "email").read[LaxEmailAddress] and
      (JsPath \ "userId").read[UserId] and
      (JsPath \ "firstName").read[String] and
      (JsPath \ "lastName").read[String] and
      (JsPath \ "verified").read[Boolean] and
      ((JsPath \ "mfaDetails").read[List[MfaDetail]] or Reads.pure(List.empty[MfaDetail])) and
      ((JsPath \ "emailPreferences").read[EmailPreferences] or Reads.pure(EmailPreferences.noPreferences)) and
      (JsPath \ "registrationTime").readNullable[Instant] and
      (JsPath \ "failedLogins").readWithDefault[Int](0) and
      (JsPath \ "lastLogin").readNullable[Instant]
  )(RegisteredUser.apply _)

  val registeredUserWrites: OWrites[RegisteredUser]         = Json.writes[RegisteredUser]
  implicit val registeredUserFormat: Format[RegisteredUser] = Format(registeredUserReads, registeredUserWrites)
}

case class UnregisteredUser(email: LaxEmailAddress, userId: UserId) extends AbstractUser {
  val firstName = "n/a"
  val lastName  = "n/a"
}

case class Developer(
    user: AbstractUser,
    applications: List[ApplicationWithCollaborators],
    xmlServiceNames: Set[String] = Set.empty,
    xmlOrganisations: List[XmlOrganisation] = List.empty,
    deskproOrganisations: Option[List[DeskproOrganisation]] = None
  ) {
  lazy val fullName = user.fullName

  lazy val email = user.email

  lazy val userId = user.userId

  lazy val mfaDetails: List[MfaDetail] = user match {
    case UnregisteredUser(_, _) => List.empty
    case r: RegisteredUser      => r.mfaDetails
  }

  lazy val firstName: String = user match {
    case UnregisteredUser(_, _) => "n/a"
    case r: RegisteredUser      => r.firstName
  }

  lazy val lastName: String = user match {
    case UnregisteredUser(_, _) => "n/a"
    case r: RegisteredUser      => r.lastName
  }

  lazy val verified: Boolean = user match {
    case UnregisteredUser(_, _) => false
    case r: RegisteredUser      => r.verified
  }

  lazy val mfaEnabled: Boolean = user match {
    case UnregisteredUser(_, _) => false
    case r: RegisteredUser      => MfaDetailHelper.isMfaVerified(r.mfaDetails)
  }

  lazy val emailPreferences: EmailPreferences = user match {
    case UnregisteredUser(_, _) => EmailPreferences.noPreferences
    case r: RegisteredUser      => r.emailPreferences
  }

  lazy val sortField: String = user match {
    case UnregisteredUser(_, _) => AbstractUser.asSortField(lastName, firstName)
    case r: RegisteredUser      => r.sortField
  }

  lazy val status: StatusFilter = AbstractUser.status(user)

  lazy val id: String = user.userId.value.toString
}

case class UserPaginatedResponse(totalCount: Int, users: List[RegisteredUser])

object UserPaginatedResponse {
  implicit val format: Format[UserPaginatedResponse] = Json.format
}
