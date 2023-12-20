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

package uk.gov.hmrc.gatekeeper.models.applications

import play.api.libs.json.{JsValue, Json, OFormat}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, Environment, LaxEmailAddress, UserId}

case class User(userId: UserId, email: LaxEmailAddress, firstName: String, lastName: String) {
  def asJson: JsValue = User.userFormat.writes(this)
}

object User {
  implicit val userFormat: OFormat[User] = Json.format[User]
}

case class ApplicationWithUsers(applicationId: ApplicationId, name: String, environment: Environment, users: Set[User]) {
  def asJson: JsValue = ApplicationWithUsers.applicationWithUsersFormat.writes(this)
}

object ApplicationWithUsers {
  implicit val applicationWithUsersFormat: OFormat[ApplicationWithUsers] = Json.format[ApplicationWithUsers]
}
