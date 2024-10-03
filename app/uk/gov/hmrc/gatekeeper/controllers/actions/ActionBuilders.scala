/*
 * Copyright 2023 HM Revenue & Customs
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

import scala.concurrent.{ExecutionContext, Future}

import play.api.mvc.Results.{BadRequest, NotFound}
import play.api.mvc.{MessagesRequest, Result}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithSubscriptionFields
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.gatekeeper.config.ErrorHandler
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.services.{ApmService, ApplicationService}

trait ActionBuilders extends ApplicationLogger {
  def errorHandler: ErrorHandler
  def applicationService: ApplicationService
  def apmService: ApmService

  def withApp(appId: ApplicationId)(f: ApplicationWithHistory => Future[Result])(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    applicationService.fetchApplication(appId).flatMap(f)
  }

  def withStandardApp(
      appId: ApplicationId
    )(
      f: (ApplicationWithHistory, Access.Standard) => Future[Result]
    )(implicit request: MessagesRequest[_],
      ec: ExecutionContext,
      hc: HeaderCarrier
    ): Future[Result] = {
    applicationService.fetchApplication(appId).flatMap(appWithHistory =>
      appWithHistory.application.access match {
        case access: Access.Standard => f(appWithHistory, access)
        case _                       => errorHandler.badRequestTemplate("Application must have standard access for this call").map(BadRequest(_))
      }
    )
  }

  def withAppAndSubsData(
      appId: ApplicationId
    )(
      f: ApplicationWithSubscriptionFields => Future[Result]
    )(implicit request: MessagesRequest[_],
      ec: ExecutionContext,
      hc: HeaderCarrier
    ): Future[Result] = {
    apmService.fetchApplicationById(appId).flatMap {
      case Some(appWithSubsData) => f(appWithSubsData)
      case None                  => errorHandler.notFoundTemplate("Application not found").map(NotFound(_))
    }
  }

  def withAppAndSubscriptionsAndStateHistory(
      appId: ApplicationId
    )(
      action: ApplicationWithSubscriptionDataAndStateHistory => Future[Result]
    )(implicit request: MessagesRequest[_],
      ec: ExecutionContext,
      hc: HeaderCarrier
    ): Future[Result] = {
    apmService.fetchApplicationById(appId).flatMap {
      case Some(value) =>
        logger.info(s"FETCHED VALUE - $value")
        applicationService.fetchStateHistory(appId, value.details.deployedTo).flatMap(history => action(ApplicationWithSubscriptionDataAndStateHistory(value, history)))
      case None        => errorHandler.notFoundTemplate("Application not found").map(NotFound(_))
    }
  }

  private def filterApiDefinitions(allApiDefintions: ApiDefinitionFields.Alias, applicationSubscriptions: Set[ApiIdentifier]): ApiDefinitionFields.Alias = {
    val apiContexts: List[ApiContext] = applicationSubscriptions.map(apiIdentifier => apiIdentifier.context).toList

    val apiDefinitionsFilteredByContext = allApiDefintions.filter(contextMap => apiContexts.contains(contextMap._1))

    apiDefinitionsFilteredByContext.map(contextMap =>
      contextMap._1 -> contextMap._2.filter(versionMap =>
        applicationSubscriptions.contains(ApiIdentifier(contextMap._1, versionMap._1))
      )
    )
  }

  def withAppAndSubscriptionsAndFieldDefinitions(
      appId: ApplicationId
    )(
      action: ApplicationWithSubscriptionDataAndFieldDefinitions => Future[Result]
    )(implicit request: MessagesRequest[_],
      ec: ExecutionContext,
      hc: HeaderCarrier
    ): Future[Result] = {
    apmService.fetchApplicationById(appId).flatMap {
      case Some(applicationWithSubs) => {
        val applicationWithSubscriptionDataAndFieldDefinitions = for {
          allApiDefinitions <- apmService.getAllFieldDefinitions(applicationWithSubs.details.deployedTo)
          apiDefinitions     = filterApiDefinitions(allApiDefinitions, applicationWithSubs.subscriptions)
          allPossibleSubs   <- apmService.fetchAllPossibleSubscriptions(appId)
        } yield ApplicationWithSubscriptionDataAndFieldDefinitions(applicationWithSubs, apiDefinitions, allPossibleSubs)

        applicationWithSubscriptionDataAndFieldDefinitions.flatMap(appSubsData => action(appSubsData))
      }
      case None                      => errorHandler.notFoundTemplate("Application not found").map(NotFound(_))
    }
  }
}
