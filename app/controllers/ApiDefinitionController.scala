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

package controllers

import config.AppConfig
import javax.inject.Inject
import play.api.mvc.Action
import services.ApiDefinitionService
import model._

import scala.concurrent.ExecutionContext

case class ApiDefinitionView(apiName: String, apiVersion: String, status: String)

class ApiDefinitionController @Inject()(apiDefinitionService: ApiDefinitionService)
                                       (implicit override val appConfig: AppConfig, val ec: ExecutionContext)
  extends BaseController {

  def something() = Action.async { implicit request =>
    val definitions = apiDefinitionService.fetchAllApiDefinitions()

    // TODO: Sorting
    // Environment

    definitions.map(allDefinitions => {
      val allDefinitionsAsRows = allDefinitions
        .flatMap(d => toViewModel(d))
        .map(vm => s"${vm.apiName},${vm.apiVersion},${vm.status}")
      Ok(allDefinitionsAsRows.mkString(System.lineSeparator()))
    })
  }

  private def toViewModel(apiDefinition: APIDefinition): Seq[ApiDefinitionView] = {
    apiDefinition.versions.map(v => 
      ApiDefinitionView(apiDefinition.name, v.version, v.displayedStatus)
    )
  }
}
