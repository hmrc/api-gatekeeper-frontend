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
import java.util.UUID
import uk.gov.hmrc.gatekeeper.models.ApplicationId

trait ApplicationEventTestData {

  val teamMemberAddedModel: TeamMemberAddedEvent = TeamMemberAddedEvent(
    id = EventId.random,
    applicationId = UUID.randomUUID().toString,
    eventDateTime = LocalDateTime.now(),
    OldActor("iam@admin.com", ActorType.GATEKEEPER),
    teamMemberEmail = "jkhkhk",
    teamMemberRole = "ADMIN")

  def makeTeamMemberAddedEvent(appId: Option[String] = None): TeamMemberAddedEvent = {
    teamMemberAddedModel.copy(applicationId = appId.fold(UUID.randomUUID.toString)(identity), id = EventId.random, eventDateTime = LocalDateTime.now())  
  }
  
  val teamMemberRemovedModel: TeamMemberRemovedEvent = TeamMemberRemovedEvent(
    id = EventId.random,
    applicationId = UUID.randomUUID().toString,
    eventDateTime = LocalDateTime.now(),
    OldActor("iam@admin.com", ActorType.GATEKEEPER),
    teamMemberEmail = "jkhkhk",
    teamMemberRole = "ADMIN")

  def makeTeamMemberRemovedEvent(appId: Option[String] = None): TeamMemberRemovedEvent = {
    teamMemberRemovedModel.copy(applicationId = appId.fold(UUID.randomUUID.toString)(identity), id = EventId.random, eventDateTime = LocalDateTime.now())  
  }
  
  val clientSecretAddedModel: ClientSecretAddedEvent = ClientSecretAddedEvent(
    id = EventId.random,
    applicationId = UUID.randomUUID().toString,
    eventDateTime = LocalDateTime.now(),
    OldActor("iam@admin.com", ActorType.GATEKEEPER),
    clientSecretId = "jkhkhk")

  def makeClientSecretAddedEvent(appId: Option[String] = None): ClientSecretAddedEvent = {
    clientSecretAddedModel.copy(applicationId = appId.fold(UUID.randomUUID.toString)(identity), id = EventId.random, eventDateTime = LocalDateTime.now())  
  }
  
  val clientSecretRemovedModel: ClientSecretRemovedEvent = ClientSecretRemovedEvent(
    id = EventId.random,
    applicationId = UUID.randomUUID().toString,
    eventDateTime = LocalDateTime.now(),
    OldActor("iam@admin.com", ActorType.GATEKEEPER),
    clientSecretId = "jkhkhk")

  def makeClientSecretRemovedEvent(appId: Option[String] = None): ClientSecretRemovedEvent = {
    clientSecretRemovedModel.copy(applicationId = appId.fold(UUID.randomUUID.toString)(identity), id = EventId.random, eventDateTime = LocalDateTime.now())  
  }
  
  val redirectUrisUpdatedModel: RedirectUrisUpdatedEvent = RedirectUrisUpdatedEvent(
    id = EventId.random,
    applicationId = UUID.randomUUID().toString,
    eventDateTime = LocalDateTime.now(),
    OldActor("iam@admin.com", ActorType.GATEKEEPER),
    oldRedirectUris = "oldru",
    newRedirectUris = "newru")

  def makeRedirectUrisUpdatedEvent(appId: Option[String] = None): RedirectUrisUpdatedEvent = {
    redirectUrisUpdatedModel.copy(applicationId = appId.fold(UUID.randomUUID.toString)(identity), id = EventId.random, eventDateTime = LocalDateTime.now())  
  }
  
  val apiSubscribedModel: ApiSubscribedEvent = ApiSubscribedEvent(
    id = EventId.random,
    applicationId = UUID.randomUUID().toString,
    eventDateTime = LocalDateTime.now(),
    OldActor("iam@admin.com", ActorType.GATEKEEPER),
    context = "apicontext",
    version = "1.0")

  def makeApiSubscribedEvent(appId: Option[String] = None): ApiSubscribedEvent = {
    apiSubscribedModel.copy(applicationId = appId.fold(UUID.randomUUID.toString)(identity), id = EventId.random, eventDateTime = LocalDateTime.now())  
  }
  
  val apiUnsubscribedModel: ApiUnsubscribedEvent = ApiUnsubscribedEvent(
    id = EventId.random,
    applicationId = UUID.randomUUID().toString,
    eventDateTime = LocalDateTime.now(),
    OldActor("iam@admin.com", ActorType.GATEKEEPER),
    context = "apicontext",
    version = "1.0")

  def makeApiUnsubscribedEvent(appId: Option[String] = None): ApiUnsubscribedEvent = {
    apiUnsubscribedModel.copy(applicationId = appId.fold(UUID.randomUUID.toString)(identity), id = EventId.random, eventDateTime = LocalDateTime.now())  
  }
  
  val ppnsCallBackUriUpdatedEvent: PpnsCallBackUriUpdatedEvent = PpnsCallBackUriUpdatedEvent(
    id = EventId.random,
    applicationId = UUID.randomUUID().toString,
    eventDateTime = LocalDateTime.now(),
    OldActor("iam@admin.com", ActorType.GATEKEEPER),
    boxId = "boxId",
    boxName = "boxName",
    oldCallbackUrl = "some/url/",
    newCallbackUrl = "some/url/here")

  def makePpnsCallBackUriUpdatedEvent(appId: Option[String] = None): PpnsCallBackUriUpdatedEvent = {
    ppnsCallBackUriUpdatedEvent.copy(applicationId = appId.fold(UUID.randomUUID.toString)(identity), id = EventId.random, eventDateTime = LocalDateTime.now())  
  }
  
  val productionAppNameChangedEvent: ProductionAppNameChangedEvent = ProductionAppNameChangedEvent(
    id = EventId.random,
    applicationId = UUID.randomUUID().toString,
    eventDateTime = LocalDateTime.now(),
    actor = GatekeeperUserActor("iam@admin.com"),
    oldAppName = "old app name",
    newAppName = "new app name",
    requestingAdminEmail = "admin@example.com")

  def makeProductionAppNameChangedEvent(appId: Option[String] = None): ProductionAppNameChangedEvent = {
    productionAppNameChangedEvent.copy(applicationId = appId.fold(UUID.randomUUID.toString)(identity), id = EventId.random, eventDateTime = LocalDateTime.now())  
  }
  
  val productionAppPrivacyPolicyLocationChangedEvent: ProductionAppPrivacyPolicyLocationChanged = ProductionAppPrivacyPolicyLocationChanged(
    id = EventId.random,
    applicationId = UUID.randomUUID().toString,
    eventDateTime = LocalDateTime.now(),
    actor = CollaboratorActor("iam@admin.com"),
    oldLocation = PrivacyPolicyLocation.InDesktopSoftware,
    newLocation = PrivacyPolicyLocation.Url("http://example.com"),
    requestingAdminEmail = "admin@example.com")

  def makeProductionAppPrivacyPolicyLocationChangedEvent(appId: Option[String] = None): ProductionAppPrivacyPolicyLocationChanged = {
    productionAppPrivacyPolicyLocationChangedEvent.copy(applicationId = appId.fold(UUID.randomUUID.toString)(identity), id = EventId.random, eventDateTime = LocalDateTime.now())  
  }
  
  val productionLegacyAppPrivacyPolicyLocationChangedEvent: ProductionLegacyAppPrivacyPolicyLocationChanged = ProductionLegacyAppPrivacyPolicyLocationChanged(
    id = EventId.random,
    applicationId = UUID.randomUUID().toString,
    eventDateTime = LocalDateTime.now(),
    actor = CollaboratorActor("iam@admin.com"),
    oldUrl = "http://example.com/old",
    newUrl = "http://example.com/new",
    requestingAdminEmail = "admin@example.com")

  def makeProductionLegacyAppPrivacyPolicyLocationChanged(appId: Option[String] = None): ProductionLegacyAppPrivacyPolicyLocationChanged = {
    productionLegacyAppPrivacyPolicyLocationChangedEvent.copy(applicationId = appId.fold(UUID.randomUUID.toString)(identity), id = EventId.random, eventDateTime = LocalDateTime.now())  
  }
  
  val productionAppTermsConditionsLocationChangedEvent: ProductionAppTermsConditionsLocationChanged = ProductionAppTermsConditionsLocationChanged(
    id = EventId.random,
    applicationId = UUID.randomUUID().toString,
    eventDateTime = LocalDateTime.now(),
    actor = CollaboratorActor("iam@admin.com"),
    oldLocation = TermsAndConditionsLocation.InDesktopSoftware,
    newLocation = TermsAndConditionsLocation.Url("http://example.com"),
    requestingAdminEmail = "admin@example.com")

  def makeProductionAppTermsConditionsLocationChanged(appId: Option[String] = None): ProductionAppTermsConditionsLocationChanged = {
    productionAppTermsConditionsLocationChangedEvent.copy(applicationId = appId.fold(UUID.randomUUID.toString)(identity), id = EventId.random, eventDateTime = LocalDateTime.now())  
  }
  
  val productionLegacyAppTermsConditionsLocationChangedEvent: ProductionLegacyAppTermsConditionsLocationChanged = ProductionLegacyAppTermsConditionsLocationChanged(
    id = EventId.random,
    applicationId = UUID.randomUUID().toString,
    eventDateTime = LocalDateTime.now(),
    actor = CollaboratorActor("iam@admin.com"),
    oldUrl = "http://example.com/old",
    newUrl = "http://example.com/new",
    requestingAdminEmail = "admin@example.com")

  def makeProductionLegacyAppTermsConditionsLocationChanged(appId: Option[String] = None): ProductionLegacyAppTermsConditionsLocationChanged = {
    productionLegacyAppTermsConditionsLocationChangedEvent.copy(applicationId = appId.fold(UUID.randomUUID.toString)(identity), id = EventId.random, eventDateTime = LocalDateTime.now())  
  }
  
  val responsibleIndividualChangedEvent: ResponsibleIndividualChanged = ResponsibleIndividualChanged(
    id = EventId.random,
    applicationId = UUID.randomUUID().toString,
    eventDateTime = LocalDateTime.now(),
    actor = CollaboratorActor("iam@admin.com"),
    newResponsibleIndividualName = "Mrs Responsible",
    newResponsibleIndividualEmail = "ri@example.com",
    previousResponsibleIndividualName = "Old Mr Responsible",
    previousResponsibleIndividualEmail = "old-ri@example.com",
    submissionId = UUID.randomUUID().toString,
    submissionIndex = 1,
    code = "323423421353245234624626",
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = "admin@example.com")

  def makeResponsibleIndividualChanged(appId: Option[String] = None): ResponsibleIndividualChanged = {
    responsibleIndividualChangedEvent.copy(applicationId = appId.fold(UUID.randomUUID.toString)(identity), id = EventId.random, eventDateTime = LocalDateTime.now())  
  }
  
  val responsibleIndividualChangedToSelfEvent: ResponsibleIndividualChangedToSelf = ResponsibleIndividualChangedToSelf(
    id = EventId.random,
    applicationId = UUID.randomUUID().toString,
    eventDateTime = LocalDateTime.now(),
    actor = CollaboratorActor("iam@admin.com"),
    previousResponsibleIndividualName = "Mr Old Responsible",
    previousResponsibleIndividualEmail = "old-ri@example.com",
    submissionId = UUID.randomUUID().toString,
    submissionIndex = 1,
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = "admin@example.com")

  def makeResponsibleIndividualChangedToSelf(appId: Option[String] = None): ResponsibleIndividualChangedToSelf = {
    responsibleIndividualChangedToSelfEvent.copy(applicationId = appId.fold(UUID.randomUUID.toString)(identity), id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val responsibleIndividualSetEvent: ResponsibleIndividualSet = ResponsibleIndividualSet(
    id = EventId.random,
    applicationId = UUID.randomUUID().toString,
    eventDateTime = LocalDateTime.now(),
    actor = CollaboratorActor("iam@admin.com"),
    responsibleIndividualName = "Mr Responsible",
    responsibleIndividualEmail = "ri@example.com",
    submissionId = UUID.randomUUID().toString,
    submissionIndex = 1,
    code = UUID.randomUUID().toString,
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = "admin@example.com")

  def makeResponsibleIndividualSet(appId: Option[String] = None): ResponsibleIndividualSet = {
    responsibleIndividualSetEvent.copy(applicationId = appId.fold(UUID.randomUUID.toString)(identity), id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val applicationStateChangedEvent: ApplicationStateChanged = ApplicationStateChanged(
    id = EventId.random,
    applicationId = UUID.randomUUID().toString,
    eventDateTime = LocalDateTime.now(),
    actor = CollaboratorActor("iam@admin.com"),
    oldAppState = "PENDING_RESPONSIBLE_INDIVIDUAL_VERIFICATION",
    newAppState = "PENDING_GATEKEEPER_APPROVAL",
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = "admin@example.com")

  def makeApplicationStateChanged(appId: Option[String] = None): ApplicationStateChanged = {
    applicationStateChangedEvent.copy(applicationId = appId.fold(UUID.randomUUID.toString)(identity), id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val responsibleIndividualVerificationStarted: ResponsibleIndividualVerificationStarted = ResponsibleIndividualVerificationStarted(
    id = EventId.random,
    applicationId = UUID.randomUUID().toString,
    applicationName = "my app",
    eventDateTime = LocalDateTime.now(),
    actor = CollaboratorActor("iam@admin.com"),
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = "admin@example.com",
    responsibleIndividualName = "Mr Responsible",
    responsibleIndividualEmail = "ri@example.com",
    submissionId = UUID.randomUUID().toString,
    submissionIndex = 1,
    verificationId = UUID.randomUUID().toString)

  def makeResponsibleIndividualVerificationStarted(appId: Option[String] = None): ResponsibleIndividualVerificationStarted = {
    responsibleIndividualVerificationStarted.copy(applicationId = appId.fold(UUID.randomUUID.toString)(identity), id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val responsibleIndividualDeclinedEvent: ResponsibleIndividualDeclined = ResponsibleIndividualDeclined(
    id = EventId.random,
    applicationId = UUID.randomUUID().toString,
    eventDateTime = LocalDateTime.now(),
    actor = CollaboratorActor("iam@admin.com"),
    responsibleIndividualName = "Mr Responsible",
    responsibleIndividualEmail = "ri@example.com",
    submissionId = UUID.randomUUID().toString,
    submissionIndex = 1,
    code = "123456789",
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = "admin@example.com")

  def makeResponsibleIndividualDeclined(appId: Option[String] = None): ResponsibleIndividualDeclined = {
    responsibleIndividualDeclinedEvent.copy(applicationId = appId.fold(UUID.randomUUID.toString)(identity), id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val responsibleIndividualDeclinedUpdateEvent: ResponsibleIndividualDeclinedUpdate = ResponsibleIndividualDeclinedUpdate(
    id = EventId.random,
    applicationId = UUID.randomUUID().toString,
    eventDateTime = LocalDateTime.now(),
    actor = CollaboratorActor("iam@admin.com"),
    responsibleIndividualName = "Mr Responsible",
    responsibleIndividualEmail = "ri@example.com",
    submissionId = UUID.randomUUID().toString,
    submissionIndex = 1,
    code = "123456789",
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = "admin@example.com")

  def makeResponsibleIndividualDeclinedUpdate(appId: Option[String] = None): ResponsibleIndividualDeclinedUpdate = {
    responsibleIndividualDeclinedUpdateEvent.copy(applicationId = appId.fold(UUID.randomUUID.toString)(identity), id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val responsibleIndividualDidNotVerifyEvent: ResponsibleIndividualDidNotVerify = ResponsibleIndividualDidNotVerify(
    id = EventId.random,
    applicationId = UUID.randomUUID().toString,
    eventDateTime = LocalDateTime.now(),
    actor = CollaboratorActor("iam@admin.com"),
    responsibleIndividualName = "Mr Responsible",
    responsibleIndividualEmail = "ri@example.com",
    submissionId = UUID.randomUUID().toString,
    submissionIndex = 1,
    code = "123456789",
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = "admin@example.com")

  def makeResponsibleIndividualDidNotVerify(appId: Option[String] = None): ResponsibleIndividualDidNotVerify = {
    responsibleIndividualDidNotVerifyEvent.copy(applicationId = appId.fold(UUID.randomUUID.toString)(identity), id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val applicationApprovalRequestDeclinedEvent: ApplicationApprovalRequestDeclined = ApplicationApprovalRequestDeclined(
    id = EventId.random,
    applicationId = UUID.randomUUID().toString,
    eventDateTime = LocalDateTime.now(),
    actor = CollaboratorActor("iam@admin.com"),
    decliningUserName = "Mr Responsible",
    decliningUserEmail = "ri@example.com",
    submissionId = UUID.randomUUID().toString,
    submissionIndex = 1,
    reasons = "reason text",
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = "admin@example.com")

  def makeApplicationApprovalRequestDeclined(appId: Option[String] = None): ApplicationApprovalRequestDeclined = {
    applicationApprovalRequestDeclinedEvent.copy(applicationId = appId.fold(UUID.randomUUID.toString)(identity), id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val exampleAppId = ApplicationId.random
  private val arg = Some(exampleAppId.value)
  val examplesOfAllApplicationEvents = 
    makeApiSubscribedEvent(arg) :: 
    makeApiUnsubscribedEvent(arg) ::
    makeTeamMemberAddedEvent(arg) ::
    makeTeamMemberRemovedEvent(arg) ::
    makeClientSecretAddedEvent(arg) ::
    makeClientSecretRemovedEvent(arg) ::
    makePpnsCallBackUriUpdatedEvent(arg) ::
    makeProductionAppNameChangedEvent(arg) ::
    makeProductionAppPrivacyPolicyLocationChangedEvent(arg) ::
    makeProductionAppTermsConditionsLocationChanged(arg) ::
    makeProductionLegacyAppPrivacyPolicyLocationChanged(arg) ::
    makeProductionLegacyAppTermsConditionsLocationChanged(arg) ::
    makeRedirectUrisUpdatedEvent(arg) ::
    makeResponsibleIndividualSet(arg) ::
    makeResponsibleIndividualChanged(arg) ::
    makeResponsibleIndividualChangedToSelf(arg) ::
    makeApplicationStateChanged(arg) ::
    makeResponsibleIndividualVerificationStarted(arg) ::
    makeResponsibleIndividualDeclined(arg) ::
    makeResponsibleIndividualDeclinedUpdate(arg) ::
    makeResponsibleIndividualDidNotVerify(arg) ::
    makeApplicationApprovalRequestDeclined(arg) ::
    Nil
}

