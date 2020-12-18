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

package controllers

import config.AppConfig
import connectors.AuthConnector
import javax.inject.{Inject, Singleton}
import model._
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ApiDefinitionService, ApplicationService, DeveloperService, ApmService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{ActionBuilders, ErrorHelper, GatekeeperAuthWrapper}
import views.html.{ErrorTemplate, ForbiddenView}
import views.html.developers._

import scala.concurrent.ExecutionContext

@Singleton
class DevelopersController @Inject()(developerService: DeveloperService,
                                     val applicationService: ApplicationService,
                                     val forbiddenView: ForbiddenView,
                                     apiDefinitionService: ApiDefinitionService,
                                     override val authConnector: AuthConnector,
                                     mcc: MessagesControllerComponents,
                                     developersView: DevelopersView,
                                     developerDetailsView: DeveloperDetailsView,
                                     removeMfaView: RemoveMfaView,
                                     removeMfaSuccessView: RemoveMfaSuccessView,
                                     deleteDeveloperView: DeleteDeveloperView,
                                     deleteDeveloperSuccessView: DeleteDeveloperSuccessView,
                                     override val errorTemplate: ErrorTemplate,
                                     val apmService: ApmService
                                    )(implicit val appConfig: AppConfig, val ec: ExecutionContext)
  extends FrontendController(mcc) with ErrorHelper with GatekeeperAuthWrapper with ActionBuilders with I18nSupport {

  def developersPage(filter: Option[String], status: Option[String], environment: Option[String]) = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>

      println("********************")
      println("OLD DEVELOPERS PAGE")
      println("********************")

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
      } yield Ok(developersView(sortedUsers, emails, groupApisByStatus(apis), filter, status, environment))
  }

  def developerPage(email: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      developerService.fetchDeveloper(email).map(developer => Ok(developerDetailsView(developer.toDeveloper, isAtLeastSuperUser)))
  }

  def removeMfaPage(email: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit request =>
      developerService.fetchDeveloper(email).map(developer => Ok(removeMfaView(developer.toDeveloper)))
  }

  def removeMfaAction(email:String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit request =>
      developerService.removeMfa(email, loggedIn.userFullName.get) map { _ =>
        Ok(removeMfaSuccessView(email))
      } recover {
        case e: Exception =>
          Logger.error(s"Failed to remove MFA for user: $email", e)
          technicalDifficulties
      }
  }

  def deleteDeveloperPage(email: String) = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit request =>
      developerService.fetchDeveloper(email).map(developer => Ok(deleteDeveloperView(developer.toDeveloper)))
  }

  def deleteDeveloperAction(email:String) = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit request =>
      developerService.deleteDeveloper(email, loggedIn.userFullName.get).map {
        case DeveloperDeleteSuccessResult => Ok(deleteDeveloperSuccessView(email))
        case _ => technicalDifficulties
      }
  }

  private def groupApisByStatus(apis: Seq[ApiDefinition]): Map[String, Seq[VersionSummary]] = {
    val versions = for {
      api <- apis
      version <- api.versions
    } yield VersionSummary(api.name, version.status, ApiIdentifier(api.context, version.version))

    versions.groupBy(v => ApiStatus.displayedStatus(v.status))
  }
}
