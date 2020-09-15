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

import model.SubscriptionFields.{SubscriptionFieldDefinition, SubscriptionFieldValue, SubscriptionFieldsWrapper}
import play.api.libs.json.{JsSuccess, Json, KeyReads, KeyWrites}

trait APIDefinitionFormatters {

  implicit val formatFieldValue = Json.valueFormat[FieldValue]
  implicit val formatFieldName = Json.valueFormat[FieldName]
  implicit val keyReadsFieldName: KeyReads[FieldName] = key => JsSuccess(FieldName(key))
  implicit val keyWritesFieldName: KeyWrites[FieldName] = _.value

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
}

object APIDefinitionFormatters extends APIDefinitionFormatters