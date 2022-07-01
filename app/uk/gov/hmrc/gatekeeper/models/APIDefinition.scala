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

package uk.gov.hmrc.gatekeeper.models

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import uk.gov.hmrc.gatekeeper.models.ApiStatus.ApiStatus
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields._
import play.api.libs.json.Json

import scala.util.Random
import java.net.URLEncoder.encode

case class ApiContext(value: String) extends AnyVal {
  def urlEncode = encode(value, "UTF-8")
}

object ApiContext {
  implicit val ordering: Ordering[ApiContext] = new Ordering[ApiContext] {
    override def compare(x: ApiContext, y: ApiContext): Int = x.value.compareTo(y.value)
  }

  def random = ApiContext(Random.alphanumeric.take(10).mkString)
}

case class ApiVersion(value: String) extends AnyVal {
  def urlEncode = encode(value, "UTF-8")
}

object ApiVersion {
  implicit val ordering: Ordering[ApiVersion] = new Ordering[ApiVersion] {
    override def compare(x: ApiVersion, y: ApiVersion): Int = x.value.compareTo(y.value)
  }

  def random = ApiVersion(Random.nextDouble().toString)
}

case class ApiDefinition(serviceName: String,
                         serviceBaseUrl: String,
                         name: String,
                         description: String,
                         context: ApiContext,
                         versions: List[ApiVersionDefinition],
                         requiresTrust: Option[Boolean],
                         categories: Option[List[APICategory]]) {

  def descendingVersion(v1: VersionSubscription, v2: VersionSubscription) = {
    v1.version.version.value.toDouble > v2.version.version.value.toDouble
  }
}

case class APICategory(value: String) extends AnyVal
object APICategory{
  implicit val formatApiCategory = Json.valueFormat[APICategory]
}

case class APICategoryDetails(category: String, name: String){
  def toAPICategory: APICategory ={
    APICategory(category)
  }
}
object APICategoryDetails{
  implicit val formatApiCategory = Json.format[APICategoryDetails]
}
case class VersionSubscription(version: ApiVersionDefinition,
                               subscribed: Boolean,
                               fields: SubscriptionFieldsWrapper)

case class ApiVersionDefinition(version: ApiVersion, status: ApiStatus, access: Option[ApiAccess] = None) {
  val displayedStatus = ApiStatus.displayedStatus(status)

  val accessType = access.map(_.`type`).getOrElse(APIAccessType.PUBLIC)

  val displayedAccessType = accessType.toString().toLowerCase().capitalize
}

object ApiStatus extends Enumeration {
  type ApiStatus = Value
  val ALPHA, BETA, STABLE, DEPRECATED, RETIRED = Value

  val displayedStatus: (ApiStatus) => String = {
    case ApiStatus.ALPHA => "Alpha"
    case ApiStatus.BETA => "Beta"
    case ApiStatus.STABLE => "Stable"
    case ApiStatus.DEPRECATED => "Deprecated"
    case ApiStatus.RETIRED => "Retired"
  }
}

case class ApiAccess(`type`: APIAccessType, isTrial : Option[Boolean] = None)

sealed trait APIAccessType extends EnumEntry

object APIAccessType extends Enum[APIAccessType] with PlayJsonEnum[APIAccessType] {
  val values = findValues
  case object PRIVATE extends APIAccessType
  case object PUBLIC extends APIAccessType
}

case class ApiIdentifier(context: ApiContext, version: ApiVersion)
object ApiIdentifier {
  def random() = ApiIdentifier(ApiContext.random, ApiVersion.random)
}

class FetchApiDefinitionsFailed extends Throwable
class FetchApiCategoriesFailed extends Throwable

case class VersionSummary(name: String, status: ApiStatus, apiIdentifier: ApiIdentifier)

case class SubscriptionResponse(apiIdentifier: ApiIdentifier, applications: List[String])

case class Subscription(name: String,
                        serviceName: String,
                        context: ApiContext,
                        versions: List[VersionSubscription]) {
  lazy val subscriptionNumberText = Subscription.subscriptionNumberLabel(versions)
}

case class SubscriptionWithoutFields(name: String,
                                     serviceName: String,
                                     context: ApiContext,
                                     versions: List[VersionSubscriptionWithoutFields]) {
  lazy val subscriptionNumberText = SubscriptionWithoutFields.subscriptionNumberLabel(versions)
}

case class VersionSubscriptionWithoutFields(version: ApiVersionDefinition, subscribed: Boolean)

object Subscription {
  def subscriptionNumberLabel(versions: List[VersionSubscription]) = versions.count(_.subscribed) match {
    case 1 => s"1 subscription"
    case number => s"$number subscriptions"
  }
}

object SubscriptionWithoutFields {
  def subscriptionNumberLabel(versions: List[VersionSubscriptionWithoutFields]) = versions.count(_.subscribed) match {
    case 1 => s"1 subscription"
    case number => s"$number subscriptions"
  }
}
