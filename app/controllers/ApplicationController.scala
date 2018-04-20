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

import connectors.AuthConnector
import model.Forms._
import model._
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.{ApiDefinitionService, ApplicationService, DeveloperService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{GatekeeperAuthProvider, GatekeeperAuthWrapper, SubscriptionEnhancer}
import views.html.applications._
import views.html.error_template

import scala.concurrent.Future

object ApplicationController extends ApplicationController with WithAppConfig {
  override val applicationService = ApplicationService
  override val apiDefinitionService = ApiDefinitionService
  override val developerService = DeveloperService
  override def authConnector = AuthConnector
  override def authProvider = GatekeeperAuthProvider
}

trait ApplicationController extends BaseController with GatekeeperAuthWrapper {

  val applicationService: ApplicationService
  val apiDefinitionService: ApiDefinitionService
  val developerService: DeveloperService

  def applicationsPage: Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
      for {
        apps <- applicationService.fetchAllSubscribedApplications
        apis <- apiDefinitionService.fetchAllApiDefinitions
        subApps = SubscriptionEnhancer.combine(apps, apis)
      } yield Ok(applications(subApps, groupApisByStatus(apis)))
  }

  def applicationPage(appId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
      val applicationFuture = applicationService.fetchApplication(appId)
      val subscriptionsFuture = applicationService.fetchApplicationSubscriptions(appId)

      def lastApproval(app: ApplicationWithHistory): StateHistory = {
        app.history.filter(_.state == State.PENDING_REQUESTER_VERIFICATION)
          .sortWith(StateHistory.ascendingDateForAppId)
          .lastOption.getOrElse(throw new InconsistentDataState("pending requester verification state history item not found"))
      }

      def administrators(app: ApplicationWithHistory): Future[Seq[User]] = {
        val emails: Set[String] = app.application.admins.map(_.emailAddress)
        Future.successful(Seq.empty)
      }

      def approvedApplication(app: ApplicationResponse, approved: StateHistory, admins: Seq[User], submissionDetails: SubmissionDetails) = {
        val verified = app.state.name == State.PRODUCTION
        val details = applicationReviewDetails(app, submissionDetails)(request)

        ApprovedApplication(details, admins, approved.actor.id, approved.changedAt, verified)
      }

      for {
        applicationWithHistory <- applicationFuture
        approval = lastApproval(applicationWithHistory)
        submission <- lastSubmission(applicationWithHistory)
        admins <- administrators(applicationWithHistory)
        subscriptions <- subscriptionsFuture
        approvedApp = approvedApplication(applicationWithHistory.application, approval, admins, submission)
        appResponse = applicationWithHistory.application.copy(approvedDetails = Some(approvedApp))
      } yield Ok(application(applicationWithHistory.copy(application = appResponse), subscriptions.filter(sub => sub.versions.exists(version => version.subscribed)).sortWith(_.name.toLowerCase < _.name.toLowerCase), isSuperUser))
  }
  def resendVerification(appId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
      for {
        _ <- applicationService.resendVerification(appId, loggedIn.get)
      } yield {
        Redirect(routes.DashboardController.approvedApplicationPage(appId))
          .flashing("success" -> "Verification email has been sent")
      }
  }

  def manageSubscription(appId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper, requiresSuperUser = true) {
    implicit request => implicit hc =>

      val applicationFuture = applicationService.fetchApplication(appId)
      val subscriptionsFuture = applicationService.fetchApplicationSubscriptions(appId)

      for {
        app <- applicationFuture
        subs <- subscriptionsFuture

      } yield Ok(manage_subscriptions(app, subs.sortWith(_.name.toLowerCase < _.name.toLowerCase), isSuperUser))
  }

  def subscribeToApi(appId: String, context: String, version: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper, requiresSuperUser = true) {
    implicit request => implicit hc =>
    applicationService.subscribeToApi(appId, context, version).map(_ => Redirect(routes.ApplicationController.manageSubscription(appId)))
  }

  def unsubscribeFromApi(appId: String, context: String, version: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper, requiresSuperUser = true) {
    implicit request => implicit hc =>
    applicationService.unsubscribeFromApi(appId, context, version).map(_ => Redirect(routes.ApplicationController.manageSubscription(appId)))
  }

  def manageAccessOverrides(appId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper, requiresSuperUser = true) {
    implicit request => implicit hc => withApp(appId) { app =>
      app.application.access match {
        case access: Standard => {
          Future.successful(Ok(manage_access_overrides(app.application, accessOverridesForm.fill(access.overrides), isSuperUser)))
        }
      }
    }
  }

  def updateAccessOverrides(appId: String) = requiresRole(Role.APIGatekeeper, requiresSuperUser = true) {
    implicit request => implicit hc => withApp(appId) { app =>
      def formFieldForOverrideFlag(overrideFlag: OverrideFlag): String = overrideFlag match {
        case SuppressIvForAgents(_) => FormFields.suppressIvForAgentsScopes
        case SuppressIvForOrganisations(_) => FormFields.suppressIvForOrganisationsScopes
        case GrantWithoutConsent(_) => FormFields.grantWithoutConsentScopes
      }

      def handleValidForm(overrides: Set[OverrideFlag]) = {
        applicationService.updateOverrides(app.application, overrides).map {
          case UpdateOverridesFailureResult(overrideFlagErrors) =>
            var form = accessOverridesForm.fill(overrides)

            overrideFlagErrors.foreach(err => form = form.withError(formFieldForOverrideFlag(err), Messages("invalid.scope")))

            BadRequest(manage_access_overrides(app.application, form, isSuperUser))
          case UpdateOverridesSuccessResult => Redirect(routes.ApplicationController.applicationPage(appId))
        }
      }

      def handleFormError(form: Form[Set[OverrideFlag]]) = {
        Future.successful(BadRequest(manage_access_overrides(app.application, form, isSuperUser)))
      }

      accessOverridesForm.bindFromRequest.fold(handleFormError, handleValidForm)
    }
  }

  def manageScopes(appId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper, requiresSuperUser = true) {
    implicit request => implicit hc => withApp(appId) { app =>
      app.application.access match {
        case access: AccessWithRestrictedScopes => {
          val form = scopesForm.fill(access.scopes)
          Future.successful(Ok(manage_scopes(app.application, form, isSuperUser)))
        }
        case _ => Future.failed(new RuntimeException("Invalid access type on application"))
      }
    }
  }

  def updateScopes(appId: String) = requiresRole(Role.APIGatekeeper, requiresSuperUser = true) {
    implicit request => implicit hc => withApp(appId) { app =>
      def handleValidForm(scopes: Set[String]) = {
        applicationService.updateScopes(app.application, scopes).map { _ =>
          Redirect(routes.ApplicationController.applicationPage(appId))
        }
      }

      def handleFormError(form: Form[Set[String]]) = {
        Future.successful(BadRequest(manage_scopes(app.application, form, isSuperUser)))
      }

      scopesForm.bindFromRequest.fold(handleFormError, handleValidForm)
    }
  }

  def manageRateLimitTier(appId: String) = requiresRole(Role.APIGatekeeper, requiresSuperUser = true) {
    implicit request => implicit hc => withApp(appId) { app =>
      val form = UpdateRateLimitForm.form.fill(UpdateRateLimitForm(app.application.rateLimitTier.toString))
      Future.successful(Ok(manage_rate_limit(app.application, form, isSuperUser)))
    }
  }

  def updateRateLimitTier(appId: String) = requiresRole(Role.APIGatekeeper, requiresSuperUser = true) {
    implicit request => implicit hc => withApp(appId) { app =>
      def handleValidForm(form: UpdateRateLimitForm) = {
        applicationService.updateRateLimitTier(appId, RateLimitTier.withName(form.tier)).map { _ =>
          Redirect(routes.ApplicationController.applicationPage(appId))
        }
      }

      def handleFormError(form: Form[UpdateRateLimitForm]) = {
        Future.successful(BadRequest(manage_rate_limit(app.application, form, isSuperUser)))
      }
      UpdateRateLimitForm.form.bindFromRequest.fold(handleFormError, handleValidForm)
    }
  }

  def deleteApplicationPage(appId: String) = requiresRole(Role.APIGatekeeper, requiresSuperUser = true) {
    implicit request => implicit hc => withApp(appId) { app =>
      Future.successful(Ok(delete_application(app, isSuperUser, deleteApplicationForm.fill(DeleteApplicationForm("", Option(""))))))
    }
  }

  def deleteApplicationAction(appId: String) = requiresRole(Role.APIGatekeeper, requiresSuperUser = true) {
    implicit request => implicit hc => withApp(appId) { app =>
      def handleValidForm(form: DeleteApplicationForm) = {
        if(app.application.name == form.applicationNameConfirmation) {
          applicationService.deleteApplication(appId, loggedIn.get, form.collaboratorEmail.get).map {
            case ApplicationDeleteSuccessResult => Ok(delete_application_success(app, isSuperUser))
            case ApplicationDeleteFailureResult => technicalDifficulties
          }
        }
        else {
          val formWithErrors = deleteApplicationForm.fill(form).withError(FormFields.applicationNameConfirmation, Messages("application.confirmation.error"))

          Future.successful(BadRequest(delete_application(app, isSuperUser, formWithErrors)))
        }
      }

      def handleFormError(form: Form[DeleteApplicationForm]) = {
        Future.successful(BadRequest(delete_application(app, isSuperUser, form)))
      }

      deleteApplicationForm.bindFromRequest.fold(handleFormError, handleValidForm)
    }
  }

  private def groupApisByStatus(apis: Seq[APIDefinition]): Map[String, Seq[VersionSummary]] = {

    val versions = for {
      api <- apis
      version <- api.versions
    } yield VersionSummary(api.name, version.status, APIIdentifier(api.context, version.version))

    versions.groupBy(v => APIStatus.displayedStatus(v.status))
  }

  private def withApp(appId: String)(f: ApplicationWithHistory => Future[Result])(implicit request: Request[_]) = {
    applicationService.fetchApplication(appId).flatMap(f)
  }

  private def lastSubmission(app: ApplicationWithHistory)(implicit hc: HeaderCarrier): Future[SubmissionDetails] = {
    val submission: StateHistory = app.history.filter(_.state == State.PENDING_GATEKEEPER_APPROVAL)
      .sortWith(StateHistory.ascendingDateForAppId)
      .lastOption.getOrElse(throw new InconsistentDataState("pending gatekeeper approval state history item not found"))

      developerService.fetchDeveloper(submission.actor.id).map(s =>
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
}
