/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatform.modules.deskpro.controllers

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.apiplatform.modules.deskpro.connectors.DeskproHorizonConnector
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.{Action, AnyContent}
import play.api.libs.json.Json

@Singleton
class DeskproHorizonController @Inject()(
  connector: DeskproHorizonConnector,
  mcc: MessagesControllerComponents
)(implicit val ec: ExecutionContext) extends FrontendController(mcc) {

  def getOrganisations(): Action[AnyContent] = Action.async { implicit request =>
    import uk.gov.hmrc.apiplatform.modules.deskpro.models.DeskproOrganisationsResponse._

    connector.getOrganisations().map(response => Ok(Json.toJson(response)))
  }
}
