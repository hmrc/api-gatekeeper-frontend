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

package uk.gov.hmrc.gatekeeper.views.helper

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.Collaborator

object EmailsFormatter {

  def format(collaborators: Set[Collaborator], maybeFilter: Option[Collaborator.Role] = None): String = {
    (maybeFilter match {
      case Some(Collaborator.Roles.ADMINISTRATOR) => collaborators.filter(_.isAdministrator)
      case Some(Collaborator.Roles.DEVELOPER)     => collaborators.filter(_.isDeveloper)
      case _                                      => collaborators
    }).map(_.emailAddress.text).mkString("; ")
  }
}
