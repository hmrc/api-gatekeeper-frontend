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
import model.view.{EditApiMetadataForm, SubscriptionField, SubscriptionFieldValueForm, SubscriptionVersion}
import model.SubscriptionFields.{Fields, SaveSubscriptionFieldsFailureResponse, SaveSubscriptionFieldsSuccessResponse}
import org.joda.time.DateTime
import play.api.data
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ApplicationService, SubscriptionFieldsService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{ActionBuilders, ErrorHelper, GatekeeperAuthWrapper}
import views.html.{ErrorTemplate, Forbidden}
import views.html.applications.subscriptionConfiguration._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionConfigurationController @Inject()(val applicationService: ApplicationService,
                                                    val subscriptionFieldsService: SubscriptionFieldsService,
                                                    val authConnector: AuthConnector,
                                                    mcc: MessagesControllerComponents,
                                                    listSubscriptionConfiguration: ListSubscriptionConfirmationView,
                                                    editSubscriptionConfiguration: EditSubscriptionConfirmationView,
                                                    override val errorTemplate: ErrorTemplate,
                                                    forbiddenView: Forbidden
                                                   )(implicit val appConfig: AppConfig, val ec: ExecutionContext)
  extends FrontendController(mcc) with ErrorHelper with GatekeeperAuthWrapper with ActionBuilders with I18nSupport {

  implicit val dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)

  def listConfigurations(appId: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.SUPERUSER, forbiddenView) {
    implicit request =>
        withAppAndFieldDefinitions(appId) {
          app => {
            Future.successful(Ok(listSubscriptionConfiguration(app.application,  SubscriptionVersion(app.subscriptionsWithFieldDefinitions))))
          }
        }
  }

  def editConfigurations(appId: String, context: String, version: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.SUPERUSER, forbiddenView) {
    implicit request =>
        withAppAndSubscriptionVersion(appId, context, version) {
          app => {
            val subscriptionFields = SubscriptionField(app.version.fields)
            val subscriptionViewModel = SubscriptionVersion(app.subscription, app.version, subscriptionFields)

            val form = EditApiMetadataForm.form
              .fill(EditApiMetadataForm(fields = subscriptionFields.map(sf => SubscriptionFieldValueForm(sf.name, sf.value)).toList))

            Future.successful(Ok(editSubscriptionConfiguration(app.application, subscriptionViewModel, form)))
          }
        }
  }

  def saveConfigurations(appId: String, context: String, version: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.SUPERUSER, forbiddenView) {
    implicit  request => {

      withAppAndSubscriptionVersion(appId, context, version) {
        app => {
          val requestForm: Form[EditApiMetadataForm] = EditApiMetadataForm.form.bindFromRequest

          def errors(errors: Form[EditApiMetadataForm]) = {
            Future.successful(technicalDifficulties)
          }

          def validationErrorResult(fieldErrors: Map[String, String], form: EditApiMetadataForm) = {
            val errors = fieldErrors.map(fe => data.FormError(fe._1, fe._2)).toSeq

            val errorForm = EditApiMetadataForm.form.fill(form).copy(errors = errors)

            val subscriptionFields = SubscriptionField(app.version.fields)
            val subscriptionViewModel = SubscriptionVersion(app.subscription, app.version, subscriptionFields)

            val view = editSubscriptionConfiguration(app.application, subscriptionViewModel, errorForm)

            BadRequest(view)
          }

          def doSaveConfigurations(form: EditApiMetadataForm) = {
            val fields: Fields = EditApiMetadataForm.toFields(form)

            subscriptionFieldsService.saveFieldValues(app.application.application, context, version, fields)
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
}


