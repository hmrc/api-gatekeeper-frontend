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

package uk.gov.hmrc.gatekeeper.controllers

import uk.gov.hmrc.gatekeeper.config.AppConfig
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.Environment._
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.gatekeeper.services.ApiDefinitionService
import uk.gov.hmrc.gatekeeper.utils.ErrorHelper
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

import uk.gov.hmrc.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.modules.gkauth.services.StrideAuthorisationService

import scala.concurrent.ExecutionContext
import uk.gov.hmrc.gatekeeper.utils.CsvHelper._

case class ApiDefinitionView(apiName: String, apiVersion: ApiVersion, status: String, access: String, isTrial: Boolean, environment: String)

@Singleton
class ApiDefinitionController @Inject()(
  apiDefinitionService: ApiDefinitionService,
  val forbiddenView: ForbiddenView,
  mcc: MessagesControllerComponents,
  override val errorTemplate: ErrorTemplate,
  strideAuthorisationService: StrideAuthorisationService
)(implicit val appConfig: AppConfig, override val ec: ExecutionContext)
  extends GatekeeperBaseController(strideAuthorisationService, mcc) with ErrorHelper {
    
  def apis() = anyStrideUserAction { implicit request =>
    val definitions = apiDefinitionService.apis

    definitions.map(allDefinitions => {
      val allDefinitionsAsRows = allDefinitions
        .flatMap { case(d, env) => toViewModel(d, env) }
        .sortBy((vm: ApiDefinitionView) => (vm.apiName, vm.apiVersion))
        
      val columnDefinitions : Seq[ColumnDefinition[ApiDefinitionView]] = Seq(
        ColumnDefinition("name",(vm => vm.apiName)),
        ColumnDefinition("version",(vm => vm.apiVersion.value)),
        ColumnDefinition("status",(vm => vm.status)),
        ColumnDefinition("access", (vm => vm.access)),
        ColumnDefinition("isTrial", (vm => vm.isTrial.toString())),
        ColumnDefinition("environment", (vm => vm.environment))
       )

      Ok(toCsvString(columnDefinitions, allDefinitionsAsRows))
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
