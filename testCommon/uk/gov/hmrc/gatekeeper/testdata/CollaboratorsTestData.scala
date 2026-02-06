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

package uk.gov.hmrc.gatekeeper.testdata

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{Collaborator, Collaborators}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{LaxEmailAddress, UserId, UserIdData}
import uk.gov.hmrc.gatekeeper.utils.CollaboratorTracker

trait CollaboratorsTestData extends CommonTestData with CollaboratorTracker {

  val collaboratorsAdminAndUnverifiedDev: Set[Collaborator] = Set(
    Collaborators.Administrator(UserIdData.one, administratorEmail),
    Collaborators.Developer(UserIdData.two, developerEmail),
    Collaborators.Developer(unverifiedUser.userId, unverifiedUser.email)
  )

  val collaboratorsTwoAdminAndUnverifiedDev: Set[Collaborator] = Set(
    Collaborators.Administrator(UserIdData.one, administrator2Email),
    Collaborators.Administrator(UserIdData.two, administratorEmail),
    Collaborators.Developer(UserIdData.three, developerEmail),
    Collaborators.Developer(UserId(MockDataSugar.developer5Id), LaxEmailAddress(MockDataSugar.developer5))
  )

  val collaboratorsDevAndUnverifiedAdmin: Set[Collaborator] = Set(
    developerEmail.asDeveloperCollaborator,
    Collaborators.Administrator(unverifiedUser.userId, unverifiedUser.email)
  )
}
