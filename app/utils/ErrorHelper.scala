/*
 * Copyright 2021 HM Revenue & Customs
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

package utils

import views.html.ErrorTemplate

import play.api.i18n.MessagesProvider
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound}
import play.api.mvc.{Request, Result}

trait ErrorHelper {
  val errorTemplate: ErrorTemplate

  def technicalDifficulties(implicit request: Request[_], messagesProvider: MessagesProvider) : Result = {
    InternalServerError(errorTemplate("Technical difficulties", "Technical difficulties",
      "Sorry, we’re experiencing technical difficulties"))
  }

  def notFound(errors: String)(implicit request: Request[_], messagesProvider: MessagesProvider) : Result = {
    NotFound(errorTemplate("Not found", "404 - Not found", errors))
  }

  def badRequest(errors: String)(implicit request: Request[_], messagesProvider: MessagesProvider) : Result = {
    BadRequest(errorTemplate("Bad request", "400 - Bad request", errors))
  }
}
