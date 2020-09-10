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

package model

import model.APIStatus.APIStatus
import model.SubscriptionFields.{SubscriptionFieldDefinition, SubscriptionFieldValue, SubscriptionFieldsWrapper}
import play.api.libs.json.Json

import scala.util.Try
import scala.util.Random
import java.net.URLEncoder.encode

case class ApiContext(value: String) extends AnyVal {
  def urlEncode(encoding: String = "UTF-8") = encode(value, encoding)
}

object ApiContext {

  implicit val formatApiContext = Json.valueFormat[ApiContext]

  implicit val ordering: Ordering[ApiContext] = new Ordering[ApiContext] {
    override def compare(x: ApiContext, y: ApiContext): Int = x.value.compareTo(y.value)
  }

  def random = ApiContext(Random.nextString(10))
}

case class ApiVersion(value: String) extends AnyVal {
  def urlEncode(encoding: String = "UTF-8") = encode(value, encoding)
}

object ApiVersion {

  implicit val formatApiVersion = Json.valueFormat[ApiVersion]

  implicit val ordering: Ordering[ApiVersion] = new Ordering[ApiVersion] {
    override def compare(x: ApiVersion, y: ApiVersion): Int = x.value.compareTo(y.value)
  }

  def random = ApiVersion((Random.nextInt()/10).toString)
}

case class APIDefinition(serviceName: String,
                         serviceBaseUrl: String,
                         name: String,
                         description: String,
                         context: ApiContext,
                         versions: Seq[ApiVersionDefinition],
                         requiresTrust: Option[Boolean]) {

  def descendingVersion(v1: VersionSubscription, v2: VersionSubscription) = {
    v1.version.version.value.toDouble > v2.version.version.value.toDouble
  }
}

object APIDefinition {
  implicit val formatAPIStatus = APIStatusJson.apiStatusFormat(APIStatus)
  implicit val formatAPIAccessType = EnumJson.enumFormat(APIAccessType)
  implicit val formatAPIAccess = Json.format[APIAccess]
  implicit val formatAPIVersion = Json.format[ApiVersionDefinition]
  implicit val formatSubscriptionFieldDefinition = Json.format[SubscriptionFieldDefinition]
  implicit val formatSubscriptionFieldValue = Json.format[SubscriptionFieldValue]
  implicit val formatSubscriptionFields = Json.format[SubscriptionFieldsWrapper]
  implicit val formatVersionSubscription = Json.format[VersionSubscription]
  implicit val formatAPIIdentifier = Json.format[APIIdentifier]
  implicit val formatApiDefinitions = Json.format[APIDefinition]

  implicit val versionSubscriptionWithoutFieldsJsonFormatter = Json.format[VersionSubscriptionWithoutFields]
  implicit val subscriptionWithoutFieldsJsonFormatter = Json.format[SubscriptionWithoutFields]

  private val nonNumericOrPeriodRegex = "[^\\d^.]*"
  private val fallback = Array(1, 0, 0)

  private def versionSorter(v1: ApiVersionDefinition, v2: ApiVersionDefinition) = {
    val v1Parts = Try(v1.version.value.replaceAll(nonNumericOrPeriodRegex, "").split("\\.").map(_.toInt)).getOrElse(fallback)
    val v2Parts = Try(v2.version.value.replaceAll(nonNumericOrPeriodRegex, "").split("\\.").map(_.toInt)).getOrElse(fallback)
    val pairs = v1Parts.zip(v2Parts)

    val firstUnequalPair = pairs.find { case (one, two) => one != two }
    firstUnequalPair.fold(v1.version.value.length > v2.version.value.length) { case (a, b) => a > b }
  }

  def descendingVersion(v1: VersionSubscriptionWithoutFields, v2: VersionSubscriptionWithoutFields) = {
    versionSorter(v1.version, v2.version)
  }
}

case class APICategory(category: String, name: String)
object APICategory{
  implicit val formatApiCategory = Json.format[APICategory]
}

case class VersionSubscription(version: ApiVersionDefinition,
                               subscribed: Boolean,
                               fields: SubscriptionFieldsWrapper)

case class ApiVersionDefinition(version: ApiVersion, status: APIStatus, access: Option[APIAccess] = None) {
  val displayedStatus = APIStatus.displayedStatus(status)

  val accessType = access.map(_.`type`).getOrElse(APIAccessType.PUBLIC)
}

object APIStatus extends Enumeration {
  type APIStatus = Value
  val ALPHA, BETA, STABLE, DEPRECATED, RETIRED = Value

  val displayedStatus: (APIStatus) => String = {
    case APIStatus.ALPHA => "Alpha"
    case APIStatus.BETA => "Beta"
    case APIStatus.STABLE => "Stable"
    case APIStatus.DEPRECATED => "Deprecated"
    case APIStatus.RETIRED => "Retired"
  }
}

case class APIAccess(`type`: APIAccessType.Value, isTrial : Option[Boolean] = None)

object APIAccessType extends Enumeration {
  type APIAccessType = Value
  val PRIVATE, PUBLIC = Value
}

case class APIIdentifier(context: ApiContext, version: ApiVersion)
object APIIdentifier {
  implicit val format = Json.format[APIIdentifier]
}

class FetchApiDefinitionsFailed extends Throwable
class FetchApiCategoriesFailed extends Throwable

case class VersionSummary(name: String, status: APIStatus, apiIdentifier: APIIdentifier)

case class SubscriptionResponse(apiIdentifier: APIIdentifier, applications: Seq[String])

object SubscriptionResponse {
  implicit val format1 = Json.format[APIIdentifier]
  implicit val format2 = Json.format[SubscriptionResponse]
}

case class Subscription(name: String,
                        serviceName: String,
                        context: ApiContext,
                        versions: Seq[VersionSubscription]) {
  lazy val subscriptionNumberText = Subscription.subscriptionNumberLabel(versions)
}

case class SubscriptionWithoutFields(name: String,
                                     serviceName: String,
                                     context: ApiContext,
                                     versions: Seq[VersionSubscriptionWithoutFields])

case class VersionSubscriptionWithoutFields(version: ApiVersionDefinition, subscribed: Boolean)

object Subscription {
  def subscriptionNumberLabel(versions: Seq[VersionSubscription]) = versions.count(_.subscribed) match {
    case 1 => s"1 subscription"
    case number => s"$number subscriptions"
  }

  implicit val formatAPIStatus = APIStatusJson.apiStatusFormat(APIStatus)
  implicit val formatAPIAccessType = EnumJson.enumFormat(APIAccessType)
  implicit val formatAPIAccess = Json.format[APIAccess]
  implicit val formatAPIVersion = Json.format[ApiVersionDefinition]
  implicit val formatSubscriptionFieldValue = Json.format[SubscriptionFieldDefinition]
  implicit val subscriptionFieldValue = Json.format[SubscriptionFieldValue]
  implicit val formatSubscriptionFieldsWrapper = Json.format[SubscriptionFieldsWrapper]
  implicit val formatVersionSubscription = Json.format[VersionSubscription]
  implicit val subscriptionJsonFormatter = Json.format[Subscription]

  implicit val versionSubscriptionWithoutFieldsJsonFormatter = Json.format[VersionSubscriptionWithoutFields]
  implicit val subscriptionWithoutFieldsJsonFormatter = Json.format[SubscriptionWithoutFields]
}
