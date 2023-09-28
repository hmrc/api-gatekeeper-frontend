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

import play.api.libs.json.Json

import uk.gov.hmrc.apiplatform.modules.common.domain.services.SealedTraitJsonFormatting

// TODO - Remove Enumeration
object EmailOptionChoice extends Enumeration {
  type EmailOptionChoice = Value
  val EMAIL_PREFERENCES, API_SUBSCRIPTION, EMAIL_ALL_USERS = Value
  implicit val emailOptionsFormat                          = Json.formatEnum(EmailOptionChoice)

  val optionLabel: EmailOptionChoice => String = {
    case EMAIL_PREFERENCES => "Email preferences"
    case API_SUBSCRIPTION  => "API subscription"
    case EMAIL_ALL_USERS   => "Email all users"
  }

  val optionHint: EmailOptionChoice => String = {
    case EMAIL_PREFERENCES => "Email users based on their preferences"
    case API_SUBSCRIPTION  => "Email users mandatory information about APIs they subscribe to"
    case EMAIL_ALL_USERS   => "Email all users with a Developer Hub account"
  }
}

object EmailPreferencesChoice extends Enumeration {
  type EmailPreferencesChoice = Value

  val TOPIC, TAX_REGIME, SPECIFIC_API = Value

  implicit val emailPreferencesChoiceFormat = Json.formatEnum(EmailPreferencesChoice)

  val optionLabel: EmailPreferencesChoice => String = {
    case SPECIFIC_API => "Users interested in a specific API or APIs"
    case TAX_REGIME   => "Users interested in a tax regime"
    case TOPIC        => "Users interested in a topic"
  }

  val optionHint: EmailPreferencesChoice => String = {
    case SPECIFIC_API => "For example, VAT MTD, PAYE Online"
    case TAX_REGIME   => "For example, Income Tax"
    case TOPIC        => "For example, event invites"
  }
}

sealed trait TopicOptionChoice {
  lazy val optionLabel = TopicOptionChoice.optionLabel(this)
  lazy val optionHint  = TopicOptionChoice.optionHint(this)
}

object TopicOptionChoice {
  case object BUSINESS_AND_POLICY extends TopicOptionChoice
  case object TECHNICAL           extends TopicOptionChoice
  case object RELEASE_SCHEDULES   extends TopicOptionChoice
  case object EVENT_INVITES       extends TopicOptionChoice

  val values = Set[TopicOptionChoice](BUSINESS_AND_POLICY, TECHNICAL, RELEASE_SCHEDULES, EVENT_INVITES)

  def apply(text: String): Option[TopicOptionChoice] = {
    TopicOptionChoice.values.find(_.toString == text.toUpperCase)
  }

  def unsafeApply(text: String): TopicOptionChoice =
    apply(text).getOrElse(throw new RuntimeException(s"$text is not a valid Topic Option Choice"))

  implicit val formatTopicOptionAndChoice = SealedTraitJsonFormatting.createFormatFor[TopicOptionChoice]("Topic Option Choice", apply)

  val optionLabel: TopicOptionChoice => String = {
    case BUSINESS_AND_POLICY => "Business and policy"
    case TECHNICAL           => "Technical"
    case RELEASE_SCHEDULES   => "Release schedules"
    case EVENT_INVITES       => "Event Invites"
  }

  val optionHint: TopicOptionChoice => String = {
    case BUSINESS_AND_POLICY => "Policy compliance, legislative changes and business guidance support."
    case TECHNICAL           => "Specifications, service guides, bux fixes and known errors."
    case RELEASE_SCHEDULES   => "Notifications about planned releases and outages."
    case EVENT_INVITES       => "Get invites to knowledge share events and user research opportunities."
  }
}
