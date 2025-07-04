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

package uk.gov.hmrc.gatekeeper.config

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.Configuration
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler

import uk.gov.hmrc.gatekeeper.views.html.ErrorTemplate

@Singleton
class ErrorHandler @Inject() (
    val messagesApi: MessagesApi,
    val configuration: Configuration,
    errorTemplate: ErrorTemplate
  )(implicit val appConfig: AppConfig,
    val ec: ExecutionContext
  ) extends FrontendErrorHandler {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: RequestHeader) = {
    Future.successful(errorTemplate(pageTitle, heading, message))
  }

  def notFoundTemplate(message: String)(implicit request: RequestHeader) =
    standardErrorTemplate(
      Messages("global.error.pageNotFound404.title"),
      Messages("global.error.pageNotFound404.heading"),
      Messages(message)
    )

  def badRequestTemplate(message: String)(implicit request: RequestHeader) =
    standardErrorTemplate(
      Messages("global.error.badRequest400.title"),
      Messages("global.error.badRequest400.heading"),
      Messages(message)
    )

  def publishErrorTemplate()(implicit request: RequestHeader) =
    standardErrorTemplate(
      "API Publishing error",
      "API Publishing error",
      "API was successfully approved but publishing failed. Please check API Approval history for the error details."
    )

}
