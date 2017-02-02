/*
 * Copyright 2017 HM Revenue & Customs
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

import connectors.{ApiDefinitionConnector, AuthConnector}
import model._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import services.{ApplicationService, DeveloperService}
import utils.{GatekeeperAuthProvider, GatekeeperAuthWrapper}
import views.html.developers.developers

object DevelopersController extends DevelopersController {
  override val developerService = DeveloperService
  override val applicationService = ApplicationService
  override val apiDefinitionConnector = ApiDefinitionConnector
  override def authConnector = AuthConnector
  override def authProvider = GatekeeperAuthProvider
}

trait DevelopersController extends BaseController with GatekeeperAuthWrapper {
  val applicationService: ApplicationService
  val developerService: DeveloperService
  val apiDefinitionConnector: ApiDefinitionConnector

  def developersPage(filter: Option[String], status: Option[String]) = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>

      val apiFilter = ApiFilter(filter)
      val statusFilter = StatusFilter(status)

      for {
        apps <- applicationService.fetchApplications(apiFilter)
        apis <- apiDefinitionConnector.fetchAll
        devs <- developerService.fetchDevelopers(apps)
        filterOps = (developerService.filterUsersBy(apiFilter, apps) _
          andThen developerService.filterUsersBy(statusFilter))
        filteredUsers = filterOps(devs)
        sortedUsers = filteredUsers.sortBy(_.email.toLowerCase)
        emails = sortedUsers.map(_.email).mkString("; ")
      } yield Ok(developers(sortedUsers, emails, groupApisByStatus(apis), filter, status))
  }


  private def groupApisByStatus(apis: Seq[APIDefinition]): Map[String, Seq[VersionSummary]] = {

    val versions = for {
      api <- apis
      version <- api.versions
    } yield VersionSummary(api.name, version.status, APIIdentifier(api.context, version.version))

    versions.groupBy(v => APIStatus.displayedStatus(v.status))
  }
}
