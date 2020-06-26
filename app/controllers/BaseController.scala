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
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.LoggedInUser
import play.api.mvc.MessagesControllerComponents
import play.api.i18n.MessagesProvider
import views.html.{error_template, forbidden}

import scala.concurrent.ExecutionContext

abstract class BaseController(mcc: MessagesControllerComponents, errorTemplate: error_template, val forbiddenView: forbidden)
                             (implicit val appConfig: AppConfig) extends FrontendController(mcc) {

  implicit val ec: ExecutionContext

  def technicalDifficulties(implicit request: Request[_], messagesProvider: MessagesProvider) : Result = {
    implicit val loggedInUser = LoggedInUser(None)

    InternalServerError(errorTemplate("Technical difficulties", "Technical difficulties",
      "Sorry, we’re experiencing technical difficulties")(implicitly))
  }

  def notFound(errors: String)(implicit request: Request[_], messagesProvider: MessagesProvider) : Result = {
    implicit val loggedInUser = LoggedInUser(None)

    NotFound(errorTemplate("Not found", "404 - Not found", errors)(implicitly))
  }
}
