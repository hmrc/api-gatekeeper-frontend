/*
 * Copyright 2023 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.Logging
import play.api.http.Status.{NOT_FOUND, NO_CONTENT}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, UpstreamErrorResponse, _}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.gatekeeper.connectors.XmlServicesConnector.Config
import uk.gov.hmrc.gatekeeper.models.xml.{XmlApi, XmlOrganisation}
import uk.gov.hmrc.gatekeeper.models.{
  RemoveAllCollaboratorsForUserIdFailureResult,
  RemoveAllCollaboratorsForUserIdRequest,
  RemoveAllCollaboratorsForUserIdResult,
  RemoveAllCollaboratorsForUserIdSuccessResult
}

@Singleton
class XmlServicesConnector @Inject() (config: Config, http: HttpClientV2)(implicit ec: ExecutionContext) extends Logging {

  val baseUrl = s"${config.serviceBaseUrl}/api-platform-xml-services"

  /*
   * TODO This handler can be removed once the backend is permanently deployed with no expectation of being rolled back
   * See also XmlServicesConnectorSpec
   */
  private def handleUpstream404s[A](returnIf404: A): PartialFunction[Throwable, A] = (err: Throwable) =>
    err match {
      case _: NotFoundException                                  => returnIf404
      case e: UpstreamErrorResponse if e.statusCode == NOT_FOUND => returnIf404
      case e: Throwable                                          => throw e
    }

  def getAllApis()(implicit hc: HeaderCarrier): Future[List[XmlApi]] = {
    http.get(url"$baseUrl/xml/apis").execute[List[XmlApi]]
  }

  def getApisForCategories(categories: List[String])(implicit hc: HeaderCarrier): Future[List[XmlApi]] = {
    val queryParams = categories.map("categoryFilter" -> _)
    http.get(url"$baseUrl/xml/apis/filtered?$queryParams").execute[List[XmlApi]]
      .recover(handleUpstream404s[List[XmlApi]](List.empty[XmlApi]))
  }

  def findOrganisationsByUserId(userId: UserId)(implicit hc: HeaderCarrier): Future[List[XmlOrganisation]] = {
    val userIdParams = Seq("userId" -> userId.value.toString)
    val sortByParams = Seq("sortBy" -> "ORGANISATION_NAME")

    val queryParams = userIdParams ++ sortByParams

    http.get(url"$baseUrl/organisations?$queryParams").execute[List[XmlOrganisation]]
  }

  def getAllOrganisations()(implicit hc: HeaderCarrier): Future[List[XmlOrganisation]] = {
    val sortByParams = Seq("sortBy" -> "VENDOR_ID")
    val queryParams  = sortByParams

    http.get(url"$baseUrl/organisations?$queryParams").execute[List[XmlOrganisation]]
  }

  def removeCollaboratorsForUserId(userId: UserId, gatekeeperUser: String)(implicit hc: HeaderCarrier): Future[RemoveAllCollaboratorsForUserIdResult] = {
    val request = RemoveAllCollaboratorsForUserIdRequest(userId, gatekeeperUser)
    http.post(url"$baseUrl/organisations/all/remove-collaborators")
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .map(response =>
        response.status match {
          case NO_CONTENT => RemoveAllCollaboratorsForUserIdSuccessResult
          case _          => RemoveAllCollaboratorsForUserIdFailureResult
        }
      )
  }
}

object XmlServicesConnector {
  case class Config(serviceBaseUrl: String)
}
