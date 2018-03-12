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

import model.User.UserStatus
import play.api.libs.json.Json

case class User(email: String, firstName: String, lastName: String, verified: Option[Boolean])
  extends BaseUser with Ordered[User] {

  override def compare(that: User): Int = this.sortField.compare(that.sortField)

  def toDeveloper(apps: Seq[Application]) = Developer(email, firstName, lastName, verified, apps)
}

object User {
  implicit val format = Json.format[User]
  type UserStatus = StatusFilter
}

case object UnregisteredCollaborator {
  def apply(email: String) = User(email, "n/a", "n/a", verified = None)
}


case class Developer(email: String, firstName: String, lastName: String, verified: Option[Boolean],
                     apps: Seq[Application]) extends ApplicationDeveloper

object Developer {
  def createUnregisteredDeveloper(email: String, apps: Set[Application] = Set.empty) = {
    Developer(email, "n/a", "n/a", None, apps.toSeq)
  }

  def createFromUser(user: User, apps: Seq[Application] = Seq.empty) =
    Developer(user.email, user.firstName, user.lastName, user.verified, apps)
}

trait BaseUser {
  val email: String
  val firstName: String
  val lastName: String
  val verified: Option[Boolean]
  val sortField = s"${lastName.trim().toLowerCase()} ${firstName.trim().toLowerCase()}"
  val fullName = s"$firstName $lastName"

  lazy val status: UserStatus = verified match {
    case Some(true) => VerifiedStatus
    case Some(false) => UnverifiedStatus
    case None => UnregisteredStatus
  }
}

trait ApplicationDeveloper extends BaseUser with Ordered[ApplicationDeveloper] {
  val apps: Seq[Application]
  override def compare(that: ApplicationDeveloper): Int = this.sortField.compare(that.sortField)
  def toDeveloper = Developer(email, firstName, lastName, verified, apps)
}
