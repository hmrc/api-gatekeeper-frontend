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

package uk.gov.hmrc.apiplatform.modules.events.connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, _}

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.gatekeeper.connectors.EbridgeConfigurator

object ApiPlatformEventsConnector {

  case class QueryResponse(events: List[DisplayEvent])

  object QueryResponse {
    implicit val format: OFormat[QueryResponse] = Json.format[QueryResponse]
  }

}

@Singleton
class EnvironmentAwareApiPlatformEventsConnector @Inject() (subordinate: SubordinateApiPlatformEventsConnector, principal: PrincipalApiPlatformEventsConnector) {

  protected def connectorFor(deployedTo: Environment): ApiPlatformEventsConnector = deployedTo match {
    case Environment.PRODUCTION => principal
    case Environment.SANDBOX    => subordinate
  }

  def fetchQueryableValues(appId: ApplicationId, deployedTo: Environment)(implicit hc: HeaderCarrier): Future[QueryableValues] =
    connectorFor(deployedTo).fetchQueryableValues(appId)

  def query(appId: ApplicationId, deployedTo: Environment, tag: Option[String], actorType: Option[String])(implicit hc: HeaderCarrier): Future[List[DisplayEvent]] =
    connectorFor(deployedTo).query(appId, tag, actorType)
}

abstract class ApiPlatformEventsConnector(implicit ec: ExecutionContext) extends ApplicationLogger {
  val environment: Environment
  val serviceBaseUrl: String

  def http: HttpClientV2

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder

  import ApiPlatformEventsConnector._

  private lazy val applicationEventsUri = s"$serviceBaseUrl/application-event"

  def fetchQueryableValues(appId: ApplicationId)(implicit hc: HeaderCarrier): Future[QueryableValues] = {
    configureEbridgeIfRequired(http.get(url"$applicationEventsUri/${appId.value.toString()}/values")).execute[Option[QueryableValues]]
      .map {
        case None     => QueryableValues(List.empty, List.empty)
        case Some(qv) => qv
      }
  }

  def query(appId: ApplicationId, tag: Option[String], actorType: Option[String])(implicit hc: HeaderCarrier): Future[List[DisplayEvent]] = {
    val queryParams =
      Seq(
        tag.map(et => "eventTag" -> et),
        actorType.map(at => "actorType" -> at)
      ).collect {
        case Some((a, b)) => a -> b
      }

    configureEbridgeIfRequired(http.get(url"$applicationEventsUri/${appId}?$queryParams")).execute[Option[QueryResponse]]
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
    val http: HttpClientV2
  )(implicit val ec: ExecutionContext
  ) extends ApiPlatformEventsConnector {

  val serviceBaseUrl: String = config.serviceBaseUrl
  val environment            = Environment.SANDBOX

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder =
    EbridgeConfigurator.configure(config.useProxy, config.bearerToken, config.apiKey)(requestBuilder)
}

object PrincipalApiPlatformEventsConnector {

  case class Config(
      serviceBaseUrl: String
    )
}

@Singleton
class PrincipalApiPlatformEventsConnector @Inject() (
    val config: PrincipalApiPlatformEventsConnector.Config,
    val http: HttpClientV2
  )(implicit val ec: ExecutionContext
  ) extends ApiPlatformEventsConnector {

  val environment    = Environment.PRODUCTION
  val serviceBaseUrl = config.serviceBaseUrl

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder = requestBuilder
}
