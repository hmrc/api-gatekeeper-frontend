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

import scala.collection.immutable.ListSet

import play.api.libs.json.{Json, OFormat}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiAccessType, ApiCategory}

sealed trait ApiType {}

object ApiType {
  case object REST_API extends ApiType
  case object XML_API  extends ApiType

  val values: ListSet[ApiType] = ListSet[ApiType](REST_API, XML_API)

  def apply(text: String): Option[ApiType] = ApiType.values.find(_.toString() == text.toUpperCase)

  import play.api.libs.json.Format
  import uk.gov.hmrc.apiplatform.modules.common.domain.services.SealedTraitJsonFormatting
  implicit val format: Format[ApiType] = SealedTraitJsonFormatting.createFormatFor[ApiType]("API Type", ApiType.apply)
}

//TODO -change accessType from being an option when APM version which starts returning this data
// is deployed to production
case class CombinedApi(displayName: String, serviceName: String, categories: Set[ApiCategory], apiType: ApiType, accessType: Option[ApiAccessType])

object CombinedApi {
  implicit val formatCombinedApi: OFormat[CombinedApi] = Json.format[CombinedApi]
}
