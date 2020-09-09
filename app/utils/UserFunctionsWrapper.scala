/*
 * Copyright 2020 HM Revenue & Customs
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

package utils

import model.{APIDefinition, APIStatus, APIVersion, ApiContextVersion, DropDownValue, LoggedInRequest, User}
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController

trait UserFunctionsWrapper {
  self: FrontendBaseController =>


  def mapEmptyStringToNone(filter: Option[String]): Option[String] = {
    filter match {
      case None | Some("")  => None
      case _ => filter
    }
  }

  def getQueryParametersAsKeyValues(request: LoggedInRequest[_]) = {
    request.queryString.map { case (k, v) => k -> v.mkString }
  }

  def usersToEmailCopyText(users: Seq[User]): String = {
    users.map(_.email).mkString("; ")
  }

  def getApiVersionsDropDownValues(apiDefinitions: Seq[APIDefinition]) = {
    def toKeyValue(api: APIDefinition, version: APIVersion) = {
      val value: String = ApiContextVersion(api.context, version.version).toStringValue.trim
      val displayedStatus: String = APIStatus.displayedStatus(version.status).trim
      val description: String = s"${api.name} (${version.version}) ($displayedStatus)"

      DropDownValue(value, description)
    }

    (for {
      api <- apiDefinitions
      version <- api.versions
    } yield toKeyValue(api, version))
      .distinct
      .sortBy(keyValue => keyValue.description)
  }
}
