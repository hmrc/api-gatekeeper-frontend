/*
 * Copyright 2021 HM Revenue & Customs
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

package mocks.services

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import services.ApplicationService

import scala.concurrent.Future.{failed,successful}
import model._
import scala.concurrent.Future

trait ApplicationServiceMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  import mocks.PaginatedApplicationResponseBuilder._

  val mockApplicationService = mock[ApplicationService]

  object ApplicationServiceMock {

    object FetchApplications {
      def returns(apps: ApplicationResponse*) = when(mockApplicationService.fetchApplications(*)).thenReturn(successful(apps.toList))
      def returnsFor(apiFilter: ApiFilter[String], envFilter: ApiSubscriptionInEnvironmentFilter, apps: ApplicationResponse*) =
          when(mockApplicationService.fetchApplications(eqTo(apiFilter), eqTo(envFilter))(*)).thenReturn(successful(apps.toList))
    }

    object BlockApplication {
      def succeeds() = when(mockApplicationService.blockApplication(*,*)(*)).thenReturn(successful(ApplicationBlockSuccessResult))
    }
    
    object CreatePrivOrROPCApp {
      def returns(result: CreatePrivOrROPCAppResult) = when(mockApplicationService.createPrivOrROPCApp(*, *, *, *, *)(*)).thenReturn(successful(result))
    }

    object SearchApplications {
      def returns(apps: ApplicationResponse*) = when(mockApplicationService.searchApplications(*, *)(*)).thenReturn(successful(buildPaginatedApplicationResponse(apps.toList)))
    }

    object UpdateScopes {
      def succeeds() = when(mockApplicationService.updateScopes(*, *)(*)).thenReturn(successful(UpdateScopesSuccessResult))
      def failsWithInvalidScopes() =  when(mockApplicationService.updateScopes(*, *)(*)).thenReturn(successful(UpdateScopesInvalidScopesResult)) 
    }

    object ManageIpAllowlist {
      def succeeds() = when(mockApplicationService.manageIpAllowlist(*, *, *)(*)).thenReturn(successful(UpdateIpAllowlistSuccessResult))
    }

    object UpdateOverrides {
      def succeeds() = when(mockApplicationService.updateOverrides(*, *)(*)).thenReturn(successful(UpdateOverridesSuccessResult))
    }

    object UpdateRateLimitTier {
      def succeeds() = when(mockApplicationService.updateRateLimitTier(*, *)(*)).thenReturn(successful(ApplicationUpdateSuccessResult))
    }
    
    object UnblockApplication {
      def succeeds() = when(mockApplicationService.unblockApplication(*, *)(*)).thenReturn(successful(ApplicationUnblockSuccessResult))
    }

    object SubscribeToApi {
      def succeeds() = when(mockApplicationService.subscribeToApi(*, *[ApiContext], *[ApiVersion])(*)).thenReturn(Future.successful(ApplicationUpdateSuccessResult))
    }

    object UnsubscribeFromApi {
      def succeeds() = when(mockApplicationService.unsubscribeFromApi(*, *[ApiContext], *[ApiVersion])(*)).thenReturn(Future.successful(ApplicationUpdateSuccessResult))
    }

    object FetchApplication {
      def returns(app: ApplicationWithHistory) =
        when(mockApplicationService.fetchApplication(*[ApplicationId])(*)).thenReturn(successful(app))

      def verifyParams(applicationId: ApplicationId) =
        verify(mockApplicationService).fetchApplication(eqTo(applicationId))(*)
    }

    object FetchStateHistory {
      def returns(stateHistory: StateHistory*) = 
        when(mockApplicationService.fetchStateHistory(*[ApplicationId])(*)).thenReturn(successful(stateHistory.toList))

      def verifyParams(applicationId: ApplicationId) =
        verify(mockApplicationService).fetchStateHistory(eqTo(applicationId))(*)
    }

    object AddTeamMember {
      def succeeds() = when(mockApplicationService.addTeamMember(*, *)(*)).thenReturn(successful(()))

      def failsDueToExistingAlready() = 
        when(mockApplicationService.addTeamMember(*, *)(*))
        .thenReturn(failed(TeamMemberAlreadyExists))
    }

    object RemoveTeamMember {
      def succeeds() = 
        when(mockApplicationService.removeTeamMember(*, *, *)(*))
      .thenReturn(successful(ApplicationUpdateSuccessResult))

      def failsDueToLastAdmin() =
        when(mockApplicationService.removeTeamMember(*, *, *)(*))
        .thenReturn(failed(TeamMemberLastAdmin))
    }
  }
}
