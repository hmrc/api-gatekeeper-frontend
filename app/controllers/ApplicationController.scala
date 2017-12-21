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

import connectors.AuthConnector
import model.{APIDefinition, APIIdentifier, APIStatus, Role, VersionSummary}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{ApiDefinitionService, ApplicationService}
import utils.{GatekeeperAuthProvider, GatekeeperAuthWrapper, SubscriptionEnhancer}
import views.html.applications.{application, applications, subscription_manage}

import scala.concurrent.Future


object ApplicationController extends ApplicationController with WithAppConfig {
  override val applicationService = ApplicationService
  override val apiDefinitionService = ApiDefinitionService
  override def authConnector = AuthConnector
  override def authProvider = GatekeeperAuthProvider
}

trait ApplicationController extends BaseController with GatekeeperAuthWrapper {

  val applicationService: ApplicationService
  val apiDefinitionService: ApiDefinitionService

  def applicationsPage: Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
      for {
        apps <- applicationService.fetchAllSubscribedApplications
        apis <- apiDefinitionService.fetchAllApiDefinitions
        subApps = SubscriptionEnhancer.combine(apps, apis)
      } yield Ok(applications(subApps, groupApisByStatus(apis)))
  }

  def applicationPage(appId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
      val applicationFuture = applicationService.fetchApplication(appId)
      val subscriptionsFuture = applicationService.fetchApplicationSubscriptions(appId)

      for {
        app <- applicationFuture
        subs <- subscriptionsFuture
      } yield Ok(application(app, subs.sortWith(_.name.toLowerCase < _.name.toLowerCase), isSuperUser))
  }

  def resendVerification(appId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
      for {
        _ <- applicationService.resendVerification(appId, loggedIn.get)
      } yield {
        Redirect(routes.DashboardController.approvedApplicationPage(appId))
          .flashing("success" -> "Verification email has been sent")
      }
  }

  def deleteGrant(appId: String, grantId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
    // TODO: Role should be super user not APIGatekeeper
    // TODO: Delete the selected grant from the application

    Future.successful(Redirect(routes.ApplicationController.applicationPage(appId)))

  }

  def deleteSubscription(appId: String, subscriptionId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
    // TODO: Role should be super user not APIGatekeeper
    // TODO: Delete the selected subscription from the application

    Future.successful(Redirect(routes.ApplicationController.applicationPage(appId)))
  }

  def manageSubscription(appId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
    // TODO: Role should be super user not APIGatekeeper
    // TODO: Delete the selected subscription from the application

      val applicationFuture = applicationService.fetchApplication(appId)

      for {
        app <- applicationFuture
      } yield Ok(subscription_manage(app))
  }

  private def groupApisByStatus(apis: Seq[APIDefinition]): Map[String, Seq[VersionSummary]] = {

    val versions = for {
      api <- apis
      version <- api.versions
    } yield VersionSummary(api.name, version.status, APIIdentifier(api.context, version.version))

    versions.groupBy(v => APIStatus.displayedStatus(v.status))
  }
}
