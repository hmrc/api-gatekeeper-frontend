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

package uk.gov.hmrc.apiplatform.modules.events.connectors

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import play.api.libs.json.Json
import uk.gov.hmrc.apiplatform.modules.events.applications.domain.models._

object ApiPlatformEventsConnector {
  case class Config(baseUrl: String, enabled: Boolean)

  case class QueryResponse(events: Seq[AbstractApplicationEvent])

  object QueryResponse {
    import uk.gov.hmrc.apiplatform.modules.events.applications.domain.services.EventsInterServiceCallJsonFormatters._
    implicit val format = Json.format[QueryResponse]
  }

}

@Singleton
class ApiPlatformEventsConnector @Inject() (http: HttpClient, config: ApiPlatformEventsConnector.Config)(implicit val ec: ExecutionContext)
    extends ApplicationLogger {

  import ApiPlatformEventsConnector._

  val serviceBaseUrl: String         = s"${config.baseUrl}"
  private val applicationEventsUri   = s"$serviceBaseUrl/application-event"

  def fetchEventQueryValues(appId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[QueryableValues]] = {
    http.GET[Option[QueryableValues]](s"$applicationEventsUri/${appId.value}/values")
  }

  def query(appId: ApplicationId, tag: Option[EventTag])(implicit hc: HeaderCarrier): Future[Seq[AbstractApplicationEvent]] = {
    val queryParams =
      Seq(
        tag.map(et => "eventTag" -> et.toString),
      ).collect( _ match {
        case Some((a,b)) => a->b
      })

    http.GET[QueryResponse](s"$applicationEventsUri/${appId.value}", queryParams)
    .map(_.events)
  }
}
