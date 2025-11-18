/*
 * Copyright 2025 HM Revenue & Customs
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

import scala.concurrent.Future

import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse, _}

import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._

trait ApmConnectorApplicationModule extends ApmConnectorModule {
  private[this] val baseUrl = s"${config.serviceBaseUrl}/applications"

  // TODO - better return type
  // TODO - better error handling for expected errors
  def update(applicationId: ApplicationId, cmd: ApplicationCommand)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, Unit]] = {
    http.patch(url"${baseUrl}/${applicationId.value.toString()}")
      .withBody(Json.toJson(cmd))
      .execute[Either[UpstreamErrorResponse, Unit]]
  }
}
