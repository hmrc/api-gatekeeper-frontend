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
import services.ApplicationService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait ApplicationServiceMock extends MockitoSugar {

  val mockApplicationService = mock[ApplicationService]

  def fetchApplicationReturns(returns: ApplicationWithHistory) = {
    given(mockApplicationService.fetchApplication(any())(any[HeaderCarrier]))
      .willReturn(Future.successful(returns))
  }

  def fetchApplicationSubscriptionsReturns(returns: Seq[Subscription]) = {
    given(mockApplicationService.fetchApplicationSubscriptions(any(), any())(any[HeaderCarrier]))
      .willReturn(Future.successful(returns))
  }

  def verifyFetchApplication(applicationId: String) = {
    verify(mockApplicationService).fetchApplication(eqTo(applicationId))(any[HeaderCarrier])
  }

  def verifyFetchApplicationSubscriptions(application: Application, withFields: Boolean) = {
    verify(mockApplicationService).fetchApplicationSubscriptions(eqTo(application), eqTo(withFields))(any[HeaderCarrier])
  }

  def givenTheSubscriptionsWillBeReturned(application: Application, withFields: Boolean, returns: Seq[Subscription]) = {
    given(mockApplicationService.fetchApplicationSubscriptions(eqTo(application), eqTo(withFields))((any[HeaderCarrier])))
      .willReturn(Future.successful(returns))
  }
}
