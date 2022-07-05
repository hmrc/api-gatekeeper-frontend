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

import uk.gov.hmrc.gatekeeper.models.{ApiContext, ApiVersion}
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields.{Fields, SaveSubscriptionFieldsFailureResponse, SaveSubscriptionFieldsSuccessResponse}
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import uk.gov.hmrc.gatekeeper.services.SubscriptionFieldsService

import scala.concurrent.Future.successful
import uk.gov.hmrc.gatekeeper.models.applications.NewApplication

trait SubscriptionFieldsServiceMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  val mockSubscriptionFieldsService = mock[SubscriptionFieldsService]
  
  object SubscriptionFieldsServiceMock {
    object SaveFieldValues {
      def succeeds() =  
        when(mockSubscriptionFieldsService.saveFieldValues(*, *[ApiContext], *[ApiVersion], *)(*))
        .thenReturn(successful(SaveSubscriptionFieldsSuccessResponse))

      def failsWithFieldErrors(fieldErrors: Map[String, String]) =
        when(mockSubscriptionFieldsService.saveFieldValues(*, *[ApiContext], *[ApiVersion], *)(*))
        .thenReturn(successful(SaveSubscriptionFieldsFailureResponse(fieldErrors)))

      def verifyParams(application: NewApplication, apiContext: ApiContext, apiVersion: ApiVersion, fields: Fields.Alias) =
        verify(mockSubscriptionFieldsService).saveFieldValues(eqTo(application), eqTo(apiContext), eqTo(apiVersion), eqTo(fields))(*)
    }
  }
}