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
import org.mockito.stubbing.ScalaOngoingStubbing
import scala.concurrent.Future

trait ApplicationServiceMock {
  self: MockitoSugar with ArgumentMatchersSugar =>

  import mocks.PaginatedApplicationResponseBuilder._

  val mockApplicationService = mock[ApplicationService]



  object FetchApplications {
    def returns(apps: ApplicationResponse*) = when(mockApplicationService.fetchApplications(*)).thenReturn(successful(apps.toList))
    def returnsFor(apiFilter: ApiFilter[String], envFilter: ApiSubscriptionInEnvironmentFilter, apps: ApplicationResponse*) =
        when(mockApplicationService.fetchApplications(eqTo(apiFilter), eqTo(envFilter))(*)).thenReturn(successful(apps.toList))
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

  def fetchApplicationReturns(returns: ApplicationWithHistory) = {
    when(mockApplicationService.fetchApplication(*[ApplicationId])(*))
      .thenReturn(successful(returns))
  }

  def fetchStateHistoryReturns(returns: List[StateHistory]) = {
    when(mockApplicationService.fetchStateHistory(*[ApplicationId])(*))
      .thenReturn(successful(returns))
  }

  def verifyFetchApplication(applicationId: ApplicationId) = {
    verify(mockApplicationService).fetchApplication(eqTo(applicationId))(*)
  }
  
  def verifyFetchStateHistory(applicationId: ApplicationId) = {
    verify(mockApplicationService).fetchStateHistory(eqTo(applicationId))(*)
  }

  def givenAddTeamMemberSucceeds() = 
    when(mockApplicationService.addTeamMember(*, *)(*))
    .thenReturn(successful(()))

  def givenAddTeamMemberFailsDueToExistingAlready() =
    when(mockApplicationService.addTeamMember(*, *)(*))
    .thenReturn(failed(new TeamMemberAlreadyExists))

  def givenRemoveTeamMemberSucceeds() =
    when(mockApplicationService.removeTeamMember(*, *, *)(*))
    .thenReturn(successful(ApplicationUpdateSuccessResult))

  def givenRemoveTeamMemberFailsDueToBeingLastAdmin() =
    when(mockApplicationService.removeTeamMember(*, *, *)(*))
    .thenReturn(failed(new TeamMemberLastAdmin))

  def givenTheAppWillBeReturned(application: ApplicationWithHistory): ScalaOngoingStubbing[Future[ApplicationWithHistory]] = {
    when(mockApplicationService.fetchApplication(*[ApplicationId])(*)).thenReturn(successful(application))
  }

}
