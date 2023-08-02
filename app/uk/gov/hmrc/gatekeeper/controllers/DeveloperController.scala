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
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}

import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.GatekeeperAuthorisationActions
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationService, StrideAuthorisationService}
import uk.gov.hmrc.gatekeeper.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.gatekeeper.controllers.actions.ActionBuilders
import uk.gov.hmrc.gatekeeper.models.Forms.RemoveMfaConfirmationForm
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.xml.OrganisationId
import uk.gov.hmrc.gatekeeper.services.{ApiDefinitionService, ApmService, ApplicationService, DeveloperService}
import uk.gov.hmrc.gatekeeper.utils.ErrorHelper
import uk.gov.hmrc.gatekeeper.views.html.developers._
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

@Singleton
class DeveloperController @Inject() (
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
    strideAuthorisationService: StrideAuthorisationService,
    val ldapAuthorisationService: LdapAuthorisationService
  )(implicit val appConfig: AppConfig,
    override val ec: ExecutionContext
  ) extends GatekeeperBaseController(strideAuthorisationService, mcc)
    with GatekeeperAuthorisationActions
    with ErrorHelper
    with ActionBuilders
    with ApplicationLogger {

  def developerPage(developerId: DeveloperIdentifier): Action[AnyContent] = anyAuthenticatedUserAction { implicit request =>
    val buildGateKeeperXmlServicesUrlFn: (OrganisationId) => String = (organisationId) =>
      s"${appConfig.gatekeeperXmlServicesBaseUrl}/api-gatekeeper-xml-services/organisations/${organisationId.value}"

    developerService.fetchDeveloper(developerId, FetchDeletedApplications.Include).map(developer => Ok(developerDetailsView(developer, buildGateKeeperXmlServicesUrlFn)))
  }

  def removeMfaPage(developerIdentifier: DeveloperIdentifier): Action[AnyContent] = anyStrideUserAction { implicit request =>
    developerService.fetchDeveloper(developerIdentifier, FetchDeletedApplications.Exclude).map(developer => Ok(removeMfaView(developer, RemoveMfaConfirmationForm.form)))
  }

  def removeMfaAction(developerIdentifier: DeveloperIdentifier): Action[AnyContent] = anyStrideUserAction { implicit request =>
    def handleRemoveMfa() = {
      developerService.removeMfa(developerIdentifier, loggedIn.userFullName) map { user =>
        Ok(removeMfaSuccessView(user.email.text, developerIdentifier))
      } recover {
        case e: Exception =>
          logger.error(s"Failed to remove MFA for user: $developerIdentifier", e)
          technicalDifficulties
      }
    }

    def handleValidForm(form: RemoveMfaConfirmationForm): Future[Result] = {
      form.confirm match {
        case "yes" => handleRemoveMfa()
        case _     => successful(Redirect(routes.DeveloperController.developerPage(developerIdentifier).url))
      }
    }

    def handleInvalidForm(form: Form[RemoveMfaConfirmationForm]): Future[Result] = {
      successful(BadRequest)
    }

    RemoveMfaConfirmationForm.form.bindFromRequest().fold(handleInvalidForm, handleValidForm)
  }

  def deleteDeveloperPage(developerIdentifier: DeveloperIdentifier) = atLeastSuperUserAction { implicit request =>
    developerService.fetchDeveloper(developerIdentifier, FetchDeletedApplications.Exclude).map(developer => Ok(deleteDeveloperView(developer)))
  }

  def deleteDeveloperAction(developerId: DeveloperIdentifier) = atLeastSuperUserAction { implicit request =>
    developerService.deleteDeveloper(developerId, loggedIn.userFullName).map {
      case (DeveloperDeleteSuccessResult, developer) => Ok(deleteDeveloperSuccessView(developer.email.text))
      case _                                         => technicalDifficulties
    }
  }
}
