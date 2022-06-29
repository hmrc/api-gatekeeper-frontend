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
import model.UserId
import model.xml.{XmlOrganisation, XmlApi}
import play.api.Logging
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class XmlServicesConnector @Inject()(config: Config, http: HttpClient)
    (implicit ec: ExecutionContext) extends Logging {

  val baseUrl = s"${config.serviceBaseUrl}/api-platform-xml-services"

  def getAllApis()(implicit hc: HeaderCarrier): Future[Seq[XmlApi]] = {
    http.GET[Seq[XmlApi]](s"$baseUrl/xml/apis")
  }

  def findOrganisationsByUserId(userId: UserId)
                               (implicit hc: HeaderCarrier): Future[List[XmlOrganisation]] = {
    val userIdParams = Seq("userId" -> userId.value.toString)
    val sortByParams = Seq("sortBy" -> "ORGANISATION_NAME")

    val params = userIdParams ++ sortByParams

    http.GET[List[XmlOrganisation]](url = s"$baseUrl/organisations", queryParams = params)
  }

}

object XmlServicesConnector {
  case class Config(serviceBaseUrl: String)
}