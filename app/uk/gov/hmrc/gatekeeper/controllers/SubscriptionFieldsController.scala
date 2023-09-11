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

import scala.concurrent.ExecutionContext

import com.google.inject.{Inject, Singleton}

import play.api.mvc.MessagesControllerComponents

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.GatekeeperAuthorisationActions
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationService, StrideAuthorisationService}
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields.ApplicationApiFieldValues
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.services.SubscriptionFieldsService
import uk.gov.hmrc.gatekeeper.utils.CsvHelper._
import uk.gov.hmrc.gatekeeper.utils.ErrorHelper
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

@Singleton
class SubscriptionFieldsController @Inject() (
    val subscriptionFieldsService: SubscriptionFieldsService,
    val forbiddenView: ForbiddenView,
    mcc: MessagesControllerComponents,
    override val errorTemplate: ErrorTemplate,
    strideAuthorisationService: StrideAuthorisationService,
    val ldapAuthorisationService: LdapAuthorisationService
  )(implicit val appConfig: AppConfig,
    override val ec: ExecutionContext
  ) extends GatekeeperBaseController(strideAuthorisationService, mcc)
    with GatekeeperAuthorisationActions
    with ErrorHelper {

  def subscriptionFieldValues() = anyAuthenticatedUserAction { implicit request =>
    case class FlattenedSubscriptionFieldValue(clientId: ClientId, context: ApiContext, versionNbr: ApiVersionNbr, name: FieldName)

    val columnDefinitions: Seq[ColumnDefinition[FlattenedSubscriptionFieldValue]] = Seq(
      ColumnDefinition("Environment", (_ => Environment.PRODUCTION.toString())),
      ColumnDefinition("ClientId", (data => data.clientId.value)),
      ColumnDefinition("ApiContext", (data => data.context.value)),
      ColumnDefinition("ApiVersionNbr", (data => data.versionNbr.value)),
      ColumnDefinition("FieldName", (data => data.name.value))
    )

    def flattendFieldValues(subscriptionFieldValues: List[ApplicationApiFieldValues]): List[FlattenedSubscriptionFieldValue] = {
      subscriptionFieldValues.flatMap(allsubscriptionFieldValues => {
        allsubscriptionFieldValues.fields.map { fieldValue: (FieldName, FieldValue) =>
          {
            val fieldName = fieldValue._1
            FlattenedSubscriptionFieldValue(allsubscriptionFieldValues.clientId, allsubscriptionFieldValues.apiContext, allsubscriptionFieldValues.apiVersion, fieldName)
          }
        }
      })
    }

    subscriptionFieldsService.fetchAllProductionFieldValues().map(allFieldsValues => {

      val sortedAndFlattenedFields = flattendFieldValues(allFieldsValues)
        .sortBy(x => (x.clientId.value, x.context, x.versionNbr, x.name.value))

      Ok(toCsvString(columnDefinitions, sortedAndFlattenedFields))
    })
  }
}
