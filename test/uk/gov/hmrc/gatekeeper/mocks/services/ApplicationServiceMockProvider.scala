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

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaborators, StateHistory, ValidatedApplicationName}
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, Environment, LaxEmailAddress}
import uk.gov.hmrc.gatekeeper.connectors.ApplicationConnector.AppWithSubscriptionsForCsvResponse
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.services.ApplicationService

trait ApplicationServiceMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  import mocks.PaginatedApplicationsBuilder._

  val mockApplicationService = mock[ApplicationService]

  object ApplicationServiceMock {

    object FetchApplications {
      def returns(apps: ApplicationWithCollaborators*) = when(mockApplicationService.fetchApplications(*)).thenReturn(successful(apps.toList))

      def returnsFor(apiFilter: ApiFilter[String], envFilter: ApiSubscriptionInEnvironmentFilter, apps: ApplicationWithCollaborators*) =
        when(mockApplicationService.fetchApplications(eqTo(apiFilter), eqTo(envFilter))(*)).thenReturn(successful(apps.toList))
    }

    object BlockApplication {
      def succeeds() = when(mockApplicationService.blockApplication(*, *)(*)).thenReturn(successful(ApplicationBlockSuccessResult))
    }

    object CreatePrivApp {
      def returns(result: CreatePrivAppResult) = when(mockApplicationService.createPrivApp(*, *, *, *)(*)).thenReturn(successful(result))
    }

    object SearchApplications {

      def returns(apps: ApplicationWithCollaborators*) =
        when(mockApplicationService.searchApplications(*, *)(*)).thenReturn(successful(buildPaginatedApplications(apps.toList)))
    }

    object FetchApplicationsWithSubscriptions {
      def returns(apps: AppWithSubscriptionsForCsvResponse*) = when(mockApplicationService.fetchApplicationsWithSubscriptions(*)(*)).thenReturn(successful(apps.toList))
    }

    object UpdateScopes {
      def succeeds()               = when(mockApplicationService.updateScopes(*, *, *)(*)).thenReturn(successful(UpdateScopesSuccessResult))
      def failsWithInvalidScopes() = when(mockApplicationService.updateScopes(*, *, *)(*)).thenReturn(successful(UpdateScopesInvalidScopesResult))
    }

    object ManageIpAllowlist {
      def succeeds() = when(mockApplicationService.manageIpAllowlist(*, *, *, *)(*)).thenReturn(successful(ApplicationUpdateSuccessResult))
    }

    object UpdateOverrides {
      def succeeds() = when(mockApplicationService.updateOverrides(*, *, *)(*)).thenReturn(successful(UpdateOverridesSuccessResult))
    }

    object UpdateRateLimitTier {
      def succeeds() = when(mockApplicationService.updateRateLimitTier(*, *, *)(*)).thenReturn(successful(ApplicationUpdateSuccessResult))
    }

    object UpdateGrantLength {
      def succeeds() = when(mockApplicationService.updateGrantLength(*, *, *)(*)).thenReturn(successful(ApplicationUpdateSuccessResult))
    }

    object UpdateAllowDelete {
      def succeeds() = when(mockApplicationService.updateDeleteRestriction(*[ApplicationId], *, *, *)(*)).thenReturn(successful(ApplicationUpdateSuccessResult))
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

      def verifyParams(applicationId: ApplicationId, env: Environment) =
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

      def succeeds() =
        when(mockApplicationService.validateApplicationName(*[ApplicationWithCollaborators], *[String])(*)).thenReturn(successful(ApplicationNameValidationResult.Valid))

      def invalid() =
        when(mockApplicationService.validateApplicationName(*[ApplicationWithCollaborators], *[String])(*)).thenReturn(successful(ApplicationNameValidationResult.Invalid))

      def duplicate() =
        when(mockApplicationService.validateApplicationName(*[ApplicationWithCollaborators], *[String])(*)).thenReturn(successful(ApplicationNameValidationResult.Duplicate))
    }

    object ValidateNewApplicationName {
      def succeeds() = when(mockApplicationService.validateNewApplicationName(*[Environment], *[String])(*)).thenReturn(successful(ApplicationNameValidationResult.Valid))

      def invalid() =
        when(mockApplicationService.validateNewApplicationName(*[Environment], *[String])(*)).thenReturn(successful(ApplicationNameValidationResult.Invalid))

      def duplicate() =
        when(mockApplicationService.validateNewApplicationName(*[Environment], *[String])(*)).thenReturn(successful(ApplicationNameValidationResult.Duplicate))
    }

    object UpdateApplicationName {

      def succeeds() =
        when(mockApplicationService.updateApplicationName(*[ApplicationWithCollaborators], *[LaxEmailAddress], *[String], *[ValidatedApplicationName])(*)).thenReturn(successful(
          ApplicationUpdateSuccessResult
        ))

      def fails() =
        when(mockApplicationService.updateApplicationName(*[ApplicationWithCollaborators], *[LaxEmailAddress], *[String], *[ValidatedApplicationName])(*)).thenReturn(successful(
          ApplicationUpdateFailureResult
        ))
    }

    object FetchProdAppStateHistories {

      def thenReturn(applicationStateHistoryChanges: ApplicationStateHistoryChange*) =
        when(mockApplicationService.fetchProdAppStateHistories()(*)).thenReturn(successful(applicationStateHistoryChanges.toList))
    }
  }
}
