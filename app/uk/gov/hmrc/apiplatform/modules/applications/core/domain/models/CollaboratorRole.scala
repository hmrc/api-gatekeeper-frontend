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

package uk.gov.hmrc.apiplatform.modules.applications.core.domain.models

import play.api.libs.json.Json

import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{Collaborator, Collaborators}

object CollaboratorRole extends Enumeration {
  type CollaboratorRole = Value
  val DEVELOPER, ADMINISTRATOR = Value

  def displayedRole: Collaborator => String = _ match {
    case _: Collaborators.Administrator => "Administrator"
    case _                              => "Developer"
  }

  def from(role: Option[String]) = role match {
    case Some(r) => CollaboratorRole.values.find(e => e.toString == r.toUpperCase)
    case _       => Some(CollaboratorRole.DEVELOPER)
  }

  implicit val format = Json.formatEnum(CollaboratorRole)
}
