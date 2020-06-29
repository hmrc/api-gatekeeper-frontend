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
import connectors.AuthConnector
import javax.inject.Inject
import model._
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.DeploymentApprovalService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.GatekeeperAuthWrapper
import views.html.{ErrorTemplate, Forbidden}
import views.html.deploymentApproval.{deploymentApproval, deploymentReview}

import scala.concurrent.{ExecutionContext, Future}

class DeploymentApprovalController @Inject()(val authConnector: AuthConnector,
                                             deploymentApprovalService: DeploymentApprovalService,
                                             mcc: MessagesControllerComponents,
                                             deploymentApproval: deploymentApproval,
                                             deploymentReview: deploymentReview,
                                             override val errorTemplate: ErrorTemplate,
                                             forbiddenView: Forbidden
                                            )(implicit val appConfig: AppConfig, val ec: ExecutionContext)
  extends FrontendController(mcc) with BaseController with GatekeeperAuthWrapper with I18nSupport {

  def pendingPage(): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER, forbiddenView) { implicit request =>
      deploymentApprovalService.fetchUnapprovedServices().map(app => Ok(deploymentApproval(app)))
  }

  def reviewPage(serviceName: String, environment: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER, forbiddenView) { implicit request =>
      fetchApiDefinitionSummary(serviceName, environment).map(apiDefinition => Ok(deploymentReview(HandleApprovalForm.form, apiDefinition)))
  }

  def handleApproval(serviceName: String, environment: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER, forbiddenView) { implicit request =>
      val requestForm: Form[HandleApprovalForm] = HandleApprovalForm.form.bindFromRequest

      def errors(errors: Form[HandleApprovalForm]) =
        fetchApiDefinitionSummary(serviceName, environment).map(details => BadRequest(deploymentReview(errors, details)))

      def approveApplicationWithValidForm(validForm: HandleApprovalForm) = {
        validForm.approval_confirmation match {
          case "Yes" => {
            deploymentApprovalService.approveService(serviceName, Environment.withName(environment)) map {
              _ => Redirect(routes.DeploymentApprovalController.pendingPage().url, SEE_OTHER)
            }
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

