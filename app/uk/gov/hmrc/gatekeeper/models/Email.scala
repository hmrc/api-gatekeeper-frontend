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

import play.api.libs.json.Format

import uk.gov.hmrc.apiplatform.modules.common.domain.services.SimpleEnumJsonFormatting

enum EmailOptionChoice {
  case EmailPreferences, ApiSubscription, EmailAllUsers
}

object EmailOptionChoice {

  def apply(text: String): Option[EmailOptionChoice] = {
    EmailOptionChoice.values.find(e => e.toString.equalsIgnoreCase(text))
  }

  def unsafeApply(text: String): EmailOptionChoice = apply(text).getOrElse(throw new RuntimeException(s"$text is not a valid Email Option Choice"))

  given Format[EmailOptionChoice] = SimpleEnumJsonFormatting.screamingSnakeCaseFormatFor("emailOptionChoice", apply)

  extension (eoc: EmailOptionChoice) {

    def label: String = eoc match {
      case EmailPreferences => "Email preferences"
      case ApiSubscription  => "API subscription"
      case EmailAllUsers    => "Email all users"
    }

    def hint: String = eoc match {
      case EmailPreferences => "Email users based on their preferences"
      case ApiSubscription  => "Email users mandatory information about APIs they subscribe to"
      case EmailAllUsers    => "Email all users with a Developer Hub account"
    }
  }
}

enum EmailPreferencesChoice {
  case Topic, TaxRegime, SpecificApi
}

object EmailPreferencesChoice {

  def apply(text: String): Option[EmailPreferencesChoice] = {
    EmailPreferencesChoice.values.find(e => e.toString.equalsIgnoreCase(text))
  }

  def unsafeApply(text: String): EmailPreferencesChoice = apply(text).getOrElse(throw new RuntimeException(s"$text is not a valid Email Option Choice"))

  given Format[EmailPreferencesChoice] = SimpleEnumJsonFormatting.screamingSnakeCaseFormatFor("emailPreferencesChoice", apply)

  extension (epc: EmailPreferencesChoice) {

    def label: String = epc match {
      case EmailPreferencesChoice.SpecificApi => "Users interested in a specific API or APIs"
      case EmailPreferencesChoice.TaxRegime   => "Users interested in a tax regime"
      case EmailPreferencesChoice.Topic       => "Users interested in a topic"
    }

    def hint: String = epc match {
      case EmailPreferencesChoice.SpecificApi => "For example, VAT MTD, PAYE Online"
      case EmailPreferencesChoice.TaxRegime   => "For example, Income Tax"
      case EmailPreferencesChoice.Topic       => "For example, event invites"
    }
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

  given formatTopicOptionAndChoice: Format[TopicOptionChoice] = SimpleEnumJsonFormatting.screamingSnakeCaseFormatFor[TopicOptionChoice]("Topic Option Choice", apply)

  val optionLabel: TopicOptionChoice => String = {
    case TopicOptionChoice.BUSINESS_AND_POLICY => "Business and policy"
    case TopicOptionChoice.TECHNICAL           => "Technical"
    case TopicOptionChoice.RELEASE_SCHEDULES   => "Release schedules"
    case TopicOptionChoice.EVENT_INVITES       => "Event Invites"
  }

  val optionHint: TopicOptionChoice => String = {
    case TopicOptionChoice.BUSINESS_AND_POLICY => "Policy compliance, legislative changes and business guidance support."
    case TopicOptionChoice.TECHNICAL           => "Specifications, service guides, bux fixes and known errors."
    case TopicOptionChoice.RELEASE_SCHEDULES   => "Notifications about planned releases and outages."
    case TopicOptionChoice.EVENT_INVITES       => "Get invites to knowledge share events and user research opportunities."
  }
}
