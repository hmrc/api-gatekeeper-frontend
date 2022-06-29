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

import scala.concurrent.ExecutionContext

import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.modules.stride.controllers.GatekeeperBaseController
import uk.gov.hmrc.modules.stride.services.StrideAuthorisationService

import play.api.mvc.MessagesControllerComponents
import config.AppConfig
import model._
import model.SubscriptionFields.ApplicationApiFieldValues
import utils.CsvHelper._
import utils.ErrorHelper
import views.html.{ErrorTemplate, ForbiddenView}

@Singleton
class SubscriptionFieldsController @Inject()(
  val subscriptionFieldsService : services.SubscriptionFieldsService,
  val forbiddenView: ForbiddenView,
  mcc: MessagesControllerComponents,
  override val errorTemplate: ErrorTemplate,
  strideAuthorisationService: StrideAuthorisationService
)(implicit val appConfig: AppConfig, override val ec: ExecutionContext)
  extends GatekeeperBaseController(strideAuthorisationService, mcc) with ErrorHelper {

  def subscriptionFieldValues() = anyStrideUserAction { implicit request =>
    case class FlattenedSubscriptionFieldValue(clientId: ClientId, context: ApiContext, version: ApiVersion, name: FieldName)

    val columnDefinitions : Seq[ColumnDefinition[FlattenedSubscriptionFieldValue]] = Seq(
      ColumnDefinition("Environment",(_ => model.Environment.PRODUCTION.toString())),
      ColumnDefinition("ClientId", (data => data.clientId.value)),
      ColumnDefinition("ApiContext", (data => data.context.value)),
      ColumnDefinition("ApiVersion", (data => data.version.value)),
      ColumnDefinition("FieldName", (data => data.name.value))
    )

    def flattendFieldValues(subscriptionFieldValues: List[ApplicationApiFieldValues]) : List[FlattenedSubscriptionFieldValue] = {
      subscriptionFieldValues.flatMap(allsubscriptionFieldValues => {
        allsubscriptionFieldValues.fields.seq.map{ fieldValue: (FieldName, FieldValue) => {
          val fieldName = fieldValue._1
          FlattenedSubscriptionFieldValue(allsubscriptionFieldValues.clientId, allsubscriptionFieldValues.apiContext, allsubscriptionFieldValues.apiVersion, fieldName)
        }}
      })
    }

    subscriptionFieldsService.fetchAllProductionFieldValues().map(allFieldsValues =>{
      
      val sortedAndFlattenedFields = flattendFieldValues(allFieldsValues)
        .sortBy(x=> (x.clientId.value, x.context, x.version, x.name.value))
      
      Ok(toCsvString(columnDefinitions, sortedAndFlattenedFields))
    })
  }
}
