/*
 * Copyright 2017 HM Revenue & Customs
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

import controllers.routes
import play.api.mvc.{Request, Results}
import uk.gov.hmrc.play.frontend.auth.{AuthenticationProvider, WithSessionTimeoutValidation}
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object GatekeeperAuthProvider extends AuthenticationProvider with Results {

  override def id: String = "API Gatekeeper authentication provider"

  override def handleNotAuthenticated(implicit request: Request[_]) = {
    case _ => redirectToLogin.map(res => Right(res))
  }

  override def redirectToLogin(implicit request: Request[_]) =
    Future.successful((Redirect(routes.AccountController.loginPage)))

}

object SessionTimeoutValidation extends WithSessionTimeoutValidation(() => DateTimeUtils.now)
