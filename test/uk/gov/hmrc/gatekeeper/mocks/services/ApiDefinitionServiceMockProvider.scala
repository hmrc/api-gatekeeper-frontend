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

import scala.concurrent.Future.{failed, successful}

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.gatekeeper.models.{ApiDefinitionGK}
import uk.gov.hmrc.gatekeeper.services.ApiDefinitionService
import uk.gov.hmrc.apiplatform.modules.common.domain.models.Environment

trait ApiDefinitionServiceMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  val mockApiDefinitionService = mock[ApiDefinitionService]

  object FetchAllApiDefinitions {

    def inProd    = Calling(Some(Environment.PRODUCTION))
    def inSandbox = Calling(Some(Environment.SANDBOX))
    def inBoth    = Calling(None)
    def inAny     = Calling()

    class Calling private (cond: Option[Environment]) {
      private val whenClause = when(mockApiDefinitionService.fetchAllApiDefinitions(cond)(*))

      def returns(apiDefinitions: ApiDefinitionGK*) = whenClause.thenReturn(successful(apiDefinitions.toList))
      def throws(throwable: Throwable)            = whenClause.thenReturn(failed(throwable))
    }

    object Calling {
      def apply() = new Calling(*)

      def apply(env: Option[Environment]) = new Calling(eqTo(env))
    }

  }

  object FetchAllDistinctApisIgnoreVersions {

    def inProd    = Calling(Some(Environment.PRODUCTION))
    def inSandbox = Calling(Some(Environment.SANDBOX))
    def inBoth    = Calling(None)
    def inAny     = Calling()

    class Calling[T] private (cond: Option[Environment]) {
      private val whenClause = when(mockApiDefinitionService.fetchAllDistinctApisIgnoreVersions(cond)(*))

      def returns(apiDefinitions: ApiDefinitionGK*) = whenClause.thenReturn(successful(apiDefinitions.toList))
      def throws(throwable: Throwable)            = whenClause.thenReturn(failed(throwable))
    }

    object Calling {
      def apply() = new Calling(*)

      def apply(env: Option[Environment]) = new Calling(eqTo(env))
    }
  }

  object Apis {
    private val whenClause = when(mockApiDefinitionService.apis(*))

    def returns(results: (ApiDefinitionGK, Environment)*) = whenClause.thenReturn(successful(results.toList))
    def throws(throwable: Throwable)                          = whenClause.thenReturn(failed(throwable))
  }
}
