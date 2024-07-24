/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatform.modules.deskpro.connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.StringContextOps
import play.api.http.HeaderNames.AUTHORIZATION
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.apiplatform.modules.deskpro.models.DeskproOrganisationsResponse

import uk.gov.hmrc.apiplatform.modules.deskpro.config.DeskproHorizonConfig

@Singleton
class DeskproHorizonConnector @Inject()(http: HttpClientV2, config: DeskproHorizonConfig)(implicit val ec: ExecutionContext) {
  def getOrganisations()(implicit hc: HeaderCarrier): Future[DeskproOrganisationsResponse] = {
    http.get(url"${config.deskproHorizonUrl}/api/v2/organizations")
      .withProxy
      .setHeader(AUTHORIZATION -> config.deskproHorizonApiKey)
      .execute[DeskproOrganisationsResponse]
  }
}
