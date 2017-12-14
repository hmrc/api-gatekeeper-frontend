/*
 * Copyright 2017 HM Revenue & Customs
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

import connectors.ApplicationConnector
import model._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

object ApplicationService extends ApplicationService {
  override val applicationConnector = ApplicationConnector
}

trait ApplicationService {
  val applicationConnector: ApplicationConnector

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

  def fetchApplication(appId: String)(implicit hc: HeaderCarrier): Future[ApplicationWithHistory] = {
    applicationConnector.fetchApplication(appId)
  }

  def fetchApplicationScopes(appId: String)(implicit hc: HeaderCarrier): Future[None] = {
    applicationConnector.fetchApplicationScopes(appId)
  }

  def fetchAllSubscribedApplications(implicit hc: HeaderCarrier): Future[Seq[SubscribedApplicationResponse]] = {

    def addSubscriptionsToApplications(applications: Seq[ApplicationResponse], subscriptions: Seq[SubscriptionResponse]) = {

      applications.map(ar => {
        val filteredSubs = subscriptions.filter(_.applications.exists(_ == ar.id.toString))
          .map(_.apiIdentifier.context).sorted
        SubscribedApplicationResponse.createFrom(ar, filteredSubs)
      })
    }

    for {
      apps: Seq[ApplicationResponse] <- applicationConnector.fetchAllApplications()
      subs: Seq[SubscriptionResponse] <- applicationConnector.fetchAllSubscriptions()
      subscribedApplications = addSubscriptionsToApplications(apps, subs)
    } yield subscribedApplications.sortBy(_.name.toLowerCase)
  }
}
