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

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import model._
import controllers.actions.ActionBuilders
import config.{AppConfig, ErrorHandler}
import scala.concurrent.ExecutionContext
import services.ApmService
import services.ApplicationService
import com.google.inject.{Singleton, Inject}

import uk.gov.hmrc.modules.stride.controllers.GatekeeperBaseController
import uk.gov.hmrc.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.modules.stride.controllers.actions.ForbiddenHandler
import uk.gov.hmrc.modules.stride.connectors.AuthConnector
import utils.CsvHelper
import utils.CsvHelper.ColumnDefinition

@Singleton
class BoxesController @Inject()(
  mcc: MessagesControllerComponents,
  val apmService: ApmService,
  authConnector: AuthConnector,
  forbiddenHandler: ForbiddenHandler
)(implicit val appConfig: AppConfig, override val ec: ExecutionContext, strideAuthConfig: StrideAuthConfig)
  extends GatekeeperBaseController(strideAuthConfig, authConnector, forbiddenHandler, mcc) {
    
  def getAll(): Action[AnyContent] = anyStrideUserAction { implicit request =>

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
