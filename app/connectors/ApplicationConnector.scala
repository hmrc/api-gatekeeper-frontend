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
import model.ApiContext

import scala.concurrent.{ExecutionContext, Future}

abstract class ApplicationConnector(implicit val ec: ExecutionContext) extends Retries with APIDefinitionFormatters {
  protected val httpClient: HttpClient
  protected val proxiedHttpClient: ProxiedHttpClient
  val environment: Environment
  val serviceBaseUrl: String
  val useProxy: Boolean
  val bearerToken: String
  val apiKey: String

  def http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient

  def baseApplicationUrl(applicationId: ApplicationId) = s"$serviceBaseUrl/application/${applicationId.value}"

  def updateRateLimitTier(applicationId: ApplicationId, tier: RateLimitTier)
                         (implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
      http.POST[UpdateRateLimitTierRequest, HttpResponse](s"${baseApplicationUrl(applicationId)}/rate-limit-tier",
        UpdateRateLimitTierRequest(tier), Seq(CONTENT_TYPE -> JSON))
        .map(_ => ApplicationUpdateSuccessResult)
  }

  def approveUplift(applicationId: ApplicationId, gatekeeperUserId: String)
                   (implicit hc: HeaderCarrier): Future[ApproveUpliftSuccessful] = {
    http.POST[ApproveUpliftRequest, HttpResponse](s"${baseApplicationUrl(applicationId)}/approve-uplift",
      ApproveUpliftRequest(gatekeeperUserId), Seq(CONTENT_TYPE -> JSON))
      .map(_ => ApproveUpliftSuccessful)
      .recover {
        case e: Upstream4xxResponse if e.upstreamResponseCode == PRECONDITION_FAILED => throw new PreconditionFailed
      }
  }

  def rejectUplift(applicationId: ApplicationId, gatekeeperUserId: String, rejectionReason: String)
                  (implicit hc: HeaderCarrier): Future[RejectUpliftSuccessful] = {
    http.POST[RejectUpliftRequest, HttpResponse](s"${baseApplicationUrl(applicationId)}/reject-uplift",
      RejectUpliftRequest(gatekeeperUserId, rejectionReason), Seq(CONTENT_TYPE -> JSON))
      .map(_ => RejectUpliftSuccessful)
      .recover {
        case e: Upstream4xxResponse if e.upstreamResponseCode == PRECONDITION_FAILED => throw new PreconditionFailed
      }
  }

  def resendVerification(applicationId: ApplicationId, gatekeeperUserId: String)
                        (implicit hc: HeaderCarrier): Future[ResendVerificationSuccessful] = {
    http.POST[ResendVerificationRequest, HttpResponse](s"${baseApplicationUrl(applicationId)}/resend-verification",
      ResendVerificationRequest(gatekeeperUserId), Seq(CONTENT_TYPE -> JSON))
      .map(_ => ResendVerificationSuccessful)
      .recover {
        case e: Upstream4xxResponse if e.upstreamResponseCode == PRECONDITION_FAILED => throw new PreconditionFailed
      }
  }

  def fetchApplication(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[ApplicationWithHistory] = {
    retry{
      http.GET[ApplicationWithHistory](s"$serviceBaseUrl/gatekeeper/application/${applicationId.value}")
    }
  }

  def fetchStateHistory(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Seq[StateHistory]] = {
    retry {
      http.GET[Seq[StateHistory]](s"$serviceBaseUrl/gatekeeper/application/${applicationId.value}/stateHistory")
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

  def fetchApplicationSubscriptions(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Seq[SubscriptionWithoutFields]] = {
    retry{
      http.GET[Seq[SubscriptionWithoutFields]](s"${baseApplicationUrl(applicationId)}/subscription")
        .recover {
          case e: Upstream5xxResponse => throw new FetchApplicationSubscriptionsFailed
        }
    }
  }

  def updateOverrides(applicationId: ApplicationId, updateOverridesRequest: UpdateOverridesRequest)(implicit hc: HeaderCarrier): Future[UpdateOverridesResult] = {
    http.PUT[UpdateOverridesRequest, HttpResponse](s"${baseApplicationUrl(applicationId)}/access/overrides", updateOverridesRequest)
      .map(_ => UpdateOverridesSuccessResult)
      .recover {
        case e: Upstream4xxResponse => UpdateOverridesFailureResult()
      }
  }

  def updateScopes(applicationId: ApplicationId, updateScopesRequest: UpdateScopesRequest)(implicit hc: HeaderCarrier): Future[UpdateScopesResult] = {
    http.PUT[UpdateScopesRequest, HttpResponse](s"${baseApplicationUrl(applicationId)}/access/scopes", updateScopesRequest)
      .map(_ => UpdateScopesSuccessResult)
  }

  def updateIpAllowlist(applicationId: ApplicationId, required: Boolean, ipAllowlist: Set[String])(implicit hc: HeaderCarrier): Future[UpdateIpAllowlistResult] = {
    http.PUT[UpdateIpAllowlistRequest, HttpResponse](s"${baseApplicationUrl(applicationId)}/ipAllowlist", UpdateIpAllowlistRequest(required, ipAllowlist))
      .map(_ => UpdateIpAllowlistSuccessResult)
  }

  def unsubscribeFromApi(applicationId: ApplicationId, apiContext: ApiContext, version: ApiVersion)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    http.DELETE[HttpResponse](s"${baseApplicationUrl(applicationId)}/subscription?context=${apiContext.value}&version=${version.value}") map { _ =>
      ApplicationUpdateSuccessResult
    }
  }

  def deleteApplication(applicationId: ApplicationId, deleteApplicationRequest: DeleteApplicationRequest)(implicit hc: HeaderCarrier): Future[ApplicationDeleteResult] = {
    http.POST[DeleteApplicationRequest, HttpResponse](s"${baseApplicationUrl(applicationId)}/delete", deleteApplicationRequest, Seq(CONTENT_TYPE -> JSON))
      .map(response => response.status match {
        case NO_CONTENT => ApplicationDeleteSuccessResult
        case _ => ApplicationDeleteFailureResult
      })
      .recover {
        case _ => ApplicationDeleteFailureResult
      }
  }

  def blockApplication(applicationId: ApplicationId, blockApplicationRequest: BlockApplicationRequest)(implicit hc: HeaderCarrier): Future[ApplicationBlockResult] = {
    http.POST[BlockApplicationRequest, HttpResponse](s"${baseApplicationUrl(applicationId)}/block", blockApplicationRequest, Seq(CONTENT_TYPE -> JSON))
      .map(response => response.status match {
        case OK => ApplicationBlockSuccessResult
        case _ => ApplicationBlockFailureResult
      })
      .recover {
        case _ => ApplicationBlockFailureResult
      }
  }

  def unblockApplication(applicationId: ApplicationId, unblockApplicationRequest: UnblockApplicationRequest)(implicit hc: HeaderCarrier): Future[ApplicationUnblockResult] = {
    http.POST[UnblockApplicationRequest, HttpResponse](s"${baseApplicationUrl(applicationId)}/unblock", unblockApplicationRequest, Seq(CONTENT_TYPE -> JSON))
      .map(response => response.status match {
        case OK => ApplicationUnblockSuccessResult
        case _ => ApplicationUnblockFailureResult
      })
      .recover {
        case _ => ApplicationUnblockFailureResult
      }
  }

  def removeCollaborator(applicationId: ApplicationId, emailAddress: String, gatekeeperUserId: String, adminsToEmail: Seq[String])(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    http.DELETE[HttpResponse](s"${baseApplicationUrl(applicationId)}/collaborator/${urlEncode(emailAddress)}?admin=${urlEncode(gatekeeperUserId)}&adminsToEmail=${urlEncode(adminsToEmail.mkString(","))}") map { _ =>
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

  def searchCollaborators(apiContext: ApiContext, apiVersion: ApiVersion, partialEmailMatch: Option[String])(implicit hc: HeaderCarrier): Future[Seq[String]] = {
    val queryParameters = List(
      "context" -> apiContext.value,
      "version" -> apiVersion.value
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
