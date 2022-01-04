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
import connectors.AuthConnector
import javax.inject.{Inject, Singleton}
import model._
import model.Environment._
import play.api.mvc.MessagesControllerComponents
import services.ApiDefinitionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{ErrorHelper, GatekeeperAuthWrapper}
import views.html.{ErrorTemplate, ForbiddenView}

import scala.concurrent.ExecutionContext

case class ApiDefinitionView(apiName: String, apiVersion: ApiVersion, status: String, access: String, isTrial: Boolean, environment: String)

@Singleton
class ApiDefinitionController @Inject()(apiDefinitionService: ApiDefinitionService,
                                        override val authConnector: AuthConnector,
                                        val forbiddenView: ForbiddenView,
                                        mcc: MessagesControllerComponents,
                                        override val errorTemplate: ErrorTemplate)
                                       (implicit val appConfig: AppConfig, val ec: ExecutionContext)
  extends FrontendController(mcc) with ErrorHelper with GatekeeperAuthWrapper {
    
  def apis() = requiresAtLeast(GatekeeperRole.USER) { implicit request =>
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
