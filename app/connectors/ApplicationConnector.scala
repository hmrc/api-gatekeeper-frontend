/*
 * Copyright 2021 HM Revenue & Customs
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

package connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import play.api.http.Status._
import config.AppConfig
import model.Environment.Environment
import model.RateLimitTier.RateLimitTier
import model.{ApiContext, UserId, _}

import scala.concurrent.Future.{failed, successful}

object ApplicationConnector {
  import play.api.libs.json.Json
  import model.APIDefinitionFormatters._

  case class SearchCollaboratorsRequest(apiContext: ApiContext, apiVersion: ApiVersion, partialEmailMatch: Option[String])

  implicit val writes = Json.writes[SearchCollaboratorsRequest]
}

abstract class ApplicationConnector(implicit val ec: ExecutionContext) extends APIDefinitionFormatters {
  import ApplicationConnector._

  protected val httpClient: HttpClient
  val environment: Environment
  val serviceBaseUrl: String

  def http: HttpClient

  def baseApplicationUrl(applicationId: ApplicationId) = s"$serviceBaseUrl/application/${applicationId.value}"
  
  import uk.gov.hmrc.http.HttpReads.Implicits._

  def updateRateLimitTier(applicationId: ApplicationId, tier: RateLimitTier)
                         (implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    http.POST[UpdateRateLimitTierRequest, Either[UpstreamErrorResponse, Unit]](s"${baseApplicationUrl(applicationId)}/rate-limit-tier", UpdateRateLimitTierRequest(tier))
    .map(_ match {
      case Right(_) => ApplicationUpdateSuccessResult
      case Left(err) => throw err
    })
  }

  def approveUplift(applicationId: ApplicationId, gatekeeperUserId: String)
                   (implicit hc: HeaderCarrier): Future[ApproveUpliftSuccessful] = {
    http.POST[ApproveUpliftRequest, Either[UpstreamErrorResponse, Unit]](s"${baseApplicationUrl(applicationId)}/approve-uplift", ApproveUpliftRequest(gatekeeperUserId))
    .map(_ match {
      case Right(_) => ApproveUpliftSuccessful
      case Left(UpstreamErrorResponse(_, PRECONDITION_FAILED, _, _)) => throw PreconditionFailedException
      case Left(err) => throw err
    })
  }

  def rejectUplift(applicationId: ApplicationId, gatekeeperUserId: String, rejectionReason: String)
                  (implicit hc: HeaderCarrier): Future[RejectUpliftSuccessful] = {
    http.POST[RejectUpliftRequest, Either[UpstreamErrorResponse, Unit]](s"${baseApplicationUrl(applicationId)}/reject-uplift", RejectUpliftRequest(gatekeeperUserId, rejectionReason))
    .map(_ match {
      case Right(_) => RejectUpliftSuccessful
      case Left(UpstreamErrorResponse(_, PRECONDITION_FAILED, _, _)) => throw PreconditionFailedException
      case Left(err) => throw err
    })
  }

  def resendVerification(applicationId: ApplicationId, gatekeeperUserId: String)
                        (implicit hc: HeaderCarrier): Future[ResendVerificationSuccessful] = {
    http.POST[ResendVerificationRequest, Either[UpstreamErrorResponse, Unit]](s"${baseApplicationUrl(applicationId)}/resend-verification", ResendVerificationRequest(gatekeeperUserId))
    .map(_ match {
      case Right(_) => ResendVerificationSuccessful
      case Left(UpstreamErrorResponse(_, PRECONDITION_FAILED, _, _)) => throw PreconditionFailedException
      case Left(err) => throw err
    })
  }

  def fetchApplication(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[ApplicationWithHistory] = {
    http.GET[ApplicationWithHistory](s"$serviceBaseUrl/gatekeeper/application/${applicationId.value}")
  }

  def fetchStateHistory(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[List[StateHistory]] = {
    http.GET[List[StateHistory]](s"$serviceBaseUrl/gatekeeper/application/${applicationId.value}/stateHistory")
  }

  def fetchApplicationsByUserId(userId: UserId)(implicit hc: HeaderCarrier): Future[List[ApplicationResponse]] = {
    http.GET[List[ApplicationResponse]](s"$serviceBaseUrl/developer/${userId.asText}/applications")
      .recover {
        case e: UpstreamErrorResponse => throw new FetchApplicationsFailed(e)
      }
  }

  def fetchAllApplicationsBySubscription(subscribesTo: String, version: String)(implicit hc: HeaderCarrier): Future[List[ApplicationResponse]] = {
    http.GET[List[ApplicationResponse]](s"$serviceBaseUrl/application?subscribesTo=$subscribesTo&version=$version")
      .recover {
        case e: UpstreamErrorResponse => throw new FetchApplicationsFailed(e)
      }
  }

  def fetchAllApplicationsWithNoSubscriptions()(implicit hc: HeaderCarrier): Future[List[ApplicationResponse]] = {
    http.GET[List[ApplicationResponse]](s"$serviceBaseUrl/application?noSubscriptions=true")
      .recover {
        case e: UpstreamErrorResponse => throw new FetchApplicationsFailed(e)
      }
  }

  def fetchAllApplications()(implicit hc: HeaderCarrier): Future[List[ApplicationResponse]] = {
    http.GET[List[ApplicationResponse]](s"$serviceBaseUrl/application")
      .recover {
        case e: UpstreamErrorResponse => throw new FetchApplicationsFailed(e)
      }
  }

  def updateOverrides(applicationId: ApplicationId, updateOverridesRequest: UpdateOverridesRequest)(implicit hc: HeaderCarrier): Future[UpdateOverridesResult] = {
    http.PUT[UpdateOverridesRequest, Either[UpstreamErrorResponse, HttpResponse]](s"${baseApplicationUrl(applicationId)}/access/overrides", updateOverridesRequest)
    .map( _ match {
      case Right(_) => UpdateOverridesSuccessResult
      case Left(UpstreamErrorResponse(_, status, _, _)) if(HttpErrorFunctions.is4xx(status)) => UpdateOverridesFailureResult()
      case Left(err) => throw err
    })
  }

  def updateScopes(applicationId: ApplicationId, updateScopesRequest: UpdateScopesRequest)(implicit hc: HeaderCarrier): Future[UpdateScopesResult] = {
    http.PUT[UpdateScopesRequest, Either[UpstreamErrorResponse, HttpResponse]](s"${baseApplicationUrl(applicationId)}/access/scopes", updateScopesRequest)
   .map( _ match {
      case Right(result) => UpdateScopesSuccessResult
      case Left(err) => throw err
    })
  }

  def updateIpAllowlist(applicationId: ApplicationId, required: Boolean, ipAllowlist: Set[String])(implicit hc: HeaderCarrier): Future[UpdateIpAllowlistResult] = {
    http.PUT[UpdateIpAllowlistRequest, Either[UpstreamErrorResponse, HttpResponse]](s"${baseApplicationUrl(applicationId)}/ipAllowlist", UpdateIpAllowlistRequest(required, ipAllowlist))
   .map( _ match {
      case Right(result) => UpdateIpAllowlistSuccessResult
      case Left(err) => throw err
    })
  }


  def unsubscribeFromApi(applicationId: ApplicationId, apiContext: ApiContext, version: ApiVersion)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    http.DELETE[Either[UpstreamErrorResponse, HttpResponse]](s"${baseApplicationUrl(applicationId)}/subscription?context=${apiContext.value}&version=${version.value}")
   .map( _ match {
      case Right(result) => ApplicationUpdateSuccessResult
      case Left(err) => throw err
    })
  }

  def deleteApplication(applicationId: ApplicationId, deleteApplicationRequest: DeleteApplicationRequest)(implicit hc: HeaderCarrier): Future[ApplicationDeleteResult] = {
    http.POST[DeleteApplicationRequest, Either[UpstreamErrorResponse, HttpResponse]](s"${baseApplicationUrl(applicationId)}/delete", deleteApplicationRequest)
    .map( _ match {
      case Right(result) => ApplicationDeleteSuccessResult
      case Left(_) => ApplicationDeleteFailureResult
    })
  }

  def blockApplication(applicationId: ApplicationId, blockApplicationRequest: BlockApplicationRequest)(implicit hc: HeaderCarrier): Future[ApplicationBlockResult] = {
    http.POST[BlockApplicationRequest, Either[UpstreamErrorResponse, HttpResponse]](s"${baseApplicationUrl(applicationId)}/block", blockApplicationRequest)
   .map( _ match {
      case Right(result) => ApplicationBlockSuccessResult
      case Left(_) => ApplicationBlockFailureResult
      })
  }
  
  def unblockApplication(applicationId: ApplicationId, unblockApplicationRequest: UnblockApplicationRequest)(implicit hc: HeaderCarrier): Future[ApplicationUnblockResult] = {
    http.POST[UnblockApplicationRequest, Either[UpstreamErrorResponse, HttpResponse]](s"${baseApplicationUrl(applicationId)}/unblock", unblockApplicationRequest)
   .map( _ match {
      case Right(result) => ApplicationUnblockSuccessResult
      case Left(_) => ApplicationUnblockFailureResult
    })
  }

  def removeCollaborator(applicationId: ApplicationId, teamMemberToDelete: String, gatekeeperUserId: String, adminsToEmail: Set[String])(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    val url = s"${baseApplicationUrl(applicationId)}/collaborator/delete"
    val request = DeleteCollaboratorRequest(teamMemberToDelete, adminsToEmail, true)

    http.POST[DeleteCollaboratorRequest, Either[UpstreamErrorResponse, Unit]](url, request)
    .flatMap(_ match {
      case Right(_)                                               => successful(ApplicationUpdateSuccessResult)
      case Left(UpstreamErrorResponse(_, FORBIDDEN, _, _))        => failed(TeamMemberLastAdmin)
      case Left(UpstreamErrorResponse(_, NOT_FOUND, _, _))        => failed(ApplicationNotFound)
      case Left(err)                       => failed(err)
    })
  }

  def createPrivOrROPCApp(createPrivOrROPCAppRequest: CreatePrivOrROPCAppRequest)(implicit hc: HeaderCarrier): Future[CreatePrivOrROPCAppResult] = {
    http.POST[CreatePrivOrROPCAppRequest, Either[UpstreamErrorResponse, CreatePrivOrROPCAppSuccessResult]](s"$serviceBaseUrl/application", createPrivOrROPCAppRequest)
   .map( _ match {
      case Right(result) => result
      case Left(_) => CreatePrivOrROPCAppFailureResult
     })
  }

  def searchApplications(params: Map[String, String])(implicit hc: HeaderCarrier): Future[PaginatedApplicationResponse] = {
    http.GET[PaginatedApplicationResponse](s"$serviceBaseUrl/applications", params.toSeq)
  }

  def searchCollaborators(apiContext: ApiContext, apiVersion: ApiVersion, partialEmailMatch: Option[String])(implicit hc: HeaderCarrier): Future[List[String]] = {
    val request = SearchCollaboratorsRequest(apiContext, apiVersion, partialEmailMatch)

    http.POST[SearchCollaboratorsRequest,List[String]](s"$serviceBaseUrl/collaborators", request)
  }
}

@Singleton
class SandboxApplicationConnector @Inject()(val appConfig: AppConfig,
                                            val httpClient: HttpClient,
                                            val proxiedHttpClient: ProxiedHttpClient)(implicit override val ec: ExecutionContext)
  extends ApplicationConnector {

  val environment = Environment.SANDBOX
  val serviceBaseUrl = appConfig.applicationSandboxBaseUrl
  val useProxy = appConfig.applicationSandboxUseProxy
  val bearerToken = appConfig.applicationSandboxBearerToken
  val apiKey = appConfig.applicationSandboxApiKey

  val http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient

}

@Singleton
class ProductionApplicationConnector @Inject()(val appConfig: AppConfig,
                                               val httpClient: HttpClient)(implicit override val ec: ExecutionContext)
  extends ApplicationConnector {

  val environment = Environment.PRODUCTION
  val serviceBaseUrl = appConfig.applicationProductionBaseUrl

  val http = httpClient
}
