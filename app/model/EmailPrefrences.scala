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

package model

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import play.api.libs.json.Json

case class TaxRegimeInterests(regime: String, services: Set[String])
object TaxRegimeInterests {
  implicit val formatTaxRegimeInterests = Json.format[TaxRegimeInterests]
}

case class EmailPreferences(interests: List[TaxRegimeInterests], topics: Set[EmailTopic])
object EmailPreferences {
  implicit val formatEmailPreferences = Json.format[EmailPreferences]

  def noPreferences: EmailPreferences = EmailPreferences(List.empty, Set.empty)
}

sealed trait EmailTopic extends EnumEntry

object EmailTopic extends Enum[EmailTopic] with PlayJsonEnum[EmailTopic] {

  val values = findValues

  case object BUSINESS_AND_POLICY extends EmailTopic
  case object TECHNICAL extends EmailTopic
  case object RELEASE_SCHEDULES extends EmailTopic
  case object EVENT_INVITES extends EmailTopic
}
