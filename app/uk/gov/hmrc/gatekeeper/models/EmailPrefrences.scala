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

case class TaxRegimeInterests(regime: String, services: Set[String])

object TaxRegimeInterests {
  implicit val formatTaxRegimeInterests: OFormat[TaxRegimeInterests] = Json.format[TaxRegimeInterests]
}

sealed trait EmailTopic

object EmailTopic {
  case object BUSINESS_AND_POLICY extends EmailTopic
  case object TECHNICAL           extends EmailTopic
  case object RELEASE_SCHEDULES   extends EmailTopic
  case object EVENT_INVITES       extends EmailTopic

  val values: ListSet[EmailTopic] = ListSet[EmailTopic](BUSINESS_AND_POLICY, TECHNICAL, RELEASE_SCHEDULES, EVENT_INVITES)

  def apply(text: String): Option[EmailTopic] = EmailTopic.values.find(_.toString() == text.toUpperCase)

  import play.api.libs.json.Format
  import uk.gov.hmrc.apiplatform.modules.common.domain.services.SealedTraitJsonFormatting
  implicit val format: Format[EmailTopic] = SealedTraitJsonFormatting.createFormatFor[EmailTopic]("Email Topic", EmailTopic.apply)
}

case class EmailPreferences(interests: List[TaxRegimeInterests], topics: Set[EmailTopic])

object EmailPreferences {
  implicit val formatEmailPreferences: OFormat[EmailPreferences] = Json.format[EmailPreferences]

  def noPreferences: EmailPreferences = EmailPreferences(interests = List.empty[TaxRegimeInterests], Set.empty[EmailTopic])
}
