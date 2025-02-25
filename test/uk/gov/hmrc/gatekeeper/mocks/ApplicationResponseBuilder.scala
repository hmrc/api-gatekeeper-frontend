/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.mocks

import java.time.{Instant, LocalDateTime, ZoneOffset}

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.common.domain.models.FullName
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.submissions.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._

object ApplicationResponseBuilder {

  def buildApplication(applicationId: ApplicationId, clientId: ClientId, userId1: UserId): ApplicationWithCollaborators = {
    val standardAccess = Access.Standard(
      importantSubmissionData = Some(ImportantSubmissionData(
        organisationUrl = Some("https://www.example.com"),
        responsibleIndividual = ResponsibleIndividual(FullName("Bob Fleming"), LaxEmailAddress("bob@example.com")),
        serverLocations = Set(ServerLocation.InUK),
        termsAndConditionsLocation = TermsAndConditionsLocations.InDesktopSoftware,
        privacyPolicyLocation = PrivacyPolicyLocations.InDesktopSoftware,
        termsOfUseAcceptances = List(TermsOfUseAcceptance(
          responsibleIndividual = ResponsibleIndividual(FullName("Bob Fleming"), LaxEmailAddress("bob@example.com")),
          dateTime = LocalDateTime.parse("2022-10-08T12:24:31.123").toInstant(ZoneOffset.UTC),
          submissionId = SubmissionId.random,
          submissionInstance = 0
        ))
      ))
    )

    def buildCollaborator(userId: UserId) = {
      Collaborator(
        emailAddress = LaxEmailAddress("bob@example.com"),
        role = Collaborator.Roles.ADMINISTRATOR,
        userId = userId
      )
    }

    ApplicationWithCollaborators(
      CoreApplication(
        id = applicationId,
        clientId = clientId,
        gatewayId = "gateway-id",
        name = ApplicationName("Petes test application"),
        deployedTo = Environment.PRODUCTION,
        description = Some("Petes test application description"),
        createdOn = Instant.parse("2022-12-23T12:24:31.123Z"),
        lastAccess = Some(Instant.parse("2023-10-02T12:24:31.123Z")),
        grantLength = GrantLength.EIGHTEEN_MONTHS,
        lastAccessTokenUsage = None,
        access = standardAccess,
        state = ApplicationState(name = State.TESTING, updatedOn = Instant.parse("2022-10-08T12:24:31.123Z")),
        rateLimitTier = RateLimitTier.BRONZE,
        checkInformation = None,
        blocked = false,
        ipAllowlist = IpAllowlist(false, Set.empty),
        lastActionActor = ActorType.UNKNOWN,
        deleteRestriction = DeleteRestriction.NoRestriction
      ),
      collaborators = Set(buildCollaborator(userId1))
    )
  }

}
