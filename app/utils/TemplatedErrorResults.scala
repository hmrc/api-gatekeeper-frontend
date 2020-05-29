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

package utils

import config.AppConfig
import play.api.i18n.Messages
import play.api.mvc.{Request, Result, Results}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.error_template
import scala.concurrent.Future

trait TemplatedErrorResults extends Results {
   def notFound()(implicit request: LoggedInRequest[_], messages: Messages, appConfig: AppConfig) : Future[Result] = {
        implicit val loggedInUser = LoggedInUser(None)

        Future.successful(NotFound(error_template("Not found", "404 - Not found",
              "Subscription or version not found")))
  }
}