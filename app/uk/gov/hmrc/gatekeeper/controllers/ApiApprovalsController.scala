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
import play.api.data.Forms.{mapping, optional, text}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.Environment
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.GatekeeperAuthorisationActions
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationService, StrideAuthorisationService}
import uk.gov.hmrc.gatekeeper.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.gatekeeper.connectors.ApiCataloguePublishConnector
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.services.DeploymentApprovalService
import uk.gov.hmrc.gatekeeper.utils.ErrorHelper
import uk.gov.hmrc.gatekeeper.views.html.ErrorTemplate
import uk.gov.hmrc.gatekeeper.views.html.apiapprovals._

object ApiApprovalsController {

  case class FilterForm(
      newStatus: Option[String],
      approvedStatus: Option[String],
      failedStatus: Option[String],
      resubmittedStatus: Option[String]
    )

  val filterForm: Form[FilterForm] = Form(
    mapping(
      "newStatus"         -> optional(text),
      "approvedStatus"    -> optional(text),
      "failedStatus"      -> optional(text),
      "resubmittedStatus" -> optional(text)
    )(FilterForm.apply)(FilterForm.unapply)
  )

  case class ReviewForm(
      approve: Option[String],
      approveDetail: Option[String],
      declineDetail: Option[String]
    )

  val reviewForm: Form[ReviewForm] = Form(
    mapping(
      "approve"       -> optional(text).verifying("api.approvals.review.action.required", _.isDefined),
      "approveDetail" -> optional(text(maxLength = 500)),
      "declineDetail" -> optional(text(maxLength = 500))
    )(ReviewForm.apply)(ReviewForm.unapply)
      .verifying(
        "api.approvals.review.decline.reason.required",
        fields =>
          fields match {
            case data: ReviewForm =>
              if (data.approve.contains("false") && data.declineDetail.isEmpty) false else true
          }
      )
  )

  case class CommentForm(
      comment: Option[String]
    )

  val commentForm: Form[CommentForm] = Form(
    mapping(
      "comment" -> optional(text).verifying("api.approvals.comment.required", _.isDefined)
    )(CommentForm.apply)(CommentForm.unapply)
  )
}

class ApiApprovalsController @Inject() (
    deploymentApprovalService: DeploymentApprovalService,
    mcc: MessagesControllerComponents,
    apiCataloguePublishConnector: ApiCataloguePublishConnector,
    apiApprovalsFilterView: ApiApprovalsFilterView,
    apiApprovalsHistoryView: ApiApprovalsHistoryView,
    apiApprovalsReviewView: ApiApprovalsReviewView,
    apiApprovalsApprovedSuccessView: ApiApprovalsApprovedSuccessView,
    apiApprovalsDeclinedSuccessView: ApiApprovalsDeclinedSuccessView,
    apiApprovalsCommentSuccessView: ApiApprovalsCommentSuccessView,
    apiApprovalsCommentView: ApiApprovalsCommentView,
    override val errorTemplate: ErrorTemplate,
    val errorHandler: ErrorHandler,
    strideAuthorisationService: StrideAuthorisationService,
    val ldapAuthorisationService: LdapAuthorisationService
  )(implicit val appConfig: AppConfig,
    override val ec: ExecutionContext
  ) extends GatekeeperBaseController(strideAuthorisationService, mcc)
    with GatekeeperAuthorisationActions
    with ErrorHelper {
  import ApiApprovalsController._

  def filterPage(defaultFiltering: Boolean): Action[AnyContent] = anyAuthenticatedUserAction { implicit request =>
    def getQueryParamsFromForm(form: FilterForm): Seq[(String, String)] = {
      getQueryParamFromStatusVar("NEW", form.newStatus) ++
        getQueryParamFromStatusVar("APPROVED", form.approvedStatus) ++
        getQueryParamFromStatusVar("FAILED", form.failedStatus) ++
        getQueryParamFromStatusVar("RESUBMITTED", form.resubmittedStatus)
    }

    def getQueryParamFromStatusVar(key: String, value: Option[String]): Seq[(String, String)] = {
      if (value.contains("true")) {
        Seq("status" -> key)
      } else {
        Seq.empty
      }
    }

    def handleValidForm(inputForm: FilterForm) = {
      val form = if (defaultFiltering)
        FilterForm(newStatus = Some("true"), approvedStatus = None, failedStatus = None, resubmittedStatus = Some("true"))
      else
        inputForm

      val params: Seq[(String, String)] = getQueryParamsFromForm(form)
      val queryForm                     = filterForm.fill(form)

      for {
        apps <- deploymentApprovalService.searchServices(params)
      } yield Ok(apiApprovalsFilterView(queryForm, apps))
    }

    def handleInvalidForm(form: Form[FilterForm]) = {

      for {
        apps <- deploymentApprovalService.fetchAllServices()
      } yield Ok(apiApprovalsFilterView(form, apps))
    }

    ApiApprovalsController.filterForm.bindFromRequest().fold(handleInvalidForm, handleValidForm)
  }

  def historyPage(serviceName: String, environment: String): Action[AnyContent] = anyStrideUserAction { implicit request =>
    fetchApiDefinitionSummary(serviceName, Environment.unsafeApply(environment)).map(apiDefinition => Ok(apiApprovalsHistoryView(apiDefinition)))
  }

  def reviewPage(serviceName: String, environment: String): Action[AnyContent] = anyStrideUserAction { implicit request =>
    fetchApiDefinitionSummary(serviceName, Environment.unsafeApply(environment)).map(apiDefinition => Ok(apiApprovalsReviewView(reviewForm, apiDefinition)))
  }

  def reviewAction(serviceName: String, environment: String): Action[AnyContent] = anyAuthenticatedUserAction { implicit request =>
    val env = Environment.unsafeApply(environment)

    val requestForm: Form[ReviewForm] = reviewForm.bindFromRequest()

    def errors(errors: Form[ReviewForm]) =
      fetchApiDefinitionSummary(serviceName, env).map(details => BadRequest(apiApprovalsReviewView(errors, details)))

    def doCalls(serviceName: String, environment: Environment, approve: Boolean, approveDetail: Option[String], declineDetail: Option[String]): Future[Unit] = {
      approve match {
        case true  => deploymentApprovalService.approveService(serviceName, environment, gatekeeperUser.get, approveDetail)
            .flatMap(_ =>
              environment match {
                case (environment) if (environment == Environment.PRODUCTION) && approve => apiCataloguePublishConnector.publishByServiceName(serviceName).map(_ => ())
                case _                                                                   => successful(())
              }
            )
        case false => deploymentApprovalService.declineService(serviceName, environment, gatekeeperUser.get, declineDetail)
      }
    }

    def updateApiWithValidForm(validForm: ReviewForm) = {

      val approve = validForm.approve.contains("true")

      doCalls(serviceName, env, approve, validForm.approveDetail, validForm.declineDetail) map {
        _ =>
          approve match {
            case true  => Ok(apiApprovalsApprovedSuccessView(serviceName))
            case false => Ok(apiApprovalsDeclinedSuccessView(serviceName))
          }
      } recoverWith {
        case UpstreamErrorResponse(message, _, _, _) =>
          deploymentApprovalService.addComment(serviceName, env, gatekeeperUser.get, s"PUBLISH FAILED: $message") flatMap {
            _ => errorHandler.publishErrorTemplate().map(BadRequest(_))
          }
      }
    }

    requestForm.fold(errors, updateApiWithValidForm)
  }

  def commentPage(serviceName: String, environment: String): Action[AnyContent] = anyStrideUserAction { implicit request =>
    fetchApiDefinitionSummary(serviceName, Environment.unsafeApply(environment)).map(apiDefinition => Ok(apiApprovalsCommentView(commentForm, apiDefinition)))
  }

  def addComment(serviceName: String, environment: String): Action[AnyContent] = anyStrideUserAction { implicit request =>
    val env = Environment.unsafeApply(environment)

    val requestForm: Form[CommentForm] = commentForm.bindFromRequest()

    def errors(errors: Form[CommentForm]) =
      fetchApiDefinitionSummary(serviceName, env).map(details => BadRequest(apiApprovalsCommentView(errors, details)))

    def addCommentWithValidForm(validForm: CommentForm) = {
      deploymentApprovalService.addComment(serviceName, env, gatekeeperUser.get, validForm.comment.get).map(_ => Ok(apiApprovalsCommentSuccessView(serviceName)))
    }

    requestForm.fold(errors, addCommentWithValidForm)
  }

  private def fetchApiDefinitionSummary(serviceName: String, environment: Environment)(implicit hc: HeaderCarrier): Future[APIApprovalSummary] = {
    deploymentApprovalService.fetchApprovalSummary(serviceName, environment)
  }
}
