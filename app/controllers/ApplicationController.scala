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
import model.UpliftAction.{APPROVE, REJECT}
import model._
import org.joda.time.DateTime
import play.api.Logger
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.{ApiDefinitionService, ApplicationService, DeveloperService, SubscriptionFieldsService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{GatekeeperAuthProvider, GatekeeperAuthWrapper, SubscriptionEnhancer}
import views.html.applications._
import views.html.approvedApplication.approved
import views.html.include.subscriptionFields
import views.html.review.review

import scala.concurrent.Future

object ApplicationController extends ApplicationController with WithAppConfig {
  override val applicationService = ApplicationService
  override val apiDefinitionService = ApiDefinitionService
  override val developerService = DeveloperService
  override val subscriptionFieldsService = SubscriptionFieldsService
  override def authProvider = GatekeeperAuthProvider
  override def authConnector = AuthConnector
}

trait ApplicationController extends BaseController with GatekeeperAuthWrapper {

  val applicationService: ApplicationService
  val apiDefinitionService: ApiDefinitionService
  val developerService: DeveloperService
  val subscriptionFieldsService: SubscriptionFieldsService
  implicit val dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)

  def applicationsPage: Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    println("Loading the applications page")
    implicit request => implicit hc =>
      for {
        apps <- applicationService.fetchAllSubscribedApplications
        apis <- apiDefinitionService.fetchAllApiDefinitions
        subApps = SubscriptionEnhancer.combine(apps, apis)
      } yield Ok(applications(subApps, groupApisByStatus(apis), isSuperUser))
  }

  def applicationPage(appId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
        println("Loading the application page")
    implicit request =>
      implicit hc =>
        withApp(appId) { app =>

          def latestTOUAgreement(appWithHistory: ApplicationWithHistory): Option[TermsOfUseAgreement] = {
            appWithHistory.application.checkInformation.flatMap {
              _.termsOfUseAgreements match {
                case Nil => None
                case agreements => Option(agreements.maxBy(_.timeStamp))
              }
            }
          }

          applicationService.fetchApplicationSubscriptions(app.application).map(subscriptions => Ok(application(
            app,
            subscriptions.filter(sub => sub.versions.exists(version => version.subscribed)).sortWith(_.name.toLowerCase < _.name.toLowerCase),
            isSuperUser,
            latestTOUAgreement(app))))
        }
  }

  def resendVerification(appId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
      for {
        _ <- applicationService.resendVerification(appId, loggedIn.get)
      } yield {
        Redirect(routes.ApplicationController.applicationPage(appId))
          .flashing("success" -> "Verification email has been sent")
      }
  }

  def manageSubscription(appId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper, requiresSuperUser = true) {
    implicit request =>
      implicit hc =>
        withApp(appId) { app =>
          applicationService.fetchApplicationSubscriptions(app.application, withFields = true).map {
            subs => Ok(manage_subscriptions(app, subs.sortWith(_.name.toLowerCase < _.name.toLowerCase), isSuperUser))
          }
        }
  }

  def subscribeToApi(appId: String, context: String, version: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper, requiresSuperUser = true) {
    implicit request => implicit hc =>
      applicationService.subscribeToApi(appId, context, version).map(_ => Redirect(routes.ApplicationController.manageSubscription(appId)))
  }

  def unsubscribeFromApi(appId: String, context: String, version: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper, requiresSuperUser = true) {
    implicit request => implicit hc => withApp(appId) { app =>
      applicationService.unsubscribeFromApi(app.application, context, version).map(_ => Redirect(routes.ApplicationController.manageSubscription(appId)))
    }
  }

  def updateSubscriptionFields(appId: String, apiContext: String, apiVersion: String): Action[AnyContent] = {
    requiresRole(Role.APIGatekeeper, requiresSuperUser = true) {
      implicit request => implicit hc => withApp(appId) { app =>
        def handleValidForm(validForm: SubscriptionFieldsForm) = {
          def saveFields(validForm: SubscriptionFieldsForm)(implicit hc: HeaderCarrier): Future[Any] = {
            if (validForm.fields.nonEmpty) {
              val fields = Map(validForm.fields.map(f => f.name -> f.value.getOrElse("")): _ *)
              subscriptionFieldsService.saveFieldValues(app.application.clientId, apiContext, apiVersion, fields)
            } else {
              Future.successful(())
            }
          }

          saveFields(validForm).map { _ => Redirect(routes.ApplicationController.manageSubscription(appId)) }
        }

        def handleInvalidForm(formWithErrors: Form[SubscriptionFieldsForm]) =
          Future.successful(Redirect(routes.ApplicationController.manageSubscription(appId)))

        SubscriptionFieldsForm.form.bindFromRequest.fold(handleInvalidForm, handleValidForm)
      }
    }
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
        applicationService.updateScopes(app.application, scopes).map {
          case UpdateScopesInvalidScopesResult =>
            val form = scopesForm.fill(scopes).withError("scopes", Messages("invalid.scope"))
            BadRequest(manage_scopes(app.application, form, isSuperUser))

          case UpdateScopesSuccessResult => Redirect(routes.ApplicationController.applicationPage(appId))
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
        if (app.application.name == form.applicationNameConfirmation) {
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

  def reviewPage(appId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper) { implicit request => implicit hc =>
    redirectIfExternalTestEnvironment {
      fetchApplicationReviewDetails(appId) map (details => Ok(review(HandleUpliftForm.form, details)))
    }
  }

  private def fetchApplicationReviewDetails(appId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[ApplicationReviewDetails] = {
    for {
      app <- applicationService.fetchApplication(appId)
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
      developerService.fetchDevelopersByEmails(emails.toSeq)
    }

    def application(app: ApplicationResponse, approved: StateHistory, admins: Seq[User], submissionDetails: SubmissionDetails) = {
      val verified = app.state.name == State.PRODUCTION
      val details = applicationReviewDetails(app, submissionDetails)(request)

      ApprovedApplication(details, admins, approved.actor.id, approved.changedAt, verified)
    }

    redirectIfExternalTestEnvironment {

      for {
        app <- applicationService.fetchApplication(appId)
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

    developerService.fetchUser(submission.actor.id).map(s =>
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
            applicationService.approveUplift(appId, loggedIn.get) map (
              ApproveUpliftSuccessful => Redirect(routes.ApplicationController.applicationPage(appId))) recover recovery
          case Some(REJECT) =>
            applicationService.rejectUplift(appId, loggedIn.get, validForm.reason.get) map (
              RejectUpliftSuccessful => Redirect(routes.ApplicationController.applicationPage(appId))) recover recovery
        }
      }

      requestForm.fold(errors, addApplicationWithValidForm)
    }
  }

  def handleUpdateRateLimitTier(appId: String): Action[AnyContent] =
    requiresRole(Role.APIGatekeeper) { implicit request => implicit hc =>
      redirectIfExternalTestEnvironment {
        val result = Redirect(routes.ApplicationController.applicationPage(appId))
        if (!isSuperUser) {
          Future.successful(result)
        } else {
          val newTier = RateLimitTier.withName(UpdateRateLimitForm.form.bindFromRequest().get.tier)
          applicationService.updateRateLimitTier(appId, newTier) map {
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

  def createPrivOrROPCApplicationPage(): Action[AnyContent] = { requiresRole(Role.APIGatekeeper, requiresSuperUser = true) {
    implicit request =>
      implicit hc => {
        Future.successful(Ok(create_application(createPrivOrROPCAppForm.fill(CreatePrivOrROPCAppForm()))))
      }
    }
  }

  def createPrivOrROPCApplicationAction(): Action[AnyContent] = {
    requiresRole(Role.APIGatekeeper, requiresSuperUser = true) {
      implicit request => implicit hc => {

        def handleInvalidForm(form: Form[CreatePrivOrROPCAppForm]) = {
          Future.successful(BadRequest(create_application(form)))
        }

        def handleValidForm(form: CreatePrivOrROPCAppForm): Future[Result] = {

          val env = if(appConfig.isExternalTestEnvironment) Environment.SANDBOX else Environment.PRODUCTION

          def handleFormWithValidName: Future[Result] =
            form.accessType.flatMap(AccessType.from) match {
            case Some(accessType) => {
              val collaborators = Seq(Collaborator(form.adminEmail, CollaboratorRole.ADMINISTRATOR))

              applicationService.createPrivOrROPCApp(env, form.applicationName, form.applicationDescription, collaborators, AppAccess(accessType, Seq())) flatMap {
                case CreatePrivOrROPCAppSuccessResult(appId, appName, appEnv, clientId, Some(totp), access) =>
                  Future.successful(Ok(create_application_success(appId, appName, appEnv, Some(access.accessType), Some(totp), clientId)))
                case CreatePrivOrROPCAppSuccessResult(appId, appName, appEnv, clientId, None, access) =>
                  applicationService.getClientSecret(appId).map(secret => Ok(create_application_success(appId, appName, appEnv, Some(access.accessType), None, clientId, Some(secret))))
              }
            }
          }

          def handleFormWithInvalidName: Future[Result] = {
            val formWithErrors = CreatePrivOrROPCAppForm.invalidAppName(createPrivOrROPCAppForm.fill(form))
            Future.successful(BadRequest(create_application(formWithErrors)))
          }

          def hasValidName(apps: Seq[ApplicationResponse]) = env match {
            case Environment.PRODUCTION => !apps.exists(app => (app.deployedTo == Environment.PRODUCTION.toString) && (app.name == form.applicationName))
            case _ => true
          }

          for {
            appNameIsValid <- applicationService.fetchApplications.map(hasValidName)
            result <- if (appNameIsValid) handleFormWithValidName else handleFormWithInvalidName
          } yield result
        }

        createPrivOrROPCAppForm.bindFromRequest.fold(handleInvalidForm, handleValidForm)
      }
    }
  }
}
