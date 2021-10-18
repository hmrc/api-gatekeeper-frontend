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

package controllers

import javax.inject.{Inject, Singleton}
import connectors.ApiCataloguePublishConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import play.api.mvc.MessagesControllerComponents
import views.html.ErrorTemplate
import views.html.apicataloguepublish.PublishTemplate
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import utils.GatekeeperAuthWrapper
import play.api.i18n.I18nSupport
import model.GatekeeperRole
import config.AppConfig
import views.html.ForbiddenView
import connectors.AuthConnector
import play.api.libs.json.Json
import model.{PublishAllResponse, PublishResponse}
import model.ApiCataloguePublishResponse._

@Singleton
class ApiCataloguePublishController @Inject() (
    connector: ApiCataloguePublishConnector,
    val forbiddenView: ForbiddenView,
    override val authConnector: AuthConnector,
    mcc: MessagesControllerComponents,
    errorTemplate: ErrorTemplate,
    publishTemplate: PublishTemplate
  )(implicit ec: ExecutionContext,
    implicit val appConfig: AppConfig)
    extends FrontendController(mcc)
    with GatekeeperAuthWrapper
    with I18nSupport {

  def start() = requiresAtLeast(GatekeeperRole.ADMIN) { implicit request =>
    Future.successful(Ok(publishTemplate("Publish Page", "Publish Page", "Welcome to the publish page")))
  }

  def publishAll() = requiresAtLeast(GatekeeperRole.ADMIN) { implicit request =>
    connector.publishAll()
      .map(result =>
        result match {
          case Right(response: PublishAllResponse) =>
            Ok(publishTemplate("Publish Page", "Publish Page", s"Publish All Called ok - ${response.message}"))
          case Left(_)                                                          => Ok(publishTemplate("Publish all Failed", "Publish All Failed", "Something went wrong with publish all"))
        }
      )
  }

    def publishByServiceName(serviceName: String) = requiresAtLeast(GatekeeperRole.ADMIN) { implicit request =>
    connector.publishByServiceName(serviceName)
      .map(result =>
        result match {
          case Right(response: PublishResponse) =>
            Ok(publishTemplate("Publish Page", "Publish Page", s"Publish by servcieName called ok $serviceName - ${Json.toJson(response).toString}"))
          case Left(_)                                                          => Ok(publishTemplate("Publish by ServiceName Failed", "Publish by ServiceName failed", s"Something went wrong with publish by serviceName $serviceName"))
        }
      )
  }
}
