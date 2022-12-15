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
import uk.gov.hmrc.gatekeeper.connectors.ProxiedHttpClient
import uk.gov.hmrc.gatekeeper.models.Environment

object ApiPlatformEventsConnector {

  case class QueryResponse(events: List[AbstractApplicationEvent])

  object QueryResponse {
    import uk.gov.hmrc.apiplatform.modules.events.applications.domain.services.EventsInterServiceCallJsonFormatters._
    implicit val format = Json.format[QueryResponse]
  }

}

@Singleton
class EnvironmentAwareApiPlatformEventsConnector @Inject() (subordinate: SubordinateApiPlatformEventsConnector, principal: PrincipalApiPlatformEventsConnector) {

  protected def connectorFor(deployedTo: String): ApiPlatformEventsConnector = deployedTo match {
    case "PRODUCTION" => principal
    case "SANDBOX"    => subordinate
  }

  def fetchQueryableEventTags(appId: ApplicationId, deployedTo: String)(implicit hc: HeaderCarrier): Future[List[EventTag]] = connectorFor(deployedTo).fetchQueryableEventTags(appId)

  def query(appId: ApplicationId, deployedTo: String, tag: Option[EventTag])(implicit hc: HeaderCarrier): Future[List[AbstractApplicationEvent]] =
    connectorFor(deployedTo).query(appId, tag)
}

abstract class ApiPlatformEventsConnector(implicit ec: ExecutionContext) extends ApplicationLogger {
  protected val httpClient: HttpClient
  val environment: Environment.Environment
  val serviceBaseUrl: String

  def http: HttpClient

  import ApiPlatformEventsConnector._

  private lazy val applicationEventsUri = s"$serviceBaseUrl/application-event"

  def fetchQueryableEventTags(appId: ApplicationId)(implicit hc: HeaderCarrier): Future[List[EventTag]] = {
    http.GET[Option[QueryableValues]](s"$applicationEventsUri/${appId.value.toString()}/values")
      .map {
        case None     => List.empty
        case Some(qv) => qv.eventTags
      }
  }

  def query(appId: ApplicationId, tag: Option[EventTag])(implicit hc: HeaderCarrier): Future[List[AbstractApplicationEvent]] = {
    val queryParams =
      Seq(
        tag.map(et => "eventTag" -> et.toString)
      ).collect(_ match {
        case Some((a, b)) => a -> b
      })

    http.GET[Option[QueryResponse]](s"$applicationEventsUri/${appId.value.toString()}", queryParams)
      .map {
        case None           => List.empty
        case Some(response) => response.events
      }
  }
}

object SubordinateApiPlatformEventsConnector {

  case class Config(
      serviceBaseUrl: String,
      useProxy: Boolean,
      bearerToken: String,
      apiKey: String
    )
}

@Singleton
class SubordinateApiPlatformEventsConnector @Inject() (
    val config: SubordinateApiPlatformEventsConnector.Config,
    val httpClient: HttpClient,
    val proxiedHttpClient: ProxiedHttpClient
  )(implicit val ec: ExecutionContext
  ) extends ApiPlatformEventsConnector {

  import config._
  val serviceBaseUrl: String = config.serviceBaseUrl
  val environment            = Environment.SANDBOX

  val http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient

}

object PrincipalApiPlatformEventsConnector {

  case class Config(
      serviceBaseUrl: String
    )
}

@Singleton
class PrincipalApiPlatformEventsConnector @Inject() (val config: PrincipalApiPlatformEventsConnector.Config, val httpClient: HttpClient)(implicit val ec: ExecutionContext)
    extends ApiPlatformEventsConnector {

  val http: HttpClient = httpClient

  val environment    = Environment.PRODUCTION
  val serviceBaseUrl = config.serviceBaseUrl
}
