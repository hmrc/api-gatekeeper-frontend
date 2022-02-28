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

package connectors

import javax.inject.{Inject, Singleton}
import config.AppConfig

import model._

import encryption._

import play.api.libs.json.Json
import uk.gov.hmrc.http.{HttpResponse, HeaderCarrier}
import uk.gov.hmrc.http.HttpClient
import scala.util.control.NonFatal
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logging
import model.xml.XmlApi

@Singleton
class XmlServicesConnector @Inject()(appConfig: XmlServicesConnector.Config, http: HttpClient)
    (implicit ec: ExecutionContext) extends Logging {

  def getAllApis()(implicit hc: HeaderCarrier): Future[Either[Throwable, Seq[XmlApi]]] = {
    handleResult(http.GET[Seq[XmlApi]](url = s"${appConfig.serviceBaseUrl}/xml/apis"))
  }

    private def handleResult[A](result: Future[A]): Future[Either[Throwable, A]] = {
    result.map(x => Right(x))
      .recover {
        case NonFatal(e) => logger.error(e.getMessage)
          Left(e)
      }
  }
}

object XmlServicesConnector {
  case class Config(serviceBaseUrl: String)
}