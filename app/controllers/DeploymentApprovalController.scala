/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.DeploymentApprovalService
import uk.gov.hmrc.http.HeaderCarrier
import utils.GatekeeperAuthWrapper
import views.html.deploymentApproval.{deploymentApproval, deploymentReview}

import scala.concurrent.{ExecutionContext, Future}

class DeploymentApprovalController @Inject()(val authConnector: AuthConnector,
                                             deploymentApprovalService: DeploymentApprovalService
                                            )(implicit override val appConfig: AppConfig, val ec: ExecutionContext)
  extends BaseController with GatekeeperAuthWrapper {

  def pendingPage(): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) { implicit request => implicit hc =>
      deploymentApprovalService.fetchUnapprovedServices().map(app => Ok(deploymentApproval(app)))
  }

  def reviewPage(serviceName: String, environment: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) { implicit request =>implicit hc =>
      fetchApiDefinitionSummary(serviceName, environment).map(apiDefinition => Ok(deploymentReview(HandleApprovalForm.form, apiDefinition)))
  }

  def handleApproval(serviceName: String, environment: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) { implicit request =>implicit hc =>
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

