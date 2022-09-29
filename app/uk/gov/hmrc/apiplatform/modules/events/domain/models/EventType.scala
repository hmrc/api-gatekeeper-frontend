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
  case object RESPONSIBLE_INDIVIDUAL_SET extends EventType
  case object RESPONSIBLE_INDIVIDUAL_CHANGED extends EventType
  case object RESPONSIBLE_INDIVIDUAL_CHANGED_TO_SELF extends EventType
  case object APPLICATION_STATE_CHANGED extends EventType
  case object RESPONSIBLE_INDIVIDUAL_VERIFICATION_STARTED extends EventType
  case object RESPONSIBLE_INDIVIDUAL_DECLINED extends EventType
  case object RESPONSIBLE_INDIVIDUAL_DECLINED_UPDATE extends EventType
  case object RESPONSIBLE_INDIVIDUAL_DID_NOT_VERIFY extends EventType
  case object APPLICATION_APPROVAL_REQUEST_DECLINED extends EventType

  case object  API_SUBSCRIBED extends EventType
  case object  API_UNSUBSCRIBED extends EventType

  case object  CLIENT_SECRET_ADDED extends EventType
  case object  CLIENT_SECRET_REMOVED extends EventType

  case object  PPNS_CALLBACK_URI_UPDATED extends EventType

  case object  REDIRECT_URIS_UPDATED extends EventType

  case object  TEAM_MEMBER_ADDED extends EventType
  case object  TEAM_MEMBER_REMOVED extends EventType

  private val toDescription: Map[EventType, String] = Map(
    PROD_APP_NAME_CHANGED -> "Application Name Changed",
    PROD_APP_PRIVACY_POLICY_LOCATION_CHANGED -> "Privacy Policy Changed",
    PROD_LEGACY_APP_PRIVACY_POLICY_LOCATION_CHANGED -> "Legacy Privacy Policy Changed",
    PROD_APP_TERMS_CONDITIONS_LOCATION_CHANGED -> "T&Cs Location Changed",
    PROD_LEGACY_APP_TERMS_CONDITIONS_LOCATION_CHANGED -> "Legacy T&Cs Location Changed",
    RESPONSIBLE_INDIVIDUAL_SET -> "Responsible Individual Set",
    RESPONSIBLE_INDIVIDUAL_CHANGED -> "Responsible Individual Changed",
    RESPONSIBLE_INDIVIDUAL_CHANGED_TO_SELF -> "Responsible Individual Changed to Self",
    APPLICATION_STATE_CHANGED -> "Application State Changed",
    RESPONSIBLE_INDIVIDUAL_VERIFICATION_STARTED -> "Responsible Individual Verification Started",
    RESPONSIBLE_INDIVIDUAL_DECLINED -> "Responsible Individual Declined",
    RESPONSIBLE_INDIVIDUAL_DECLINED_UPDATE -> "Responsible Individual Declined Update",
    RESPONSIBLE_INDIVIDUAL_DID_NOT_VERIFY -> "Responsible Individual Did Not Verify",
    APPLICATION_APPROVAL_REQUEST_DECLINED -> "Application Approval Request Declined",

    API_SUBSCRIBED -> "Subscribed to API",
    API_UNSUBSCRIBED -> "Unsubscribed from API",
    CLIENT_SECRET_ADDED -> "Client Secret Added",
    CLIENT_SECRET_REMOVED -> "Client Secret Removed",
    PPNS_CALLBACK_URI_UPDATED -> "PPNS Callback URI Updated",
    REDIRECT_URIS_UPDATED -> "Redirect URIs Updated",
    TEAM_MEMBER_ADDED -> "Team Member Added",
    TEAM_MEMBER_REMOVED -> "Team Member Removed"    
  )

  def describe(et: EventType): String = toDescription(et)

  def fromDescription(in: String): Option[EventType] = toDescription.find { case (k,v) => v == in }.map(_._1)
}