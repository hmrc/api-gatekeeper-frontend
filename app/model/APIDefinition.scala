/*
 * Copyright 2017 HM Revenue & Customs
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

package model

import model.APIStatus.APIStatus
import model.CollaboratorRole.CollaboratorRole
import play.api.libs.json.Json

case class APISubscription(name: String, serviceName: String, context: String, versions: Seq[VersionSubscription], requiresTrust: Option[Boolean])

case class APIDefinition(
                          serviceName: String,
                          serviceBaseUrl: String,
                          name: String,
                          description: String,
                          context: String,
                          versions: Seq[APIVersion],
                          requiresTrust: Option[Boolean]) {

  private def uniqueVersions = {
    !versions.map(_.version).groupBy(identity).mapValues(_.size).exists(_._2 > 1)
  }

  def descendingVersion(v1: VersionSubscription, v2: VersionSubscription) = {
    v1.version.version.toDouble > v2.version.version.toDouble
  }
}

object APIDefinition {
  implicit val formatAPIStatus = EnumJson.enumFormat(APIStatus)
  implicit val formatAPIAccessType = EnumJson.enumFormat(APIAccessType)
  implicit val formatAPIAccess = Json.format[APIAccess]
  implicit val formatAPIVersion = Json.format[APIVersion]
  implicit val formatVersionSubscription = Json.format[VersionSubscription]
  implicit val formatAPISubscription = Json.format[APISubscription]
  implicit val formatAPIIdentifier = Json.format[APIIdentifier]
  implicit val formatApiDefinitions = Json.format[APIDefinition]
}

case class VersionSubscription(version: APIVersion, subscribed: Boolean)

case class APIVersion(version: String, status: APIStatus, access: Option[APIAccess] = None) {
  val displayedStatus = {
    status match {
      case APIStatus.PROTOTYPED => "Beta"
      case APIStatus.PUBLISHED => "Stable"
      case APIStatus.DEPRECATED => "Deprecated"
      case APIStatus.RETIRED => "Retired"
    }
  }

  val accessType = access.map(_.`type`).getOrElse(APIAccessType.PUBLIC)
}


object APIStatus extends Enumeration {
  type APIStatus = Value
  val PROTOTYPED, PUBLISHED, DEPRECATED, RETIRED = Value

  val displayedStatus: (APIStatus) => String = {
    case APIStatus.PROTOTYPED => "Beta"
    case APIStatus.PUBLISHED => "Stable"
    case APIStatus.DEPRECATED => "Deprecated"
    case APIStatus.RETIRED => "Retired"
  }
}

case class APIAccess(`type`: APIAccessType.Value)

object APIAccessType extends Enumeration {
  type APIAccessType = Value
  val PRIVATE, PUBLIC = Value
}

case class APIIdentifier(context: String, version: String)

case class APISubscriptionStatus(name: String, serviceName: String,
                                 context: String, version: APIVersion, subscribed: Boolean, requiresTrust: Boolean) {
  def canUnsubscribe(role: CollaboratorRole) = {
    role == CollaboratorRole.ADMINISTRATOR && subscribed && version.status != APIStatus.DEPRECATED
  }
}

class FetchApiDefinitionsFailed extends Throwable

case class VersionSummary(name: String, status: APIStatus, apiIdentifier: APIIdentifier)

case class SubscriptionResponse(apiIdentifier: APIIdentifier, applications: Seq[String])

object SubscriptionResponse {
  implicit val format1 = Json.format[APIIdentifier]
  implicit val format2 = Json.format[SubscriptionResponse]
}
