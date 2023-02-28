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

package uk.gov.hmrc.gatekeeper.utils

import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{Collaborator, Collaborators}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress

import scala.collection.mutable
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax


trait UserIdTracker {
  private val idsByEmail = mutable.Map[String, UserId]()

  def idOf(email: String): UserId = idsByEmail.getOrElseUpdate(email, UserId.random)
}

trait CollaboratorTracker extends UserIdTracker {

  implicit class CollaboratorSyntax(email: LaxEmailAddress) {
    def asAdministratorCollaborator: Collaborator = Collaborators.Administrator(idOf(email), email)
    def asDeveloperCollaborator: Collaborator    = Collaborators.Developer(idOf(email), email)

  }
}
