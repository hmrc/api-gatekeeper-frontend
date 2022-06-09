/*
 * Copyright 2022 HM Revenue & Customs
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
import controllers.actions.ActionBuilders
import model.ApiStatus.ApiStatus
import model.Forms._
import model.SubscriptionFields.Fields.Alias
import model.UpliftAction.{APPROVE, REJECT}
import model._
import model.subscriptions.ApiData
import model.view.ApplicationViewModel
import org.joda.time.DateTime
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{ApiDefinitionService, ApmService, ApplicationService, DeveloperService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.modules.stride.controllers.GatekeeperBaseController
import uk.gov.hmrc.modules.stride.controllers.actions.ForbiddenHandler
import uk.gov.hmrc.modules.stride.controllers.models.LoggedInRequest
import utils.CsvHelper._
import utils.{ApplicationLogger, ErrorHelper}
import views.html.applications._
import views.html.approvedApplication.ApprovedView
import views.html.review.ReviewView
import views.html.{ErrorTemplate, ForbiddenView}

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class UpdateApplicationNameController @Inject()(
  val applicationService: ApplicationService,
  val forbiddenView: ForbiddenView,
  mcc: MessagesControllerComponents,
  override val errorTemplate: ErrorTemplate,
  manageApplicationNameView: ManageApplicationNameView,
  manageApplicationNameAdminListView: ManageApplicationNameAdminListView,
  manageApplicationNameSingleAdminView: ManageApplicationNameSingleAdminView,
  manageApplicationNameSuccessView: ManageApplicationNameSuccessView,
  val apmService: ApmService,
  val errorHandler: ErrorHandler,
  authConnector: AuthConnector,
  forbiddenHandler: ForbiddenHandler
)(implicit val appConfig: AppConfig, override val ec: ExecutionContext, strideAuthConfig: StrideAuthConfig)
  extends GatekeeperBaseController(strideAuthConfig, authConnector, forbiddenHandler, mcc)
    with ErrorHelper
    with ActionBuilders
    with ApplicationLogger {

  val newAppNameSessionKey = "newApplicationName"

  def updateApplicationNamePage(appId: ApplicationId) = anyStrideUserAction { implicit request =>
    withApp(appId) { app =>
      val form = UpdateApplicationNameForm.form.fill(UpdateApplicationNameForm(app.application.name))
      Future.successful(Ok(manageApplicationNameView(app.application, form)))
    }
  }

  def updateApplicationNameAction(appId: ApplicationId) = anyStrideUserAction { implicit request =>
    withApp(appId) { app =>
      def handleValidForm(form: UpdateApplicationNameForm) = {
        if (form.applicationName.equalsIgnoreCase(app.application.name)) {
          val formWithErrors = UpdateApplicationNameForm.form.fill(form)
            .withError(FormFields.applicationName, messagesApi.preferred(request)("application.name.unchanged.error"))
          Future.successful(Ok(manageApplicationNameView(app.application, formWithErrors)))

        } else {
          applicationService.validateApplicationName(app.application, form.applicationName).map(_ match {
            case ValidateApplicationNameSuccessResult =>
              Redirect(routes.UpdateApplicationNameController.updateApplicationNameAdminEmailPage(appId))
                .withSession(request.session + (newAppNameSessionKey -> form.applicationName))
            case failure => {
              val errorMsg = failure match {
                case ValidateApplicationNameFailureInvalidResult => "application.name.invalid.error"
                case ValidateApplicationNameFailureDuplicateResult => "application.name.duplicate.error"
              }
              val formWithErrors = UpdateApplicationNameForm.form.fill(form)
                .withError(FormFields.applicationName, messagesApi.preferred(request)(errorMsg))
              Ok(manageApplicationNameView(app.application, formWithErrors))
            }
          })
        }
      }

      def handleFormError(form: Form[UpdateApplicationNameForm]) = {
        Future.successful(BadRequest(manageApplicationNameView(app.application, form)))
      }

      UpdateApplicationNameForm.form.bindFromRequest.fold(handleFormError, handleValidForm)
    }
  }

  def updateApplicationNameAdminEmailPage(appId: ApplicationId) = anyStrideUserAction { implicit request =>
    withApp(appId) { app =>
      val adminEmails = app.application.collaborators.filter(_.role == CollaboratorRole.ADMINISTRATOR).map(_.emailAddress)
      Future.successful(adminEmails.size match {
        case 1 => Ok(manageApplicationNameSingleAdminView(app.application, adminEmails.head))
        case _ => Ok(manageApplicationNameAdminListView(app.application, adminEmails, UpdateApplicationNameAdminEmailForm.form))
      })
    }
  }

  def updateApplicationNameAdminEmailAction(appId: ApplicationId) = anyStrideUserAction { implicit request =>
    withApp(appId) { app =>
      def handleValidForm(form: UpdateApplicationNameAdminEmailForm) = {
        val newApplicationName = request.session.get(newAppNameSessionKey).get
        val gatekeeperUser = loggedIn.userFullName.get
        val adminEmail = form.adminEmail.get
        applicationService.updateApplicationName(app.application, adminEmail, gatekeeperUser, newApplicationName).map( _ match {
          case ApplicationUpdateSuccessResult => Redirect(routes.UpdateApplicationNameController.updateApplicationNameSuccessPage(appId))
            .withSession(request.session - newAppNameSessionKey)
          case ApplicationUpdateFailureResult => {
            val formWithErrors = UpdateApplicationNameForm.form.fill(UpdateApplicationNameForm(newApplicationName))
              .withError(FormFields.applicationName, messagesApi.preferred(request)("application.name.updatefailed.error"))
            Ok(manageApplicationNameView(app.application, formWithErrors))
          }
        })
      }

      def handleFormError(form: Form[UpdateApplicationNameAdminEmailForm]) = {
        val adminEmails = app.application.collaborators.filter(_.role == CollaboratorRole.ADMINISTRATOR).map(_.emailAddress)
        Future.successful(adminEmails.size match {
          case 1 => Ok(manageApplicationNameSingleAdminView(app.application, adminEmails.head))
          case _ => Ok(manageApplicationNameAdminListView(app.application, adminEmails, form))
        })
      }

      UpdateApplicationNameAdminEmailForm.form.bindFromRequest.fold(handleFormError, handleValidForm)
    }
  }

  def updateApplicationNameSuccessPage(appId: ApplicationId) = anyStrideUserAction { implicit request =>
    withApp(appId) { app =>
      Future.successful(Ok(manageApplicationNameSuccessView(app.application)))
    }
  }

}
