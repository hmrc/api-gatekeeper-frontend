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

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiDefinition, ApiVersion}
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInRequest
import uk.gov.hmrc.gatekeeper.models._

trait UserFunctionsWrapper {

  def mapEmptyStringToNone(filter: Option[String]): Option[String] = {
    filter match {
      case None | Some("") => None
      case _               => filter
    }
  }

  def getQueryParametersAsKeyValues(request: LoggedInRequest[_]) = {
    request.queryString.map { case (k, v) => k -> v.mkString }
  }

  def usersToEmailCopyText(users: List[RegisteredUser]): String = {
    users.map(_.email.text).sorted.mkString("; ")
  }

  def getApiVersionsDropDownValues(apiDefinitions: List[ApiDefinition]) = {
    def toKeyValue(api: ApiDefinition, version: ApiVersion) = {
      val value: String           = ApiContextVersion(api.context, version.versionNbr).toStringValue.trim
      val displayedStatus: String = version.status.displayText
      val description: String     = s"${api.name} (${version.versionNbr}) ($displayedStatus)"

      DropDownValue(value, description)
    }

    (for {
      api     <- apiDefinitions
      version <- api.versions.values
    } yield toKeyValue(api, version))
      .distinct
      .sortBy(keyValue => keyValue.description)
  }
}
