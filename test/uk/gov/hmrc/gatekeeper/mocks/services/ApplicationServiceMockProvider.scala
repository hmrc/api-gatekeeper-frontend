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

package mocks.services

import scala.concurrent.Future.successful

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.services.ApplicationService

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
      def succeeds() = when(mockApplicationService.blockApplication(*, *)(*)).thenReturn(successful(ApplicationBlockSuccessResult))
    }

    object CreatePrivOrROPCApp {
      def returns(result: CreatePrivOrROPCAppResult) = when(mockApplicationService.createPrivOrROPCApp(*, *, *, *, *)(*)).thenReturn(successful(result))
    }

    object SearchApplications {
      def returns(apps: ApplicationResponse*) = when(mockApplicationService.searchApplications(*, *)(*)).thenReturn(successful(buildPaginatedApplicationResponse(apps.toList)))
    }

    object FetchApplicationsWithSubscriptions {
      def returns(apps: ApplicationWithSubscriptionsResponse*) = when(mockApplicationService.fetchApplicationsWithSubscriptions(*)(*)).thenReturn(successful(apps.toList))
    }

    object UpdateScopes {
      def succeeds()               = when(mockApplicationService.updateScopes(*, *)(*)).thenReturn(successful(UpdateScopesSuccessResult))
      def failsWithInvalidScopes() = when(mockApplicationService.updateScopes(*, *)(*)).thenReturn(successful(UpdateScopesInvalidScopesResult))
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

    object UpdateGrantLength {
      def succeeds() = when(mockApplicationService.updateGrantLength(*, *)(*)).thenReturn(successful(ApplicationUpdateSuccessResult))
    }

    object UnblockApplication {
      def succeeds() = when(mockApplicationService.unblockApplication(*, *)(*)).thenReturn(successful(ApplicationUnblockSuccessResult))
    }

    object FetchApplication {

      def returns(app: ApplicationWithHistory) =
        when(mockApplicationService.fetchApplication(*[ApplicationId])(*)).thenReturn(successful(app))

      def verifyParams(applicationId: ApplicationId) =
        verify(mockApplicationService).fetchApplication(eqTo(applicationId))(*)
    }

    object FetchStateHistory {

      def returns(stateHistory: StateHistory*) =
        when(mockApplicationService.fetchStateHistory(*[ApplicationId], *)(*)).thenReturn(successful(stateHistory.toList))

      def verifyParams(applicationId: ApplicationId, env: Environment.Environment) =
        verify(mockApplicationService).fetchStateHistory(eqTo(applicationId), eqTo(env))(*)
    }

    object DoesApplicationHaveSubmissions {

      def succeedsFalse() =
        when(mockApplicationService.doesApplicationHaveSubmissions(*[ApplicationId])(*))
          .thenReturn(successful(false))
    }

    object DoesApplicationHaveTermsOfUseInvitation {

      def succeedsFalse() =
        when(mockApplicationService.doesApplicationHaveTermsOfUseInvitation(*[ApplicationId])(*))
          .thenReturn(successful(false))
    }

    object ValidateApplicationName {
      def succeeds() = when(mockApplicationService.validateApplicationName(*[ApplicationResponse], *[String])(*)).thenReturn(successful(ValidateApplicationNameSuccessResult))

      def invalid() = when(mockApplicationService.validateApplicationName(*[ApplicationResponse], *[String])(*)).thenReturn(successful(ValidateApplicationNameFailureInvalidResult))

      def duplicate() =
        when(mockApplicationService.validateApplicationName(*[ApplicationResponse], *[String])(*)).thenReturn(successful(ValidateApplicationNameFailureDuplicateResult))
    }

    object UpdateApplicationName {

      def succeeds() =
        when(mockApplicationService.updateApplicationName(*[ApplicationResponse], *[LaxEmailAddress], *[String], *[String])(*)).thenReturn(successful(ApplicationUpdateSuccessResult))

      def fails() =
        when(mockApplicationService.updateApplicationName(*[ApplicationResponse], *[LaxEmailAddress], *[String], *[String])(*)).thenReturn(successful(ApplicationUpdateFailureResult))
    }

    object FetchProdAppStateHistories {

      def thenReturn(applicationStateHistoryChanges: ApplicationStateHistoryChange*) =
        when(mockApplicationService.fetchProdAppStateHistories()(*)).thenReturn(successful(applicationStateHistoryChanges.toList))
    }
  }
}
