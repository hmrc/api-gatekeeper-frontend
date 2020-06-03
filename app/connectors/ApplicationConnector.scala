/*
 * Copyright 2020 HM Revenue & Customs
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

import java.net.URLEncoder.encode

import akka.actor.ActorSystem
import akka.pattern.FutureTimeoutSupport
import config.AppConfig
import javax.inject.{Inject, Singleton}
import model.Environment.Environment
import model.RateLimitTier.RateLimitTier
import model._
import play.api.http.ContentTypes.JSON
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.Status._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.Retries
import model.Subscription._

import scala.concurrent.{ExecutionContext, Future}

abstract class ApplicationConnector(implicit val ec: ExecutionContext) extends Retries {
  protected val httpClient: HttpClient
  protected val proxiedHttpClient: ProxiedHttpClient
  val environment: Environment
  val serviceBaseUrl: String
  val useProxy: Boolean
  val bearerToken: String
  val apiKey: String

  def http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient

  def updateRateLimitTier(applicationId: String, tier: RateLimitTier)
                         (implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
      http.POST[UpdateRateLimitTierRequest, HttpResponse](s"$serviceBaseUrl/application/$applicationId/rate-limit-tier",
        UpdateRateLimitTierRequest(tier), Seq(CONTENT_TYPE -> JSON))
        .map(_ => ApplicationUpdateSuccessResult)
  }

  def approveUplift(applicationId: String, gatekeeperUserId: String)
                   (implicit hc: HeaderCarrier): Future[ApproveUpliftSuccessful] = {
    http.POST[ApproveUpliftRequest, HttpResponse](s"$serviceBaseUrl/application/$applicationId/approve-uplift",
      ApproveUpliftRequest(gatekeeperUserId), Seq(CONTENT_TYPE -> JSON))
      .map(_ => ApproveUpliftSuccessful)
      .recover {
        case e: Upstream4xxResponse if e.upstreamResponseCode == PRECONDITION_FAILED => throw new PreconditionFailed
      }
  }

  def rejectUplift(applicationId: String, gatekeeperUserId: String, rejectionReason: String)
                  (implicit hc: HeaderCarrier): Future[RejectUpliftSuccessful] = {
    http.POST[RejectUpliftRequest, HttpResponse](s"$serviceBaseUrl/application/$applicationId/reject-uplift",
      RejectUpliftRequest(gatekeeperUserId, rejectionReason), Seq(CONTENT_TYPE -> JSON))
      .map(_ => RejectUpliftSuccessful)
      .recover {
        case e: Upstream4xxResponse if e.upstreamResponseCode == PRECONDITION_FAILED => throw new PreconditionFailed
      }
  }

  def resendVerification(applicationId: String, gatekeeperUserId: String)
                        (implicit hc: HeaderCarrier): Future[ResendVerificationSuccessful] = {
    http.POST[ResendVerificationRequest, HttpResponse](s"$serviceBaseUrl/application/$applicationId/resend-verification",
      ResendVerificationRequest(gatekeeperUserId), Seq(CONTENT_TYPE -> JSON))
      .map(_ => ResendVerificationSuccessful)
      .recover {
        case e: Upstream4xxResponse if e.upstreamResponseCode == PRECONDITION_FAILED => throw new PreconditionFailed
      }
  }

  def fetchApplication(applicationId: String)(implicit hc: HeaderCarrier): Future[ApplicationWithHistory] = {
    retry{
      http.GET[ApplicationWithHistory](s"$serviceBaseUrl/gatekeeper/application/$applicationId")
    }
  }

  def fetchApplicationsByEmail(email: String)(implicit hc: HeaderCarrier): Future[Seq[ApplicationResponse]] = {
    retry{
      http.GET[Seq[ApplicationResponse]](s"$serviceBaseUrl/developer/applications", Seq("emailAddress" -> email))
        .recover {
          case e =>
            throw new FetchApplicationsFailed(e)
        }
    }
  }

  def fetchAllApplicationsBySubscription(subscribesTo: String, version: String)(implicit hc: HeaderCarrier): Future[Seq[ApplicationResponse]] = {
    retry{
      http.GET[Seq[ApplicationResponse]](s"$serviceBaseUrl/application?subscribesTo=$subscribesTo&version=$version")
        .recover {
          case e: Upstream5xxResponse => throw new FetchApplicationsFailed(e)
        }
    }
  }

  def fetchAllApplicationsWithNoSubscriptions()(implicit hc: HeaderCarrier): Future[Seq[ApplicationResponse]] = {
    retry{
      http.GET[Seq[ApplicationResponse]](s"$serviceBaseUrl/application?noSubscriptions=true")
        .recover {
          case e: Upstream5xxResponse => throw new FetchApplicationsFailed(e)
        }
    }
  }

  def fetchAllApplications()(implicit hc: HeaderCarrier): Future[Seq[ApplicationResponse]] = {
    retry{
      http.GET[Seq[ApplicationResponse]](s"$serviceBaseUrl/application")
        .recover {
          case e: Upstream5xxResponse => throw new FetchApplicationsFailed(e)
        }
    }
  }

  def fetchAllSubscriptions()(implicit hc: HeaderCarrier): Future[Seq[SubscriptionResponse]] = {
    retry{
      http.GET[Seq[SubscriptionResponse]](s"$serviceBaseUrl/application/subscriptions")
        .recover {
          case e: Upstream5xxResponse => throw new FetchApplicationsFailed(e)
        }
    }
  }

  def fetchApplicationSubscriptions(applicationId: String)(implicit hc: HeaderCarrier): Future[Seq[SubscriptionWithoutFields]] = {
    retry{
      http.GET[Seq[SubscriptionWithoutFields]](s"$serviceBaseUrl/application/$applicationId/subscription")
        .recover {
          case e: Upstream5xxResponse => throw new FetchApplicationSubscriptionsFailed
        }
    }
  }

  def updateOverrides(applicationId: String, updateOverridesRequest: UpdateOverridesRequest)(implicit hc: HeaderCarrier): Future[UpdateOverridesResult] = {
    http.PUT[UpdateOverridesRequest, HttpResponse](s"$serviceBaseUrl/application/$applicationId/access/overrides", updateOverridesRequest)
      .map(_ => UpdateOverridesSuccessResult)
      .recover {
        case e: Upstream4xxResponse => UpdateOverridesFailureResult()
      }
  }

  def updateScopes(applicationId: String, updateScopesRequest: UpdateScopesRequest)(implicit hc: HeaderCarrier): Future[UpdateScopesResult] = {
    http.PUT[UpdateScopesRequest, HttpResponse](s"$serviceBaseUrl/application/$applicationId/access/scopes", updateScopesRequest)
      .map(_ => UpdateScopesSuccessResult)
  }

  def manageIpWhitelist(applicationId: String, ipWhitelist: Set[String])(implicit hc: HeaderCarrier): Future[UpdateIpWhitelistResult] = {
    http.PUT[UpdateIpWhitelistRequest, HttpResponse](s"$serviceBaseUrl/application/$applicationId/ipWhitelist", UpdateIpWhitelistRequest(ipWhitelist))
      .map(_ => UpdateIpWhitelistSuccessResult)
  }

  def subscribeToApi(applicationId: String, apiIdentifier: APIIdentifier)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    http.POST[APIIdentifier, HttpResponse](s"$serviceBaseUrl/application/$applicationId/subscription", apiIdentifier, Seq(CONTENT_TYPE -> JSON)) map { _ =>
      ApplicationUpdateSuccessResult
    }
  }

  def unsubscribeFromApi(applicationId: String, context: String, version: String)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    http.DELETE[HttpResponse](s"$serviceBaseUrl/application/$applicationId/subscription?context=$context&version=$version") map { _ =>
      ApplicationUpdateSuccessResult
    }
  }

  def deleteApplication(applicationId: String, deleteApplicationRequest: DeleteApplicationRequest)(implicit hc: HeaderCarrier): Future[ApplicationDeleteResult] = {
    http.POST[DeleteApplicationRequest, HttpResponse](s"$serviceBaseUrl/application/$applicationId/delete", deleteApplicationRequest, Seq(CONTENT_TYPE -> JSON))
      .map(response => response.status match {
        case NO_CONTENT => ApplicationDeleteSuccessResult
        case _ => ApplicationDeleteFailureResult
      })
      .recover {
        case _ => ApplicationDeleteFailureResult
      }
  }

  def blockApplication(applicationId: String, blockApplicationRequest: BlockApplicationRequest)(implicit hc: HeaderCarrier): Future[ApplicationBlockResult] = {
    http.POST[BlockApplicationRequest, HttpResponse](s"$serviceBaseUrl/application/$applicationId/block", blockApplicationRequest, Seq(CONTENT_TYPE -> JSON))
      .map(response => response.status match {
        case OK => ApplicationBlockSuccessResult
        case _ => ApplicationBlockFailureResult
      })
      .recover {
        case _ => ApplicationBlockFailureResult
      }
  }

  def unblockApplication(applicationId: String, unblockApplicationRequest: UnblockApplicationRequest)(implicit hc: HeaderCarrier): Future[ApplicationUnblockResult] = {
    http.POST[UnblockApplicationRequest, HttpResponse](s"$serviceBaseUrl/application/$applicationId/unblock", unblockApplicationRequest, Seq(CONTENT_TYPE -> JSON))
      .map(response => response.status match {
        case OK => ApplicationUnblockSuccessResult
        case _ => ApplicationUnblockFailureResult
      })
      .recover {
        case _ => ApplicationUnblockFailureResult
      }
  }

  def addCollaborator(applicationId: String, addTeamMemberRequest: AddTeamMemberRequest)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    http.POST[AddTeamMemberRequest, HttpResponse](s"$serviceBaseUrl/application/$applicationId/collaborator", addTeamMemberRequest, Seq(CONTENT_TYPE -> JSON)) map {
      _ => ApplicationUpdateSuccessResult
    } recover {
      case e: Upstream4xxResponse if e.upstreamResponseCode == CONFLICT => throw new TeamMemberAlreadyExists
      case _: NotFoundException => throw new ApplicationNotFound
    }
  }

  def removeCollaborator(applicationId: String, emailAddress: String, gatekeeperUserId: String, adminsToEmail: Seq[String])(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    http.DELETE[HttpResponse](s"$serviceBaseUrl/application/$applicationId/collaborator/${urlEncode(emailAddress)}?admin=${urlEncode(gatekeeperUserId)}&adminsToEmail=${urlEncode(adminsToEmail.mkString(","))}") map { _ =>
      ApplicationUpdateSuccessResult
    } recover {
      case e: Upstream4xxResponse if e.upstreamResponseCode == FORBIDDEN => throw new TeamMemberLastAdmin
    }
  }

  def createPrivOrROPCApp(createPrivOrROPCAppRequest: CreatePrivOrROPCAppRequest)(implicit hc: HeaderCarrier): Future[CreatePrivOrROPCAppResult] = {
    http.POST[CreatePrivOrROPCAppRequest, CreatePrivOrROPCAppSuccessResult](s"$serviceBaseUrl/application", createPrivOrROPCAppRequest, Seq(CONTENT_TYPE -> JSON))
      .recover {
        case failure => CreatePrivOrROPCAppFailureResult
      }
  }

  def searchApplications(params: Map[String, String])(implicit hc: HeaderCarrier): Future[PaginatedApplicationResponse] = {
    retry{
      http.GET[PaginatedApplicationResponse](s"$serviceBaseUrl/applications", params.toSeq)
    }
  }

  private def urlEncode(str: String, encoding: String = "UTF-8") = {
    encode(str, encoding)
  }

  def searchCollaborators(apiContext: String, apiVersion: String, partialEmailMatch: Option[String])(implicit hc: HeaderCarrier): Future[Seq[String]] = {
    val queryParameters = List(
      "context" -> apiContext,
      "version" -> apiVersion
    )

    val withOptionalQueryParameters = partialEmailMatch match {
      case Some(email) => queryParameters ++ List(("partialEmailMatch", email))
      case None => queryParameters
    }

    retry{
      http.GET[Seq[String]](s"$serviceBaseUrl/collaborators", withOptionalQueryParameters)
    }
  }
}

@Singleton
class SandboxApplicationConnector @Inject()(val appConfig: AppConfig,
                                            val httpClient: HttpClient,
                                            val proxiedHttpClient: ProxiedHttpClient,
                                            val actorSystem: ActorSystem,
                                            val futureTimeout: FutureTimeoutSupport)(implicit override val ec: ExecutionContext)
  extends ApplicationConnector {

  val environment = Environment.SANDBOX
  val serviceBaseUrl = appConfig.applicationSandboxBaseUrl
  val useProxy = appConfig.applicationSandboxUseProxy
  val bearerToken = appConfig.applicationSandboxBearerToken
  val apiKey = appConfig.applicationSandboxApiKey
}

@Singleton
class ProductionApplicationConnector @Inject()(val appConfig: AppConfig,
                                               val httpClient: HttpClient,
                                               val proxiedHttpClient: ProxiedHttpClient,
                                               val actorSystem: ActorSystem,
                                               val futureTimeout: FutureTimeoutSupport)(implicit override val ec: ExecutionContext)
  extends ApplicationConnector {

  val environment = Environment.PRODUCTION
  val serviceBaseUrl = appConfig.applicationProductionBaseUrl
  val useProxy = appConfig.applicationProductionUseProxy
  val bearerToken = appConfig.applicationProductionBearerToken
  val apiKey = appConfig.applicationProductionApiKey
}
