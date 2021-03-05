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

package mocks.service

import mocks.PaginatedApplicationResponseBuilder
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import services.ApplicationService

import scala.concurrent.Future.{failed,successful}
import model._
import org.mockito.stubbing.ScalaOngoingStubbing
import scala.concurrent.Future

trait ApplicationServiceMock extends MockitoSugar with ArgumentMatchersSugar with PaginatedApplicationResponseBuilder {
  val mockApplicationService = mock[ApplicationService]

  object SearchApplications {
    def returns(apps: ApplicationResponse*) = when(mockApplicationService.searchApplications(*, *)(*)).thenReturn(successful(buildPaginatedApplicationResponse(apps.toList)))
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
