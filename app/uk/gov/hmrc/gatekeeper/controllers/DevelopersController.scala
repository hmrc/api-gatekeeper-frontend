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

import uk.gov.hmrc.gatekeeper.config.{AppConfig, ErrorHandler}

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.gatekeeper.models._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.gatekeeper.services.{ApiDefinitionService, ApmService, ApplicationService, DeveloperService}
import uk.gov.hmrc.gatekeeper.utils.ErrorHelper
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}
import uk.gov.hmrc.gatekeeper.views.html.developers._
import uk.gov.hmrc.gatekeeper.utils.ApplicationLogger
import uk.gov.hmrc.gatekeeper.controllers.actions.ActionBuilders
import uk.gov.hmrc.gatekeeper.models.xml.OrganisationId
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService

import scala.concurrent.ExecutionContext

@Singleton
class DevelopersController @Inject()(
  developerService: DeveloperService,
  val applicationService: ApplicationService,
  val forbiddenView: ForbiddenView,
  apiDefinitionService: ApiDefinitionService,
  mcc: MessagesControllerComponents,
  developerDetailsView: DeveloperDetailsView,
  removeMfaView: RemoveMfaView,
  removeMfaSuccessView: RemoveMfaSuccessView,
  deleteDeveloperView: DeleteDeveloperView,
  deleteDeveloperSuccessView: DeleteDeveloperSuccessView,
  override val errorTemplate: ErrorTemplate,
  val apmService: ApmService,
  val errorHandler: ErrorHandler,
  strideAuthorisationService: StrideAuthorisationService
)(implicit val appConfig: AppConfig, override val ec: ExecutionContext)
  extends GatekeeperBaseController(strideAuthorisationService, mcc)
    with ErrorHelper
    with ActionBuilders
    with ApplicationLogger {

  def developerPage(developerId: DeveloperIdentifier): Action[AnyContent] = anyStrideUserAction { implicit request =>
    val buildGateKeeperXmlServicesUrlFn: (OrganisationId) => String = (organisationId) =>
        s"${appConfig.gatekeeperXmlServicesBaseUrl}/api-gatekeeper-xml-services/organisations/${organisationId.value}"

    developerService.fetchDeveloper(developerId).map(developer => Ok(developerDetailsView(developer, buildGateKeeperXmlServicesUrlFn)))
  }

  def removeMfaPage(developerIdentifier: DeveloperIdentifier): Action[AnyContent] = anyStrideUserAction { implicit request =>
    developerService.fetchDeveloper(developerIdentifier).map(developer => Ok(removeMfaView(developer)))
  }

  def removeMfaAction(developerIdentifier: DeveloperIdentifier): Action[AnyContent] = anyStrideUserAction { implicit request =>
    developerService.removeMfa(developerIdentifier, loggedIn.userFullName.get) map { user =>
      Ok(removeMfaSuccessView(user.email))
    } recover {
      case e: Exception =>
        logger.error(s"Failed to remove MFA for user: $developerIdentifier", e)
        technicalDifficulties
    }
  }

  def deleteDeveloperPage(developerIdentifier: DeveloperIdentifier) = atLeastSuperUserAction { implicit request =>
    developerService.fetchDeveloper(developerIdentifier).map(developer => Ok(deleteDeveloperView(developer)))
  }

  def deleteDeveloperAction(developerId: DeveloperIdentifier) = atLeastSuperUserAction { implicit request =>
    developerService.deleteDeveloper(developerId, loggedIn.userFullName.get).map {
      case (DeveloperDeleteSuccessResult, developer) => Ok(deleteDeveloperSuccessView(developer.email))
      case _ => technicalDifficulties
    }
  }
}
