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

package uk.gov.hmrc.gatekeeper.controllers

import com.google.inject.{Inject, Singleton}
import play.api.http.HeaderNames
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.GatekeeperAuthorisationActions
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationService, StrideAuthorisationService}
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models.ApplicationStateHistoryChange
import uk.gov.hmrc.gatekeeper.services.ApplicationService
import uk.gov.hmrc.gatekeeper.utils.CsvHelper
import uk.gov.hmrc.gatekeeper.utils.CsvHelper.ColumnDefinition

import scala.concurrent.ExecutionContext

@Singleton
class ApplicationStatesController @Inject()(
  mcc: MessagesControllerComponents,
  val applicationService: ApplicationService,
  strideAuthorisationService: StrideAuthorisationService,
  val ldapAuthorisationService: LdapAuthorisationService
)(implicit val appConfig: AppConfig, override val ec: ExecutionContext)
  extends GatekeeperBaseController(strideAuthorisationService, mcc) with GatekeeperAuthorisationActions {

  val csvFileName = "applicationStateHistory.csv"

  def csv(): Action[AnyContent] = anyAuthenticatedUserAction { implicit request =>
    applicationService.fetchProdAppStateHistories().map(appStateHistoryChange => {
      val columnDefinitions : Seq[ColumnDefinition[ApplicationStateHistoryChange]] = Seq(
        ColumnDefinition("applicationId", _.applicationId),
        ColumnDefinition("applicationName", _.appName),
        ColumnDefinition("journeyVersion", _.journeyVersion),
        ColumnDefinition("oldState", _.oldState),
        ColumnDefinition("oldTimestamp", _.oldTimestamp),
        ColumnDefinition("newState", _.newState),
        ColumnDefinition("newTimestamp", _.newTimestamp)
      )
      
      Ok(CsvHelper.toCsvString(columnDefinitions, appStateHistoryChange))
        .withHeaders((HeaderNames.CONTENT_DISPOSITION, s"attachment;filename=${csvFileName}"))
        .as("text/csv")
    })
  }
}
