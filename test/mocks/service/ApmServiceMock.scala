/*
 * Copyright 2020 HM Revenue & Customs
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

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.mockito.BDDMockito.`given`
import services.ApmService
import model.ApplicationId
import model.applications.ApplicationWithSubscriptionData
import scala.concurrent.Future
import model.ApiContext
import model.subscriptions.ApiData
import model.ApiDefinitions
import model.Environment.Environment

trait ApmServiceMock extends MockitoSugar with ArgumentMatchersSugar {
  val mockApmService = mock[ApmService]

  def fetchApplicationByIdReturns(returns: Option[ApplicationWithSubscriptionData]) = {
    given(mockApmService.fetchApplicationById(*[ApplicationId])(*))
      .willReturn(Future.successful(returns))
  }

  def fetchAllPossibleSubscriptionsReturns(returns: Map[ApiContext, ApiData]) = {
    given(mockApmService.fetchAllPossibleSubscriptions(*[ApplicationId])(*))
      .willReturn(Future.successful(returns))
  }

  def getAllFieldDefinitionsReturns(returns: ApiDefinitions.Alias) = {
    given(mockApmService.getAllFieldDefinitions(*[Environment])(*))
      .willReturn(Future.successful(returns))
  }

  def verifyFetchApplicationById(applicationId: ApplicationId) = {
    verify(mockApmService).fetchApplicationById(eqTo(applicationId))(*)
  }

  def verifyAllPossibleSubscriptions(applicationId: ApplicationId) = {
    verify(mockApmService).fetchAllPossibleSubscriptions(eqTo(applicationId))(*)
  }

  def verifyGetAllFieldDefinitionsReturns(environment: Environment) = {
    verify(mockApmService).getAllFieldDefinitions(eqTo(environment))(*)
  }

}
