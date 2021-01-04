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

import model.{Application, ApplicationId, ApplicationWithHistory, Subscription}
import org.mockito.BDDMockito.`given`
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import services.ApplicationService

import scala.concurrent.Future
import model.StateHistory

trait ApplicationServiceMock extends MockitoSugar with ArgumentMatchersSugar {
  val mockApplicationService = mock[ApplicationService]

  def fetchApplicationReturns(returns: ApplicationWithHistory) = {
    given(mockApplicationService.fetchApplication(*[ApplicationId])(*))
      .willReturn(Future.successful(returns))
  }

  def fetchStateHistoryReturns(returns: Seq[StateHistory]) = {
    given(mockApplicationService.fetchStateHistory(*[ApplicationId])(*))
      .willReturn(Future.successful(returns))
  }

  def verifyFetchApplication(applicationId: ApplicationId) = {
    verify(mockApplicationService).fetchApplication(eqTo(applicationId))(*)
  }
  
  def verifyFetchStateHistory(applicationId: ApplicationId) = {
    verify(mockApplicationService).fetchStateHistory(eqTo(applicationId))(*)
  }
}
