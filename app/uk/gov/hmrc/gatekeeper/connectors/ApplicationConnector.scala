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

import play.api.libs.json.{OWrites, Reads}
import uk.gov.hmrc.http.{HttpClient, _}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{GKApplicationResponse, StateHistory}
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models._

object ApplicationConnector {
  import play.api.libs.json.Json

  case class ValidateApplicationNameResponseErrorDetails(invalidName: Boolean, duplicateName: Boolean)
  case class ValidateApplicationNameResponse(errors: Option[ValidateApplicationNameResponseErrorDetails])

  implicit val validateApplicationNameResponseErrorDetailsReads: Reads[ValidateApplicationNameResponseErrorDetails] = Json.reads[ValidateApplicationNameResponseErrorDetails]
  implicit val validateApplicationNameResponseReads: Reads[ValidateApplicationNameResponse]                         = Json.reads[ValidateApplicationNameResponse]

  case class SearchCollaboratorsRequest(apiContext: ApiContext, apiVersion: ApiVersionNbr, partialEmailMatch: Option[String])

  implicit val writes: OWrites[SearchCollaboratorsRequest] = Json.writes[SearchCollaboratorsRequest]

  case class TermsOfUseInvitationResponse(applicationId: ApplicationId)

  implicit val termsOfUseInvitationResponseReads: Reads[TermsOfUseInvitationResponse] = Json.reads[TermsOfUseInvitationResponse]
}

abstract class ApplicationConnector(implicit val ec: ExecutionContext) extends APIDefinitionFormatters {
  import ApplicationConnector._

  protected val httpClient: HttpClient
  val environment: Environment
  val serviceBaseUrl: String

  def http: HttpClient

  def baseApplicationUrl(applicationId: ApplicationId) = s"$serviceBaseUrl/application/${applicationId}"

  def baseTpaGatekeeperUrl(applicationId: ApplicationId) = s"$serviceBaseUrl/gatekeeper/application/${applicationId}"

  import uk.gov.hmrc.http.HttpReads.Implicits._

  def fetchApplication(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[ApplicationWithHistory] = {
    http.GET[ApplicationWithHistory](baseTpaGatekeeperUrl(applicationId))
  }

  def fetchStateHistory(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[List[StateHistory]] = {
    http.GET[List[StateHistory]](s"${baseTpaGatekeeperUrl(applicationId)}/stateHistory")
  }

  def fetchApplicationsByUserId(userId: UserId)(implicit hc: HeaderCarrier): Future[List[GKApplicationResponse]] = {
    http.GET[List[GKApplicationResponse]](s"$serviceBaseUrl/gatekeeper/developer/${userId}/applications")
      .recover {
        case e: UpstreamErrorResponse => throw new FetchApplicationsFailed(e)
      }
  }

  def fetchApplicationsExcludingDeletedByUserId(userId: UserId)(implicit hc: HeaderCarrier): Future[List[GKApplicationResponse]] = {
    http.GET[List[GKApplicationResponse]](s"$serviceBaseUrl/developer/${userId}/applications")
      .recover {
        case e: UpstreamErrorResponse => throw new FetchApplicationsFailed(e)
      }
  }

  def fetchAllApplicationsBySubscription(subscribesTo: String, version: String)(implicit hc: HeaderCarrier): Future[List[GKApplicationResponse]] = {
    http.GET[List[GKApplicationResponse]](s"$serviceBaseUrl/application?subscribesTo=$subscribesTo&version=$version")
      .recover {
        case e: UpstreamErrorResponse => throw new FetchApplicationsFailed(e)
      }
  }

  def fetchAllApplicationsWithNoSubscriptions()(implicit hc: HeaderCarrier): Future[List[GKApplicationResponse]] = {
    http.GET[List[GKApplicationResponse]](s"$serviceBaseUrl/application?noSubscriptions=true")
      .recover {
        case e: UpstreamErrorResponse => throw new FetchApplicationsFailed(e)
      }
  }

  def fetchAllApplications()(implicit hc: HeaderCarrier): Future[List[GKApplicationResponse]] = {
    http.GET[List[GKApplicationResponse]](s"$serviceBaseUrl/application")
      .recover {
        case e: UpstreamErrorResponse => throw new FetchApplicationsFailed(e)
      }
  }

  def fetchAllApplicationsWithStateHistories()(implicit hc: HeaderCarrier): Future[List[ApplicationStateHistory]] = {
    http.GET[List[ApplicationStateHistory]](s"$serviceBaseUrl/gatekeeper/applications/stateHistory")
      .recover {
        case e: UpstreamErrorResponse => throw new FetchApplicationsFailed(e)
      }
  }

  def validateApplicationName(applicationId: Option[ApplicationId], name: String)(implicit hc: HeaderCarrier): Future[ValidateApplicationNameResult] = {
    http.POST[ValidateApplicationNameRequest, Either[UpstreamErrorResponse, ValidateApplicationNameResponse]](
      s"$serviceBaseUrl/application/name/validate",
      ValidateApplicationNameRequest(name, applicationId)
    )
      .map(_ match {
        case Right(ValidateApplicationNameResponse(None))                                                       => ValidateApplicationNameSuccessResult
        case Right(ValidateApplicationNameResponse(Some(ValidateApplicationNameResponseErrorDetails(true, _)))) => ValidateApplicationNameFailureInvalidResult
        case Right(ValidateApplicationNameResponse(Some(ValidateApplicationNameResponseErrorDetails(_, true)))) => ValidateApplicationNameFailureDuplicateResult
        case Left(err)                                                                                          => throw err
      })
  }

  def createPrivOrROPCApp(createPrivOrROPCAppRequest: CreatePrivOrROPCAppRequest)(implicit hc: HeaderCarrier): Future[CreatePrivOrROPCAppResult] = {
    http.POST[CreatePrivOrROPCAppRequest, Either[UpstreamErrorResponse, CreatePrivOrROPCAppSuccessResult]](s"$serviceBaseUrl/application", createPrivOrROPCAppRequest)
      .map(_ match {
        case Right(result) => result
        case Left(_)       => CreatePrivOrROPCAppFailureResult
      })
  }

  def searchApplications(params: Map[String, String])(implicit hc: HeaderCarrier): Future[PaginatedApplicationResponse] = {
    http.GET[PaginatedApplicationResponse](s"$serviceBaseUrl/applications", params.toSeq)
  }

  def fetchApplicationsWithSubscriptions()(implicit hc: HeaderCarrier): Future[List[ApplicationWithSubscriptionsResponse]] = {
    http.GET[List[ApplicationWithSubscriptionsResponse]](s"$serviceBaseUrl/gatekeeper/applications/subscriptions")
  }

  def searchCollaborators(apiContext: ApiContext, apiVersion: ApiVersionNbr, partialEmailMatch: Option[String])(implicit hc: HeaderCarrier): Future[List[LaxEmailAddress]] = {
    val request = SearchCollaboratorsRequest(apiContext, apiVersion, partialEmailMatch)

    http.POST[SearchCollaboratorsRequest, List[LaxEmailAddress]](s"$serviceBaseUrl/collaborators", request)
  }

  def doesApplicationHaveSubmissions(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Boolean] = {
    http.GET[Option[Boolean]](s"$serviceBaseUrl/submissions/latestiscompleted/${applicationId}")
      .map(_ match {
        case Some(_) => true
        case None    => false
      })
  }

  def doesApplicationHaveTermsOfUseInvitation(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Boolean] = {
    http.GET[Option[TermsOfUseInvitationResponse]](s"$serviceBaseUrl/terms-of-use/application/${applicationId}")
      .map(_ match {
        case Some(_) => true
        case None    => false
      })
  }
}

@Singleton
class SandboxApplicationConnector @Inject() (
    val appConfig: AppConfig,
    val httpClient: HttpClient,
    val proxiedHttpClient: ProxiedHttpClient
  )(implicit override val ec: ExecutionContext
  ) extends ApplicationConnector {

  val environment    = Environment.SANDBOX
  val serviceBaseUrl = appConfig.applicationSandboxBaseUrl
  val useProxy       = appConfig.applicationSandboxUseProxy
  val bearerToken    = appConfig.applicationSandboxBearerToken
  val apiKey         = appConfig.applicationSandboxApiKey

  val http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient

}

@Singleton
class ProductionApplicationConnector @Inject() (val appConfig: AppConfig, val httpClient: HttpClient)(implicit override val ec: ExecutionContext)
    extends ApplicationConnector {

  val environment    = Environment.PRODUCTION
  val serviceBaseUrl = appConfig.applicationProductionBaseUrl

  val http = httpClient
}
