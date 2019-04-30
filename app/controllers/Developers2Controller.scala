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

import javax.inject.{Inject, Singleton}
import config.AppConfig
import connectors.AuthConnector
import model._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import services.DeveloperService
import utils.GatekeeperAuthWrapper
import views.html.developers._

import scala.concurrent.Future

@Singleton
class Developers2Controller @Inject()(val authConnector: AuthConnector, developerService: DeveloperService)(override implicit val appConfig: AppConfig)
  extends BaseController with GatekeeperAuthWrapper {

  def developersPage(maybeEmailFilter: Option[String] = None) = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      implicit hc => {
        maybeEmailFilter match {
          case Some(emailFilter) => {
            developerService.searchDevelopers(emailFilter) map {
              users => Ok(developers2(users))
            }
          }
          case None => Future.successful(Ok(developers2(Seq.empty)))
        }
      }
  }
}

