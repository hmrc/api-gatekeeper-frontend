/*
 * Copyright 2018 HM Revenue & Customs
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

import connectors.{ApplicationConnector, AuthConnector, DeveloperConnector}
import model.State.{State, _}
import model.UpliftAction._
import model._
import org.joda.time.DateTime
import play.api.Logger
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import utils.{GatekeeperAuthProvider, GatekeeperAuthWrapper}
import views.html.approvedApplication._
import views.html.dashboard._
import views.html.review._

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

object DashboardController extends DashboardController with WithAppConfig {
  override val applicationConnector = ApplicationConnector
  override val developerConnector = DeveloperConnector
  override def authProvider = GatekeeperAuthProvider
  override def authConnector = AuthConnector
}

trait DashboardController extends BaseController with GatekeeperAuthWrapper {

  val applicationConnector: ApplicationConnector
  val developerConnector: DeveloperConnector
  implicit val dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)

  def dashboardPage: Action[AnyContent] = requiresRole(Role.APIGatekeeper) { implicit request => implicit hc =>
    redirectIfExternalTestEnvironment {

      def applicationsForDashboard(apps: Seq[ApplicationWithUpliftRequest]) = {
        val grouped: Map[State, Seq[ApplicationWithUpliftRequest]] = apps.groupBy(_.state)
        val pendingApproval = grouped.getOrElse(PENDING_GATEKEEPER_APPROVAL, Seq())
        val pendingVerification = grouped.getOrElse(PENDING_REQUESTER_VERIFICATION, Seq()) ++ grouped.getOrElse(PRODUCTION, Seq())

        CategorisedApplications(pendingApproval.sortBy(_.submittedOn), pendingVerification.sortBy(_.name.toLowerCase))
      }

      for {
        apps <- applicationConnector.fetchApplicationsWithUpliftRequest()
        mappedApps = applicationsForDashboard(apps)
      } yield Ok(dashboard(mappedApps))
    }
  }

  def reviewPage(appId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper) { implicit request => implicit hc =>
    redirectIfExternalTestEnvironment {
      fetchApplicationReviewDetails(appId) map (details => Ok(review(HandleUpliftForm.form, details)))
    }
  }

  private def fetchApplicationReviewDetails(appId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[ApplicationReviewDetails] = {
    for {
      app <- applicationConnector.fetchApplication(appId)
      submission <- lastSubmission(app)
    } yield applicationReviewDetails(app.application, submission)
  }

  def approvedApplicationPage(appId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper) { implicit request => implicit hc =>

    def lastApproval(app: ApplicationWithHistory): StateHistory = {
      app.history.filter(_.state == State.PENDING_REQUESTER_VERIFICATION)
        .sortWith(StateHistory.ascendingDateForAppId)
        .lastOption.getOrElse(throw new InconsistentDataState("pending requester verification state history item not found"))
    }

    def administrators(app: ApplicationWithHistory): Future[Seq[User]] = {
      val emails: Set[String] = app.application.admins.map(_.emailAddress)
      developerConnector.fetchByEmails(emails.toSeq)
    }

    def application(app: ApplicationResponse, approved: StateHistory, admins: Seq[User], submissionDetails: SubmissionDetails) = {
      val verified = app.state.name == State.PRODUCTION
      val details = applicationReviewDetails(app, submissionDetails)(request)

      ApprovedApplication(details, admins, approved.actor.id, approved.changedAt, verified)
    }

    redirectIfExternalTestEnvironment {

      for {
        app <- applicationConnector.fetchApplication(appId)
        approval = lastApproval(app)
        submission <- lastSubmission(app)
        admins <- administrators(app)
        approvedApp: ApprovedApplication = application(app.application, approval, admins, submission)
      } yield Ok(approved(approvedApp))
    }
  }

  private def lastSubmission(app: ApplicationWithHistory)(implicit hc: HeaderCarrier): Future[SubmissionDetails] = {
    val submission: StateHistory = app.history.filter(_.state == State.PENDING_GATEKEEPER_APPROVAL)
      .sortWith(StateHistory.ascendingDateForAppId)
      .lastOption.getOrElse(throw new InconsistentDataState("pending gatekeeper approval state history item not found"))

    developerConnector.fetchByEmail(submission.actor.id).map(s =>
      SubmissionDetails(s"${s.firstName} ${s.lastName}", s.email, submission.changedAt))
  }

  private def applicationReviewDetails(app: ApplicationResponse, submission: SubmissionDetails)(implicit request: Request[_]) = {

    val currentRateLimitTierToDisplay = if (isSuperUser) Some(app.rateLimitTier) else None

    val contactDetails = for {
      checkInformation <- app.checkInformation
      contactDetails <- checkInformation.contactDetails
    } yield contactDetails

    val reviewContactName = contactDetails.map(_.fullname)
    val reviewContactEmail = contactDetails.map(_.email)
    val reviewContactTelephone = contactDetails.map(_.telephoneNumber)
    val applicationDetails = app.checkInformation.flatMap(_.applicationDetails)

    ApplicationReviewDetails(
      app.id.toString,
      app.name,
      app.description.getOrElse(""),
      currentRateLimitTierToDisplay,
      submission,
      reviewContactName,
      reviewContactEmail,
      reviewContactTelephone,
      applicationDetails,
      app.termsAndConditionsUrl,
      app.privacyPolicyUrl)
  }

  def handleUplift(appId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper) { implicit request => implicit hc =>
    redirectIfExternalTestEnvironment {
      val requestForm = HandleUpliftForm.form.bindFromRequest

      def errors(errors: Form[HandleUpliftForm]) =
        fetchApplicationReviewDetails(appId) map (details => BadRequest(review(errors, details)))

      def recovery: PartialFunction[Throwable, play.api.mvc.Result] = {
        case e: PreconditionFailed => {
          Logger.warn("Rejecting the uplift failed as the application might have already been rejected.", e)
          Redirect(routes.ApplicationController.applicationsPage())
        }
      }

      def addApplicationWithValidForm(validForm: HandleUpliftForm) = {
        UpliftAction.from(validForm.action) match {
          case Some(APPROVE) =>
            applicationConnector.approveUplift(appId, loggedIn.get) map (
              ApproveUpliftSuccessful => Redirect(routes.ApplicationController.applicationsPage())) recover recovery
          case Some(REJECT) =>
            applicationConnector.rejectUplift(appId, loggedIn.get, validForm.reason.get) map (
              RejectUpliftSuccessful => Redirect(routes.ApplicationController.applicationsPage())) recover recovery
        }
      }

      requestForm.fold(errors, addApplicationWithValidForm)
    }
  }

  def handleUpdateRateLimitTier(appId: String): Action[AnyContent] =
    requiresRole(Role.APIGatekeeper) { implicit request => implicit hc =>
      redirectIfExternalTestEnvironment {
        val result = Redirect(routes.DashboardController.approvedApplicationPage(appId))
        if (!isSuperUser) {
          Future.successful(result)
        } else {
          val newTier = RateLimitTier.withName(UpdateRateLimitForm.form.bindFromRequest().get.tier)
          applicationConnector.updateRateLimitTier(appId, newTier) map {
            case ApplicationUpdateSuccessResult =>
              result.flashing("success" -> s"Rate limit tier has been changed to $newTier")
          }
        }
      }
    }

  private def redirectIfExternalTestEnvironment(body: => Future[Result]) = {
    appConfig.isExternalTestEnvironment match {
      case true => Future.successful(Redirect(routes.ApplicationController.applicationsPage))
      case false => body
    }
  }
}
