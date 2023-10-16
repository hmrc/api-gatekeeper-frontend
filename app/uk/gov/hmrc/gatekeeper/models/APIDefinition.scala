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

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields._

case class VersionSubscription(version: ApiVersion, subscribed: Boolean, fields: SubscriptionFieldsWrapper)

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

case class VersionSubscriptionWithoutFields(version: ApiVersion, subscribed: Boolean)

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
