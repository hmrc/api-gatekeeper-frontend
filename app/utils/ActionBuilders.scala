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

package utils

import config.AppConfig
import model._
import play.api.i18n.Messages
import play.api.mvc.Result
import services.ApplicationService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait ActionBuilders extends ErrorHelper {
  val applicationService: ApplicationService

  def withApp(appId: String)(f: ApplicationWithHistory => Future[Result])
             (implicit request: LoggedInRequest[_], ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    applicationService.fetchApplication(appId).flatMap(f)
  }

  def withAppAndSubscriptions(appId: String)(action: ApplicationAndSubscriptionsWithHistory => Future[Result])
                             (implicit request: LoggedInRequest[_], ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {

    withApp(appId) {
      appWithHistory => {
        val app = appWithHistory.application
        applicationService.fetchApplicationSubscriptions(app).flatMap {
          allApis => {
            val subscriptions = filterSubscriptionsVersions(allApis)(v => v.subscribed)
            action(ApplicationAndSubscriptionsWithHistory(appWithHistory, subscriptions))
          }
        }
      }
    }
  }

  def withAppAndFieldDefinitions(appId: String)(action: ApplicationAndSubscribedFieldDefinitionsWithHistory => Future[Result])
                                (implicit request: LoggedInRequest[_], ec: ExecutionContext, hc: HeaderCarrier) : Future[Result] = {

    withAppAndSubscriptions(appId) {
      appWithFieldSubscriptions: ApplicationAndSubscriptionsWithHistory => {
        val app = appWithFieldSubscriptions.application
        val subscriptionsWithFieldDefinitions = filterHasSubscriptionFields(appWithFieldSubscriptions.subscriptions)
        action(ApplicationAndSubscribedFieldDefinitionsWithHistory(app, subscriptionsWithFieldDefinitions))
      }
    }
  }

  def withAppAndSubscriptionVersion(appId: String,
                                    apiContext: String,
                                    apiVersion: String)
                                   (action: ApplicationAndSubscriptionVersion => Future[Result])
                                   (implicit request: LoggedInRequest[_],
                                    messages: Messages,
                                    appConfig: AppConfig,
                                    ec: ExecutionContext,
                                    hc: HeaderCarrier): Future[Result] = {
    withAppAndSubscriptions(appId) {
      appWithFieldSubscriptions: ApplicationAndSubscriptionsWithHistory => {
        (for{
          subscription <- appWithFieldSubscriptions.subscriptions.find(sub => sub.context == apiContext)
          version <- subscription.versions.find(v => v.version.version == apiVersion)
        } yield action(ApplicationAndSubscriptionVersion(appWithFieldSubscriptions.application, subscription, version)))
          .getOrElse(Future.successful(notFound("Subscription or version not found")))
      }
    }
  }

  private def filterSubscriptionsVersions(subscriptions: Seq[Subscription])(predicate: VersionSubscription => Boolean): Seq[Subscription] = {
    subscriptions
      .map(api => api.copy(versions = api.versions.filter(predicate)))
      .filterNot(api => api.versions.isEmpty)
      .sortWith(_.name.toLowerCase < _.name.toLowerCase)
  }

  def filterHasSubscriptionFields(subscriptions : Seq[Subscription]) : Seq[Subscription] = {
    filterSubscriptionsVersions(subscriptions)(v => v.fields.fields.nonEmpty)
  }
}
