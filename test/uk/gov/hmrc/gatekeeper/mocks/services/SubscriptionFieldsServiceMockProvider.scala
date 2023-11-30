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

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationResponse
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields.{Fields, SaveSubscriptionFieldsFailureResponse, SaveSubscriptionFieldsSuccessResponse}
import uk.gov.hmrc.gatekeeper.services.SubscriptionFieldsService

trait SubscriptionFieldsServiceMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  val mockSubscriptionFieldsService = mock[SubscriptionFieldsService]

  object SubscriptionFieldsServiceMock {

    object SaveFieldValues {

      def succeeds() =
        when(mockSubscriptionFieldsService.saveFieldValues(*, *[ApiContext], *[ApiVersionNbr], *)(*))
          .thenReturn(successful(SaveSubscriptionFieldsSuccessResponse))

      def failsWithFieldErrors(fieldErrors: Map[String, String]) =
        when(mockSubscriptionFieldsService.saveFieldValues(*, *[ApiContext], *[ApiVersionNbr], *)(*))
          .thenReturn(successful(SaveSubscriptionFieldsFailureResponse(fieldErrors)))

      def verifyParams(application: ApplicationResponse, apiContext: ApiContext, apiVersion: ApiVersionNbr, fields: Fields.Alias) =
        verify(mockSubscriptionFieldsService).saveFieldValues(eqTo(application), eqTo(apiContext), eqTo(apiVersion), eqTo(fields))(*)
    }
  }
}
