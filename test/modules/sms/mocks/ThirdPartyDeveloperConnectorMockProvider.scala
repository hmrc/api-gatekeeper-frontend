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

package modules.sms.mocks

import modules.sms.connectors.{SendSmsResponse, ThirdPartyDeveloperConnector}
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future.successful

trait ThirdPartyDeveloperConnectorMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  val mockThirdPartyDeveloperConnector = mock[ThirdPartyDeveloperConnector]

  object ThirdPartyDeveloperConnectorMock {

    object SendSms {

      def returnsSendSmsResponse(sendSmsResponse: SendSmsResponse) = when(mockThirdPartyDeveloperConnector.sendSms(*)(*))
        .thenReturn(successful(Right(sendSmsResponse)))

      def returnsError = when(mockThirdPartyDeveloperConnector.sendSms(*)(*))
        .thenReturn(successful(Left(UpstreamErrorResponse("error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR, Map.empty))))
    }
  }

}
