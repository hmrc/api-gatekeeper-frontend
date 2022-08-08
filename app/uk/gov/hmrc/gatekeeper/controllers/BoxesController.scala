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

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.gatekeeper.models.pushpullnotifications.Box
import uk.gov.hmrc.gatekeeper.config.AppConfig
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.gatekeeper.services.ApmService
import com.google.inject.{Singleton, Inject}

import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService

import uk.gov.hmrc.gatekeeper.utils.CsvHelper
import uk.gov.hmrc.gatekeeper.utils.CsvHelper.ColumnDefinition
import uk.gov.hmrc.apiplatform.modules.gkauth.services.LdapAuthorisationService
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.GatekeeperAuthorisationActions

@Singleton
class BoxesController @Inject()(
  mcc: MessagesControllerComponents,
  val apmService: ApmService,
  strideAuthorisationService: StrideAuthorisationService,
  val ldapAuthorisationService: LdapAuthorisationService
)(implicit val appConfig: AppConfig, override val ec: ExecutionContext)
  extends GatekeeperBaseController(strideAuthorisationService, mcc)
  with GatekeeperAuthorisationActions {
    
  def getAll(): Action[AnyContent] = anyAuthenticatedUserAction { implicit request =>

    apmService.fetchAllBoxes().map(boxes => {
      val columnDefinitions : Seq[ColumnDefinition[Box]] = Seq(
        ColumnDefinition("environment",(box => box.environment.toString())),
        ColumnDefinition("applicationId",(box => box.applicationId.fold("")(_.value))),
        ColumnDefinition("clientId",(box => box.boxCreator.clientId.value)),
        ColumnDefinition("name",(box => box.boxName)),
        ColumnDefinition("boxId",(box => box.boxId.value)),
        ColumnDefinition("subscriptionType",(box => (box.subscriber.fold("")(s=>s.subscriptionType.toString())))),
        ColumnDefinition("callbackUrl", (box => box.subscriber.fold("")(s=>s.callBackUrl)))
      )
      
      Ok(CsvHelper.toCsvString(columnDefinitions, boxes))
    })
  }
}
