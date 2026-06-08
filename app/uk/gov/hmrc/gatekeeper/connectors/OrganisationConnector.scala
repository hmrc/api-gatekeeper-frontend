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
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, _}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.apiplatform.modules.organisations.domain.models.Organisation

@Singleton
class OrganisationConnector @Inject() (
    config: OrganisationConnector.Config,
    http: HttpClientV2
  )(implicit ec: ExecutionContext
  ) extends CommonResponseHandlers with Logging {

  def fetchOrganisationsByUserId(userId: UserId)(implicit hc: HeaderCarrier): Future[List[Organisation]] = {
    http.get(url"${config.serviceBaseUrl}/organisation/user/$userId")
      .execute[List[Organisation]]
  }
}

object OrganisationConnector {
  case class Config(serviceBaseUrl: String)
}
