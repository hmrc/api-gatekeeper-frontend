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

package uk.gov.hmrc.apiplatform.modules.events.domain.services

import play.api.libs.json.{OFormat, Format, Json}
import uk.gov.hmrc.play.json.Union
import uk.gov.hmrc.apiplatform.modules.events.domain.models._

trait EventJsonFormatters {

  implicit val eventIdFormat: Format[EventId] = Json.valueFormat[EventId]
  implicit val oldActorFormat: OFormat[OldActor] = Json.format[OldActor]

  implicit val teamMemberAddedEventFormats: OFormat[TeamMemberAddedEvent] = Json.format[TeamMemberAddedEvent]
  implicit val teamMemberRemovedEventFormats: OFormat[TeamMemberRemovedEvent] = Json.format[TeamMemberRemovedEvent]
  implicit val clientSecretAddedEventFormats: OFormat[ClientSecretAddedEvent] = Json.format[ClientSecretAddedEvent]
  implicit val clientSecretRemovedEventFormats: OFormat[ClientSecretRemovedEvent] = Json.format[ClientSecretRemovedEvent]
  implicit val urisUpdatedEventFormats: OFormat[RedirectUrisUpdatedEvent] = Json.format[RedirectUrisUpdatedEvent]
  implicit val apiSubscribedEventFormats: OFormat[ApiSubscribedEvent] = Json.format[ApiSubscribedEvent]
  implicit val apiUnsubscribedEventFormats: OFormat[ApiUnsubscribedEvent] = Json.format[ApiUnsubscribedEvent]
  implicit val PpnsCallBackUriUpdatedEventFormats: OFormat[PpnsCallBackUriUpdatedEvent] = Json.format[PpnsCallBackUriUpdatedEvent]

  implicit val gatekeeperUserActorFormat: OFormat[GatekeeperUserActor] = Json.format[GatekeeperUserActor]
  implicit val collaboratorActorFormat: OFormat[CollaboratorActor] = Json.format[CollaboratorActor]
  implicit val formatActor: OFormat[Actor] = Union.from[Actor]("actorType")
    .and[GatekeeperUserActor](ActorType.GATEKEEPER.toString)
    .and[CollaboratorActor](ActorType.COLLABORATOR.toString)
    .format

  implicit val productionAppNameChangedEventFormats: OFormat[ProductionAppNameChangedEvent] = Json.format[ProductionAppNameChangedEvent]
  implicit val productionAppPrivacyPolicyLocationChangedFormats: OFormat[ProductionAppPrivacyPolicyLocationChanged] = Json.format[ProductionAppPrivacyPolicyLocationChanged]
  implicit val productionLegacyAppPrivacyPolicyLocationChangedFormats: OFormat[ProductionLegacyAppPrivacyPolicyLocationChanged] = Json.format[ProductionLegacyAppPrivacyPolicyLocationChanged]
  implicit val productionAppTermsConditionsLocationChangedFormats: OFormat[ProductionAppTermsConditionsLocationChanged] = Json.format[ProductionAppTermsConditionsLocationChanged]
  implicit val productionLegacyAppTermsConditionsLocationChangedFormats: OFormat[ProductionLegacyAppTermsConditionsLocationChanged] = Json.format[ProductionLegacyAppTermsConditionsLocationChanged]
  implicit val responsibleIndividualChangedFormats: OFormat[ResponsibleIndividualChanged] = Json.format[ResponsibleIndividualChanged]

  implicit val formatApplicationEvent: OFormat[ApplicationEvent] = Union.from[ApplicationEvent]("eventType")
    .and[ProductionAppNameChangedEvent](EventType.PROD_APP_NAME_CHANGED.toString)
    .and[ProductionAppPrivacyPolicyLocationChanged](EventType.PROD_APP_PRIVACY_POLICY_LOCATION_CHANGED.toString)
    .and[ProductionLegacyAppPrivacyPolicyLocationChanged](EventType.PROD_LEGACY_APP_PRIVACY_POLICY_LOCATION_CHANGED.toString)
    .and[ProductionAppTermsConditionsLocationChanged](EventType.PROD_APP_TERMS_CONDITIONS_LOCATION_CHANGED.toString)
    .and[ProductionLegacyAppTermsConditionsLocationChanged](EventType.PROD_LEGACY_APP_TERMS_CONDITIONS_LOCATION_CHANGED.toString)
    .and[ResponsibleIndividualChanged](EventType.RESPONSIBLE_INDIVIDUAL_CHANGED.toString)
    .and[TeamMemberAddedEvent](EventType.TEAM_MEMBER_ADDED.toString)
    .and[TeamMemberRemovedEvent](EventType.TEAM_MEMBER_REMOVED.toString)
    .and[ClientSecretAddedEvent](EventType.CLIENT_SECRET_ADDED.toString)
    .and[ClientSecretRemovedEvent](EventType.CLIENT_SECRET_REMOVED.toString)
    .and[RedirectUrisUpdatedEvent](EventType.REDIRECT_URIS_UPDATED.toString)
    .and[PpnsCallBackUriUpdatedEvent](EventType.PPNS_CALLBACK_URI_UPDATED.toString)
    .and[ApiSubscribedEvent](EventType.API_SUBSCRIBED.toString)
    .and[ApiUnsubscribedEvent](EventType.API_UNSUBSCRIBED.toString)
    .format
}

object EventJsonFormatters extends EventJsonFormatters
