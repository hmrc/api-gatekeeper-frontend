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

import connectors.XmlServicesConnector.Config
import model.xml.XmlApi
import play.api.Logging
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class XmlServicesConnector @Inject()(config: Config, http: HttpClient)
    (implicit ec: ExecutionContext) extends Logging {

  def getAllApis()(implicit hc: HeaderCarrier): Future[Seq[XmlApi]] = {
    http.GET[Seq[XmlApi]](s"${config.serviceBaseUrl}/api-platform-xml-services/xml/apis")
  }
}

object XmlServicesConnector {
  case class Config(serviceBaseUrl: String)
}