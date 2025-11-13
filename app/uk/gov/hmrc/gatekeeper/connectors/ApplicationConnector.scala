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

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.libs.json._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationName
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.CreateApplicationRequestV1
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.connectors.ApplicationConnector.TermsOfUseInvitationResponse
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.applications.ApplicationsByAnswer

object ApplicationConnector {
  import play.api.libs.json.Json

  case class SearchCollaboratorsRequest(apiContext: ApiContext, apiVersion: ApiVersionNbr)

  implicit val writes: OWrites[SearchCollaboratorsRequest] = Json.writes[SearchCollaboratorsRequest]

  case class TermsOfUseInvitationResponse(applicationId: ApplicationId)

  implicit val termsOfUseInvitationResponseReads: Reads[TermsOfUseInvitationResponse] = Json.reads[TermsOfUseInvitationResponse]

  case class AppWithSubscriptionsForCsvResponse(id: ApplicationId, name: ApplicationName, lastAccess: Option[Instant], apiIdentifiers: Set[ApiIdentifier])

  implicit val appWithSubscriptionsForCsvResponseReads: Reads[AppWithSubscriptionsForCsvResponse] = Json.reads[AppWithSubscriptionsForCsvResponse]
}

abstract class ApplicationConnector(implicit val ec: ExecutionContext) extends APIDefinitionFormatters {
  import ApplicationConnector._

  val environment: Environment
  val serviceBaseUrl: String

  def http: HttpClientV2

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder

  def baseApplicationUrl(applicationId: ApplicationId) = s"$serviceBaseUrl/application/${applicationId}"

  def baseTpaGatekeeperUrl(applicationId: ApplicationId) = s"$serviceBaseUrl/gatekeeper/application/${applicationId}"

  import uk.gov.hmrc.http.HttpReads.Implicits._

  def createPrivApp(request: CreateApplicationRequestV1)(implicit hc: HeaderCarrier): Future[CreatePrivAppResult] = {
    configureEbridgeIfRequired(http.post(url"$serviceBaseUrl/application"))
      .withBody(Json.toJson(request))
      .execute[Either[UpstreamErrorResponse, CreatePrivAppSuccessResult]]
      .map(_ match {
        case Right(result) => result
        case Left(_)       => CreatePrivAppFailureResult
      })
  }

  def searchCollaborators(apiContext: ApiContext, apiVersion: ApiVersionNbr)(implicit hc: HeaderCarrier): Future[List[LaxEmailAddress]] = {
    val request = SearchCollaboratorsRequest(apiContext, apiVersion)

    configureEbridgeIfRequired(http.post(url"$serviceBaseUrl/collaborators"))
      .withBody(Json.toJson(request))
      .execute[List[LaxEmailAddress]]
  }

  def fetchApplicationsByAnswer(question: String)(implicit hc: HeaderCarrier): Future[List[ApplicationsByAnswer]] = {
    configureEbridgeIfRequired(http.get(url"$serviceBaseUrl/submissions/answers/$question")).execute[List[ApplicationsByAnswer]]
  }
}

@Singleton
class SandboxApplicationConnector @Inject() (
    val appConfig: AppConfig,
    val http: HttpClientV2
  )(implicit override val ec: ExecutionContext
  ) extends ApplicationConnector {

  val environment    = Environment.SANDBOX
  val serviceBaseUrl = appConfig.applicationSandboxBaseUrl
  val useProxy       = appConfig.applicationSandboxUseProxy
  val bearerToken    = appConfig.applicationSandboxBearerToken
  val apiKey         = appConfig.applicationSandboxApiKey

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder =
    EbridgeConfigurator.configure(useProxy, bearerToken, apiKey)(requestBuilder)
}

@Singleton
class ProductionApplicationConnector @Inject() (
    val appConfig: AppConfig,
    val http: HttpClientV2
  )(implicit override val ec: ExecutionContext
  ) extends ApplicationConnector {

  val environment    = Environment.PRODUCTION
  val serviceBaseUrl = appConfig.applicationProductionBaseUrl

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder = requestBuilder

  def fetchAllApplicationsWithStateHistories()(implicit hc: HeaderCarrier): Future[List[ApplicationStateHistory]] = {
    http.get(url"$serviceBaseUrl/gatekeeper/applications/stateHistory")
      .execute[List[ApplicationStateHistory]]
      .recover {
        case e: UpstreamErrorResponse => throw new FetchApplicationsFailed(e)
      }
  }

  def doesApplicationHaveSubmissions(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Boolean] = {
    http.get(url"$serviceBaseUrl/submissions/latestiscompleted/${applicationId}")
      .execute[Option[Boolean]]
      .map(_ match {
        case Some(_) => true
        case None    => false
      })
  }

  def doesApplicationHaveTermsOfUseInvitation(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Boolean] = {
    http.get(url"$serviceBaseUrl/terms-of-use/application/${applicationId}")
      .execute[Option[TermsOfUseInvitationResponse]]
      .map(_ match {
        case Some(_) => true
        case None    => false
      })
  }
}
