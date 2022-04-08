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
import uk.gov.hmrc.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.modules.stride.controllers.GatekeeperBaseController
import uk.gov.hmrc.modules.stride.controllers.actions.ForbiddenHandler

import play.api.mvc.MessagesControllerComponents
import config.AppConfig
import model._
import utils.CsvHelper._
import utils.ErrorHelper
import views.html.{ErrorTemplate, ForbiddenView}

@Singleton
class SubscriptionFieldsController @Inject()(
  val subscriptionFieldsService : services.SubscriptionFieldsService,
  val forbiddenView: ForbiddenView,
  mcc: MessagesControllerComponents,
  override val errorTemplate: ErrorTemplate,
  strideAuthConfig: StrideAuthConfig,
  authConnector: AuthConnector,
  forbiddenHandler: ForbiddenHandler
)(implicit val appConfig: AppConfig, override val ec: ExecutionContext)
  extends GatekeeperBaseController(strideAuthConfig, authConnector, forbiddenHandler, mcc) with ErrorHelper {

  def subscriptionFieldValues() = anyStrideUserAction { implicit request =>
    case class FlattenedSubscriptionFieldValue(clientId: ClientId, name: FieldName)

    val columnDefinitions : Seq[ColumnDefinition[FlattenedSubscriptionFieldValue]] = Seq(
      ColumnDefinition("Environment",(_ => model.Environment.PRODUCTION.toString())),
      ColumnDefinition("ClientId", (data => data.clientId.value)),
      ColumnDefinition("FieldName", (data => data.name.value))
    )

    def flattendFieldValues(subscriptionFieldValues: List[SubscriptionFields.ApplicationApiFieldValues]) : List[FlattenedSubscriptionFieldValue] = {
      subscriptionFieldValues.flatMap(allsubscriptionFieldValues => {
        allsubscriptionFieldValues.fields.seq.map{ fieldValue: (FieldName, FieldValue) => {
          val fieldName = fieldValue._1
          FlattenedSubscriptionFieldValue(allsubscriptionFieldValues.clientId, fieldName)
        }}
      })
    }

    for {
      subscriptionFieldValues: List[SubscriptionFields.ApplicationApiFieldValues] <- subscriptionFieldsService.fetchAllFieldValues()
      flattendedFieldValues = flattendFieldValues(subscriptionFieldValues).sortBy(x=> (x.clientId.value, x.name.value))
    } yield( Ok(toCsvString(columnDefinitions, flattendedFieldValues)))
  }
}
