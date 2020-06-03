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

import model.{Application, ApplicationWithHistory, Subscription}
import org.mockito.BDDMockito.`given`
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.verify
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future
import services.SubscriptionFieldsService
import model.SubscriptionFields.{Fields, SaveSubscriptionFieldsSuccessResponse, SaveSubscriptionFieldsResponse}

trait SubscriptionFieldsServiceMock extends MockitoSugar {

  val mockSubscriptionFieldsService = mock[SubscriptionFieldsService]
  
  def givenSaveSubscriptionFieldsSuccess() = {
    given(mockSubscriptionFieldsService.saveFieldValues(any(), any(), any(), any())(any[HeaderCarrier]))
        .willReturn(Future.successful(SaveSubscriptionFieldsSuccessResponse))
  }

  def verifySaveSubscriptionFields(application: Application, apiContext: String, apiVersion: String, fields: Fields) = {
    verify(mockSubscriptionFieldsService).saveFieldValues(eqTo(application), eqTo(apiContext), eqTo(apiVersion), eqTo(fields))(any[HeaderCarrier])
  }
}
