/*
 * Copyright 2026 HM Revenue & Customs
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
import play.api.http.Status._
import play.api.libs.json.{Json, Reads, Writes}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, _}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{LaxEmailAddress, OrganisationId, UserId}
import uk.gov.hmrc.apiplatform.modules.organisations.domain.models.Organisation

@Singleton
class OrganisationConnector @Inject() (
    config: OrganisationConnector.Config,
    http: HttpClientV2
  )(implicit ec: ExecutionContext
  ) extends CommonResponseHandlers with Logging {

  import OrganisationConnector._

  def fetchOrganisationsByUserId(userId: UserId)(implicit hc: HeaderCarrier): Future[List[Organisation]] = {
    http.get(url"${config.serviceBaseUrl}/organisation/user/$userId")
      .execute[List[Organisation]]
  }

  def removeCollaboratorFromOrganisation(id: OrganisationId, userId: UserId, email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[Organisation] = {
    http.delete(url"${config.serviceBaseUrl}/organisation/${id.value}/member/$userId")
      .withBody(Json.toJson(RemoveMemberRequest(userId, email)))
      .execute[HttpResponse]
      .map(resp =>
        resp.status match {
          case OK          => resp.json.as[Organisation]
          case BAD_REQUEST => throw new BadRequestException(resp.json.as[ErrorMessage].message)
          case status      => throw new InternalServerException(s"Failed to remove user $userId from organisation $id with HTTP status $status")
        }
      )
  }
}

object OrganisationConnector {
  case class Config(serviceBaseUrl: String)

  case class RemoveMemberRequest(userId: UserId, email: LaxEmailAddress)
  implicit val writesRemoveMemberRequest: Writes[RemoveMemberRequest] = Json.writes[RemoveMemberRequest]

  case class ErrorMessage(message: String)
  implicit val readsErrorMessage: Reads[ErrorMessage] = Json.reads[ErrorMessage]
}
