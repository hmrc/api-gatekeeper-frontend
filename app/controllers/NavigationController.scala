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

import javax.inject.{Inject, Singleton}
import config.AppConfig
import model.{NavLink, StaticNavLinks}
import play.api.libs.json._
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NavigationController @Inject()(mcc: MessagesControllerComponents)
                                    (implicit val appConfig: AppConfig, val ec: ExecutionContext)
  extends FrontendController(mcc) {

  def navLinks() = Action.async { _ =>
    Future.successful(Ok(Json.toJson(StaticNavLinks())))
  }
}
