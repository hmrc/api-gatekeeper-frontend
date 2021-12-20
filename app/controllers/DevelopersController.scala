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

package controllers

import config.{AppConfig, ErrorHandler}
import javax.inject.{Inject, Singleton}
import model._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ApiDefinitionService, ApplicationService, DeveloperService, ApmService}
import utils.ErrorHelper
import views.html.{ErrorTemplate, ForbiddenView}
import views.html.developers._
import utils.ApplicationLogger
import controllers.actions.ActionBuilders

import uk.gov.hmrc.modules.stride.controllers.GatekeeperBaseController
import uk.gov.hmrc.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.modules.stride.controllers.actions.ForbiddenHandler
import uk.gov.hmrc.modules.stride.connectors.AuthConnector

import scala.concurrent.ExecutionContext

@Singleton
class DevelopersController @Inject()(
  developerService: DeveloperService,
  val applicationService: ApplicationService,
  val forbiddenView: ForbiddenView,
  apiDefinitionService: ApiDefinitionService,
  mcc: MessagesControllerComponents,
  developerDetailsView: DeveloperDetailsView,
  removeMfaView: RemoveMfaView,
  removeMfaSuccessView: RemoveMfaSuccessView,
  deleteDeveloperView: DeleteDeveloperView,
  deleteDeveloperSuccessView: DeleteDeveloperSuccessView,
  override val errorTemplate: ErrorTemplate,
  val apmService: ApmService,
  val errorHandler: ErrorHandler,
  strideAuthConfig: StrideAuthConfig,
  authConnector: AuthConnector,
  forbiddenHandler: ForbiddenHandler
)(implicit val appConfig: AppConfig, override val ec: ExecutionContext)
  extends GatekeeperBaseController(strideAuthConfig, authConnector, forbiddenHandler, mcc)
    with ErrorHelper
    with ActionBuilders
    with ApplicationLogger {

  // TODO - Find better way of passing strid auth config to helper functions like isAtLeastSuperUser
  implicit val authConfig = strideAuthConfig

  def developerPage(developerId: DeveloperIdentifier): Action[AnyContent] = anyStrideUserAction { implicit request =>
    developerService.fetchDeveloper(developerId).map(developer => Ok(developerDetailsView(developer, isAtLeastSuperUser)))
  }

  def removeMfaPage(developerIdentifier: DeveloperIdentifier): Action[AnyContent] = anyStrideUserAction { implicit request =>
    developerService.fetchDeveloper(developerIdentifier).map(developer => Ok(removeMfaView(developer)))
  }

  def removeMfaAction(developerIdentifier: DeveloperIdentifier): Action[AnyContent] = anyStrideUserAction { implicit request =>
    developerService.removeMfa(developerIdentifier, loggedIn.userFullName.get) map { user =>
      Ok(removeMfaSuccessView(user.email))
    } recover {
      case e: Exception =>
        logger.error(s"Failed to remove MFA for user: $developerIdentifier", e)
        technicalDifficulties
    }
  }

  def deleteDeveloperPage(developerIdentifier: DeveloperIdentifier) = atLeastSuperUserAction { implicit request =>
    developerService.fetchDeveloper(developerIdentifier).map(developer => Ok(deleteDeveloperView(developer)))
  }

  def deleteDeveloperAction(developerId: DeveloperIdentifier) = atLeastSuperUserAction { implicit request =>
    developerService.deleteDeveloper(developerId, loggedIn.userFullName.get).map {
      case (DeveloperDeleteSuccessResult, developer) => Ok(deleteDeveloperSuccessView(developer.email))
      case _ => technicalDifficulties
    }
  }
}
