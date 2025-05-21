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

import javax.inject.Inject
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.Environment
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.GatekeeperAuthorisationActions
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationService, StrideAuthorisationService}
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.connectors.ApiCataloguePublishConnector
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.services.DeploymentApprovalService
import uk.gov.hmrc.gatekeeper.utils.ErrorHelper
import uk.gov.hmrc.gatekeeper.views.html.deploymentApproval._
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

class DeploymentApprovalController @Inject() (
    val forbiddenView: ForbiddenView,
    deploymentApprovalService: DeploymentApprovalService,
    apiCataloguePublishConnector: ApiCataloguePublishConnector,
    mcc: MessagesControllerComponents,
    deploymentApproval: DeploymentApprovalView,
    deploymentReview: DeploymentReviewView,
    override val errorTemplate: ErrorTemplate,
    strideAuthorisationService: StrideAuthorisationService,
    val ldapAuthorisationService: LdapAuthorisationService
  )(implicit val appConfig: AppConfig,
    override val ec: ExecutionContext
  ) extends GatekeeperBaseController(strideAuthorisationService, mcc)
    with GatekeeperAuthorisationActions
    with ErrorHelper {

  def reviewPage(serviceName: String, environment: String): Action[AnyContent] = anyStrideUserAction { implicit request =>
    fetchApiDefinitionSummary(serviceName, Environment.unsafeApply(environment)).map(apiDefinition => Ok(deploymentReview(HandleApprovalForm.form, apiDefinition)))
  }

  def handleApproval(serviceName: String, environment: String): Action[AnyContent] = anyStrideUserAction { implicit request =>
    val env = Environment.unsafeApply(environment)

    val requestForm: Form[HandleApprovalForm] = HandleApprovalForm.form.bindFromRequest()

    def errors(errors: Form[HandleApprovalForm]) =
      fetchApiDefinitionSummary(serviceName, env).map(details => BadRequest(deploymentReview(errors, details)))

    def doCalls(serviceName: String, environment: Environment): Future[Unit] = {
      deploymentApprovalService.approveService(serviceName, environment, gatekeeperUser.get)
        .flatMap(_ =>
          environment match {
            case Environment.PRODUCTION => apiCataloguePublishConnector.publishByServiceName(serviceName).map(_ => ())
            case _                      => successful(())
          }
        )
    }

    def approveApplicationWithValidForm(validForm: HandleApprovalForm) = {
      validForm.approval_confirmation match {
        case "Yes" =>
          doCalls(serviceName, env) map {
            _ => Redirect(routes.ApiApprovalsController.filterPage().url, SEE_OTHER)
          }
        case _     => throw new UnsupportedOperationException("Can't Reject Service Approval")
      }
    }

    requestForm.fold(errors, approveApplicationWithValidForm)
  }

  private def fetchApiDefinitionSummary(serviceName: String, environment: Environment)(implicit hc: HeaderCarrier): Future[APIApprovalSummary] = {
    deploymentApprovalService.fetchApprovalSummary(serviceName, environment)
  }
}
