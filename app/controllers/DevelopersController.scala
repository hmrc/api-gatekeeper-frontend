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

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

import config.AppConfig
import connectors.AuthConnector
import model._
import services.{ApiDefinitionService, ApmService, ApplicationService, DeveloperService}
import utils.{ActionBuilders, ErrorHelper, GatekeeperAuthWrapper}
import views.html.developers._
import views.html.{ErrorTemplate, ForbiddenView}

import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

@Singleton
class DevelopersController @Inject()(developerService: DeveloperService,
                                     val applicationService: ApplicationService,
                                     val forbiddenView: ForbiddenView,
                                     apiDefinitionService: ApiDefinitionService,
                                     override val authConnector: AuthConnector,
                                     mcc: MessagesControllerComponents,
                                     developerDetailsView: DeveloperDetailsView,
                                     removeMfaView: RemoveMfaView,
                                     removeMfaSuccessView: RemoveMfaSuccessView,
                                     deleteDeveloperView: DeleteDeveloperView,
                                     deleteDeveloperSuccessView: DeleteDeveloperSuccessView,
                                     override val errorTemplate: ErrorTemplate,
                                     val apmService: ApmService
                                    )(implicit val appConfig: AppConfig, val ec: ExecutionContext)
  extends FrontendController(mcc) with ErrorHelper with GatekeeperAuthWrapper with ActionBuilders with I18nSupport {

  // def developersPage(filter: Option[String], status: Option[String], environment: Option[String]) = requiresAtLeast(GatekeeperRole.USER) {
  //   implicit request =>

  //     val apiFilter = ApiFilter(filter)
  //     val statusFilter = StatusFilter(status)
  //     val apiSubscriptionInEnvironmentFilter = ApiSubscriptionInEnvironmentFilter(environment)

  //     val appsF = applicationService.fetchApplications(apiFilter, apiSubscriptionInEnvironmentFilter)
  //     val apisF = apiDefinitionService.fetchAllApiDefinitions()
  //     val usersF = developerService.fetchUsers

  //     for {
  //       apps <- appsF
  //       apis <- apisF
  //       users <- usersF
  //       filterOps = (developerService.filterUsersBy(apiFilter, apps) _
  //         andThen developerService.filterUsersBy(statusFilter))
  //       devs = developerService.getDevelopersWithApps(apps, users)
  //       filteredUsers = filterOps(devs)
  //       sortedDevelopers = filteredUsers.sortBy(_.user.email.toLowerCase)
  //       emails = sortedDevelopers.map(_.user.email).mkString("; ")
  //     } yield Ok(developersView(sortedDevelopers, emails, groupApisByStatus(apis), filter, status, environment))
  // }

  def developerPage(developerId: DeveloperIdentifier): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      developerService.fetchDeveloper(developerId).map(developer => Ok(developerDetailsView(developer, isAtLeastSuperUser)))
  }

  def removeMfaPage(developerIdentifier: DeveloperIdentifier): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      developerService.fetchDeveloper(developerIdentifier).map(developer => Ok(removeMfaView(developer)))
  }

  def removeMfaAction(developerIdentifier: DeveloperIdentifier): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      developerService.removeMfa(developerIdentifier, loggedIn.userFullName.get) map { user =>
        Ok(removeMfaSuccessView(user.email))
      } recover {
        case e: Exception =>
          Logger.error(s"Failed to remove MFA for user: $developerIdentifier", e)
          technicalDifficulties
      }
  }

  def deleteDeveloperPage(developerIdentifier: DeveloperIdentifier) = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit request =>
      developerService.fetchDeveloper(developerIdentifier).map(developer => Ok(deleteDeveloperView(developer)))
  }

  def deleteDeveloperAction(developerId: DeveloperIdentifier) = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit request =>
      developerService.deleteDeveloper(developerId, loggedIn.userFullName.get).map {
        case (DeveloperDeleteSuccessResult, developer) => Ok(deleteDeveloperSuccessView(developer.email))
        case _ => technicalDifficulties
      }
  }

  private def groupApisByStatus(apis: List[ApiDefinition]): Map[String, List[VersionSummary]] = {
    val versions = for {
      api <- apis
      version <- api.versions
    } yield VersionSummary(api.name, version.status, ApiIdentifier(api.context, version.version))

    versions.groupBy(v => ApiStatus.displayedStatus(v.status))
  }
}
