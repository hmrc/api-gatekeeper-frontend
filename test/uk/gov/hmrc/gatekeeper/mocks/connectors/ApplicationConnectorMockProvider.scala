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

package mocks.connectors

import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import scala.concurrent.Future.{failed, successful}
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.connectors.{ApplicationConnector, ProductionApplicationConnector, SandboxApplicationConnector}
import play.api.http.Status._
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId

trait ApplicationConnectorMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  val mockProductionApplicationConnector = mock[ProductionApplicationConnector]
  val mockSandboxApplicationConnector    = mock[SandboxApplicationConnector]

  trait ApplicationConnectorMock {
    def mock: ApplicationConnector

    import mocks.PaginatedApplicationResponseBuilder._

    object SearchApplications {
      def returns(apps: ApplicationResponse*) = when(mock.searchApplications(*)(*)).thenReturn(successful(buildPaginatedApplicationResponse(apps.toList)))
    }

    object SearchCollaborators {
      def returns(emails: String*) = when(mock.searchCollaborators(*[ApiContext], *[ApiVersion], *)(*)).thenReturn(successful(emails.toList))

      def returnsFor(apiContext: ApiContext, apiVersion: ApiVersion, partialEmailMatch: Option[String])(collaborators: String*) =
        when(mock.searchCollaborators(eqTo(apiContext), eqTo(apiVersion), eqTo(partialEmailMatch))(*))
          .thenReturn(successful(collaborators.toList))
    }

    object FetchAllApplications {
      def returns(apps: ApplicationResponse*) = when(mock.fetchAllApplications()(*)).thenReturn(successful(apps.toList))
    }

    object FetchAllApplicationsWithStateHistories {

      def returns(applicationStateHistories: ApplicationStateHistory*) = when(mock.fetchAllApplicationsWithStateHistories()(*))
        .thenReturn(successful(applicationStateHistories.toList))
    }

    object FetchAllApplicationsWithNoSubscriptions {
      def returns(apps: ApplicationResponse*) = when(mock.fetchAllApplicationsWithNoSubscriptions()(*)).thenReturn(successful(apps.toList))
    }

    object FetchAllApplicationsBySubscription {
      def returns(apps: ApplicationResponse*) = when(mock.fetchAllApplicationsBySubscription(*, *)(*)).thenReturn(successful(apps.toList))
    }

    object UnsubscribeFromApi {

      def succeeds() = when(mock.unsubscribeFromApi(*[ApplicationId], *[ApiContext], *[ApiVersion])(*))
        .thenReturn(successful(ApplicationUpdateSuccessResult))
    }

    object FetchApplication {
      def returns(app: ApplicationWithHistory) = when(mock.fetchApplication(*[ApplicationId])(*)).thenReturn(successful(app))

      def failsNotFound() =
        when(mock.fetchApplication(*[ApplicationId])(*)).thenReturn(failed(UpstreamErrorResponse("Not Found", NOT_FOUND)))
    }

    object FetchApplicationsByUserId {
      def returns(apps: ApplicationResponse*) = when(mock.fetchApplicationsByUserId(*[UserId])(*)).thenReturn(successful(apps.toList))
    }

    object FetchApplicationsExcludingDeletedByUserId {
      def returns(apps: ApplicationResponse*) = when(mock.fetchApplicationsExcludingDeletedByUserId(*[UserId])(*)).thenReturn(successful(apps.toList))
    }

    object RemoveCollaborator {
      def succeeds() = when(mock.removeCollaborator(*[ApplicationId], *, *, *)(*)).thenReturn(successful(ApplicationUpdateSuccessResult))

      def succeedsFor(id: ApplicationId, memberToRemove: String, requestingUser: String) =
        when(mock.removeCollaborator(eqTo(id), eqTo(memberToRemove), eqTo(requestingUser), *)(*))
          .thenReturn(successful(ApplicationUpdateSuccessResult))

      def failsWithLastAdminFor(id: ApplicationId, memberToRemove: String, requestingUser: String) =
        when(mock.removeCollaborator(eqTo(id), eqTo(memberToRemove), eqTo(requestingUser), *)(*))
          .thenReturn(failed(TeamMemberLastAdmin))
    }

    object UpdateScopes {
      def succeeds() = when(mock.updateScopes(*[ApplicationId], *)(*)).thenReturn(successful(UpdateScopesSuccessResult))
    }

    object UpdateIpAllowlist {
      def succeeds()     = when(mock.updateIpAllowlist(*[ApplicationId], *, *)(*)).thenReturn(successful(UpdateIpAllowlistSuccessResult))
      def failsWithISE() = when(mock.updateIpAllowlist(*[ApplicationId], *, *)(*)).thenReturn(failed(UpstreamErrorResponse("Error", 500)))
    }

    object UnblockApplication {
      def succeeds() = when(mock.unblockApplication(*[ApplicationId], *)(*)).thenReturn(successful(ApplicationUnblockSuccessResult))
      def fails()    = when(mock.unblockApplication(*[ApplicationId], *)(*)).thenReturn(successful(ApplicationUnblockFailureResult))
    }

    object BlockApplication {
      def succeeds() = when(mock.blockApplication(*[ApplicationId], *)(*)).thenReturn(successful(ApplicationBlockSuccessResult))
      def fails()    = when(mock.blockApplication(*[ApplicationId], *)(*)).thenReturn(successful(ApplicationBlockFailureResult))
    }

    object DeleteApplication {
      def succeeds() = when(mock.deleteApplication(*[ApplicationId], *)(*)).thenReturn(successful(ApplicationUpdateSuccessResult))
      def fails()    = when(mock.deleteApplication(*[ApplicationId], *)(*)).thenReturn(successful(ApplicationUpdateFailureResult))
    }

    object CreatePrivOrROPCApp {
      def returns(result: CreatePrivOrROPCAppResult) = when(mock.createPrivOrROPCApp(*)(*)).thenReturn(successful(result))
    }
  }

  object ApplicationConnectorMock {

    object Prod extends ApplicationConnectorMock {
      val mock = mockProductionApplicationConnector
    }

    object Sandbox extends ApplicationConnectorMock {
      val mock = mockSandboxApplicationConnector
    }
  }
}
