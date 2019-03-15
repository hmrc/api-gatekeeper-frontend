/*
 * Copyright 2019 HM Revenue & Customs
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
import config.AppConfig
import connectors.AuthConnector
import model._
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{ApiDefinitionService, ApplicationService, DeveloperService}
import utils.GatekeeperAuthWrapper
import views.html.developers._

@Singleton
class DevelopersController @Inject()(developerService: DeveloperService,
                                     applicationService: ApplicationService,
                                     apiDefinitionService: ApiDefinitionService,
                                     override val authConnector: AuthConnector)(override implicit val appConfig: AppConfig)
  extends BaseController with GatekeeperAuthWrapper {

  def developersPage(filter: Option[String], status: Option[String], environment: Option[String]) = requiresAtLeast(GatekeeperRole.USER) {
    implicit request => implicit hc =>

      val apiFilter = ApiFilter(filter)
      val statusFilter = StatusFilter(status)
      val apiSubscriptionInEnvironmentFilter = ApiSubscriptionInEnvironmentFilter(environment)

      val appsF = applicationService.fetchApplications(apiFilter, apiSubscriptionInEnvironmentFilter)
      val apisF = apiDefinitionService.fetchAllApiDefinitions()
      val usersF = developerService.fetchUsers

      for {
        apps <- appsF
        apis <- apisF
        users <- usersF
        filterOps = (developerService.filterUsersBy(apiFilter, apps) _
          andThen developerService.filterUsersBy(statusFilter))
        devs = developerService.getDevelopersWithApps(apps, users)
        filteredUsers = filterOps(devs)
        sortedUsers = filteredUsers.sortBy(_.email.toLowerCase)
        emails = sortedUsers.map(_.email).mkString("; ")
      } yield Ok(developers(sortedUsers, emails, groupApisByStatus(apis), filter, status, environment))
  }

  def developerPage(email: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request => implicit hc =>
      developerService.fetchDeveloper(email).map(developer => Ok(developer_details(developer.toDeveloper, isAtLeastSuperUser)))
  }

  def removeMfaPage(email: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit request => implicit hc =>
      developerService.fetchDeveloper(email).map(developer => Ok(remove_mfa(developer.toDeveloper)))
  }

  def removeMfaAction(email:String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit request => implicit hc =>
      developerService.removeMfa(email, loggedIn.get) map { _ =>
        Ok(remove_mfa_success(email))
      } recover {
        case e: Exception =>
          Logger.error(s"Failed to remove MFA for user: $email", e)
          technicalDifficulties
      }
  }

  def deleteDeveloperPage(email: String) = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit request => implicit hc =>
      developerService.fetchDeveloper(email).map(developer => Ok(delete_developer(developer.toDeveloper)))
  }

  def deleteDeveloperAction(email:String) = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit request => implicit hc =>
      developerService.deleteDeveloper(email, loggedIn.get).map {
        case DeveloperDeleteSuccessResult => Ok(delete_developer_success(email))
        case _ => technicalDifficulties
      }
  }

  private def groupApisByStatus(apis: Seq[APIDefinition]): Map[String, Seq[VersionSummary]] = {

    val versions = for {
      api <- apis
      version <- api.versions
    } yield VersionSummary(api.name, version.status, APIIdentifier(api.context, version.version))

    versions.groupBy(v => APIStatus.displayedStatus(v.status))
  }
}
