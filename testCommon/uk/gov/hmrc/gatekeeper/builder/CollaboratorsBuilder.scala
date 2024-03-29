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

package uk.gov.hmrc.gatekeeper.builder

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{Collaborator, Collaborators}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId

trait CollaboratorsBuilder {
  import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax

  def buildCollaborators(collaborators: Seq[(String, Collaborator.Role)]): Set[Collaborator] = {
    collaborators.map {
      case (email, Collaborator.Roles.ADMINISTRATOR) => Collaborators.Administrator(UserId.random, email.toLaxEmail)
      case (email, Collaborator.Roles.DEVELOPER)     => Collaborators.Developer(UserId.random, email.toLaxEmail)
    }.toSet
  }
}
