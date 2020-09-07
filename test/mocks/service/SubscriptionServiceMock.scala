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

import model.Application
import model.SubscriptionFields.{Fields, SaveSubscriptionFieldsFailureResponse, SaveSubscriptionFieldsSuccessResponse}
import org.mockito.BDDMockito.`given`
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import services.SubscriptionFieldsService

import scala.concurrent.Future

trait SubscriptionFieldsServiceMock extends MockitoSugar with ArgumentMatchersSugar {
  val mockSubscriptionFieldsService = mock[SubscriptionFieldsService]
  
  def givenSaveSubscriptionFieldsSuccess() = {
    given(mockSubscriptionFieldsService.saveFieldValues(*, *, *, *)(*))
        .willReturn(Future.successful(SaveSubscriptionFieldsSuccessResponse))
  }

  def givenSaveSubscriptionFieldsFailure(fieldErrors : Map[String, String]) = {
    given(mockSubscriptionFieldsService.saveFieldValues(*, *, *, *)(*))
        .willReturn(Future.successful(SaveSubscriptionFieldsFailureResponse(fieldErrors)))
  }

  def verifySaveSubscriptionFields(application: Application, apiContext: String, apiVersion: String, fields: Fields) = {
    verify(mockSubscriptionFieldsService).saveFieldValues(eqTo(application), eqTo(apiContext), eqTo(apiVersion), eqTo(fields))(*)
  }
}
