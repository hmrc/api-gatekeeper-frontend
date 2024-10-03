/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.controllers

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.data
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.gatekeeper.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.gatekeeper.controllers.actions.ActionBuilders
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields.{Fields, SaveSubscriptionFieldsFailureResponse, SaveSubscriptionFieldsSuccessResponse}
import uk.gov.hmrc.gatekeeper.models.view.{EditApiMetadataForm, SubscriptionFieldValueForm, SubscriptionVersion}
import uk.gov.hmrc.gatekeeper.services.{ApmService, ApplicationService, SubscriptionFieldsService}
import uk.gov.hmrc.gatekeeper.utils.ErrorHelper
import uk.gov.hmrc.gatekeeper.views.html.applications.subscriptionConfiguration._
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

@Singleton
class SubscriptionConfigurationController @Inject() (
    val applicationService: ApplicationService,
    val subscriptionFieldsService: SubscriptionFieldsService,
    val forbiddenView: ForbiddenView,
    mcc: MessagesControllerComponents,
    listSubscriptionConfiguration: ListSubscriptionConfigurationView,
    editSubscriptionConfiguration: EditSubscriptionConfigurationView,
    override val errorTemplate: ErrorTemplate,
    val apmService: ApmService,
    val errorHandler: ErrorHandler,
    strideAuthorisationService: StrideAuthorisationService
  )(implicit val appConfig: AppConfig,
    override val ec: ExecutionContext
  ) extends GatekeeperBaseController(strideAuthorisationService, mcc) with ErrorHelper with ActionBuilders {

  implicit val dateTimeOrdering: Ordering[LocalDateTime] = Ordering.fromLessThan(_ isBefore _)

  def listConfigurations(appId: ApplicationId): Action[AnyContent] = atLeastSuperUserAction { implicit request =>
    withAppAndSubscriptionsAndFieldDefinitions(appId) {
      app =>
        {
          Future.successful(Ok(listSubscriptionConfiguration(app.applicationWithSubscriptionData.asAppWithCollaborators, SubscriptionVersion(app))))
        }
    }
  }

  def editConfigurations(appId: ApplicationId, apiContext: ApiContext, versionNbr: ApiVersionNbr): Action[AnyContent] = atLeastSuperUserAction { implicit request =>
    withAppAndSubscriptionsAndFieldDefinitions(appId) {
      app =>
        {
          val subscriptionVersionsForApp: List[SubscriptionVersion]       = SubscriptionVersion(app)
          val subscriptionFieldsForContextAndVersion: SubscriptionVersion = subscriptionVersionsForApp.filter(sv => sv.apiContext == apiContext && sv.versionNbr == versionNbr).head
          val subscriptionFields                                          = subscriptionFieldsForContextAndVersion.fields

          val form = EditApiMetadataForm.form
            .fill(EditApiMetadataForm(fields = subscriptionFields.map(sf => SubscriptionFieldValueForm(sf.name, sf.value)).toList))

          Future.successful(Ok(editSubscriptionConfiguration(app.applicationWithSubscriptionData.asAppWithCollaborators, subscriptionFieldsForContextAndVersion, form)))
        }
    }
  }

  def saveConfigurations(appId: ApplicationId, apiContext: ApiContext, versionNbr: ApiVersionNbr): Action[AnyContent] = atLeastSuperUserAction { implicit request =>
    withAppAndSubscriptionsAndFieldDefinitions(appId) {
      app =>
        {
          val requestForm: Form[EditApiMetadataForm] = EditApiMetadataForm.form.bindFromRequest()

          def errors(errors: Form[EditApiMetadataForm]) = {
            Future.successful(technicalDifficulties)
          }

          def validationErrorResult(fieldErrors: Map[String, String], form: EditApiMetadataForm) = {
            val errors = fieldErrors.map(fe => data.FormError(fe._1, fe._2)).toList

            val errorForm = EditApiMetadataForm.form.fill(form).copy(errors = errors)

            val subscriptionVersionsForApp: List[SubscriptionVersion]       = SubscriptionVersion(app)
            val subscriptionFieldsForContextAndVersion: SubscriptionVersion =
              subscriptionVersionsForApp.filter(sv => sv.apiContext == apiContext && sv.versionNbr == versionNbr).head

            val view = editSubscriptionConfiguration(app.applicationWithSubscriptionData.asAppWithCollaborators, subscriptionFieldsForContextAndVersion, errorForm)

            BadRequest(view)
          }

          def doSaveConfigurations(form: EditApiMetadataForm) = {
            val fields: Fields.Alias = EditApiMetadataForm.toFields(form)

            subscriptionFieldsService.saveFieldValues(app.applicationWithSubscriptionData.details, apiContext, versionNbr, fields)
              .map({
                case SaveSubscriptionFieldsSuccessResponse              => Redirect(routes.SubscriptionConfigurationController.listConfigurations(appId))
                case SaveSubscriptionFieldsFailureResponse(fieldErrors) => validationErrorResult(fieldErrors, form)
              })
          }

          requestForm.fold(errors, doSaveConfigurations)
        }
    }
  }
}
