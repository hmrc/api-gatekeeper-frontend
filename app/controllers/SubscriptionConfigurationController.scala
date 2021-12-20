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
import model.view.{EditApiMetadataForm, SubscriptionFieldValueForm, SubscriptionVersion}
import model.SubscriptionFields.{Fields, SaveSubscriptionFieldsFailureResponse, SaveSubscriptionFieldsSuccessResponse}
import org.joda.time.DateTime
import play.api.data
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ApplicationService, SubscriptionFieldsService, ApmService}
import utils.ErrorHelper
import views.html.{ErrorTemplate, ForbiddenView}
import views.html.applications.subscriptionConfiguration._
import controllers.actions.ActionBuilders

import uk.gov.hmrc.modules.stride.controllers.GatekeeperBaseController
import uk.gov.hmrc.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.modules.stride.controllers.actions.ForbiddenHandler
import uk.gov.hmrc.modules.stride.connectors.AuthConnector

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionConfigurationController @Inject()(
  val applicationService: ApplicationService,
  val subscriptionFieldsService: SubscriptionFieldsService,
  val forbiddenView: ForbiddenView,
  mcc: MessagesControllerComponents,
  listSubscriptionConfiguration: ListSubscriptionConfigurationView,
  editSubscriptionConfiguration: EditSubscriptionConfigurationView,
  override val errorTemplate: ErrorTemplate,
  val apmService: ApmService,
  val errorHandler: ErrorHandler,
  strideAuthConfig: StrideAuthConfig,
  authConnector: AuthConnector,
  forbiddenHandler: ForbiddenHandler
)(implicit val appConfig: AppConfig, override val ec: ExecutionContext)
  extends GatekeeperBaseController(strideAuthConfig, authConnector, forbiddenHandler, mcc) with ErrorHelper with ActionBuilders {

  implicit val dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)

  def listConfigurations(appId: ApplicationId): Action[AnyContent] = atLeastSuperUserAction { implicit request =>
    withAppAndSubscriptionsAndFieldDefinitions(appId) {
      app => {
        Future.successful(Ok(listSubscriptionConfiguration(app.applicationWithSubscriptionData.application,  SubscriptionVersion(app))))
      }
    }
  }

  def editConfigurations(appId: ApplicationId, apiContext: ApiContext, apiVersion: ApiVersion): Action[AnyContent] = atLeastSuperUserAction { implicit request =>
    withAppAndSubscriptionsAndFieldDefinitions(appId) {
      app => {
        val subscriptionVersionsForApp: List[SubscriptionVersion] = SubscriptionVersion(app)
        val subscriptionFieldsForContextAndVersion: SubscriptionVersion = subscriptionVersionsForApp.filter(sv => sv.apiContext == apiContext && sv.version == apiVersion).head
        val subscriptionFields = subscriptionFieldsForContextAndVersion.fields

        val form = EditApiMetadataForm.form
          .fill(EditApiMetadataForm(fields = subscriptionFields.map(sf => SubscriptionFieldValueForm(sf.name, sf.value)).toList))

        Future.successful(Ok(editSubscriptionConfiguration(app.applicationWithSubscriptionData.application, subscriptionFieldsForContextAndVersion, form)))
      }
    }
  }

  def saveConfigurations(appId: ApplicationId, apiContext: ApiContext, apiVersion: ApiVersion): Action[AnyContent] = atLeastSuperUserAction { implicit  request =>
    withAppAndSubscriptionsAndFieldDefinitions(appId) {
      app => {
        val requestForm: Form[EditApiMetadataForm] = EditApiMetadataForm.form.bindFromRequest

        def errors(errors: Form[EditApiMetadataForm]) = {
          Future.successful(technicalDifficulties)
        }

        def validationErrorResult(fieldErrors: Map[String, String], form: EditApiMetadataForm) = {
          val errors = fieldErrors.map(fe => data.FormError(fe._1, fe._2)).toList

          val errorForm = EditApiMetadataForm.form.fill(form).copy(errors = errors)

          val subscriptionVersionsForApp: List[SubscriptionVersion] = SubscriptionVersion(app)
          val subscriptionFieldsForContextAndVersion: SubscriptionVersion = subscriptionVersionsForApp.filter(sv => sv.apiContext == apiContext && sv.version == apiVersion).head

          val view = editSubscriptionConfiguration(app.applicationWithSubscriptionData.application, subscriptionFieldsForContextAndVersion, errorForm)

          BadRequest(view)
        }

        def doSaveConfigurations(form: EditApiMetadataForm) = {
          val fields: Fields.Alias = EditApiMetadataForm.toFields(form)

          subscriptionFieldsService.saveFieldValues(app.applicationWithSubscriptionData.application, apiContext, apiVersion, fields)
          .map({
            case SaveSubscriptionFieldsSuccessResponse => Redirect(routes.SubscriptionConfigurationController.listConfigurations(appId))
            case SaveSubscriptionFieldsFailureResponse(fieldErrors) => validationErrorResult(fieldErrors, form)
          })
        }

        requestForm.fold(errors, doSaveConfigurations)
      }
    }
  }
}


