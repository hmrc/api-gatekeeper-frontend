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

package uk.gov.hmrc.gatekeeper.connectors

import uk.gov.hmrc.gatekeeper.models.applications.ApplicationWithSubscriptionData
import uk.gov.hmrc.gatekeeper.models.pushpullnotifications.{Box, BoxCreator, BoxId, BoxSubscriber}
import uk.gov.hmrc.gatekeeper.models.{APIDefinitionFormatters, ApiStatusJson}

private[connectors] object ApmConnectorJsonFormatters extends ApiStatusJson with APIDefinitionFormatters {

  import uk.gov.hmrc.apiplatform.modules.common.domain.services.LocalDateTimeFormatter._
  import uk.gov.hmrc.gatekeeper.models.subscriptions.{ApiData, VersionData}
  import play.api.libs.json._

  implicit val readsVersionData: Reads[VersionData] = Json.reads[VersionData]
  implicit val readsApiData: Reads[ApiData]         = Json.reads[ApiData]
  implicit val readsApplicationWithSubscriptionData = Json.reads[ApplicationWithSubscriptionData]

  implicit val readsBoxId         = Json.valueFormat[BoxId]
  implicit val readsBoxCreator    = Json.reads[BoxCreator]
  implicit val readsBoxSubscriber = Json.reads[BoxSubscriber]
  implicit val readsBox           = Json.reads[Box]
}
