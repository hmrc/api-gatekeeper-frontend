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
import javax.inject.Inject
import model._
import model.view.{SubscriptionVersion, SubscriptionField, SubscriptionFieldValueForm}
import org.joda.time.DateTime
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.ApplicationService
import utils.{ActionBuilders, GatekeeperAuthWrapper}
import views.html.applications.subscriptionConfiguration.{list_subscription_configuration, edit_subscription_configuration}

import scala.concurrent.{ExecutionContext, Future}
import model.view.EditApiMetadataForm
import play.api.data.Form
import play.api.data
import play.api.data.Forms._
import services.SubscriptionFieldsService
import model.SubscriptionFields.{Fields, SaveSubscriptionFieldsSuccessResponse, SaveSubscriptionFieldsFailureResponse}
import play.i18n.Messages

class SubscriptionConfigurationController @Inject()(val applicationService: ApplicationService,
                                                    val subscriptionFieldsService: SubscriptionFieldsService,
                                                    override val authConnector: AuthConnector
                                                   )(implicit override val appConfig: AppConfig, val ec: ExecutionContext)
  extends BaseController with GatekeeperAuthWrapper with ActionBuilders {

  implicit val dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)

  def listConfigurations(appId: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit request =>
      implicit hc =>
        withAppAndFieldDefinitions(appId) {
          app => {
            Future.successful(Ok(list_subscription_configuration(app.application,  SubscriptionVersion(app.subscriptionsWithFieldDefinitions))))
          }
        }
  }

  def editConfigurations(appId: String, context: String, version: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit request =>
      implicit hc =>
        withAppAndSubscriptionVersion(appId, context, version) {
          app => {

            var subscription = app.subscription
            var version = app.version
        
            val subscriptionFields = SubscriptionField.apply(version.fields)
            val subscriptionViewModel = SubscriptionVersion(subscription.name, subscription.context, version.version.version, version.version.displayedStatus, subscriptionFields)

            val form = EditApiMetadataForm.form
              .fill(EditApiMetadataForm(fields = subscriptionFields.map(sf => SubscriptionFieldValueForm(sf.name, sf.value)).toList))

            Future.successful(Ok(edit_subscription_configuration(app.application, subscriptionViewModel, form)))
          }
        }
  }

  def saveConfigurations(appId: String, context: String, version: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit  request => implicit hc => {
      
      withAppAndSubscriptionVersion(appId, context, version) {
        app => {
          val requestForm: Form[EditApiMetadataForm] = EditApiMetadataForm.form.bindFromRequest

          def errors(errors: Form[EditApiMetadataForm]) = {
            Future.successful(technicalDifficulties)
          }

      //   saveFields(validForm) map {
      //   case SaveSubscriptionFieldsSuccessResponse => Redirect(successRedirect)
      //   case SaveSubscriptionFieldsFailureResponse(fieldErrors) =>
      //     val errors = fieldErrors.map(fe => data.FormError(fe._1, fe._2)).toSeq
      //     val errorForm = EditApiMetadata.form.fill(validForm).copy(errors = errors)
      //     val vm = EditApiMetadataViewModel(validForm.apiName, apiContext, apiVersion, validForm.displayedStatus, errorForm)

      //     BadRequest(validationFailureView(vm))
      // }

          def doSaveConfigurations(validForm: EditApiMetadataForm) = {
            val fields: Fields = EditApiMetadataForm.toFields(validForm)

            subscriptionFieldsService.saveFieldValues(app.application.application, context, version, fields)
            .map({
                   case SaveSubscriptionFieldsSuccessResponse => Redirect(routes.SubscriptionConfigurationController.listConfigurations(appId))
                   case SaveSubscriptionFieldsFailureResponse(fieldErrors) =>
                    // Future.successful(Ok(edit_subscription_configuration(app.application, subscriptionViewModel, form)))
                    val errors = fieldErrors.map(fe => data.FormError(fe._1, fe._2)).toSeq

                    println("**** " + fieldErrors)

                    val errorForm = EditApiMetadataForm.form.fill(validForm).copy(errors = errors)
                    // val vm = EditApiMetadataViewModel(validForm.apiName, apiContext, apiVersion, validForm.displayedStatus, errorForm)

                    // TODO: Refactor with above
                    var subscription = app.subscription
                    var version = app.version
        
                    val subscriptionFields = SubscriptionField.apply(version.fields)
                    val subscriptionViewModel = SubscriptionVersion(subscription.name, subscription.context, version.version.version, version.version.displayedStatus, subscriptionFields)

                    val view = edit_subscription_configuration(app.application, subscriptionViewModel, errorForm)

                    BadRequest(view)
              })
          }

          requestForm.fold(errors, doSaveConfigurations)
        }
      }
    }
  }
}


