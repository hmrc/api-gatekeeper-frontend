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

import java.time.{Clock, Instant, ZoneOffset}
import java.util.UUID

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{Collaborator, Collaborators}
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.events.connectors.DisplayEvent

trait DisplayEventTestDataBuilder {

  val fixedClock: Clock = Clock.fixed(Instant.now, ZoneOffset.UTC)

  val appCollaboratorActor: Actors.AppCollaborator = Actors.AppCollaborator(LaxEmailAddress("iam@admin.com"))
  val gkActor: Actors.GatekeeperUser               = Actors.GatekeeperUser("iam@admin.com")
  val nowInstant: Instant                          = Instant.now(fixedClock)

  val userId                                           = UserId.random
  def makeAppAdministrator(counter: Int): Collaborator = Collaborators.Administrator(UserId(UUID.randomUUID()), LaxEmailAddress(s"AppAdmin-$counter"))
  def makeAppDeveloper(counter: Int): Collaborator     = Collaborators.Developer(UserId(UUID.randomUUID()), LaxEmailAddress(s"AppDev-$counter"))

  val teamMemberAddedExample: DisplayEvent = DisplayEvent(
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    gkActor,
    eventTagDescription = "Team member",
    eventType = "Collaborator Added",
    metaData = List("Added", "Blah")
  )

  def makeTeamMemberAddedEvent(appId: ApplicationId, counter: Int): DisplayEvent = {
    teamMemberAddedExample.copy(applicationId = appId, metaData = s"Flag $counter" :: teamMemberAddedExample.metaData)
  }

  def makeTeamMemberAddedEventByAdmin(appId: ApplicationId, counter: Int): DisplayEvent = {
    teamMemberAddedExample.copy(applicationId = appId, metaData = s"Flag $counter" :: teamMemberAddedExample.metaData, actor = appCollaboratorActor)
  }

  val teamMemberRemovedExample: DisplayEvent = DisplayEvent(
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    gkActor,
    eventTagDescription = "Team member",
    eventType = "Collaborator Removed",
    metaData = List("Removed", "Blah")
  )

  def makeTeamMemberRemovedEvent(appId: ApplicationId, counter: Int): DisplayEvent = {
    teamMemberRemovedExample.copy(applicationId = appId, metaData = s"Flag $counter" :: teamMemberRemovedExample.metaData)
  }

  val apiSubscribedV2Example: DisplayEvent = DisplayEvent(
    applicationId = ApplicationId.random,
    eventDateTime = nowInstant,
    gkActor,
    eventTagDescription = "Subscriptions",
    eventType = "Subscribed to API",
    metaData = List("API xxxx-v1.0", "Blah")
  )

  def makeApiSubscribedV2Event(appId: ApplicationId, counter: Int): DisplayEvent = {
    apiSubscribedV2Example.copy(applicationId = appId, metaData = s"Flag $counter" :: teamMemberRemovedExample.metaData)
  }
}
