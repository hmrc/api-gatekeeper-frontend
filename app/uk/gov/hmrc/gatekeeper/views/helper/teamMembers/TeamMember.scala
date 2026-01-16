/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.views.helper.teamMembers

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.Collaborator
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.Collaborator.Role
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.Collaborator.Roles.ADMINISTRATOR
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInUser
import uk.gov.hmrc.apiplatform.modules.tpd.core.domain.models.User
import uk.gov.hmrc.apiplatform.modules.tpd.session.domain.models.UserSession
import uk.gov.hmrc.gatekeeper.models.RegisteredUser

object TeamMember {

  def verified(user: Option[RegisteredUser]): Boolean = {
    user match {
      case Some(u) => u.verified
      case None    => false
    }
  }

  def oneOnlyVerifiedAdmin(collaboratorUsers: List[RegisteredUser], appCollaborators: Set[Collaborator]): Boolean = {
    val adminEmails: Set[LaxEmailAddress] = appCollaborators.filter(_.isAdministrator).map(_.emailAddress)
    collaboratorUsers.filter(u => adminEmails.contains(u.email)).count(_.verified) == 1
  }

  def verifiedStatusDisplay(collaborator: Collaborator, collaboratorUsers: List[RegisteredUser]): String = {
    if (unregistered(collaborator, collaboratorUsers) || !verified(collaboratorUsers.find(_.email == collaborator.emailAddress))) {
      "No"
    } else {
      "Yes"
    }
  }

  private def unregistered(collaborator: Collaborator, collaboratorUsers: List[RegisteredUser]) = {
    !collaboratorUsers.exists(_.email == collaborator.emailAddress)
  }

  def collaboratorIsVerifiedAdmin(collaboratorUsers: List[RegisteredUser], appCollaborator: Collaborator): Boolean = {
    collaboratorUsers.filter(cu => cu.email == appCollaborator.emailAddress && appCollaborator.isAdministrator).exists(ru => ru.verified)
  }
}
