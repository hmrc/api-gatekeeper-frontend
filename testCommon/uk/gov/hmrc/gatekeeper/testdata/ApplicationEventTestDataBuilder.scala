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


import uk.gov.hmrc.apiplatform.modules.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.events.applications.domain.models._

import java.util.UUID
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiContext
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiVersion
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{Actors, LaxEmailAddress}
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.apiplatform.modules.events.domain.models.ApplicationEventTestData
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.SubmissionId

trait ApplicationEventTestDataBuilder extends ApplicationEventTestData {
  def makeAppAdministrator(counter: Int): Collaborator = Collaborators.Administrator(UserId(UUID.randomUUID()), LaxEmailAddress(s"AppAdmin-$counter"))
  def makeAppDeveloper(counter: Int): Collaborator     = Collaborators.Developer(UserId(UUID.randomUUID()), LaxEmailAddress(s"AppDev-$counter"))

  val teamMemberAddedExample: TeamMemberAddedEvent = TeamMemberAddedEvent(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    gkActor,
    teamMemberEmail = LaxEmailAddress("jkhkhk"),
    teamMemberRole = "ADMIN"
  )

  def makeTeamMemberAddedEvent(applicationId: ApplicationId, counter: Int): TeamMemberAddedEvent = {
    val teamMember = makeAppAdministrator(counter)
    teamMemberAddedExample.copy(
      applicationId = applicationId,
      id = EventId.random,
      eventDateTime = nowInstant,
      teamMemberEmail = teamMember.emailAddress,
      teamMemberRole = "ADMIN"
    )
  }

  val teamMemberRemovedExample: TeamMemberRemovedEvent = TeamMemberRemovedEvent(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    gkActor,
    teamMemberEmail = LaxEmailAddress("jkhkhk"),
    teamMemberRole = "ADMIN"
  )

  def makeTeamMemberRemovedEvent(applicationId: ApplicationId, counter: Int): TeamMemberRemovedEvent = {
    val teamMember = makeAppAdministrator(counter)
    teamMemberRemovedExample.copy(
      applicationId = applicationId,
      id = EventId.random,
      eventDateTime = nowInstant,
      teamMemberEmail = teamMember.emailAddress,
      teamMemberRole = "ADMIN"
    )
  }



  val collaboratorAdded: CollaboratorAddedV2 = CollaboratorAddedV2(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    appCollaboratorActor,
    Collaborators.Administrator(UserId(UUID.randomUUID()), LaxEmailAddress("jkhkhk")),
    verifiedAdminsToEmail = Set(LaxEmailAddress("email"))
  )

  def makeCollaboratorAdded(applicationId: ApplicationId, counter: Int): CollaboratorAddedV2 = {
    val teamMember = makeAppAdministrator(counter)
    collaboratorAdded.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant, collaborator = teamMember)
  }

  val collaboratorRemoved: CollaboratorRemovedV2 = CollaboratorRemovedV2(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    appCollaboratorActor,
    Collaborators.Administrator(userId, LaxEmailAddress("jkhkhk")),
    verifiedAdminsToEmail = Set("email1", "email2", "email3").map(LaxEmailAddress(_))
  )

  def makeCollaboratorRemoved(applicationId: ApplicationId): CollaboratorRemovedV2 = {
    collaboratorRemoved.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant)
  }

  val clientSecretAddedExample: ClientSecretAddedEvent = ClientSecretAddedEvent(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    gkActor,
    clientSecretId = "jkhkhk"
  )

  val clientSecretAddedV2Example: ClientSecretAddedV2 = ClientSecretAddedV2(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    appCollaboratorActor,
    clientSecretId = "jkhkhk",
    clientSecretName = "****hkhk"
  )

  def makeClientSecretAddedEvent(applicationId: ApplicationId): ClientSecretAddedEvent = {
    clientSecretAddedExample.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant)
  }

  def makeClientSecretAdded(applicationId: ApplicationId): ClientSecretAddedV2 = {
    clientSecretAddedV2Example.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant)
  }

  val clientSecretRemovedExample: ClientSecretRemovedEvent = ClientSecretRemovedEvent(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    gkActor,
    clientSecretId = "jkhkhk"
  )

  val clientSecretRemovedV2Example: ClientSecretRemovedV2 = ClientSecretRemovedV2(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    appCollaboratorActor,
    clientSecretId = "jkhkhk",
    clientSecretName = "****hkhk"
  )

  def makeClientSecretRemovedEvent(applicationId: ApplicationId): ClientSecretRemovedEvent = {
    clientSecretRemovedExample.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant)
  }

  def makeClientSecretRemoved(applicationId: ApplicationId): ClientSecretRemovedV2 = {
    clientSecretRemovedV2Example.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant)
  }

  val redirectUrisUpdatedExample: RedirectUrisUpdatedEvent = RedirectUrisUpdatedEvent(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    gkActor,
    oldRedirectUris = "oldru",
    newRedirectUris = "newru"
  )

  def makeRedirectUrisUpdatedEvent(applicationId: ApplicationId): RedirectUrisUpdatedEvent = {
    redirectUrisUpdatedExample.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant)
  }

  val redirectUrisUpdatedV2Example: RedirectUrisUpdatedV2 = RedirectUrisUpdatedV2(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    appCollaboratorActor,
    oldRedirectUris = List("oldru"),
    newRedirectUris = List("newru", "newuri2")
  )

  def makeRedirectUrisUpdatedV2(applicationId: ApplicationId): RedirectUrisUpdatedV2 = {
    redirectUrisUpdatedV2Example.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant)
  }

  val apiSubscribedExample: ApiSubscribedEvent = ApiSubscribedEvent(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    gkActor,
    context = "apicontext",
    version = "1.0"
  )

  val apiSubscribedV2Example: ApiSubscribedV2 = ApiSubscribedV2(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    appCollaboratorActor,
    ApiContext("apicontext"),
    ApiVersion("1.0")
  )

  def makeApiSubscribedEvent(applicationId: ApplicationId): ApiSubscribedEvent = {
    apiSubscribedExample.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant)
  }

  def makeApiSubscribedV2(applicationId: ApplicationId): ApiSubscribedV2 = {
    apiSubscribedV2Example.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant)
  }

  val apiUnsubscribedExample: ApiUnsubscribedEvent = ApiUnsubscribedEvent(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    gkActor,
    context = "apicontext",
    version = "1.0"
  )

  val apiUnsubscribedV2Example: ApiUnsubscribedV2 = ApiUnsubscribedV2(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    appCollaboratorActor,
    ApiContext("apicontext"),
    ApiVersion("1.0")
  )

  def makeApiUnsubscribedEvent(applicationId: ApplicationId): ApiUnsubscribedEvent = {
    apiUnsubscribedExample.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant)
  }

  def makeApiUnsubscribed(applicationId: ApplicationId): ApiUnsubscribedV2 = {
    apiUnsubscribedV2Example.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant)
  }

  val ppnsCallBackUriUpdatedEvent: PpnsCallBackUriUpdatedEvent = PpnsCallBackUriUpdatedEvent(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    Actors.GatekeeperUser("iam@admin.com"),
    boxId = "boxId",
    boxName = "boxName",
    oldCallbackUrl = "some/url/",
    newCallbackUrl = "some/url/here"
  )

  def makePpnsCallBackUriUpdatedEvent(applicationId: ApplicationId, counter: Int = 1): PpnsCallBackUriUpdatedEvent = {
    ppnsCallBackUriUpdatedEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant)
  }

  val productionAppNameChangedEvent: ProductionAppNameChangedEvent = ProductionAppNameChangedEvent(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    actor = appCollaboratorActor,
    oldAppName = "old app name",
    newAppName = "new app name",
    requestingAdminEmail = LaxEmailAddress("admin@example.com")
  )

  def makeProductionAppNameChangedEvent(applicationId: ApplicationId, counter: Int = 1): ProductionAppNameChangedEvent = {
    productionAppNameChangedEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant)
  }

  val productionAppPrivacyPolicyLocationChangedEvent: ProductionAppPrivacyPolicyLocationChanged = ProductionAppPrivacyPolicyLocationChanged(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    actor = appCollaboratorActor,
    oldLocation = PrivacyPolicyLocations.InDesktopSoftware,
    newLocation = PrivacyPolicyLocations.Url("http://example.com")
  )

  def makeProductionAppPrivacyPolicyLocationChangedEvent(applicationId: ApplicationId, counter: Int = 1): ProductionAppPrivacyPolicyLocationChanged = {
    productionAppPrivacyPolicyLocationChangedEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant)
  }

  val productionLegacyAppPrivacyPolicyLocationChangedEvent: ProductionLegacyAppPrivacyPolicyLocationChanged = ProductionLegacyAppPrivacyPolicyLocationChanged(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    actor = appCollaboratorActor,
    oldUrl = "http://example.com/old",
    newUrl = "http://example.com/new"
  )

  def makeProductionLegacyAppPrivacyPolicyLocationChanged(applicationId: ApplicationId, counter: Int = 1): ProductionLegacyAppPrivacyPolicyLocationChanged = {
    productionLegacyAppPrivacyPolicyLocationChangedEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant)
  }

  val productionAppTermsConditionsLocationChangedEvent: ProductionAppTermsConditionsLocationChanged = ProductionAppTermsConditionsLocationChanged(
      id = EventId.random,
      applicationId = ApplicationId.random,
      eventDateTime = nowInstant,
      actor = appCollaboratorActor,
      oldLocation = TermsAndConditionsLocations.InDesktopSoftware,
      newLocation = TermsAndConditionsLocations.Url("http://example.com")
    )

  def makeProductionAppTermsConditionsLocationChanged(applicationId: ApplicationId): ProductionAppTermsConditionsLocationChanged = {
    productionAppTermsConditionsLocationChangedEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant)
  }

  val productionLegacyAppTermsConditionsLocationChangedEvent: ProductionLegacyAppTermsConditionsLocationChanged = ProductionLegacyAppTermsConditionsLocationChanged(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    actor = appCollaboratorActor,
    oldUrl = "http://example.com/old",
    newUrl = "http://example.com/new"
  )

  def makeProductionLegacyAppTermsConditionsLocationChanged(applicationId: ApplicationId): ProductionLegacyAppTermsConditionsLocationChanged = {
    productionLegacyAppTermsConditionsLocationChangedEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant)
  }

  val responsibleIndividualSetEvent: ResponsibleIndividualSet = ResponsibleIndividualSet(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    actor = appCollaboratorActor,
    responsibleIndividualName = "Mr Responsible",
    responsibleIndividualEmail = LaxEmailAddress("ri@example.com"),
    submissionId = SubmissionId(UUID.randomUUID().toString()),
    submissionIndex = 1,
    code = UUID.randomUUID().toString(),
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = LaxEmailAddress("admin@example.com")
  )

  def makeResponsibleIndividualSet(applicationId: ApplicationId): ResponsibleIndividualSet = {
    responsibleIndividualSetEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant)
  }

  val applicationStateChangedEvent: ApplicationStateChanged = ApplicationStateChanged(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    actor = appCollaboratorActor,
    oldAppState = "PENDING_RESPONSIBLE_INDIVIDUAL_VERIFICATION",
    newAppState = "PENDING_GATEKEEPER_APPROVAL",
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = LaxEmailAddress("admin@example.com")
  )

  def makeApplicationStateChanged(applicationId: ApplicationId): ApplicationStateChanged = {
    applicationStateChangedEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant)
  }

  val responsibleIndividualVerificationStarted: ResponsibleIndividualVerificationStarted = ResponsibleIndividualVerificationStarted(
    id = EventId.random,
    applicationId = ApplicationId.random,
    applicationName = "my app",
    eventDateTime = nowInstant,
    actor = appCollaboratorActor,
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = LaxEmailAddress("admin@example.com"),
    responsibleIndividualName = "Mr Responsible",
    responsibleIndividualEmail = LaxEmailAddress("ri@example.com"),
    submissionId = SubmissionId(UUID.randomUUID().toString()),
    submissionIndex = 1,
    verificationId = UUID.randomUUID().toString()
  )

  def makeResponsibleIndividualVerificationStarted(applicationId: ApplicationId): ResponsibleIndividualVerificationStarted = {
    responsibleIndividualVerificationStarted.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant)
  }

  val responsibleIndividualDeclinedEvent: ResponsibleIndividualDeclined = ResponsibleIndividualDeclined(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    actor = appCollaboratorActor,
    responsibleIndividualName = "Mr Responsible",
    responsibleIndividualEmail = LaxEmailAddress("ri@example.com"),
    submissionId = SubmissionId(UUID.randomUUID().toString()),
    submissionIndex = 1,
    code = "123456789",
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = LaxEmailAddress("admin@example.com")
  )

  def makeResponsibleIndividualDeclined(applicationId: ApplicationId): ResponsibleIndividualDeclined = {
    responsibleIndividualDeclinedEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant)
  }

  val responsibleIndividualDeclinedUpdateEvent: ResponsibleIndividualDeclinedUpdate = ResponsibleIndividualDeclinedUpdate(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    actor = appCollaboratorActor,
    responsibleIndividualName = "Mr Responsible",
    responsibleIndividualEmail = LaxEmailAddress("ri@example.com"),
    submissionId = SubmissionId(UUID.randomUUID().toString()),
    submissionIndex = 1,
    code = "123456789",
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = LaxEmailAddress("admin@example.com")
  )

  def makeResponsibleIndividualDeclinedUpdate(applicationId: ApplicationId): ResponsibleIndividualDeclinedUpdate = {
    responsibleIndividualDeclinedUpdateEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant)
  }

  val responsibleIndividualDidNotVerifyEvent: ResponsibleIndividualDidNotVerify = ResponsibleIndividualDidNotVerify(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    actor = Actors.AppCollaborator(LaxEmailAddress("iam@admin.com")),
    responsibleIndividualName = "Mr Responsible",
    responsibleIndividualEmail = LaxEmailAddress("ri@example.com"),
    submissionId = SubmissionId(UUID.randomUUID().toString),
    submissionIndex = 1,
    code = "123456789",
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = LaxEmailAddress("admin@example.com")
  )

  def makeResponsibleIndividualDidNotVerify(applicationId: ApplicationId): ResponsibleIndividualDidNotVerify = {
    responsibleIndividualDidNotVerifyEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant)
  }

  val applicationApprovalRequestDeclinedEvent: ApplicationApprovalRequestDeclined = ApplicationApprovalRequestDeclined(
    id = EventId.random,
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    actor = Actors.AppCollaborator(LaxEmailAddress("iam@admin.com")),
    decliningUserName = "Mr Responsible",
    decliningUserEmail = LaxEmailAddress("ri@example.com"),
    submissionId = SubmissionId(UUID.randomUUID().toString()),
    submissionIndex = 1,
    reasons = "reason text",
    requestingAdminName = "Mr Admin",
    requestingAdminEmail = LaxEmailAddress("admin@example.com")
  )

  def makeApplicationApprovalRequestDeclined(applicationId: ApplicationId): ApplicationApprovalRequestDeclined = {
    applicationApprovalRequestDeclinedEvent.copy(applicationId = applicationId, id = EventId.random, eventDateTime = nowInstant)
  }
}
