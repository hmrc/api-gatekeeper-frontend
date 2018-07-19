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

package services

import connectors.{ApiScopeConnector, ApplicationConnector}
import model.Environment.Environment
import model.RateLimitTier.RateLimitTier
import model._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ApplicationService extends ApplicationService {
  override val applicationConnector = ApplicationConnector
  override val apiScopeConnector = ApiScopeConnector
}

trait ApplicationService {
  val applicationConnector: ApplicationConnector
  val apiScopeConnector: ApiScopeConnector

  def resendVerification(applicationId: String, gatekeeperUserId: String)
                        (implicit hc: HeaderCarrier): Future[ResendVerificationSuccessful] = {
    applicationConnector.resendVerification(applicationId, gatekeeperUserId)
  }

  def fetchApplications(implicit hc: HeaderCarrier): Future[Seq[ApplicationResponse]] = {
    applicationConnector.fetchAllApplications()
  }

  def fetchApplications(filter: ApiFilter[String])(implicit hc: HeaderCarrier): Future[Seq[ApplicationResponse]] = {
    filter match {
      case OneOrMoreSubscriptions => for {
        all <- applicationConnector.fetchAllApplications()
        noSubs <- applicationConnector.fetchAllApplicationsWithNoSubscriptions()
      } yield {
        all.filterNot(app => noSubs.contains(app))
      }
      case NoSubscriptions => applicationConnector.fetchAllApplicationsWithNoSubscriptions()
      case Value(flt) => applicationConnector.fetchAllApplicationsBySubscription(flt)
      case _ => applicationConnector.fetchAllApplications()
    }
  }

  def fetchApplicationsByEmail(email: String)(implicit hc: HeaderCarrier) = {
    applicationConnector.fetchApplicationsByEmail(email)
  }

  def fetchApplication(appId: String)(implicit hc: HeaderCarrier): Future[ApplicationWithHistory] = {
    applicationConnector.fetchApplication(appId)
  }

  def fetchAllSubscribedApplications(implicit hc: HeaderCarrier): Future[Seq[SubscribedApplicationResponse]] = {

    def addSubscriptionsToApplications(applications: Seq[ApplicationResponse], subscriptions: Seq[SubscriptionResponse]) = {

      applications.map(ar => {
        val filteredSubs = subscriptions.filter(_.applications.exists(_ == ar.id.toString))
          .map(sub => SubscriptionNameAndVersion(sub.apiIdentifier.context, sub.apiIdentifier.version)).sortBy(sub => sub.name)
        SubscribedApplicationResponse.createFrom(ar, filteredSubs)
      })
    }

    for {
      apps: Seq[ApplicationResponse] <- applicationConnector.fetchAllApplications()
      subs: Seq[SubscriptionResponse] <- applicationConnector.fetchAllSubscriptions()
      subscribedApplications = addSubscriptionsToApplications(apps, subs)
    } yield subscribedApplications.sortBy(_.name.toLowerCase)
  }

  def fetchApplicationSubscriptions(applicationId: String)(implicit hc: HeaderCarrier): Future[Seq[Subscription]] = {
    applicationConnector.fetchApplicationSubscriptions(applicationId)
  }

  def updateOverrides(application: ApplicationResponse, overrides: Set[OverrideFlag])(implicit hc: HeaderCarrier): Future[UpdateOverridesResult] = {
    def findOverrideTypesWithInvalidScopes(overrides: Set[OverrideFlag], validScopes: Set[String]): Future[Set[OverrideFlag]] = {
      def containsInvalidScopes(validScopes: Set[String], scopes: Set[String]) = {
        !scopes.forall(validScopes)
      }

      def doesOverrideTypeContainInvalidScopes(overrideFlag: OverrideFlag): Boolean = overrideFlag match {
        case overrideFlagWithScopes: OverrideFlagWithScopes => containsInvalidScopes(validScopes, overrideFlagWithScopes.scopes)
        case _ => false
      }

      Future.successful(overrides.filter(doesOverrideTypeContainInvalidScopes))
    }

    application.access match {
      case _: Standard => {
        (
          for {
            validScopes <- apiScopeConnector.fetchAll()
            overrideTypesWithInvalidScopes <- findOverrideTypesWithInvalidScopes(overrides, validScopes.map(_.key).toSet)
          } yield overrideTypesWithInvalidScopes
        ).flatMap(overrideTypes =>
          if(overrideTypes.nonEmpty) {
            Future.successful(UpdateOverridesFailureResult(overrideTypes))
          } else {
            applicationConnector.updateOverrides(application.id.toString, UpdateOverridesRequest(overrides))
          }
        )
      }
    }
  }

  def updateScopes(application: ApplicationResponse, scopes: Set[String])(implicit hc: HeaderCarrier): Future[UpdateScopesResult] = {

    application.access match {
      case _: AccessWithRestrictedScopes => {
        (
          for {
            validScopes <- apiScopeConnector.fetchAll()
            hasInvalidScopes = !scopes.subsetOf(validScopes.map(_.key).toSet)
          } yield hasInvalidScopes
        ).flatMap(hasInvalidScopes =>
          if (hasInvalidScopes) Future.successful(UpdateScopesInvalidScopesResult)
          else applicationConnector.updateScopes(application.id.toString, UpdateScopesRequest(scopes))
        )
      }
    }
  }

  def subscribeToApi(applicationId: String, context: String, version: String)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    applicationConnector.subscribeToApi(applicationId, APIIdentifier(context, version))
  }

  def unsubscribeFromApi(applicationId: String, context: String, version: String)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    applicationConnector.unsubscribeFromApi(applicationId, context, version)
  }

  def updateRateLimitTier(applicationId: String, tier: RateLimitTier)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    applicationConnector.updateRateLimitTier(applicationId, tier)
  }

  def deleteApplication(applicationId: String, gatekeeperUserId: String, requestByEmailAddress: String)(implicit hc: HeaderCarrier): Future[ApplicationDeleteResult] = {
    applicationConnector.deleteApplication(applicationId, DeleteApplicationRequest(gatekeeperUserId, requestByEmailAddress))
  }

  def approveUplift(applicationId: String, gatekeeperUserId: String)(implicit hc: HeaderCarrier): Future[ApproveUpliftSuccessful] = {
    applicationConnector.approveUplift(applicationId, gatekeeperUserId)
  }

  def rejectUplift(applicationId: String, gatekeeperUserId: String, rejectionReason: String)
                  (implicit hc: HeaderCarrier): Future[RejectUpliftSuccessful] = {
    applicationConnector.rejectUplift(applicationId, gatekeeperUserId, rejectionReason)
  }

  def createPrivOrROPCApp(appEnv: Environment, appName: String, appDescription: String, collaborators: Seq[Collaborator], access: AppAccess)(implicit hc: HeaderCarrier): Future[CreatePrivOrROPCAppResult] = {
    applicationConnector.createPrivOrROPCApp(CreatePrivOrROPCAppRequest(appEnv.toString, appName, appDescription, collaborators, access))
  }

}
