/*
 * Copyright 2021 HM Revenue & Customs
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

import CollaboratorRole.CollaboratorRole

object NewModel {

  case class CoreUserDetails(email: String, id: UserId)

  case class Collaborator(emailAddress: String, userId: UserId, role: CollaboratorRole)
  
  trait User {
    def email: String
    // def id: UserId     // Need to use newmodel collaborator which means all apps must have ids on collaborators  // TODO APIS-5153
    def firstName: String
    def lastName: String
    lazy val sortField = User.asSortField(lastName, firstName)
    lazy val fullName = s"${firstName} ${lastName}"
  }

  object User {
    def asSortField(lastName: String, firstName: String): String = s"${lastName.trim().toLowerCase()} ${firstName.trim().toLowerCase()}"
    
    def status(user: User): StatusFilter = user match {
      case u : NewModel.RegisteredUser if (u.verified) => VerifiedStatus
      case u : NewModel.RegisteredUser if (!u.verified) => UnverifiedStatus
      case _ : NewModel.UnregisteredUser => UnregisteredStatus
    }
  }

  case class RegisteredUser(
    email: String,
    userId: UserId,
    firstName: String,
    lastName: String,
    verified: Boolean,
    organisation: Option[String] = None,
    mfaEnabled: Boolean = false) extends User {
  }

  object RegisteredUser {
    import UserId._
    import play.api.libs.json._
    
    implicit val reads = Json.format[RegisteredUser]
  }

  case class UnregisteredUser(email: String) extends User {
    // val id: UserId     // Need to use newmodel collaborator which means all apps must have ids on collaborators // TODO APIS-5153
    val firstName = "n/a"
    val lastName = "n/a"
  }

  case class Developer(user: User, applications: Seq[Application]) {
    lazy val fullName = user.fullName
    
    lazy val email: String = user.email
    
    lazy val firstName: String = user match {
      case UnregisteredUser(_) => "n/a"
      case r : RegisteredUser => r.firstName
    }
    
    lazy val lastName: String = user match {
      case UnregisteredUser(_) => "n/a"
      case r : RegisteredUser => r.lastName
    }
    
    lazy val organisation: Option[String] = user match {
      case UnregisteredUser(_) => None
      case r : RegisteredUser => r.organisation
    }

    lazy val verified: Boolean = user match {
      case UnregisteredUser(_) => false
      case r : RegisteredUser => r.verified
    }

    lazy val mfaEnabled: Boolean = user match {
      case UnregisteredUser(_) => false
      case r : RegisteredUser => r.mfaEnabled
    }

    lazy val sortField: String = user match {
      case UnregisteredUser(_) => User.asSortField(lastName, firstName)
      case r : RegisteredUser => r.sortField
    }

    lazy val status: StatusFilter = User.status(user)
  }
}
