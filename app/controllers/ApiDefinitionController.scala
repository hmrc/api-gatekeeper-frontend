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

import config.AppConfig
import javax.inject.{Inject, Singleton}
import model._
import model.Environment._
import play.api.mvc.MessagesControllerComponents
import services.ApiDefinitionService
import utils.ErrorHelper
import views.html.{ErrorTemplate, ForbiddenView}

import uk.gov.hmrc.modules.stride.controllers.GatekeeperBaseController
import uk.gov.hmrc.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.modules.stride.controllers.actions.ForbiddenHandler
import uk.gov.hmrc.modules.stride.connectors.AuthConnector

import scala.concurrent.ExecutionContext

case class ApiDefinitionView(apiName: String, apiVersion: ApiVersion, status: String, access: String, isTrial: Boolean, environment: String)

@Singleton
class ApiDefinitionController @Inject()(
  apiDefinitionService: ApiDefinitionService,
  val forbiddenView: ForbiddenView,
  mcc: MessagesControllerComponents,
  override val errorTemplate: ErrorTemplate,
  strideAuthConfig: StrideAuthConfig,
  authConnector: AuthConnector,
  forbiddenHandler: ForbiddenHandler
)(implicit val appConfig: AppConfig, override val ec: ExecutionContext)
  extends GatekeeperBaseController(strideAuthConfig, authConnector, forbiddenHandler, mcc) with ErrorHelper {
    
  def apis() = anyStrideUserAction { implicit request =>
    val definitions = apiDefinitionService.apis

    definitions.map(allDefinitions => {
      val allDefinitionsAsRows = allDefinitions
        .flatMap { case(d, env) => toViewModel(d, env) }
        .sortBy((vm: ApiDefinitionView) => (vm.apiName, vm.apiVersion))
        .map(vm => Seq(vm.apiName,vm.apiVersion.value,vm.status,vm.access,vm.isTrial,vm.environment).mkString(","))

      val rowHeader = Seq("name","version","status","access", "isTrial", "environment").mkString(",")

      Ok((rowHeader +: allDefinitionsAsRows).mkString(System.lineSeparator()))
    })
  }

  private def toViewModel(apiDefinition: ApiDefinition, environment: Environment): List[ApiDefinitionView] = {
    def isTrial(apiVersion: ApiVersionDefinition) : Boolean = {
      apiVersion.access.fold(false)(access => access.isTrial.getOrElse(false))
    }

    apiDefinition.versions.map(v =>
      ApiDefinitionView(apiDefinition.name, v.version, v.displayedStatus, v.accessType.toString, isTrial(v), environment.toString)
    )
  }
}
