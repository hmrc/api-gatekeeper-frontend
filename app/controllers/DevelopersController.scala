/*
 * Copyright 2018 HM Revenue & Customs
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

import connectors.AuthConnector
import model._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{ApiDefinitionService, ApplicationService, DeveloperService}
import utils.{ApplicationHelper, GatekeeperAuthProvider, GatekeeperAuthWrapper}
import views.html.developers._
import views.html.error_template

import scala.concurrent.Future

object DevelopersController extends DevelopersController with WithAppConfig {
  override val developerService = DeveloperService
  override val applicationService = ApplicationService
  override val apiDefinitionService = ApiDefinitionService
  override def authConnector = AuthConnector
  override def authProvider = GatekeeperAuthProvider
}

trait DevelopersController extends BaseController with GatekeeperAuthWrapper {
  val applicationService: ApplicationService
  val developerService: DeveloperService
  val apiDefinitionService: ApiDefinitionService

  def developersPage(filter: Option[String], status: Option[String]) = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>

      val apiFilter = ApiFilter(filter)
      val statusFilter = StatusFilter(status)

      val appsF = applicationService.fetchApplications(apiFilter)
      val apisF = apiDefinitionService.fetchAllApiDefinitions

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
      } yield Ok(developers(sortedUsers, emails, groupApisByStatus(apis), filter, status))
  }

  def developerPage(email: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
      developerService.fetchDeveloper(email).map(developer => Ok(developer_details(developer.toDeveloper, isSuperUser)))
  }

  def deleteDeveloperPage(email: String) = requiresRole(Role.APIGatekeeper, requiresSuperUser = true) {
    implicit request => implicit hc =>
      developerService.fetchDeveloper(email).map(developer => Ok(delete_developer(developer.toDeveloper)))
  }

  def deleteDeveloperAction(email:String) = requiresRole(Role.APIGatekeeper, requiresSuperUser = true) {
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
