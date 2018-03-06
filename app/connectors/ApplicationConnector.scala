/*
 * Copyright 2018 HM Revenue & Customs
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

import config.WSHttp
import connectors.AuthConnector._
import model.RateLimitTier.RateLimitTier
import model.{UpdateOverridesSuccessResult, _}
import play.api.http.ContentTypes.JSON
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.Status.PRECONDITION_FAILED
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ApplicationConnector extends ApplicationConnector {
  override val applicationBaseUrl: String = s"${baseUrl("third-party-application")}"
  override val http = WSHttp
}

trait ApplicationConnector {

  val applicationBaseUrl: String

  val http: HttpPost with HttpGet with HttpPut with HttpDelete

  def updateRateLimitTier(applicationId: String, tier: RateLimitTier)
                         (implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    http.POST(s"$applicationBaseUrl/application/$applicationId/rate-limit-tier",
      UpdateRateLimitTierRequest(tier), Seq(CONTENT_TYPE -> JSON))
      .map(_ => ApplicationUpdateSuccessResult)
  }

  def approveUplift(applicationId: String, gatekeeperUserId: String)
                   (implicit hc: HeaderCarrier): Future[ApproveUpliftSuccessful] = {
    http.POST(s"$applicationBaseUrl/application/$applicationId/approve-uplift",
      ApproveUpliftRequest(gatekeeperUserId), Seq(CONTENT_TYPE -> JSON))
      .map(_ => ApproveUpliftSuccessful)
      .recover {
        case e: Upstream4xxResponse if e.upstreamResponseCode == PRECONDITION_FAILED => throw new PreconditionFailed
      }
  }

  def rejectUplift(applicationId: String, gatekeeperUserId: String, rejectionReason: String)
                  (implicit hc: HeaderCarrier): Future[RejectUpliftSuccessful] = {
    http.POST(s"$applicationBaseUrl/application/$applicationId/reject-uplift",
      RejectUpliftRequest(gatekeeperUserId, rejectionReason), Seq(CONTENT_TYPE -> JSON))
      .map(_ => RejectUpliftSuccessful)
      .recover {
        case e: Upstream4xxResponse if e.upstreamResponseCode == PRECONDITION_FAILED => throw new PreconditionFailed
      }
  }

  def resendVerification(applicationId: String, gatekeeperUserId: String)
                        (implicit hc: HeaderCarrier): Future[ResendVerificationSuccessful] = {
    http.POST(s"$applicationBaseUrl/application/$applicationId/resend-verification",
      ResendVerificationRequest(gatekeeperUserId), Seq(CONTENT_TYPE -> JSON))
      .map(_ => ResendVerificationSuccessful)
      .recover {
        case e: Upstream4xxResponse if e.upstreamResponseCode == PRECONDITION_FAILED => throw new PreconditionFailed
      }
  }

  def fetchApplicationsWithUpliftRequest()(implicit hc: HeaderCarrier): Future[Seq[ApplicationWithUpliftRequest]] = {
    http.GET[Seq[ApplicationWithUpliftRequest]](s"$applicationBaseUrl/gatekeeper/applications")
      .recover {
        case e: Upstream5xxResponse => throw new FetchApplicationsFailed
      }
  }

  def fetchApplication(applicationId: String)(implicit hc: HeaderCarrier): Future[ApplicationWithHistory] = {
    http.GET[ApplicationWithHistory](s"$applicationBaseUrl/gatekeeper/application/$applicationId")
  }

  def fetchAllApplicationsBySubscription(apiContext: String)(implicit hc: HeaderCarrier): Future[Seq[ApplicationResponse]] = {
    http.GET[Seq[ApplicationResponse]](s"$applicationBaseUrl/application?subscribesTo=$apiContext")
      .recover {
        case e: Upstream5xxResponse => throw new FetchApplicationsFailed
      }
  }

  def fetchAllApplicationsWithNoSubscriptions()(implicit hc: HeaderCarrier): Future[Seq[ApplicationResponse]] = {
    http.GET[Seq[ApplicationResponse]](s"$applicationBaseUrl/application?noSubscriptions=true")
      .recover {
        case e: Upstream5xxResponse => throw new FetchApplicationsFailed
      }
  }

  def fetchAllApplications()(implicit hc: HeaderCarrier): Future[Seq[ApplicationResponse]] = {
    http.GET[Seq[ApplicationResponse]](s"$applicationBaseUrl/application")
      .recover {
        case e: Upstream5xxResponse => throw new FetchApplicationsFailed
      }
  }

  def fetchAllSubscriptions()(implicit hc: HeaderCarrier): Future[Seq[SubscriptionResponse]] = {
    http.GET[Seq[SubscriptionResponse]](s"$applicationBaseUrl/application/subscriptions")
      .recover {
        case e: Upstream5xxResponse => throw new FetchApplicationsFailed
      }
  }

  def fetchApplicationSubscriptions(applicationId: String)(implicit hc: HeaderCarrier): Future[Seq[Subscription]] = {
    http.GET[Seq[Subscription]](s"$applicationBaseUrl/application/$applicationId/subscription")
      .recover {
        case e: Upstream5xxResponse => throw new FetchApplicationSubscriptionsFailed
      }
  }

  def updateOverrides(applicationId: String, updateOverridesRequest: UpdateOverridesRequest)(implicit hc: HeaderCarrier): Future[UpdateOverridesResult] = {
    http.PUT(s"$applicationBaseUrl/application/$applicationId/access/overrides", updateOverridesRequest)
      .map(_ => UpdateOverridesSuccessResult)
      .recover {
        case e: Upstream4xxResponse => UpdateOverridesFailureResult()
      }
  }

  def updateScopes(applicationId: String, updateScopesRequest: UpdateScopesRequest)(implicit hc: HeaderCarrier): Future[UpdateScopesResult] = {
    http.PUT(s"$applicationBaseUrl/application/$applicationId/access/scopes", updateScopesRequest)
      .map(_ => UpdateScopesSuccessResult)
  }

  def subscribeToApi(applicationId: String, apiIdentifier: APIIdentifier)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    http.POST(s"$applicationBaseUrl/application/$applicationId/subscription", apiIdentifier, Seq(CONTENT_TYPE -> JSON)) map { _ =>
      ApplicationUpdateSuccessResult
    }
  }

  def unsubscribeFromApi(applicationId: String, context: String, version: String)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    http.DELETE(s"$applicationBaseUrl/application/$applicationId/subscription?context=$context&version=$version") map { _ =>
      ApplicationUpdateSuccessResult
    }
  }

  def deleteApplication(applicationId: String, deleteApplicationRequest: DeleteApplicationRequest)(implicit hc: HeaderCarrier): Future[ApplicationDeleteResult] = {
    http.POST(s"$applicationBaseUrl/application/$applicationId/delete", deleteApplicationRequest, Seq(CONTENT_TYPE -> JSON))
      .map(response => response.status match {
        case 204 => ApplicationDeleteSuccessResult
        case _ => ApplicationDeleteFailureResult
      })
      .recover {
        case _ => ApplicationDeleteFailureResult
      }
  }
}
