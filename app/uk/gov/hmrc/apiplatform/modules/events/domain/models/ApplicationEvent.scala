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

import java.time.LocalDateTime

sealed trait ApplicationEvent {
  def id: EventId
  def applicationId: String
  def eventDateTime: LocalDateTime
}

object ApplicationEvent {
  def asEventTypeValue(evt: ApplicationEvent): EventType = evt match {
    case _: ApiSubscribedEvent => EventType.API_SUBSCRIBED
    case _: ApiUnsubscribedEvent => EventType.API_UNSUBSCRIBED
    case _: TeamMemberAddedEvent => EventType.TEAM_MEMBER_ADDED 
    case _: TeamMemberRemovedEvent => EventType.TEAM_MEMBER_REMOVED
    case _: ClientSecretAddedEvent => EventType.CLIENT_SECRET_ADDED
    case _: ClientSecretRemovedEvent => EventType.CLIENT_SECRET_REMOVED
    case _: PpnsCallBackUriUpdatedEvent => EventType.PPNS_CALLBACK_URI_UPDATED
    case _: ProductionAppNameChangedEvent => EventType.PROD_APP_NAME_CHANGED
    case _: ProductionAppPrivacyPolicyLocationChanged => EventType.PROD_APP_PRIVACY_POLICY_LOCATION_CHANGED
    case _: ProductionAppTermsConditionsLocationChanged => EventType.PROD_APP_TERMS_CONDITIONS_LOCATION_CHANGED
    case _: ProductionLegacyAppPrivacyPolicyLocationChanged => EventType.PROD_LEGACY_APP_PRIVACY_POLICY_LOCATION_CHANGED
    case _: ProductionLegacyAppTermsConditionsLocationChanged => EventType.PROD_LEGACY_APP_TERMS_CONDITIONS_LOCATION_CHANGED
    case _: RedirectUrisUpdatedEvent => EventType.REDIRECT_URIS_UPDATED
    case _: ResponsibleIndividualSet => EventType.RESPONSIBLE_INDIVIDUAL_SET
    case _: ResponsibleIndividualChanged => EventType.RESPONSIBLE_INDIVIDUAL_CHANGED
    case _: ResponsibleIndividualChangedToSelf => EventType.RESPONSIBLE_INDIVIDUAL_CHANGED_TO_SELF
    case _: ApplicationStateChanged => EventType.APPLICATION_STATE_CHANGED
    case _: ResponsibleIndividualVerificationStarted => EventType.RESPONSIBLE_INDIVIDUAL_VERIFICATION_STARTED
    case _: ResponsibleIndividualDeclined => EventType.RESPONSIBLE_INDIVIDUAL_DECLINED
    case _: ResponsibleIndividualDeclinedUpdate => EventType.RESPONSIBLE_INDIVIDUAL_DECLINED_UPDATE
    case _: ResponsibleIndividualDidNotVerify => EventType.RESPONSIBLE_INDIVIDUAL_DID_NOT_VERIFY
    case _: ApplicationApprovalRequestDeclined => EventType.APPLICATION_APPROVAL_REQUEST_DECLINED
  }

  def extractActorText(evt: ApplicationEvent): String = evt match {
    case e: ApiSubscribedEvent => e.actor.id
    case e: ApiUnsubscribedEvent => e.actor.id
    case e: TeamMemberAddedEvent => e.actor.id
    case e: TeamMemberRemovedEvent => e.actor.id
    case e: ClientSecretAddedEvent => e.actor.id
    case e: ClientSecretRemovedEvent => e.actor.id
    case e: PpnsCallBackUriUpdatedEvent => e.actor.id
    case e: ProductionAppNameChangedEvent => Actor.extractActorText(e.actor)
    case e: ProductionAppPrivacyPolicyLocationChanged => Actor.extractActorText(e.actor)
    case e: ProductionAppTermsConditionsLocationChanged => Actor.extractActorText(e.actor)
    case e: ProductionLegacyAppPrivacyPolicyLocationChanged => Actor.extractActorText(e.actor)
    case e: ProductionLegacyAppTermsConditionsLocationChanged => Actor.extractActorText(e.actor)
    case e: RedirectUrisUpdatedEvent => e.actor.id
    case e: ResponsibleIndividualSet => Actor.extractActorText(e.actor)
    case e: ResponsibleIndividualChanged => Actor.extractActorText(e.actor)
    case e: ResponsibleIndividualChangedToSelf => Actor.extractActorText(e.actor)
    case e: ApplicationStateChanged => Actor.extractActorText(e.actor)
    case e: ResponsibleIndividualVerificationStarted => Actor.extractActorText(e.actor)
    case e: ResponsibleIndividualDeclined => Actor.extractActorText(e.actor)
    case e: ResponsibleIndividualDeclinedUpdate => Actor.extractActorText(e.actor)
    case e: ResponsibleIndividualDidNotVerify => Actor.extractActorText(e.actor)
    case e: ApplicationApprovalRequestDeclined => Actor.extractActorText(e.actor)
  }
}

sealed trait HasOldActor {
  def actor: OldActor
}

sealed trait HasActor {
  def actor: Actor  
}

case class TeamMemberAddedEvent(
  id: EventId,
  applicationId: String,
  eventDateTime: LocalDateTime,
  actor: OldActor,
  teamMemberEmail: String,
  teamMemberRole: String
) extends ApplicationEvent with HasOldActor

case class TeamMemberRemovedEvent(
  id: EventId,
  applicationId: String,
  eventDateTime: LocalDateTime,
  actor: OldActor,
  teamMemberEmail: String,
  teamMemberRole: String
  ) extends ApplicationEvent with HasOldActor

case class ClientSecretAddedEvent(
  id: EventId,
  applicationId: String,
  eventDateTime: LocalDateTime,
  actor: OldActor,
  clientSecretId: String
) extends ApplicationEvent with HasOldActor

case class ClientSecretRemovedEvent(
  id: EventId,
  applicationId: String,
  eventDateTime: LocalDateTime,
  actor: OldActor,
  clientSecretId: String
) extends ApplicationEvent with HasOldActor


case class PpnsCallBackUriUpdatedEvent(
  id: EventId,
  applicationId: String,
  eventDateTime: LocalDateTime,
  actor: OldActor,
  boxId: String,
  boxName: String,
  oldCallbackUrl: String,
  newCallbackUrl: String
) extends ApplicationEvent with HasOldActor

case class RedirectUrisUpdatedEvent(
  id: EventId,
  applicationId: String,
  eventDateTime: LocalDateTime,
  actor: OldActor,
  oldRedirectUris: String,
  newRedirectUris: String
) extends ApplicationEvent with HasOldActor

case class ApiSubscribedEvent(
  id: EventId,
  applicationId: String,
  eventDateTime: LocalDateTime,
  actor: OldActor,
  context: String,
  version: String
) extends ApplicationEvent with HasOldActor

case class ApiUnsubscribedEvent(
  id: EventId,
  applicationId: String,
  eventDateTime: LocalDateTime,
  actor: OldActor,
  context: String,
  version: String
) extends ApplicationEvent with HasOldActor

case class ProductionAppNameChangedEvent(
  id: EventId,
  applicationId: String,
  eventDateTime: LocalDateTime,
  actor: Actor,
  oldAppName: String,
  newAppName: String,
  requestingAdminEmail: String
) extends ApplicationEvent with HasActor

case class ProductionAppPrivacyPolicyLocationChanged(
  id: EventId,
  applicationId: String,
  eventDateTime: LocalDateTime,
  actor: Actor,
  oldLocation: PrivacyPolicyLocation,
  newLocation: PrivacyPolicyLocation,
  requestingAdminEmail: String
) extends ApplicationEvent with HasActor

case class ProductionLegacyAppPrivacyPolicyLocationChanged(
  id: EventId,
  applicationId: String,
  eventDateTime: LocalDateTime,
  actor: Actor,
  oldUrl: String,
  newUrl: String,
  requestingAdminEmail: String
) extends ApplicationEvent with HasActor

case class ProductionAppTermsConditionsLocationChanged(
  id: EventId,
  applicationId: String,
  eventDateTime: LocalDateTime,
  actor: Actor,
  oldLocation: TermsAndConditionsLocation,
  newLocation: TermsAndConditionsLocation,
  requestingAdminEmail: String
) extends ApplicationEvent with HasActor

case class ProductionLegacyAppTermsConditionsLocationChanged(
  id: EventId,
  applicationId: String,
  eventDateTime: LocalDateTime,
  actor: Actor,
  oldUrl: String,
  newUrl: String,
  requestingAdminEmail: String
) extends ApplicationEvent with HasActor

case class ResponsibleIndividualChanged(id: EventId,
  applicationId: String,
  eventDateTime: LocalDateTime,
  actor: Actor,
  previousResponsibleIndividualName: String,
  previousResponsibleIndividualEmail: String,
  newResponsibleIndividualName: String,
  newResponsibleIndividualEmail: String,
  submissionId: String,
  submissionIndex: Int,
  code: String,
  requestingAdminName: String,
  requestingAdminEmail: String
) extends ApplicationEvent with HasActor

case class ResponsibleIndividualChangedToSelf(id: EventId,
  applicationId: String,
  eventDateTime: LocalDateTime,
  actor: Actor,
  previousResponsibleIndividualName: String,
  previousResponsibleIndividualEmail: String,
  submissionId: String,
  submissionIndex: Int,
  requestingAdminName: String,
  requestingAdminEmail: String
) extends ApplicationEvent with HasActor

case class ResponsibleIndividualSet(id: EventId,
  applicationId: String,
  eventDateTime: LocalDateTime,
  actor: Actor,
  responsibleIndividualName: String,
  responsibleIndividualEmail: String,
  submissionId: String,
  submissionIndex: Int,
  code: String,
  requestingAdminName: String,
  requestingAdminEmail: String
) extends ApplicationEvent with HasActor

case class ApplicationStateChanged(id: EventId,
  applicationId: String,
  eventDateTime: LocalDateTime,
  actor: Actor,
  oldAppState: String,
  newAppState: String,
  requestingAdminName: String,
  requestingAdminEmail: String
) extends ApplicationEvent with HasActor

case class ResponsibleIndividualVerificationStarted(id: EventId,
  applicationId: String,
  applicationName: String,
  eventDateTime: LocalDateTime,
  actor: Actor,
  requestingAdminName: String,
  requestingAdminEmail: String,
  responsibleIndividualName: String,
  responsibleIndividualEmail: String,
  submissionId: String,
  submissionIndex: Int,
  verificationId: String
) extends ApplicationEvent with HasActor

case class ResponsibleIndividualDeclined(id: EventId,
  applicationId: String,
  eventDateTime: LocalDateTime,
  actor: Actor,
  responsibleIndividualName: String,
  responsibleIndividualEmail: String,
  submissionId: String,
  submissionIndex: Int,
  code: String,
  requestingAdminName: String,
  requestingAdminEmail: String
) extends ApplicationEvent with HasActor

case class ResponsibleIndividualDeclinedUpdate(id: EventId,
  applicationId: String,
  eventDateTime: LocalDateTime,
  actor: Actor,
  responsibleIndividualName: String,
  responsibleIndividualEmail: String,
  submissionId: String,
  submissionIndex: Int,
  code: String,
  requestingAdminName: String,
  requestingAdminEmail: String
) extends ApplicationEvent with HasActor

case class ResponsibleIndividualDidNotVerify(id: EventId,
  applicationId: String,
  eventDateTime: LocalDateTime,
  actor: Actor,
  responsibleIndividualName: String,
  responsibleIndividualEmail: String,
  submissionId: String,
  submissionIndex: Int,
  code: String,
  requestingAdminName: String,
  requestingAdminEmail: String
) extends ApplicationEvent with HasActor

case class ApplicationApprovalRequestDeclined(id: EventId,
    applicationId: String,
    eventDateTime: LocalDateTime,
    actor: Actor,
    decliningUserName: String,
    decliningUserEmail: String,
    submissionId: String,
    submissionIndex: Int,
    reasons: String,
    requestingAdminName: String,
    requestingAdminEmail: String
) extends ApplicationEvent with HasActor  