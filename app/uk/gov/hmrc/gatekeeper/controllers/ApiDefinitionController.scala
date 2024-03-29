/*
 * Copyright 2023 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

import play.api.mvc.MessagesControllerComponents

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.GatekeeperAuthorisationActions
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationService, StrideAuthorisationService}
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.services.ApiDefinitionService
import uk.gov.hmrc.gatekeeper.utils.CsvHelper._
import uk.gov.hmrc.gatekeeper.utils.ErrorHelper
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

case class ApiDefinitionView(
    apiName: String,
    serviceName: String,
    apiContext: ApiContext,
    apiVersion: ApiVersionNbr,
    versionSource: ApiVersionSource,
    status: String,
    access: String,
    isTrial: Boolean,
    environment: Environment
  )

@Singleton
class ApiDefinitionController @Inject() (
    apiDefinitionService: ApiDefinitionService,
    val forbiddenView: ForbiddenView,
    mcc: MessagesControllerComponents,
    override val errorTemplate: ErrorTemplate,
    strideAuthorisationService: StrideAuthorisationService,
    val ldapAuthorisationService: LdapAuthorisationService
  )(implicit val appConfig: AppConfig,
    override val ec: ExecutionContext
  ) extends GatekeeperBaseController(strideAuthorisationService, mcc)
    with GatekeeperAuthorisationActions
    with ErrorHelper {

  def apis() = anyAuthenticatedUserAction { implicit request =>
    apiDefinitionService.apis.map(allDefinitions => {
      val allDefinitionsAsRows = allDefinitions
        .flatMap { case (d, env) => toViewModel(d, env) }
        .sortBy((vm: ApiDefinitionView) => (vm.apiName, vm.apiVersion))

      val columnDefinitions: Seq[ColumnDefinition[ApiDefinitionView]] = Seq(
        ColumnDefinition("name", (vm => vm.apiName)),
        ColumnDefinition("serviceName", (vm => vm.serviceName)),
        ColumnDefinition("context", (vm => vm.apiContext.value)),
        ColumnDefinition("version", (vm => vm.apiVersion.value)),
        ColumnDefinition("source", (vm => vm.versionSource.toString())),
        ColumnDefinition("status", (vm => vm.status)),
        ColumnDefinition("access", (vm => vm.access)),
        ColumnDefinition("isTrial", (vm => vm.isTrial.toString())),
        ColumnDefinition("environment", (vm => vm.environment.toString))
      )

      Ok(toCsvString(columnDefinitions, allDefinitionsAsRows))
    })
  }

  private def toViewModel(apiDefinition: ApiDefinition, environment: Environment): Iterable[ApiDefinitionView] = {
    def isTrial(apiVersion: ApiVersion): Boolean = {
      apiVersion.access match {
        case ApiAccess.Private(true) => true
        case _                       => false
      }
    }

    apiDefinition.versions.values.map(v =>
      ApiDefinitionView(
        apiDefinition.name,
        apiDefinition.serviceName.value,
        apiDefinition.context,
        v.versionNbr,
        v.versionSource,
        v.status.displayText,
        v.access.accessType.toString(),
        isTrial(v),
        environment
      )
    )
  }
}
