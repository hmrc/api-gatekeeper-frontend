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

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiDefinition
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithSubscriptionFields
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, _}
import uk.gov.hmrc.gatekeeper.models.ApiDefinitionFields
import uk.gov.hmrc.gatekeeper.services.ApmService

trait ApmServiceMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  val mockApmService = mock[ApmService]

  object ApmServiceMock {

    object FetchApplicationById {
      private val whenClause = when(mockApmService.fetchApplicationById(*[ApplicationId])(*))

      def returns(app: ApplicationWithSubscriptionFields) =
        whenClause.thenReturn(successful(Some(app)))

      def returnsNone(app: ApplicationWithSubscriptionFields) =
        whenClause.thenReturn(successful(None))

      def failsWith(throwable: Throwable) =
        whenClause.thenReturn(failed(throwable))
    }

    object FetchNonOpenApiDefinitions {
      private val whenClause = when(mockApmService.fetchNonOpenApis(*[Environment])(*))

      def returns(apiDefinitions: ApiDefinition*) = whenClause.thenReturn(successful(apiDefinitions.toList))
    }

    def fetchAllPossibleSubscriptionsReturns(returns: List[ApiDefinition]) = {
      when(mockApmService.fetchAllPossibleSubscriptions(*[ApplicationId])(*))
        .thenReturn(Future.successful(returns))
    }

    def getAllFieldDefinitionsReturns(returns: ApiDefinitionFields.Alias) = {
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
