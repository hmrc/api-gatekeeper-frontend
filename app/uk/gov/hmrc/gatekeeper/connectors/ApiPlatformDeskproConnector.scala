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
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, _}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.gatekeeper.models.organisations.{DeskproOrganisation, OrganisationId}

@Singleton
class ApiPlatformDeskproConnector @Inject() (config: ApiPlatformDeskproConnector.Config, http: HttpClientV2)(implicit ec: ExecutionContext) extends Logging {

  def getOrganisation(organisationId: OrganisationId)(implicit hc: HeaderCarrier): Future[DeskproOrganisation] = {
    http.get(url"${config.serviceBaseUrl}/organisation/${organisationId.value}").execute[DeskproOrganisation]
  }

  def getOrganisationsForUser(userEmailAddress: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[List[DeskproOrganisation]] = {
    http.post(url"${config.serviceBaseUrl}/organisation/query")
      .withBody(Json.toJson(ApiPlatformDeskproConnector.GetOrganisationsForUserRequest(userEmailAddress)))
      .execute[List[DeskproOrganisation]]
  }
}

object ApiPlatformDeskproConnector {
  case class Config(serviceBaseUrl: String)

  case class GetOrganisationsForUserRequest(email: LaxEmailAddress)

  object GetOrganisationsForUserRequest {
    implicit val format: OFormat[GetOrganisationsForUserRequest] = Json.format[GetOrganisationsForUserRequest]
  }
}
