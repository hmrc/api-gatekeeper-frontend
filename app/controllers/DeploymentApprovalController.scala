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
import connectors.ApiCataloguePublishConnector
import model._
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.DeploymentApprovalService
import uk.gov.hmrc.http.HeaderCarrier
import utils.ErrorHelper
import views.html.deploymentApproval._
import views.html.{ErrorTemplate, ForbiddenView}

import uk.gov.hmrc.modules.stride.controllers.GatekeeperBaseController
import uk.gov.hmrc.modules.stride.services.StrideAuthorisationService

import javax.inject.Inject
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

class DeploymentApprovalController @Inject()(
  val forbiddenView: ForbiddenView,
  deploymentApprovalService: DeploymentApprovalService,
  apiCataloguePublishConnector: ApiCataloguePublishConnector,
  mcc: MessagesControllerComponents,
  deploymentApproval: DeploymentApprovalView,
  deploymentReview: DeploymentReviewView,
  override val errorTemplate: ErrorTemplate,
  strideAuthorisationService: StrideAuthorisationService
)(implicit val appConfig: AppConfig, override val ec: ExecutionContext)
  extends GatekeeperBaseController(strideAuthorisationService, mcc) with ErrorHelper {

  def pendingPage(): Action[AnyContent] = anyStrideUserAction { implicit request =>
    deploymentApprovalService.fetchUnapprovedServices().map(app => Ok(deploymentApproval(app)))
  }

  def reviewPage(serviceName: String, environment: String): Action[AnyContent] = anyStrideUserAction { implicit request =>
    fetchApiDefinitionSummary(serviceName, environment).map(apiDefinition => Ok(deploymentReview(HandleApprovalForm.form, apiDefinition)))
  }

  def handleApproval(serviceName: String, environment: String): Action[AnyContent] = anyStrideUserAction { implicit request =>
    val requestForm: Form[HandleApprovalForm] = HandleApprovalForm.form.bindFromRequest

    def errors(errors: Form[HandleApprovalForm]) =
      fetchApiDefinitionSummary(serviceName, environment).map(details => BadRequest(deploymentReview(errors, details)))

    def doCalls(serviceName: String, environment: Environment.Value): Future[Unit] = {
      deploymentApprovalService.approveService(serviceName, environment)
        .flatMap(_ => environment match {
            case Environment.PRODUCTION => apiCataloguePublishConnector.publishByServiceName(serviceName).map(_ => ())
            case _ => successful(())
          })
    }


    def approveApplicationWithValidForm(validForm: HandleApprovalForm) = {
      validForm.approval_confirmation match {
        case "Yes" =>
          doCalls(serviceName, Environment.withName(environment)) map {
            _ => Redirect(routes.DeploymentApprovalController.pendingPage().url, SEE_OTHER)
          }
        case _ => throw new UnsupportedOperationException("Can't Reject Service Approval")
      }
    }

    requestForm.fold(errors, approveApplicationWithValidForm)
  }

  private def fetchApiDefinitionSummary(serviceName: String, environment: String)(implicit hc: HeaderCarrier): Future[APIApprovalSummary] = {
    deploymentApprovalService.fetchApprovalSummary(serviceName, Environment.withName(environment))
  }
}

