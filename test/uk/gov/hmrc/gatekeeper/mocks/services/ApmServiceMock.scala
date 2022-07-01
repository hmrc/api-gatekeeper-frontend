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

package mocks.services

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import uk.gov.hmrc.gatekeeper.services.ApmService
import uk.gov.hmrc.gatekeeper.models.ApplicationId
import uk.gov.hmrc.gatekeeper.models.applications.ApplicationWithSubscriptionData
import scala.concurrent.Future
import uk.gov.hmrc.gatekeeper.models.ApiContext
import uk.gov.hmrc.gatekeeper.models.subscriptions.ApiData
import uk.gov.hmrc.gatekeeper.models.ApiDefinitions
import uk.gov.hmrc.gatekeeper.models.Environment.Environment
import scala.concurrent.Future.{failed,successful}

trait ApmServiceMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  val mockApmService = mock[ApmService]

  object ApmServiceMock {
    object FetchApplicationById {
      private val whenClause = when(mockApmService.fetchApplicationById(*[ApplicationId])(*))

      def returns(app: ApplicationWithSubscriptionData) = 
        whenClause.thenReturn(successful(Some(app)))
      def returnsNone(app: ApplicationWithSubscriptionData) = 
        whenClause.thenReturn(successful(None))
      def failsWith(throwable: Throwable) =
        whenClause.thenReturn(failed(throwable))
    }

    def fetchAllPossibleSubscriptionsReturns(returns: Map[ApiContext, ApiData]) = {
      when(mockApmService.fetchAllPossibleSubscriptions(*[ApplicationId])(*))
        .thenReturn(Future.successful(returns))
    }

    def getAllFieldDefinitionsReturns(returns: ApiDefinitions.Alias) = {
      when(mockApmService.getAllFieldDefinitions(*[Environment])(*))
        .thenReturn(Future.successful(returns))
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
}
