/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.i18n.Messages
import play.api.mvc.Request
import uk.gov.hmrc.play.frontend.controller.FrontendController
import views.html.error_template

trait BaseController extends FrontendController {
  implicit val appConfig: AppConfig

  def technicalDifficulties(implicit request: Request[_], messages: Messages)  = InternalServerError(error_template("Technical difficulties", "Technical difficulties",
    "Sorry, we’re experiencing technical difficulties"))
}

trait WithAppConfig {
  self: BaseController =>
  override implicit val appConfig = AppConfig
}
