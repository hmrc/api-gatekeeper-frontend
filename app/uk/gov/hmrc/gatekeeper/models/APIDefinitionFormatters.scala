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

  implicit val formatFieldValue: Format[FieldValue]     = Json.valueFormat[FieldValue]
  implicit val formatFieldName: Format[FieldName]       = Json.valueFormat[FieldName]
  implicit val keyReadsFieldName: KeyReads[FieldName]   = key => JsSuccess(FieldName(key))
  implicit val keyWritesFieldName: KeyWrites[FieldName] = _.value

  implicit val formatSubscriptionFieldDefinition: OFormat[SubscriptionFieldDefinition] = Json.format[SubscriptionFieldDefinition]
  implicit val formatSubscriptionFieldValue: OFormat[SubscriptionFieldValue]           = Json.format[SubscriptionFieldValue]
  implicit val formatSubscriptionFields: OFormat[SubscriptionFieldsWrapper]            = Json.format[SubscriptionFieldsWrapper]
  implicit val formatVersionSubscription: OFormat[VersionSubscription]                 = Json.format[VersionSubscription]

  implicit val versionSubscriptionWithoutFieldsJsonFormatter: OFormat[VersionSubscriptionWithoutFields] = Json.format[VersionSubscriptionWithoutFields]
  implicit val subscriptionWithoutFieldsJsonFormatter: OFormat[SubscriptionWithoutFields]               = Json.format[SubscriptionWithoutFields]

  implicit val formatSubscriptionResponse: OFormat[SubscriptionResponse] = Json.format[SubscriptionResponse]
}

object APIDefinitionFormatters extends APIDefinitionFormatters
