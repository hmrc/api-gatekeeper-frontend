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

object EmailOptionChoice extends Enumeration {
  type EmailOptionChoice = Value
  val EMAIL_PREFERENCES, API_SUBSCRIPTION, EMAIL_ALL_USERS = Value
  implicit val emailOptionsFormat = EnumJson.enumFormat(EmailOptionChoice)

  val optionLabel: EmailOptionChoice => String = {
    case EMAIL_PREFERENCES => "Email Preferences"
    case API_SUBSCRIPTION => "Api subscription"
    case EMAIL_ALL_USERS => "Email all users"
  }

  val optionHint: EmailOptionChoice => String = {
    case EMAIL_PREFERENCES => "Email users based on their preferences"
    case API_SUBSCRIPTION => "Email users mandatory information about APIs they subscribe to"
    case EMAIL_ALL_USERS => "Email all users with a Developer Hub account"
  }
}

object EmailPreferencesChoice extends Enumeration {
  type EmailPreferencesChoice = Value

  val TOPIC, TAX_REGIME, SPECIFIC_API = Value

  implicit val emailPreferencesChoiceFormat = EnumJson.enumFormat(EmailPreferencesChoice)

  val optionLabel: EmailPreferencesChoice => String = {
    case SPECIFIC_API => "Users interested in a specific API or APIs"
    case TAX_REGIME => "Users interested in a tax regime"
    case TOPIC => "Users interested in a topic"
  }

   val optionHint: EmailPreferencesChoice => String = {
     case SPECIFIC_API => "For Example, VAT MTD"
     case TAX_REGIME => "For Example, Income Tax"
     case TOPIC => "For Example, event invites" 
   }
}
