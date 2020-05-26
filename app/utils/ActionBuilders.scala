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

import model.{ApplicationAndSubscribedFieldDefinitionsWithHistory, ApplicationAndSubscriptionsWithHistory, ApplicationWithHistory, VersionSubscription}
import play.api.mvc.{Request, Result}
import services.ApplicationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import views.html.applications.manage_subscriptions

import scala.concurrent.{ExecutionContext, Future}

trait ActionBuilders {
  val applicationService: ApplicationService

  private implicit def hc(implicit request: Request[_]): HeaderCarrier =
    HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

  def withApp(appId: String)(f: ApplicationWithHistory => Future[Result])
                     (implicit request: LoggedInRequest[_], ec: ExecutionContext): Future[Result] = {
    applicationService.fetchApplication(appId).flatMap(f)
  }

  //TODO: withSubFields flag better solution?
  def withAppAndSubscriptions(appId: String, withSubFields: Boolean)(f: ApplicationAndSubscriptionsWithHistory => Future[Result])
                             (implicit request: LoggedInRequest[_], ec: ExecutionContext): Future[Result] = {

    withApp(appId){
      appWithHistory => {

        val app = appWithHistory.application

        applicationService.fetchApplicationSubscriptions(app, withFields = withSubFields).flatMap{
          allApis => {

            // TODO: This is complex :(
            val subscriptions = allApis
              .map(api => api.copy(versions = api.versions.filter(v => v.subscribed)))
              .filterNot(api => api.versions.isEmpty)
              .sortWith(_.name.toLowerCase < _.name.toLowerCase)

            f(ApplicationAndSubscriptionsWithHistory(appWithHistory, subscriptions))
          }
        }
      }
    }
  }

  def withAppAndFieldDefinitions(appId: String)(f: ApplicationAndSubscribedFieldDefinitionsWithHistory => Future[Result])
                             (implicit request: LoggedInRequest[_], ec: ExecutionContext): Future[Result] = {

    withAppAndSubscriptions(appId, true){
      appWithFieldSubscriptions: ApplicationAndSubscriptionsWithHistory => {

        val app = appWithFieldSubscriptions.application

        // TODO: This is complex :(
        // Can we make a common version predicate filter.
        val subscriptionsWithFieldDefinitions = appWithFieldSubscriptions
          .subscriptions.map(s=>s.copy(versions = s.versions.filter(v=>v.fields.fold(false)(fields => fields.fields.nonEmpty))))
          .filterNot(s => s.versions.isEmpty)

            f(ApplicationAndSubscribedFieldDefinitionsWithHistory(app, subscriptionsWithFieldDefinitions))
          }
    }
  }
}
