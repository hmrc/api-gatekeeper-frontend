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

import model._
import play.api.mvc.{Request, Result, Results}
import services.ApplicationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import config.AppConfig

import scala.concurrent.{ExecutionContext, Future}
import views.html.error_template
import play.api.i18n.Messages

trait ActionBuilders extends Results with TemplatedErrorResults { 
  val applicationService: ApplicationService

  private implicit def hc(implicit request: Request[_]): HeaderCarrier =
    HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

  def withApp(appId: String)(f: ApplicationWithHistory => Future[Result])
             (implicit request: LoggedInRequest[_], ec: ExecutionContext): Future[Result] = {
    applicationService.fetchApplication(appId).flatMap(f)
  }

  def withAppAndSubscriptions(appId: String, withSubFields: Boolean)(action: ApplicationAndSubscriptionsWithHistory => Future[Result])
                             (implicit request: LoggedInRequest[_], ec: ExecutionContext): Future[Result] = {

    withApp(appId) {
      appWithHistory => {
        val app = appWithHistory.application
        applicationService.fetchApplicationSubscriptions(app, withFields = withSubFields).flatMap {
          allApis => {
            val subscriptions = filterSubscriptionsVersions(allApis)(v => v.subscribed)
            action(ApplicationAndSubscriptionsWithHistory(appWithHistory, subscriptions))
          }
        }
      }
    }
  }

  def withAppAndFieldDefinitions(appId: String)(action: ApplicationAndSubscribedFieldDefinitionsWithHistory => Future[Result])
                                (implicit request: LoggedInRequest[_], ec: ExecutionContext): Future[Result] = {

    withAppAndSubscriptions(appId, true) {
      appWithFieldSubscriptions: ApplicationAndSubscriptionsWithHistory => {
        val app = appWithFieldSubscriptions.application
        val subscriptionsWithFieldDefinitions = filterSubscriptionsVersions(appWithFieldSubscriptions.subscriptions)(v => v.fields.fold(false)(fields => fields.fields.nonEmpty))
        action(ApplicationAndSubscribedFieldDefinitionsWithHistory(app, subscriptionsWithFieldDefinitions))
      }
    }
  }

 

  def withAppAndSubscriptionVersion(appId: String, apiContext: String, apiVersion: String)(action: ApplicationAndSubscriptionVersion => Future[Result])
                                (implicit request: LoggedInRequest[_], ec: ExecutionContext, messages: Messages, appConfig: AppConfig): Future[Result] = {

    withAppAndSubscriptions(appId, true) {
      
      appWithFieldSubscriptions: ApplicationAndSubscriptionsWithHistory => {

        (for{
          subscription <- appWithFieldSubscriptions.subscriptions.find(sub => sub.context == apiContext)
          version <- subscription.versions.find(v => v.version.version == apiVersion)
        } yield(action(ApplicationAndSubscriptionVersion(appWithFieldSubscriptions.application, subscription, version))))
          .getOrElse(notFound())
      }
    }
  }

  private def filterSubscriptionsVersions(subscriptions: Seq[Subscription])(predicate: VersionSubscription => Boolean): Seq[Subscription] = {
    subscriptions
      .map(api => api.copy(versions = api.versions.filter(predicate)))
      .filterNot(api => api.versions.isEmpty)
      .sortWith(_.name.toLowerCase < _.name.toLowerCase)
  }
}
