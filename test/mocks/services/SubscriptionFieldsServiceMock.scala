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

import model.{ApiContext, ApiVersion}
import model.SubscriptionFields.{Fields, SaveSubscriptionFieldsFailureResponse, SaveSubscriptionFieldsSuccessResponse}
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import services.SubscriptionFieldsService

import scala.concurrent.Future.{failed,successful}
import model.applications.NewApplication

trait SubscriptionFieldsServiceMock extends MockitoSugar with ArgumentMatchersSugar {
  val mockSubscriptionFieldsService = mock[SubscriptionFieldsService]
  
  def givenSaveSubscriptionFieldsSuccess() = {
    when(mockSubscriptionFieldsService.saveFieldValues(*, *[ApiContext], *[ApiVersion], *)(*))
        .thenReturn(successful(SaveSubscriptionFieldsSuccessResponse))
  }

  def givenSaveSubscriptionFieldsFailure(fieldErrors : Map[String, String]) = {
    when(mockSubscriptionFieldsService.saveFieldValues(*, *[ApiContext], *[ApiVersion], *)(*))
        .thenReturn(successful(SaveSubscriptionFieldsFailureResponse(fieldErrors)))
  }

  def verifySaveSubscriptionFields(application: NewApplication, apiContext: ApiContext, apiVersion: ApiVersion, fields: Fields.Alias) = {
    verify(mockSubscriptionFieldsService).saveFieldValues(eqTo(application), eqTo(apiContext), eqTo(apiVersion), eqTo(fields))(*)
  }
}
