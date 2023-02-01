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

package uk.gov.hmrc.gatekeeper.testdata

import java.time.LocalDateTime
import java.util.UUID

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiContext, ApiIdentifier, ApiVersion}
import uk.gov.hmrc.apiplatform.modules.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.events.applications.domain.models._

trait ApplicationEventTestDataBuilder {
  def makeAppAdministrator(counter: Int): Collaborator = Collaborators.Administrator(UUID.randomUUID().toString, LaxEmailAddress(s"AppAdmin-$counter"))
  def makeAppDeveloper(counter: Int): Collaborator     = Collaborators.Developer(UUID.randomUUID().toString, LaxEmailAddress(s"AppDev-$counter"))

  val teamMemberAddedExample: TeamMemberAddedEvent = TeamMemberAddedEvent(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    OldStyleActors.GatekeeperUser("iam@admin.com"),
    teamMemberEmail = LaxEmailAddress("jkhkhk"),
    teamMemberRole = "ADMIN"
  )

  def makeTeamMemberAddedEvent(applicationId: ApplicationId, counter: Int = 1): TeamMemberAddedEvent = {
    val teamMember = makeAppAdministrator(counter)
    teamMemberAddedExample.copy(
      applicationId = applicationId,
      id = EventId.random,
      eventDateTime = LocalDateTime.now(),
      teamMemberEmail = teamMember.email,
      teamMemberRole = "ADMIN"
    )
  }

  val collaboratorAdded: CollaboratorAdded = CollaboratorAdded(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    Actors.Collaborator(LaxEmailAddress("iam@admin.com")),
    Collaborators.Administrator("someId", LaxEmailAddress("jkhkhk")),
    verifiedAdminsToEmail = Set(LaxEmailAddress("email"))
  )

  def makeCollaboratorAdded(applicationId: ApplicationId, counter: Int = 1): CollaboratorAdded = {
    val teamMember = makeAppAdministrator(counter)
    collaboratorAdded.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now(), collaborator = teamMember)
  }

  val teamMemberRemovedExample: TeamMemberRemovedEvent = TeamMemberRemovedEvent(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    OldStyleActors.GatekeeperUser("iam@admin.com"),
    teamMemberEmail = LaxEmailAddress("jkhkhk"),
    teamMemberRole = "ADMIN"
  )

  def makeTeamMemberRemovedEvent(applicationId: ApplicationId, counter: Int = 1): TeamMemberRemovedEvent = {
    val teamMember = makeAppAdministrator(counter)
    teamMemberRemovedExample.copy(
      applicationId = applicationId,
      id = EventId.random,
      eventDateTime = LocalDateTime.now(),
      teamMemberEmail = teamMember.email,
      teamMemberRole = "ADMIN"
    )
  }

  val collaboratorRemoved: CollaboratorRemoved = CollaboratorRemoved(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    Actors.Collaborator(LaxEmailAddress("iam@admin.com")),
    Collaborators.Administrator("someId", LaxEmailAddress("jkhkhk")),
    verifiedAdminsToEmail = Set("email1", "email2", "email3").map(LaxEmailAddress(_))
  )

  def makeCollaboratorRemoved(applicationId: ApplicationId, counter: Int = 1): CollaboratorRemoved = {
    collaboratorRemoved.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val clientSecretAddedExample: ClientSecretAddedEvent = ClientSecretAddedEvent(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    OldStyleActors.GatekeeperUser("iam@admin.com"),
    clientSecretId = "jkhkhk"
  )

  val clientSecretAddedV2Example: ClientSecretAdded = ClientSecretAdded(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    Actors.Collaborator(LaxEmailAddress("iam@admin.com")),
    clientSecretId = "jkhkhk",
    clientSecretName = "****hkhk"
  )

  def makeClientSecretAddedEvent(applicationId: ApplicationId, counter: Int = 1): ClientSecretAddedEvent = {
    clientSecretAddedExample.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  def makeClientSecretAdded(applicationId: ApplicationId, counter: Int = 1): ClientSecretAdded = {
    clientSecretAddedV2Example.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val clientSecretRemovedExample: ClientSecretRemovedEvent = ClientSecretRemovedEvent(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    OldStyleActors.GatekeeperUser("iam@admin.com"),
    clientSecretId = "jkhkhk"
  )

  val clientSecretRemovedV2Example: ClientSecretRemoved = ClientSecretRemoved(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    Actors.Collaborator(LaxEmailAddress("iam@admin.com")),
    clientSecretId = "jkhkhk",
    clientSecretName = "****hkhk"
  )

  def makeClientSecretRemovedEvent(applicationId: ApplicationId, counter: Int = 1): ClientSecretRemovedEvent = {
    clientSecretRemovedExample.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  def makeClientSecretRemoved(applicationId: ApplicationId, counter: Int = 1): ClientSecretRemoved = {
    clientSecretRemovedV2Example.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val redirectUrisUpdatedExample: RedirectUrisUpdatedEvent = RedirectUrisUpdatedEvent(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    OldStyleActors.GatekeeperUser("iam@admin.com"),
    oldRedirectUris = "oldru",
    newRedirectUris = "newru"
  )

  def makeRedirectUrisUpdatedEvent(applicationId: ApplicationId, counter: Int = 1): RedirectUrisUpdatedEvent = {
    redirectUrisUpdatedExample.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val redirectUrisUpdatedV2Example: RedirectUrisUpdated = RedirectUrisUpdated(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    Actors.Collaborator(LaxEmailAddress("iam@admin.com")),
    oldRedirectUris = List("oldru"),
    newRedirectUris = List("newru", "newuri2")
  )

  def makeRedirectUrisUpdated(applicationId: ApplicationId, counter: Int = 1): RedirectUrisUpdated = {
    redirectUrisUpdatedV2Example.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val apiSubscribedExample: ApiSubscribedEvent = ApiSubscribedEvent(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    OldStyleActors.GatekeeperUser("iam@admin.com"),
    context = "apicontext",
    version = "1.0"
  )

  val apiSubscribedV2Example: ApiSubscribed = ApiSubscribed(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    Actors.Collaborator(LaxEmailAddress("iam@admin.com")),
    ApiIdentifier(ApiContext("apicontext"), ApiVersion("1.0"))
  )

  def makeApiSubscribedEvent(applicationId: ApplicationId, counter: Int = 1): ApiSubscribedEvent = {
    apiSubscribedExample.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  def makeApiSubscribed(applicationId: ApplicationId, counter: Int = 1): ApiSubscribed = {
    apiSubscribedV2Example.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val apiUnsubscribedExample: ApiUnsubscribedEvent = ApiUnsubscribedEvent(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    OldStyleActors.GatekeeperUser("iam@admin.com"),
    context = "apicontext",
    version = "1.0"
  )

  val apiUnsubscribedV2Example: ApiUnsubscribed = ApiUnsubscribed(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    Actors.Collaborator(LaxEmailAddress("iam@admin.com")),
    ApiIdentifier(ApiContext("apicontext"), ApiVersion("1.0"))
  )

  def makeApiUnsubscribedEvent(applicationId: ApplicationId, counter: Int = 1): ApiUnsubscribedEvent = {
    apiUnsubscribedExample.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  def makeApiUnsubscribed(applicationId: ApplicationId, counter: Int = 1): ApiUnsubscribed = {
    apiUnsubscribedV2Example.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val ppnsCallBackUriUpdatedEvent: PpnsCallBackUriUpdatedEvent = PpnsCallBackUriUpdatedEvent(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    OldStyleActors.GatekeeperUser("iam@admin.com"),
    boxId = "boxId",
    boxName = "boxName",
    oldCallbackUrl = "some/url/",
    newCallbackUrl = "some/url/here"
  )

  def makePpnsCallBackUriUpdatedEvent(applicationId: ApplicationId, counter: Int = 1): PpnsCallBackUriUpdatedEvent = {
    ppnsCallBackUriUpdatedEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val productionAppNameChangedEvent: ProductionAppNameChangedEvent = ProductionAppNameChangedEvent(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    actor = Actors.Collaborator(LaxEmailAddress("iam@admin.com")),
    oldAppName = "old app name",
    newAppName = "new app name",
    requestingAdminEmail = LaxEmailAddress("admin@example.com")
  )

  def makeProductionAppNameChangedEvent(applicationId: ApplicationId, counter: Int = 1): ProductionAppNameChangedEvent = {
    productionAppNameChangedEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val productionAppPrivacyPolicyLocationChangedEvent: ProductionAppPrivacyPolicyLocationChanged = ProductionAppPrivacyPolicyLocationChanged(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    actor = Actors.Collaborator(LaxEmailAddress("iam@admin.com")),
    oldLocation = PrivacyPolicyLocations.InDesktopSoftware,
    newLocation = PrivacyPolicyLocations.Url("http://example.com")
  )

  def makeProductionAppPrivacyPolicyLocationChangedEvent(applicationId: ApplicationId, counter: Int = 1): ProductionAppPrivacyPolicyLocationChanged = {
    productionAppPrivacyPolicyLocationChangedEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val productionLegacyAppPrivacyPolicyLocationChangedEvent: ProductionLegacyAppPrivacyPolicyLocationChanged = ProductionLegacyAppPrivacyPolicyLocationChanged(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    actor = Actors.Collaborator(LaxEmailAddress("iam@admin.com")),
    oldUrl = "http://example.com/old",
    newUrl = "http://example.com/new"
  )

  def makeProductionLegacyAppPrivacyPolicyLocationChanged(applicationId: ApplicationId, counter: Int = 1): ProductionLegacyAppPrivacyPolicyLocationChanged = {
    productionLegacyAppPrivacyPolicyLocationChangedEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val productionAppTermsConditionsLocationChangedEvent: ProductionAppTermsConditionsLocationChanged = ProductionAppTermsConditionsLocationChanged(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    actor = Actors.Collaborator(LaxEmailAddress("iam@admin.com")),
    oldLocation = TermsAndConditionsLocations.InDesktopSoftware,
    newLocation = TermsAndConditionsLocations.Url("http://example.com")
  )

  def makeProductionAppTermsConditionsLocationChanged(applicationId: ApplicationId, counter: Int = 1): ProductionAppTermsConditionsLocationChanged = {
    productionAppTermsConditionsLocationChangedEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val productionLegacyAppTermsConditionsLocationChangedEvent: ProductionLegacyAppTermsConditionsLocationChanged = ProductionLegacyAppTermsConditionsLocationChanged(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    actor = Actors.Collaborator(LaxEmailAddress("iam@admin.com")),
    oldUrl = "http://example.com/old",
    newUrl = "http://example.com/new"
  )

  def makeProductionLegacyAppTermsConditionsLocationChanged(applicationId: ApplicationId, counter: Int = 1): ProductionLegacyAppTermsConditionsLocationChanged = {
    productionLegacyAppTermsConditionsLocationChangedEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val responsibleIndividualChangedEvent: ResponsibleIndividualChanged = ResponsibleIndividualChanged(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    actor = Actors.Collaborator(LaxEmailAddress("iam@admin.com")),
    previousResponsibleIndividualName = "Mr Old Responsible",
    previousResponsibleIndividualEmail = LaxEmailAddress("old-ri@example.com"),
    newResponsibleIndividualName = "Mr Responsible",
    newResponsibleIndividualEmail = LaxEmailAddress("ri@example.com"),
    submissionId = UUID.randomUUID().toString,
    submissionIndex = 1,
    code = "123456789",
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = LaxEmailAddress("admin@example.com")
  )

  def makeResponsibleIndividualChanged(applicationId: ApplicationId, counter: Int = 1): ResponsibleIndividualChanged = {
    responsibleIndividualChangedEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val responsibleIndividualChangedToSelfEvent: ResponsibleIndividualChangedToSelf = ResponsibleIndividualChangedToSelf(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    actor = Actors.Collaborator(LaxEmailAddress("iam@admin.com")),
    previousResponsibleIndividualName = "Mr Old Responsible",
    previousResponsibleIndividualEmail = LaxEmailAddress("old-ri@example.com"),
    submissionId = UUID.randomUUID().toString,
    submissionIndex = 1,
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = LaxEmailAddress("admin@example.com")
  )

  def makeResponsibleIndividualChangedToSelf(applicationId: ApplicationId, counter: Int = 1): ResponsibleIndividualChangedToSelf = {
    responsibleIndividualChangedToSelfEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val responsibleIndividualSetEvent: ResponsibleIndividualSet = ResponsibleIndividualSet(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    actor = Actors.Collaborator(LaxEmailAddress("iam@admin.com")),
    responsibleIndividualName = "Mr Responsible",
    responsibleIndividualEmail = LaxEmailAddress("ri@example.com"),
    submissionId = UUID.randomUUID().toString,
    submissionIndex = 1,
    code = UUID.randomUUID().toString,
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = LaxEmailAddress("admin@example.com")
  )

  def makeResponsibleIndividualSet(applicationId: ApplicationId, counter: Int = 1): ResponsibleIndividualSet = {
    responsibleIndividualSetEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val applicationStateChangedEvent: ApplicationStateChanged = ApplicationStateChanged(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    actor = Actors.Collaborator(LaxEmailAddress("iam@admin.com")),
    oldAppState = "PENDING_RESPONSIBLE_INDIVIDUAL_VERIFICATION",
    newAppState = "PENDING_GATEKEEPER_APPROVAL",
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = LaxEmailAddress("admin@example.com")
  )

  def makeApplicationStateChanged(applicationId: ApplicationId, counter: Int = 1): ApplicationStateChanged = {
    applicationStateChangedEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val responsibleIndividualVerificationStarted: ResponsibleIndividualVerificationStarted = ResponsibleIndividualVerificationStarted(
    id = EventId.random,
    applicationId = ApplicationId.random,
    applicationName = "my app",
    eventDateTime = LocalDateTime.now(),
    actor = Actors.Collaborator(LaxEmailAddress("iam@admin.com")),
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = LaxEmailAddress("admin@example.com"),
    responsibleIndividualName = "Mr Responsible",
    responsibleIndividualEmail = LaxEmailAddress("ri@example.com"),
    submissionId = UUID.randomUUID().toString,
    submissionIndex = 1,
    verificationId = UUID.randomUUID().toString
  )

  def makeResponsibleIndividualVerificationStarted(applicationId: ApplicationId, counter: Int = 1): ResponsibleIndividualVerificationStarted = {
    responsibleIndividualVerificationStarted.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val responsibleIndividualDeclinedEvent: ResponsibleIndividualDeclined = ResponsibleIndividualDeclined(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    actor = Actors.Collaborator(LaxEmailAddress("iam@admin.com")),
    responsibleIndividualName = "Mr Responsible",
    responsibleIndividualEmail = LaxEmailAddress("ri@example.com"),
    submissionId = UUID.randomUUID().toString,
    submissionIndex = 1,
    code = "123456789",
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = LaxEmailAddress("admin@example.com")
  )

  def makeResponsibleIndividualDeclined(applicationId: ApplicationId, counter: Int = 1): ResponsibleIndividualDeclined = {
    responsibleIndividualDeclinedEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val responsibleIndividualDeclinedUpdateEvent: ResponsibleIndividualDeclinedUpdate = ResponsibleIndividualDeclinedUpdate(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    actor = Actors.Collaborator(LaxEmailAddress("iam@admin.com")),
    responsibleIndividualName = "Mr Responsible",
    responsibleIndividualEmail = LaxEmailAddress("ri@example.com"),
    submissionId = UUID.randomUUID().toString,
    submissionIndex = 1,
    code = "123456789",
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = LaxEmailAddress("admin@example.com")
  )

  def makeResponsibleIndividualDeclinedUpdate(applicationId: ApplicationId, counter: Int = 1): ResponsibleIndividualDeclinedUpdate = {
    responsibleIndividualDeclinedUpdateEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val responsibleIndividualDidNotVerifyEvent: ResponsibleIndividualDidNotVerify = ResponsibleIndividualDidNotVerify(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    actor = Actors.Collaborator(LaxEmailAddress("iam@admin.com")),
    responsibleIndividualName = "Mr Responsible",
    responsibleIndividualEmail = LaxEmailAddress("ri@example.com"),
    submissionId = UUID.randomUUID().toString,
    submissionIndex = 1,
    code = "123456789",
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = LaxEmailAddress("admin@example.com")
  )

  def makeResponsibleIndividualDidNotVerify(applicationId: ApplicationId, counter: Int = 1): ResponsibleIndividualDidNotVerify = {
    responsibleIndividualDidNotVerifyEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }

  val applicationApprovalRequestDeclinedEvent: ApplicationApprovalRequestDeclined = ApplicationApprovalRequestDeclined(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = LocalDateTime.now(),
    actor = Actors.Collaborator(LaxEmailAddress("iam@admin.com")),
    decliningUserName = "Mr Responsible",
    decliningUserEmail = LaxEmailAddress("ri@example.com"),
    submissionId = UUID.randomUUID().toString,
    submissionIndex = 1,
    reasons = "reason text",
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = LaxEmailAddress("admin@example.com")
  )

  def makeApplicationApprovalRequestDeclined(applicationId: ApplicationId, counter: Int = 1): ApplicationApprovalRequestDeclined = {
    applicationApprovalRequestDeclinedEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = LocalDateTime.now())
  }
}
