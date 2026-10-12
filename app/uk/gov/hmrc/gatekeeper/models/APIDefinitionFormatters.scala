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

import play.api.libs.json._

import uk.gov.hmrc.gatekeeper.models.SubscriptionFields.{SubscriptionFieldDefinition, SubscriptionFieldValue, SubscriptionFieldsWrapper}

trait APIDefinitionFormatters {

  given formatSubscriptionFieldDefinition: OFormat[SubscriptionFieldDefinition] = Json.format[SubscriptionFieldDefinition]
  given formatSubscriptionFieldValue: OFormat[SubscriptionFieldValue]           = Json.format[SubscriptionFieldValue]
  given formatSubscriptionFields: OFormat[SubscriptionFieldsWrapper]            = Json.format[SubscriptionFieldsWrapper]
  given formatVersionSubscription: OFormat[VersionSubscription]                 = Json.format[VersionSubscription]

  given versionSubscriptionWithoutFieldsJsonFormatter: OFormat[VersionSubscriptionWithoutFields] = Json.format[VersionSubscriptionWithoutFields]
  given subscriptionWithoutFieldsJsonFormatter: OFormat[SubscriptionWithoutFields]               = Json.format[SubscriptionWithoutFields]

  given formatSubscriptionResponse: OFormat[SubscriptionResponse] = Json.format[SubscriptionResponse]
}

object APIDefinitionFormatters extends APIDefinitionFormatters
