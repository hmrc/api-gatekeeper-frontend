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
import play.api.libs.json.{Format, Json}

sealed trait ApiType extends EnumEntry

object ApiType extends Enum[ApiType] with PlayJsonEnum[ApiType] {
  val values = findValues
  case object REST_API extends ApiType
  case object XML_API  extends ApiType
}

case class CombinedApiCategory(value: String) extends AnyVal

object CombinedApiCategory {
  implicit val categoryFormat: Format[CombinedApiCategory]                 = Json.format[CombinedApiCategory]

  def toAPICategory(combinedApiCategory: CombinedApiCategory): APICategory = {
    APICategory(combinedApiCategory.value)
  }
}

//TODO -change accessType from being an option when APM version which starts returning this data
// is deployed to production
case class CombinedApi(displayName: String, serviceName: String, categories: List[CombinedApiCategory], apiType: ApiType, accessType: Option[APIAccessType])

object CombinedApi {
  implicit val formatCombinedApi = Json.format[CombinedApi]
}
