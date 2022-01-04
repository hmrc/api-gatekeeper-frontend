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

package controllers.apicataloguepublish

import cats.data.EitherT
import config.AppConfig
import connectors.{ApiCataloguePublishConnector, AuthConnector}
import model.GatekeeperRole
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.GatekeeperAuthWrapper
import views.html.ForbiddenView
import views.html.apicataloguepublish.PublishTemplate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiCataloguePublishController @Inject()(
                                               connector: ApiCataloguePublishConnector,
                                               val forbiddenView: ForbiddenView,
                                               override val authConnector: AuthConnector,
                                               mcc: MessagesControllerComponents,
                                               publishTemplate: PublishTemplate
                                             )(implicit ec: ExecutionContext,
                                               implicit val appConfig: AppConfig)
  extends FrontendController(mcc)
    with GatekeeperAuthWrapper
    with I18nSupport {

  def start(): Action[AnyContent] = requiresAtLeast(GatekeeperRole.ADMIN) { implicit request =>
    Future.successful(Ok(publishTemplate("Publish Page", "Publish Page", "Welcome to the publish page")))
  }

  def publishAll(): Action[AnyContent] = requiresAtLeast(GatekeeperRole.ADMIN) { implicit request =>
    EitherT(connector.publishAll())
      .fold(_ => Ok(publishTemplate("Publish all Failed", "Publish All Failed", "Something went wrong with publish all")),
        response => Ok(publishTemplate("Publish Page", "Publish Page", s"Publish All Called ok - ${response.message}")))
  }

  def publishByServiceName(serviceName: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.ADMIN) { implicit request =>
    EitherT(connector.publishByServiceName(serviceName))
      .fold(_ => Ok(publishTemplate("Publish by ServiceName Failed", "Publish by ServiceName failed", s"Something went wrong with publish by serviceName $serviceName")),
        response => Ok(publishTemplate("Publish Page", "Publish Page", s"Publish by serviceName called ok $serviceName - ${Json.toJson(response).toString}")))
  }
}
