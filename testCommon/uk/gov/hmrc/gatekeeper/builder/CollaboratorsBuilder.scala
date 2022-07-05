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

package uk.gov.hmrc.gatekeeper.builder

import uk.gov.hmrc.gatekeeper.models.Collaborator
import uk.gov.hmrc.gatekeeper.models.CollaboratorRole
import uk.gov.hmrc.gatekeeper.models.UserId

trait CollaboratorsBuilder {
  def buildCollaborators(collaborators: Seq[(String, CollaboratorRole.Value)]): Set[Collaborator] = {
    collaborators.map(
      n => Collaborator(n._1, n._2, UserId.random)
    ).toSet
  }
}