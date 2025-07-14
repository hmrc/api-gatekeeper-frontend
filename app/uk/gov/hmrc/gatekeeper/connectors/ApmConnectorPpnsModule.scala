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

import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, _}

trait ApmConnectorPpnsModule extends ApmConnectorModule {
  import uk.gov.hmrc.apiplatform.modules.common.domain.services.InstantJsonFormatter.WithTimeZone._
  import play.api.libs.json._
  import uk.gov.hmrc.gatekeeper.models.pushpullnotifications._

  private[this] val baseUrl = s"${config.serviceBaseUrl}/push-pull-notifications"

  private[this] implicit val readsBoxId: Format[BoxId]                = Json.valueFormat[BoxId]
  private[this] implicit val readsBoxCreator: Reads[BoxCreator]       = Json.reads[BoxCreator]
  private[this] implicit val readsBoxSubscriber: Reads[BoxSubscriber] = Json.reads[BoxSubscriber]
  private[this] implicit val readsBox: Reads[Box]                     = Json.reads[Box]

  def fetchAllBoxes()(implicit hc: HeaderCarrier): Future[List[Box]] = {
    http.get(url"${baseUrl}/boxes")
      .execute[List[Box]]
  }
}
