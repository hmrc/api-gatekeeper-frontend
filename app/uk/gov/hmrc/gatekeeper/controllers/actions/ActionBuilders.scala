/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.controllers.actions

import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.services.{ApmService, ApplicationService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.gatekeeper.utils.ApplicationLogger
import uk.gov.hmrc.gatekeeper.config.ErrorHandler

import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, NotFound}

import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInRequest

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.gatekeeper.models.ApiContext
import uk.gov.hmrc.gatekeeper.models.applications.ApplicationWithSubscriptionData

trait ActionBuilders extends ApplicationLogger {
  def errorHandler: ErrorHandler
  def applicationService: ApplicationService
  def apmService: ApmService

  def withApp(appId: ApplicationId)(f: ApplicationWithHistory => Future[Result])
             (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    applicationService.fetchApplication(appId).flatMap(f)
  }
  
  def withStandardApp(appId: ApplicationId)(f: (ApplicationWithHistory, Standard) => Future[Result])
             (implicit request: LoggedInRequest[_], ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    applicationService.fetchApplication(appId).flatMap(appWithHistory => appWithHistory.application.access match {
      case access : Standard => f(appWithHistory, access)
      case _ => Future.successful(BadRequest(errorHandler.badRequestTemplate("Application must have standard access for this call")))
    })
  }

  def withAppAndSubsData(appId: ApplicationId)(f: ApplicationWithSubscriptionData => Future[Result])
             (implicit request: LoggedInRequest[_], ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    apmService.fetchApplicationById(appId).flatMap {
      case Some(appWithSubsData) => f(appWithSubsData)
      case None => Future.successful(NotFound(errorHandler.notFoundTemplate("Application not found")))
    }
  }

  def withAppAndSubscriptionsAndStateHistory(appId: ApplicationId)(action: ApplicationWithSubscriptionDataAndStateHistory => Future[Result])
                                         (implicit request: LoggedInRequest[_], ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    apmService.fetchApplicationById(appId).flatMap {
      case Some(value) =>
        logger.info(s"FETCHED VALUE - $value")
        applicationService.fetchStateHistory(appId).flatMap(history => action(ApplicationWithSubscriptionDataAndStateHistory(value, history)))
      case None => Future.successful(NotFound(errorHandler.notFoundTemplate("Application not found")))
    }
  }

  private def filterApiDefinitions(allApiDefintions: ApiDefinitions.Alias, applicationSubscriptions: Set[ApiIdentifier]) : ApiDefinitions.Alias = {
    val apiContexts: List[ApiContext] = applicationSubscriptions.map(apiIdentifier => apiIdentifier.context).toList
    
    val apiDefinitionsFilteredByContext = allApiDefintions.filter(contextMap => apiContexts.contains(contextMap._1))

    apiDefinitionsFilteredByContext.map(contextMap =>
      contextMap._1 -> contextMap._2.filter(versionMap =>
        applicationSubscriptions.contains(ApiIdentifier(contextMap._1, versionMap._1))
      )
    )
  }

  def withAppAndSubscriptionsAndFieldDefinitions(appId: ApplicationId)(action: ApplicationWithSubscriptionDataAndFieldDefinitions => Future[Result])
                                              (implicit request: LoggedInRequest[_], ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    apmService.fetchApplicationById(appId).flatMap {
      case Some(applicationWithSubs) => {
        val applicationWithSubscriptionDataAndFieldDefinitions = for {
          allApiDefinitions <- apmService.getAllFieldDefinitions(applicationWithSubs.application.deployedTo)
          apiDefinitions = filterApiDefinitions(allApiDefinitions, applicationWithSubs.subscriptions)
          allPossibleSubs <- apmService.fetchAllPossibleSubscriptions(appId)
        } yield ApplicationWithSubscriptionDataAndFieldDefinitions(applicationWithSubs, apiDefinitions, allPossibleSubs)

        applicationWithSubscriptionDataAndFieldDefinitions.flatMap(appSubsData => action(appSubsData))
      }
      case None => Future.successful(NotFound(errorHandler.notFoundTemplate("Application not found")))
    }
  }
}
