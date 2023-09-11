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

package uk.gov.hmrc.gatekeeper.models

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

import play.api.libs.json.Json

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.gatekeeper.models.ApiStatus.ApiStatus
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields._

case class ApiDefinitionGK(
    serviceName: String,
    serviceBaseUrl: String,
    name: String,
    description: String,
    context: ApiContext,
    versions: List[ApiVersionGK],
    requiresTrust: Option[Boolean],
    categories: Option[List[APICategory]]
  ) {

  def descendingVersion(v1: VersionSubscription, v2: VersionSubscription) = {
    v1.version.version.value.toDouble > v2.version.version.value.toDouble
  }
}

case class APICategory(value: String) extends AnyVal

object APICategory {
  implicit val formatApiCategory = Json.valueFormat[APICategory]
}

case class APICategoryDetails(category: String, name: String) {

  def toAPICategory: APICategory = {
    APICategory(category)
  }
}

object APICategoryDetails {
  implicit val formatApiCategory = Json.format[APICategoryDetails]
}
case class VersionSubscription(version: ApiVersionGK, subscribed: Boolean, fields: SubscriptionFieldsWrapper)

case class ApiVersionGK(version: ApiVersion, versionSource: ApiVersionSource, status: ApiStatus, access: Option[ApiAccess] = None) {
  val displayedStatus = ApiStatus.displayedStatus(status)

  val accessType = access.map(_.`type`).getOrElse(ApiAccessType.PUBLIC)

  val displayedAccessType = accessType.toString().toLowerCase().capitalize
}

object ApiStatus extends Enumeration {
  type ApiStatus = Value
  val ALPHA, BETA, STABLE, DEPRECATED, RETIRED = Value

  val displayedStatus: (ApiStatus) => String = {
    case ApiStatus.ALPHA      => "Alpha"
    case ApiStatus.BETA       => "Beta"
    case ApiStatus.STABLE     => "Stable"
    case ApiStatus.DEPRECATED => "Deprecated"
    case ApiStatus.RETIRED    => "Retired"
  }
}

case class ApiAccess(`type`: ApiAccessType, isTrial: Option[Boolean] = None)

sealed trait ApiAccessType extends EnumEntry

object ApiAccessType extends Enum[ApiAccessType] with PlayJsonEnum[ApiAccessType] {
  val values = findValues
  case object PRIVATE extends ApiAccessType
  case object PUBLIC  extends ApiAccessType
}

class FetchApiDefinitionsFailed extends Throwable
class FetchApiCategoriesFailed  extends Throwable

case class VersionSummary(name: String, status: ApiStatus, apiIdentifier: ApiIdentifier)

case class SubscriptionResponse(apiIdentifier: ApiIdentifier, applications: List[String])

case class Subscription(name: String, serviceName: String, context: ApiContext, versions: List[VersionSubscription]) {
  lazy val subscriptionNumberText = Subscription.subscriptionNumberLabel(versions)
}

case class SubscriptionWithoutFields(name: String, serviceName: String, context: ApiContext, versions: List[VersionSubscriptionWithoutFields]) {
  lazy val subscriptionNumberText = SubscriptionWithoutFields.subscriptionNumberLabel(versions)
}

case class VersionSubscriptionWithoutFields(version: ApiVersionGK, subscribed: Boolean)

object Subscription {

  def subscriptionNumberLabel(versions: List[VersionSubscription]) = versions.count(_.subscribed) match {
    case 1      => s"1 subscription"
    case number => s"$number subscriptions"
  }
}

object SubscriptionWithoutFields {

  def subscriptionNumberLabel(versions: List[VersionSubscriptionWithoutFields]) = versions.count(_.subscribed) match {
    case 1      => s"1 subscription"
    case number => s"$number subscriptions"
  }
}
