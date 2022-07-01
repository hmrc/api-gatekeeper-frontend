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

package uk.gov.hmrc.gatekeeper.controllers.apicataloguepublish

import cats.data.EitherT
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.connectors.ApiCataloguePublishConnector
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.gatekeeper.views.html.ForbiddenView
import uk.gov.hmrc.gatekeeper.views.html.apicataloguepublish.PublishTemplate

import uk.gov.hmrc.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.modules.gkauth.services.StrideAuthorisationService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiCataloguePublishController @Inject()(
  strideAuthorisationService: StrideAuthorisationService,
  connector: ApiCataloguePublishConnector,
  val forbiddenView: ForbiddenView,
  mcc: MessagesControllerComponents,
  publishTemplate: PublishTemplate
)(implicit ec: ExecutionContext, implicit val appConfig: AppConfig)
  extends GatekeeperBaseController(strideAuthorisationService, mcc) {

  def start(): Action[AnyContent] = adminOnlyAction { implicit request =>
    Future.successful(Ok(publishTemplate("Publish Page", "Publish Page", "Welcome to the publish page")))
  }

  def publishAll(): Action[AnyContent] = adminOnlyAction { implicit request =>
    EitherT(connector.publishAll())
      .fold(_ => Ok(publishTemplate("Publish all Failed", "Publish All Failed", "Something went wrong with publish all")),
        response => Ok(publishTemplate("Publish Page", "Publish Page", s"Publish All Called ok - ${response.message}")))
  }

  def publishByServiceName(serviceName: String): Action[AnyContent] = adminOnlyAction { implicit request =>
    EitherT(connector.publishByServiceName(serviceName))
      .fold(_ => Ok(publishTemplate("Publish by ServiceName Failed", "Publish by ServiceName failed", s"Something went wrong with publish by serviceName $serviceName")),
        response => Ok(publishTemplate("Publish Page", "Publish Page", s"Publish by serviceName called ok $serviceName - ${Json.toJson(response).toString}")))
  }
}
