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

package uk.gov.hmrc.apiplatform.modules.events.domain.models

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed trait EventType extends EnumEntry

object EventType extends Enum[EventType] with PlayJsonEnum[EventType]  {
  val values: scala.collection.immutable.IndexedSeq[EventType] = findValues

  case object PROD_APP_NAME_CHANGED extends EventType
  case object PROD_APP_PRIVACY_POLICY_LOCATION_CHANGED extends EventType
  case object PROD_LEGACY_APP_PRIVACY_POLICY_LOCATION_CHANGED extends EventType
  case object PROD_APP_TERMS_CONDITIONS_LOCATION_CHANGED extends EventType
  case object PROD_LEGACY_APP_TERMS_CONDITIONS_LOCATION_CHANGED extends EventType
  case object RESPONSIBLE_INDIVIDUAL_CHANGED extends EventType

  case object  API_SUBSCRIBED extends EventType
  case object  API_UNSUBSCRIBED extends EventType

  case object  CLIENT_SECRET_ADDED extends EventType
  case object  CLIENT_SECRET_REMOVED extends EventType

  case object  PPNS_CALLBACK_URI_UPDATED extends EventType

  case object  REDIRECT_URIS_UPDATED extends EventType

  case object  TEAM_MEMBER_ADDED extends EventType
  case object  TEAM_MEMBER_REMOVED extends EventType

  def describe(et: EventType): String = et match {
    case PROD_APP_NAME_CHANGED => "xxxx"
    case PROD_APP_PRIVACY_POLICY_LOCATION_CHANGED => "xxxx"
    case PROD_LEGACY_APP_PRIVACY_POLICY_LOCATION_CHANGED => "xxxx"
    case PROD_APP_TERMS_CONDITIONS_LOCATION_CHANGED => "xxxx"
    case PROD_LEGACY_APP_TERMS_CONDITIONS_LOCATION_CHANGED => "xxxx"
    case RESPONSIBLE_INDIVIDUAL_CHANGED => "xxxx"

    case API_SUBSCRIBED => "xxxx"
    case API_UNSUBSCRIBED => "xxxx"
    case CLIENT_SECRET_ADDED => "xxxx"
    case CLIENT_SECRET_REMOVED => "xxxx"
    case PPNS_CALLBACK_URI_UPDATED => "xxxx"
    case REDIRECT_URIS_UPDATED => "xxxx"
    case TEAM_MEMBER_ADDED => "Team Member Added"
    case TEAM_MEMBER_REMOVED => "Team Member Removed"
  }
}