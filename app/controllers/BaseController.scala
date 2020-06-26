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

import model.LoggedInUser
import play.api.mvc.{Request, Result}
import play.api.i18n.MessagesProvider
import views.html.error_template

import play.api.mvc.Results._

trait BaseController {
  def technicalDifficulties(errorTemplate: error_template)(implicit request: Request[_], messagesProvider: MessagesProvider) : Result = {
    implicit val loggedInUser = LoggedInUser(None)

    InternalServerError(errorTemplate("Technical difficulties", "Technical difficulties",
      "Sorry, we’re experiencing technical difficulties")(implicitly, implicitly, implicitly))
  }

  def notFound(errorTemplate: error_template, errors: String)(implicit request: Request[_], messagesProvider: MessagesProvider) : Result = {
    implicit val loggedInUser = LoggedInUser(None)

    NotFound(errorTemplate("Not found", "404 - Not found", errors)(implicitly, implicitly, implicitly))
  }
}
