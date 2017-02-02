/*
 * Copyright 2017 HM Revenue & Customs
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
import model._
import play.api.Logger
import play.api.data.Form
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.DeploymentApprovalService
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{GatekeeperAuthProvider, GatekeeperAuthWrapper}
import views.html.deploymentApproval.{deploymentApproval, deploymentReview}

import scala.concurrent.Future

object DeploymentApprovalController extends DeploymentApprovalController {
  override val deploymentApprovalService = DeploymentApprovalService
  override def authConnector = AuthConnector
  override def authProvider = GatekeeperAuthProvider
  override val appConfig = AppConfig
}

trait DeploymentApprovalController extends FrontendController with GatekeeperAuthWrapper {
  val deploymentApprovalService: DeploymentApprovalService
  implicit val appConfig: AppConfig

  private def redirectToPendingPage(pageNumber: Int, pageSize: Int) = {
    val pageParams = Map(
      "pageNumber" -> Seq(pageNumber.toString),
      "pageSize" -> Seq(pageSize.toString)
    )

    val queryParams = pageParams
    Redirect("", queryParams, 303)
  }

  protected def renderPendingPage(page: PageableCollection[APIDefinitionSummary], pageNumber: Int, pageSize: Int)(implicit request: Request[_]): Result =
    Ok(deploymentApproval(page))

  def pendingPage(optionalPageSize: Option[Int], optionalPageNumber: Option[Int]): Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>

      val pageSize = optionalPageSize.getOrElse(100)
      val pageNumber = optionalPageNumber.getOrElse(1)

      for {
        apps <- deploymentApprovalService.fetchUnapprovedServices
        page = PageableCollection(apps, pageNumber, pageSize)
      } yield {
        if (page.valid) {
          renderPendingPage(page, pageNumber, pageSize)
        }
        else {
          redirectToPendingPage(1, pageSize)
        }
      }
  }

  def fetchApiDefinitionSummary(serviceName: String)(implicit hc: HeaderCarrier): Future[APIDefinitionSummary] = {
    deploymentApprovalService.fetchApiDefinitionSummary(serviceName)
  }

  def reviewPage(serviceName: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
      fetchApiDefinitionSummary(serviceName) map (apiDefinition => Ok(deploymentReview(HandleApprovalForm.form, apiDefinition)))
  }

  def handleApproval(serviceName: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
      val requestForm = HandleApprovalForm.form.bindFromRequest

      def errors(errors: Form[HandleApprovalForm]) =
        fetchApiDefinitionSummary(serviceName) map (details => BadRequest(deploymentReview(errors, details)))

      def recovery: PartialFunction[Throwable, play.api.mvc.Result] = {
        case e: PreconditionFailed => {
          Logger.warn("Rejecting the service failed.", e)
          Redirect(controllers.routes.DashboardController.dashboardPage)
        }
      }

      def approveApplicationWithValidForm(validForm: HandleApprovalForm) = {
        ApprovalAction.from(validForm.action) match {
          case Some(ApprovalAction.APPROVE) => deploymentApprovalService.approveService(serviceName) map (
            ApproveServiceSuccessful => redirectToPendingPage(1, 1)
            ) recover recovery
          case _ => throw new UnsupportedOperationException("Cant't Reject Service Approval") // TODO handle rejected Services
        }
      }

      requestForm.fold(errors, approveApplicationWithValidForm)
  }
}
