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

package modules.sms.connectors

import com.google.inject.name.Named
import config.AppConfig
import encryption.{PayloadEncryption, SecretRequest, SendsSecretRequest}
import play.api.Logging
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.UpstreamErrorResponse.Upstream5xxResponse
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal


@Singleton
class ThirdPartyDeveloperConnector @Inject()(appConfig: AppConfig,
                                             http: HttpClient,
                                             @Named("ThirdPartyDeveloper") val payloadEncryption: PayloadEncryption)
    (implicit ec: ExecutionContext) extends Logging with SendsSecretRequest {

  def sendSms(phoneNumber: String)(implicit hc: HeaderCarrier): Future[Either[Throwable, SendSmsResponse]] = {
    secretRequest(SendSmsRequest(phoneNumber)) { request =>

      http.POST[SecretRequest, SendSmsResponse](s"${appConfig.developerBaseUrl}/notify/send-sms", request) map {
        x => Right(x)
      } recover {
        case Upstream5xxResponse(e) if e.message contains """Response body: '{"message":""" =>
          Right(SendSmsResponse(s"${e.message}"))
        case NonFatal(e) =>
          logger.error(e.getMessage)
          Left(e)
      }
    }
  }
}
