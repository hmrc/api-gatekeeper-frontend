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

package uk.gov.hmrc.gatekeeper.connectors

import uk.gov.hmrc.gatekeeper.connectors.XmlServicesConnector.Config
import uk.gov.hmrc.gatekeeper.models.UserId
import uk.gov.hmrc.gatekeeper.models.xml.{XmlOrganisation, XmlApi}
import play.api.Logging
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, NotFoundException, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class XmlServicesConnector @Inject()(config: Config, http: HttpClient)
    (implicit ec: ExecutionContext) extends Logging {

  val baseUrl = s"${config.serviceBaseUrl}/api-platform-xml-services"

  /*
   * TODO This handler can be removed once the backend is permanently deployed with no expectation of being rolled back
   * See also XmlServicesConnectorSpec
   */
  private def handleUpstream404s[A](returnIf404: A): PartialFunction[Throwable, A] = (err: Throwable) => err match {
    case _: NotFoundException => returnIf404
    case e: UpstreamErrorResponse if e.statusCode == NOT_FOUND => returnIf404
    case e: Throwable => throw e
  }
  
  def getAllApis()(implicit hc: HeaderCarrier): Future[List[XmlApi]] = {
    http.GET[List[XmlApi]](s"$baseUrl/xml/apis")
  }

  def getApisForCategories(categories: List[String])(implicit hc: HeaderCarrier): Future[List[XmlApi]] = {
    http.GET[List[XmlApi]](s"$baseUrl/xml/apis/filtered", queryParams = categories.map("categoryFilter" -> _))
      .recover(handleUpstream404s[List[XmlApi]](List.empty[XmlApi]))
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