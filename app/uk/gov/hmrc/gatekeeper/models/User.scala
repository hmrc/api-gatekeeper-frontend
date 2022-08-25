/*
 * Copyright 2022 HM Revenue & Customs
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

import uk.gov.hmrc.gatekeeper.models.xml.XmlOrganisation
import play.api.libs.json._
import uk.gov.hmrc.gatekeeper.utils.MfaDetailHelper
import uk.gov.hmrc.play.json.Union
import play.api.libs.functional.syntax._
import uk.gov.hmrc.gatekeeper.models.MfaId.format

case class CoreUserDetails(email: String, id: UserId)

trait User {
  def email: String
  def userId: UserId
  def firstName: String
  def lastName: String
  lazy val sortField = User.asSortField(lastName, firstName)
  lazy val fullName = s"${firstName} ${lastName}"
}

object User {
  def asSortField(lastName: String, firstName: String): String = s"${lastName.trim().toLowerCase()} ${firstName.trim().toLowerCase()}"
  
  def status(user: User): StatusFilter = user match {
    case u : RegisteredUser if (u.verified) => VerifiedStatus
    case u : RegisteredUser if (!u.verified) => UnverifiedStatus
    case _ : UnregisteredUser => UnregisteredStatus
  }
}


case class RegisteredUser(
  email: String,
  userId: UserId,
  firstName: String,
  lastName: String,
  verified: Boolean,
  organisation: Option[String] = None,
  mfaEnabled: Boolean = false,
  mfaDetails: List[MfaDetail] = List.empty,
  emailPreferences: EmailPreferences = EmailPreferences.noPreferences) extends User {
}

object RegisteredUser {
  implicit val authenticatorAppMfaDetailFormat: OFormat[AuthenticatorAppMfaDetailSummary] = Json.format[AuthenticatorAppMfaDetailSummary]
  implicit val smsMfaDetailFormat: OFormat[SmsMfaDetail] = Json.format[SmsMfaDetail]

  implicit val mfaDetailFormat: OFormat[MfaDetail] = Union.from[MfaDetail]("mfaType")
    .and[AuthenticatorAppMfaDetailSummary](MfaType.AUTHENTICATOR_APP.toString)
    .and[SmsMfaDetail](MfaType.SMS.toString)
    .format

  val registeredUserReads: Reads[RegisteredUser] = (
      (JsPath \ "email").read[String] and
      (JsPath \ "userId").read[UserId] and
      (JsPath \ "firstName").read[String] and
      (JsPath \ "lastName").read[String] and
      (JsPath \ "verified").read[Boolean] and
      (JsPath \ "organisation").readNullable[String] and
      (JsPath \ "mfaEnabled").read[Boolean] and
      ((JsPath \ "mfaDetails").read[List[MfaDetail]] or Reads.pure(List.empty[MfaDetail])) and
      ((JsPath \ "emailPreferences").read[EmailPreferences] or Reads.pure(EmailPreferences.noPreferences))) (RegisteredUser.apply _)

  val registeredUserWrites: OWrites[RegisteredUser] = Json.writes[RegisteredUser]
  implicit val registeredUserFormat: Format[RegisteredUser] = Format(registeredUserReads, registeredUserWrites)

}

case class UnregisteredUser(email: String, userId: UserId) extends User {
  val firstName = "n/a"
  val lastName = "n/a"
}

case class Developer(user: User, applications: List[Application], xmlServiceNames: Set[String] = Set.empty,
                     xmlOrganisations: List[XmlOrganisation] = List.empty) {
  lazy val fullName = user.fullName
  
  lazy val email = user.email

  lazy val userId = user.userId

  lazy val xmlEmailPrefServices = xmlServiceNames

  lazy val xmlOrgs = xmlOrganisations

  lazy val mfaDetails: List[MfaDetail] = user match {
    case UnregisteredUser(_,_) => List.empty
    case r : RegisteredUser => r.mfaDetails
  }
  
  lazy val firstName: String = user match {
    case UnregisteredUser(_,_) => "n/a"
    case r : RegisteredUser => r.firstName
  }
  
  lazy val lastName: String = user match {
    case UnregisteredUser(_,_) => "n/a"
    case r : RegisteredUser => r.lastName
  }
  
  lazy val organisation: Option[String] = user match {
    case UnregisteredUser(_,_) => None
    case r : RegisteredUser => r.organisation
  }

  lazy val verified: Boolean = user match {
    case UnregisteredUser(_,_) => false
    case r : RegisteredUser => r.verified
  }

  lazy val mfaEnabled: Boolean = user match {
    case UnregisteredUser(_,_) => false
    case r : RegisteredUser => MfaDetailHelper.isMfaVerified(r.mfaDetails)
  }

  lazy val emailPreferences: EmailPreferences = user match {
    case UnregisteredUser(_,_) => EmailPreferences.noPreferences
    case r : RegisteredUser => r.emailPreferences
  }

  lazy val sortField: String = user match {
    case UnregisteredUser(_,_) => User.asSortField(lastName, firstName)
    case r : RegisteredUser => r.sortField
  }

  lazy val status: StatusFilter = User.status(user)

  lazy val id: String = user.userId.value.toString
}
