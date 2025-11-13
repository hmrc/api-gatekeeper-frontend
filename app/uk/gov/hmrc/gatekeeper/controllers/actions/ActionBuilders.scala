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

import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

import play.api.mvc.Results.{BadRequest, NotFound}
import play.api.mvc.{MessagesRequest, Result}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaborators, ApplicationWithSubscriptionFields}
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.gatekeeper.config.ErrorHandler
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.services.{ApmService, ApplicationQueryService, ApplicationService}

trait ActionBuilders extends ApplicationLogger {
  def errorHandler: ErrorHandler
  def applicationService: ApplicationService
  def applicationQueryService: ApplicationQueryService
  def apmService: ApmService

  def withApp(
      appId: ApplicationId
    )(
      f: ApplicationWithCollaborators => Future[Result]
    )(implicit request: MessagesRequest[_],
      ec: ExecutionContext,
      hc: HeaderCarrier
    ): Future[Result] = {
    applicationQueryService.fetchApplication(appId).flatMap {
      case Some(app) => f(app)
      case None      => errorHandler.notFoundTemplate("Application not found").map(NotFound(_))
    }
  }

  def withStandardApp(
      appId: ApplicationId
    )(
      f: (ApplicationWithCollaborators, Access.Standard) => Future[Result]
    )(implicit request: MessagesRequest[_],
      ec: ExecutionContext,
      hc: HeaderCarrier
    ): Future[Result] = {
    applicationQueryService.fetchApplication(appId).flatMap(_.fold[Future[Result]](successful(NotFound))(app =>
      app.access match {
        case access: Access.Standard => f(app, access)
        case _                       => errorHandler.badRequestTemplate("Application must have standard access for this call").map(BadRequest(_))
      }
    ))
  }

  def withAppWithSubsFields(
      appId: ApplicationId
    )(
      f: ApplicationWithSubscriptionFields => Future[Result]
    )(implicit request: MessagesRequest[_],
      ec: ExecutionContext,
      hc: HeaderCarrier
    ): Future[Result] = {
    applicationQueryService.fetchApplicationWithSubscriptionFields(appId).flatMap {
      case Some(appWithSubsData) => f(appWithSubsData)
      case None                  => errorHandler.notFoundTemplate("Application not found").map(NotFound(_))
    }
  }

  def withAppWithSubsFieldsAndHistory(
      appId: ApplicationId
    )(
      f: ApplicationWithSubscriptionFieldsAndStateHistory => Future[Result]
    )(implicit request: MessagesRequest[_],
      ec: ExecutionContext,
      hc: HeaderCarrier
    ): Future[Result] = {
    applicationQueryService.fetchApplicationWithSubscriptionFieldsAndHistory(appId).flatMap {
      case Some(value) => f(value)
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
          allApiDefinitions <- apmService.getAllFieldDefinitions(applicationWithSubs.deployedTo)
          apiDefinitions     = filterApiDefinitions(allApiDefinitions, applicationWithSubs.subscriptions)
          allPossibleSubs   <- apmService.fetchAllPossibleSubscriptions(appId)
        } yield ApplicationWithSubscriptionDataAndFieldDefinitions(applicationWithSubs, apiDefinitions, allPossibleSubs)

        applicationWithSubscriptionDataAndFieldDefinitions.flatMap(appSubsData => action(appSubsData))
      }
      case None                      => errorHandler.notFoundTemplate("Application not found").map(NotFound(_))
    }
  }
}
