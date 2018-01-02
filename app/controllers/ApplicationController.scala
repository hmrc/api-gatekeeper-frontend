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
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.{ApiDefinitionService, ApplicationService}
import utils.{GatekeeperAuthProvider, GatekeeperAuthWrapper, SubscriptionEnhancer}
import views.html.applications._
import model.Forms._
import play.api.data.Form
import views.html.error_template

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
      } yield Ok(application(app, subs.filter(sub => sub.versions.exists(version => version.subscribed)).sortWith(_.name.toLowerCase < _.name.toLowerCase), isSuperUser))
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

  def deleteSubscription(appId: String, subscriptionId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
    // TODO: Role should be super user not APIGatekeeper
    // TODO: Delete the selected subscription from the application

    Future.successful(Redirect(routes.ApplicationController.applicationPage(appId)))
  }

  def manageSubscription(appId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper, requiresSuperUser = true) {
    implicit request => implicit hc =>
    // TODO: Role should be super user not APIGatekeeper
    // TODO: Manage the subscriptions for the given application

      val applicationFuture = applicationService.fetchApplication(appId)
      val subscriptionsFuture = applicationService.fetchApplicationSubscriptions(appId)

      for {
        app <- applicationFuture
        subs <- subscriptionsFuture

      } yield Ok(manage_subscriptions(app, subs.sortWith(_.name.toLowerCase < _.name.toLowerCase), isSuperUser))
  }

  def manageAccessOverrides(appId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper, requiresSuperUser = true) {
    implicit request => implicit hc => withApp(appId) { app =>
      app.application.access match {
        case access: Standard => {
          Future.successful(Ok(manage_access_overrides(app.application, accessOverridesForm.fill(access.overrides), isSuperUser)))
        }
      }
    }
  }

  def updateAccessOverrides(appId: String) = requiresRole(Role.APIGatekeeper, requiresSuperUser = true) {
    implicit request => implicit hc => withApp(appId) { app =>
      def updateOverrides(overrides: Set[OverrideFlag]) = {
        applicationService.updateOverrides(app.application, overrides).map { _ =>
          Redirect(routes.ApplicationController.applicationPage(appId))
        }
      }

      def handleFormError(form: Form[Set[OverrideFlag]]) = {
        Future.successful(BadRequest(manage_access_overrides(app.application, form, isSuperUser)))
      }

      accessOverridesForm.bindFromRequest.fold(handleFormError, updateOverrides)
    }
  }

  def manageScopes(appId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper, requiresSuperUser = true) {
    implicit request => implicit hc => withApp(appId) { app =>
      app.application.access match {
        case access: AccessWithRestrictedScopes => {
          val form = scopesForm.fill(access.scopes)
          Future.successful(Ok(manage_scopes(app.application, form, isSuperUser)))
        }
        case _ => Future.failed(new RuntimeException("Invalid access type on application"))
      }
    }
  }

  def updateScopes(appId: String) = requiresRole(Role.APIGatekeeper, requiresSuperUser = true) {
    implicit request => implicit hc => withApp(appId) { app =>
      def updateOverrides(scopes: Set[String]) = {
        applicationService.updateScopes(app.application, scopes).map { _ =>
          Redirect(routes.ApplicationController.applicationPage(appId))
        }
      }

      def handleFormError(form: Form[Set[String]]) = {
        Future.successful(BadRequest(manage_scopes(app.application, form, isSuperUser)))
      }

      scopesForm.bindFromRequest.fold(handleFormError, updateOverrides)
    }
  }

  private def groupApisByStatus(apis: Seq[APIDefinition]): Map[String, Seq[VersionSummary]] = {

    val versions = for {
      api <- apis
      version <- api.versions
    } yield VersionSummary(api.name, version.status, APIIdentifier(api.context, version.version))

    versions.groupBy(v => APIStatus.displayedStatus(v.status))
  }

  private def withApp(appId: String)(f: ApplicationWithHistory => Future[Result])(implicit request: Request[_]) = {
    applicationService.fetchApplication(appId).flatMap(f)
  }
}
