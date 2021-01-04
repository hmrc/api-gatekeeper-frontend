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

package utils

import model._
import play.api.i18n.Messages
import play.api.mvc.Result
import services.{ApmService, ApplicationService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import model.ApiContext
import model.applications.ApplicationWithSubscriptionData

trait ActionBuilders extends ErrorHelper {
  val applicationService: ApplicationService
  val apmService: ApmService

  def withApp(appId: ApplicationId)(f: ApplicationWithHistory => Future[Result])
             (implicit request: LoggedInRequest[_], ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    applicationService.fetchApplication(appId).flatMap(f)
  }
  
  def withAppAndSubsData(appId: ApplicationId)(f: ApplicationWithSubscriptionData => Future[Result])
             (implicit request: LoggedInRequest[_], messages: Messages, ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    apmService.fetchApplicationById(appId).flatMap {
      case Some(appWithSubsData) => f(appWithSubsData)
      case None => Future.successful(notFound("Application not found"))
    }
  }

  def withAppAndSubscriptionsAndStateHistory(appId: ApplicationId)(action: ApplicationWithSubscriptionDataAndStateHistory => Future[Result])
                                         (implicit request: LoggedInRequest[_], messages: Messages, ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    apmService.fetchApplicationById(appId).flatMap {
      case Some(value) =>
        applicationService.fetchStateHistory(appId).flatMap(history => action(ApplicationWithSubscriptionDataAndStateHistory(value, history)))
      case None => Future.successful(notFound("Application not found"))
    }
  }

  private def filterApiDefinitions(allApiDefintions: ApiDefinitions.Alias, applicationSubscriptions: Set[ApiIdentifier]) : ApiDefinitions.Alias = {
    val apiContexts: Seq[ApiContext] = applicationSubscriptions.map(apiIdentifier => apiIdentifier.context).toSeq
    
    val apiDefinitionsFilteredByContext = allApiDefintions.filter(contextMap => apiContexts.contains(contextMap._1))

    apiDefinitionsFilteredByContext.map(contextMap =>
      contextMap._1 -> contextMap._2.filter(versionMap =>
        applicationSubscriptions.contains(ApiIdentifier(contextMap._1, versionMap._1))
      )
    )
  }

  def withAppAndSubscriptionsAndFieldDefinitions(appId: ApplicationId)(action: ApplicationWithSubscriptionDataAndFieldDefinitions => Future[Result])
                                              (implicit request: LoggedInRequest[_], messages: Messages, ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    apmService.fetchApplicationById(appId).flatMap {
      case Some(applicationWithSubs) => {
        val applicationWithSubscriptionDataAndFieldDefinitions = for {
          allApiDefinitions <- apmService.getAllFieldDefinitions(applicationWithSubs.application.deployedTo)
          apiDefinitions = filterApiDefinitions(allApiDefinitions, applicationWithSubs.subscriptions)
          allPossibleSubs <- apmService.fetchAllPossibleSubscriptions(appId)
        } yield ApplicationWithSubscriptionDataAndFieldDefinitions(applicationWithSubs, apiDefinitions, allPossibleSubs)

        applicationWithSubscriptionDataAndFieldDefinitions.flatMap(appSubsData => action(appSubsData))
      }
      case None => Future.successful(notFound("Application not found"))
    }
  }
}
