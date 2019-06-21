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
import model.Forms._
import model.UpliftAction.{APPROVE, REJECT}
import model._
import org.joda.time.DateTime
import play.api.Logger
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Result}
import services.{ApiDefinitionService, ApplicationService, DeveloperService, SubscriptionFieldsService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{GatekeeperAuthWrapper, LoggedInRequest, SubscriptionEnhancer}
import views.html.applications._
import views.html.approvedApplication.approved
import views.html.review.review

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ApplicationController @Inject()(applicationService: ApplicationService,
                                      apiDefinitionService: ApiDefinitionService,
                                      developerService: DeveloperService,
                                      subscriptionFieldsService: SubscriptionFieldsService,
                                      override val authConnector: AuthConnector
                                     )(implicit override val appConfig: AppConfig, val ec: ExecutionContext)
  extends BaseController with GatekeeperAuthWrapper {

  implicit val dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)

  def applicationsPage(environment: Option[String] = None): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      implicit hc =>
        val env = Try(Environment.withName(environment.getOrElse("SANDBOX"))).toOption
        val defaults = Map("page" -> "1", "pageSize" -> "100", "sort" -> "NAME_ASC")
        val params = defaults ++ request.queryString.map { case (k, v) => k -> v.mkString }

        for {
          par <- applicationService.searchApplications(env, params)
          apis <- apiDefinitionService.fetchAllApiDefinitions(env)
          subApps = SubscriptionEnhancer.combine(par, apis)
        } yield Ok(applications(subApps, groupApisByStatus(apis), isAtLeastSuperUser, params))
  }

  def applicationPage(appId: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
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

          def subscriptions: Future[Either[String, Seq[Subscription]]] = {
            applicationService.fetchApplicationSubscriptions(app.application).map(
              subs => Right(subs.filter(sub => sub.versions.exists(version => version.subscribed)).sortWith(_.name.toLowerCase < _.name.toLowerCase))
            ).recover {
              case e: FetchApplicationSubscriptionsFailed =>
                Logger.warn(s"Failed to load API subscriptions for application $appId", e)
                Left(Messages("application.subscriptions.loader.error"))
            }
          }

          for {
            subs <- subscriptions
            devs <- developerService.fetchDevelopersByEmails(app.application.collaborators.map(colab => colab.emailAddress))
          } yield Ok(application(devs.toList, app, subs, isAtLeastSuperUser, isAdmin, latestTOUAgreement(app)))
        }
  }

  def resendVerification(appId: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      implicit hc =>
        withApp(appId) { app =>
          for {
            _ <- applicationService.resendVerification(app.application, loggedIn.userFullName.get)
          } yield {
            Redirect(routes.ApplicationController.applicationPage(appId))
              .flashing("success" -> "Verification email has been sent")
          }
        }
  }

  def manageSubscription(appId: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit request =>
      implicit hc =>
        withApp(appId) { app =>
          applicationService.fetchApplicationSubscriptions(app.application, withFields = true).map {
            subs => Ok(manage_subscriptions(app, subs.sortWith(_.name.toLowerCase < _.name.toLowerCase), isAtLeastSuperUser))
          }
        }
  }

  def subscribeToApi(appId: String, context: String, version: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit request =>
      implicit hc =>
        withApp(appId) { app =>
          applicationService.subscribeToApi(app.application, context, version).map(_ => Redirect(routes.ApplicationController.manageSubscription(appId)))
        }
  }

  def unsubscribeFromApi(appId: String, context: String, version: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit request =>
      implicit hc =>
        withApp(appId) { app =>
          applicationService.unsubscribeFromApi(app.application, context, version).map(_ => Redirect(routes.ApplicationController.manageSubscription(appId)))
        }
  }

  def updateSubscriptionFields(appId: String, apiContext: String, apiVersion: String): Action[AnyContent] = {
    requiresAtLeast(GatekeeperRole.SUPERUSER) {
      implicit request =>
        implicit hc =>
          withApp(appId) { app =>
            def handleValidForm(validForm: SubscriptionFieldsForm) = {
              if (validForm.fields.nonEmpty) {
                subscriptionFieldsService.saveFieldValues(
                  app.application,
                  apiContext,
                  apiVersion,
                  Map(validForm.fields.map(f => f.name -> f.value.getOrElse("")): _ *))
              }

              Future.successful(Redirect(routes.ApplicationController.manageSubscription(appId)))
            }

            def handleInvalidForm(formWithErrors: Form[SubscriptionFieldsForm]) =
              Future.successful(Redirect(routes.ApplicationController.manageSubscription(appId)))

            SubscriptionFieldsForm.form.bindFromRequest.fold(handleInvalidForm, handleValidForm)
          }
    }
  }

  def manageAccessOverrides(appId: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit request =>
      implicit hc =>
        withApp(appId) { app =>
          app.application.access match {
            case access: Standard => {
              Future.successful(Ok(manage_access_overrides(app.application, accessOverridesForm.fill(access.overrides), isAtLeastSuperUser)))
            }
          }
        }
  }

  def updateAccessOverrides(appId: String) = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit request =>
      implicit hc =>
        withApp(appId) { app =>
          def formFieldForOverrideFlag(overrideFlag: OverrideFlag): String = overrideFlag match {
            case SuppressIvForAgents(_) => FormFields.suppressIvForAgentsScopes
            case SuppressIvForOrganisations(_) => FormFields.suppressIvForOrganisationsScopes
            case SuppressIvForIndividuals(_) => FormFields.suppressIvForIndividualsScopes
            case GrantWithoutConsent(_) => FormFields.grantWithoutConsentScopes
          }

          def handleValidForm(overrides: Set[OverrideFlag]) = {
            applicationService.updateOverrides(app.application, overrides).map {
              case UpdateOverridesFailureResult(overrideFlagErrors) =>
                var form = accessOverridesForm.fill(overrides)

                overrideFlagErrors.foreach(err => form = form.withError(formFieldForOverrideFlag(err), Messages("invalid.scope")))

                BadRequest(manage_access_overrides(app.application, form, isAtLeastSuperUser))
              case UpdateOverridesSuccessResult => Redirect(routes.ApplicationController.applicationPage(appId))
            }
          }

          def handleFormError(form: Form[Set[OverrideFlag]]) = {
            Future.successful(BadRequest(manage_access_overrides(app.application, form, isAtLeastSuperUser)))
          }

          accessOverridesForm.bindFromRequest.fold(handleFormError, handleValidForm)
        }
  }

  def manageScopes(appId: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit request =>
      implicit hc =>
        withApp(appId) { app =>
          app.application.access match {
            case access: AccessWithRestrictedScopes => {
              val form = scopesForm.fill(access.scopes)
              Future.successful(Ok(manage_scopes(app.application, form, isAtLeastSuperUser)))
            }
            case _ => Future.failed(new RuntimeException("Invalid access type on application"))
          }
        }
  }

  def updateScopes(appId: String) = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit request =>
      implicit hc =>
        withApp(appId) { app =>
          def handleValidForm(scopes: Set[String]) = {
            applicationService.updateScopes(app.application, scopes).map {
              case UpdateScopesInvalidScopesResult =>
                val form = scopesForm.fill(scopes).withError("scopes", Messages("invalid.scope"))
                BadRequest(manage_scopes(app.application, form, isAtLeastSuperUser))

              case UpdateScopesSuccessResult => Redirect(routes.ApplicationController.applicationPage(appId))
            }
          }

          def handleFormError(form: Form[Set[String]]) = {
            Future.successful(BadRequest(manage_scopes(app.application, form, isAtLeastSuperUser)))
          }

          scopesForm.bindFromRequest.fold(handleFormError, handleValidForm)
        }
  }

  def manageRateLimitTier(appId: String) = requiresAtLeast(GatekeeperRole.ADMIN) {
    implicit request =>
      implicit hc =>
        withApp(appId) { app =>
          val form = UpdateRateLimitForm.form.fill(UpdateRateLimitForm(app.application.rateLimitTier.toString))
          Future.successful(Ok(manage_rate_limit(app.application, form)))
        }
  }

  def updateRateLimitTier(appId: String) = requiresAtLeast(GatekeeperRole.ADMIN) {
    implicit request =>
      implicit hc =>
        withApp(appId) { app =>
          def handleValidForm(form: UpdateRateLimitForm) = {
            applicationService.updateRateLimitTier(app.application, RateLimitTier.withName(form.tier)).map { _ =>
              Redirect(routes.ApplicationController.applicationPage(appId))
            }
          }

          def handleFormError(form: Form[UpdateRateLimitForm]) = {
            Future.successful(BadRequest(manage_rate_limit(app.application, form)))
          }

          UpdateRateLimitForm.form.bindFromRequest.fold(handleFormError, handleValidForm)
        }
  }

  def deleteApplicationPage(appId: String) = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit request =>
      implicit hc =>
        withApp(appId) { app =>
          Future.successful(Ok(delete_application(app, isAtLeastSuperUser, deleteApplicationForm.fill(DeleteApplicationForm("", Option(""))))))
        }
  }

  def deleteApplicationAction(appId: String) = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit request =>
      implicit hc =>
        withApp(appId) { app =>
          def handleValidForm(form: DeleteApplicationForm) = {
            if (app.application.name == form.applicationNameConfirmation) {
              applicationService.deleteApplication(app.application, loggedIn.userFullName.get, form.collaboratorEmail.get).map {
                case ApplicationDeleteSuccessResult => Ok(delete_application_success(app))
                case ApplicationDeleteFailureResult => technicalDifficulties
              }
            }
            else {
              val formWithErrors = deleteApplicationForm.fill(form).withError(FormFields.applicationNameConfirmation, Messages("application.confirmation.error"))

              Future.successful(BadRequest(delete_application(app, isAtLeastSuperUser, formWithErrors)))
            }
          }

          def handleFormError(form: Form[DeleteApplicationForm]) = {
            Future.successful(BadRequest(delete_application(app, isAtLeastSuperUser, form)))
          }

          deleteApplicationForm.bindFromRequest.fold(handleFormError, handleValidForm)
        }
  }

  def blockApplicationPage(appId: String) = requiresAtLeast(GatekeeperRole.ADMIN) {
    implicit request =>
      implicit hc =>
        withApp(appId) { app =>
          Future.successful(Ok(block_application(app, isAtLeastSuperUser, blockApplicationForm.fill(BlockApplicationForm("")))))
        }
  }

  def blockApplicationAction(appId: String) = requiresAtLeast(GatekeeperRole.ADMIN) {
    implicit request =>
      implicit hc =>
        withApp(appId) { app =>
          def handleValidForm(form: BlockApplicationForm) = {
            if (app.application.name == form.applicationNameConfirmation) {
              applicationService.blockApplication(app.application, loggedIn.userFullName.get).map {
                case ApplicationBlockSuccessResult => Ok(block_application_success(app))
                case ApplicationBlockFailureResult => technicalDifficulties
              }
            }
            else {
              val formWithErrors = blockApplicationForm.fill(form).withError(FormFields.applicationNameConfirmation, Messages("application.confirmation.error"))

              Future.successful(BadRequest(block_application(app, isAtLeastSuperUser, formWithErrors)))
            }
          }

          def handleFormError(form: Form[BlockApplicationForm]) = {
            Future.successful(BadRequest(block_application(app, isAtLeastSuperUser, form)))
          }

          blockApplicationForm.bindFromRequest.fold(handleFormError, handleValidForm)
        }
  }

  def unblockApplicationPage(appId: String) = requiresAtLeast(GatekeeperRole.ADMIN) {
    implicit request =>
      implicit hc =>
        withApp(appId) { app =>
          Future.successful(Ok(unblock_application(app, isAtLeastSuperUser, unblockApplicationForm.fill(UnblockApplicationForm("")))))
        }
  }

  def unblockApplicationAction(appId: String) = requiresAtLeast(GatekeeperRole.ADMIN) {
    implicit request =>
      implicit hc =>
        withApp(appId) { app =>
          def handleValidForm(form: UnblockApplicationForm) = {
            if (app.application.name == form.applicationNameConfirmation) {
              applicationService.unblockApplication(app.application, loggedIn.userFullName.get).map {
                case ApplicationUnblockSuccessResult => Ok(unblock_application_success(app))
                case ApplicationUnblockFailureResult => technicalDifficulties
              }
            }
            else {
              val formWithErrors = unblockApplicationForm.fill(form).withError(FormFields.applicationNameConfirmation, Messages("application.confirmation.error"))

              Future.successful(BadRequest(unblock_application(app, isAtLeastSuperUser, formWithErrors)))
            }
          }

          def handleFormError(form: Form[UnblockApplicationForm]) = {
            Future.successful(BadRequest(unblock_application(app, isAtLeastSuperUser, form)))
          }

          unblockApplicationForm.bindFromRequest.fold(handleFormError, handleValidForm)
        }
  }

  private def groupApisByStatus(apis: Seq[APIDefinition]): Map[String, Seq[VersionSummary]] = {

    val versions = for {
      api <- apis
      version <- api.versions
    } yield VersionSummary(api.name, version.status, APIIdentifier(api.context, version.version))

    versions.groupBy(v => APIStatus.displayedStatus(v.status))
  }

  private def withApp(appId: String)(f: ApplicationWithHistory => Future[Result])(implicit request: LoggedInRequest[_]) = {
    applicationService.fetchApplication(appId).flatMap(f)
  }

  private def withRestrictedApp(appId: String)(f: ApplicationWithHistory => Future[Result])(implicit request: LoggedInRequest[_]) = {
    withApp(appId) { app =>
      app.application.access match {
        case _: Standard => f(app)
        case _ if isAtLeastSuperUser => f(app)
        case _ => Future.successful(Forbidden(views.html.forbidden()))
      }
    }
  }

  def reviewPage(appId: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) { implicit request =>
    implicit hc =>
      withApp(appId) { app =>
        redirectIfIsSandboxApp(app) {
          fetchApplicationReviewDetails(appId) map (details => Ok(review(HandleUpliftForm.form, details)))
        }
      }
  }

  private def fetchApplicationReviewDetails(appId: String)(implicit hc: HeaderCarrier, request: LoggedInRequest[_]): Future[ApplicationReviewDetails] = {
    for {
      app <- applicationService.fetchApplication(appId)
      submission <- lastSubmission(app)
    } yield applicationReviewDetails(app.application, submission)
  }

  def approvedApplicationPage(appId: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) { implicit request =>
    implicit hc =>
      withApp(appId) { app =>

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

        redirectIfIsSandboxApp(app) {

          for {
            submission <- lastSubmission(app)
            admins <- administrators(app)
            approval = lastApproval(app)
            approvedApp: ApprovedApplication = application(app.application, approval, admins, submission)
          } yield Ok(approved(approvedApp))
        }
      }
  }

  private def lastSubmission(app: ApplicationWithHistory)(implicit hc: HeaderCarrier): Future[SubmissionDetails] = {
    val submission: StateHistory = app.history.filter(_.state == State.PENDING_GATEKEEPER_APPROVAL)
      .sortWith(StateHistory.ascendingDateForAppId)
      .lastOption.getOrElse(throw new InconsistentDataState("pending gatekeeper approval state history item not found"))

    developerService.fetchUser(submission.actor.id).map(s =>
      SubmissionDetails(s"${s.firstName} ${s.lastName}", s.email, submission.changedAt))
  }

  private def applicationReviewDetails(app: ApplicationResponse, submission: SubmissionDetails)(implicit request: LoggedInRequest[_]) = {

    val currentRateLimitTierToDisplay = if (isAtLeastSuperUser) Some(app.rateLimitTier) else None

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

  def handleUplift(appId: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      implicit hc =>
        withApp(appId) { app =>
          redirectIfIsSandboxApp(app) {
            val requestForm = HandleUpliftForm.form.bindFromRequest

            def errors(errors: Form[HandleUpliftForm]) =
              fetchApplicationReviewDetails(appId) map (details => BadRequest(review(errors, details)))

            def recovery: PartialFunction[Throwable, play.api.mvc.Result] = {
              case e: PreconditionFailed => {
                Logger.warn("Rejecting the uplift failed as the application might have already been rejected.", e)
                Redirect(routes.ApplicationController.applicationsPage(None))
              }
            }

            def addApplicationWithValidForm(validForm: HandleUpliftForm) = {
              UpliftAction.from(validForm.action) match {
                case Some(APPROVE) =>
                  applicationService.approveUplift(app.application, loggedIn.userFullName.get) map (
                    ApproveUpliftSuccessful => Redirect(routes.ApplicationController.applicationPage(appId))) recover recovery
                case Some(REJECT) =>
                  applicationService.rejectUplift(app.application, loggedIn.userFullName.get, validForm.reason.get) map (
                    RejectUpliftSuccessful => Redirect(routes.ApplicationController.applicationPage(appId))) recover recovery
              }
            }

            requestForm.fold(errors, addApplicationWithValidForm)
          }
        }
  }

  def handleUpdateRateLimitTier(appId: String): Action[AnyContent] =
    requiresAtLeast(GatekeeperRole.USER) { implicit request =>
      implicit hc =>
        withApp(appId) { app =>
          val result = Redirect(routes.ApplicationController.applicationPage(appId))
          if (isAtLeastSuperUser) {
            val newTier = RateLimitTier.withName(UpdateRateLimitForm.form.bindFromRequest().get.tier)
            applicationService.updateRateLimitTier(app.application, newTier) map {
              case ApplicationUpdateSuccessResult =>
                result.flashing("success" -> s"Rate limit tier has been changed to $newTier")
            }
          } else {
            Future.successful(result)
          }
        }
    }

  private def redirectIfIsSandboxApp(app: ApplicationWithHistory)(body: => Future[Result]) = {
    if (app.application.deployedTo == "SANDBOX") Future.successful(Redirect(routes.ApplicationController.applicationsPage(Some("SANDBOX")))) else body
  }

  def createPrivOrROPCApplicationPage(): Action[AnyContent] = {
    requiresAtLeast(GatekeeperRole.SUPERUSER) {
      implicit request =>
        implicit hc => {
          Future.successful(Ok(create_application(createPrivOrROPCAppForm.fill(CreatePrivOrROPCAppForm()))))
        }
    }
  }

  def createPrivOrROPCApplicationAction(): Action[AnyContent] = {
    requiresAtLeast(GatekeeperRole.SUPERUSER) {
      implicit request =>
        implicit hc => {

          def handleInvalidForm(form: Form[CreatePrivOrROPCAppForm]) = {
            Future.successful(BadRequest(create_application(form)))
          }

          def handleValidForm(form: CreatePrivOrROPCAppForm): Future[Result] = {

            def handleFormWithValidName: Future[Result] =
              form.accessType.flatMap(AccessType.from) match {
                case Some(accessType) => {
                  val collaborators = Seq(Collaborator(form.adminEmail, CollaboratorRole.ADMINISTRATOR))

                  applicationService.createPrivOrROPCApp(form.environment, form.applicationName, form.applicationDescription, collaborators, AppAccess(accessType, Seq())) flatMap {
                    case CreatePrivOrROPCAppSuccessResult(appId, appName, appEnv, clientId, Some(totp), access) =>
                      Future.successful(Ok(create_application_success(appId, appName, appEnv, Some(access.accessType), Some(totp), clientId)))
                    case CreatePrivOrROPCAppSuccessResult(appId, appName, appEnv, clientId, None, access) =>
                      applicationService.getClientSecret(appId, appEnv).map(secret => Ok(create_application_success(appId, appName, appEnv, Some(access.accessType), None, clientId, Some(secret))))
                  }
                }
              }

            def handleFormWithInvalidName: Future[Result] = {
              val formWithErrors = CreatePrivOrROPCAppForm.invalidAppName(createPrivOrROPCAppForm.fill(form))
              Future.successful(BadRequest(create_application(formWithErrors)))
            }

            def hasValidName(apps: Seq[ApplicationResponse]) = form.environment match {
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

  def manageTeamMembers(appId: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      implicit hc =>
        withRestrictedApp(appId) { app =>
          Future.successful(Ok(manage_team_members(app.application)))
        }
  }

  def addTeamMember(appId: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      implicit hc =>
        withRestrictedApp(appId) { app =>
          Future.successful(Ok(add_team_member(app.application, AddTeamMemberForm.form)))
        }
  }

  def addTeamMemberAction(appId: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      implicit hc =>
        withRestrictedApp(appId) { app =>
          def handleValidForm(form: AddTeamMemberForm) = {
            applicationService.addTeamMember(app.application, Collaborator(form.email, CollaboratorRole.from(form.role).getOrElse(CollaboratorRole.DEVELOPER)), loggedIn.userFullName.get)
              .map(_ => Redirect(controllers.routes.ApplicationController.manageTeamMembers(appId))) recover {
              case _: TeamMemberAlreadyExists => BadRequest(add_team_member(app.application, AddTeamMemberForm.form.fill(form).withError("email", Messages("team.member.error.email.already.exists"))))
            }
          }

          def handleInvalidForm(formWithErrors: Form[AddTeamMemberForm]) =
            Future.successful(BadRequest(add_team_member(app.application, formWithErrors)))

          AddTeamMemberForm.form.bindFromRequest.fold(handleInvalidForm, handleValidForm)
        }
  }

  def removeTeamMember(appId: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      implicit hc =>
        withRestrictedApp(appId) { app =>
          def handleValidForm(form: RemoveTeamMemberForm) =
            Future.successful(Ok(remove_team_member(app.application, RemoveTeamMemberConfirmationForm.form, form.email)))

          def handleInvalidForm(formWithErrors: Form[RemoveTeamMemberForm]) = {
            val email = formWithErrors("email").value.getOrElse("")
            Future.successful(BadRequest(remove_team_member(app.application, RemoveTeamMemberConfirmationForm.form.fillAndValidate(RemoveTeamMemberConfirmationForm(email)), email)))
          }

          RemoveTeamMemberForm.form.bindFromRequest.fold(handleInvalidForm, handleValidForm)
        }
  }

  def removeTeamMemberAction(appId: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      implicit hc =>
        withRestrictedApp(appId) { app =>
          def handleValidForm(form: RemoveTeamMemberConfirmationForm): Future[Result] = {
            form.confirm match {
              case Some("Yes") => applicationService.removeTeamMember(app.application, form.email, loggedIn.userFullName.get).map {
                _ => Redirect(routes.ApplicationController.manageTeamMembers(appId))
              } recover {
                case _: TeamMemberLastAdmin =>
                  BadRequest(remove_team_member(app.application, RemoveTeamMemberConfirmationForm.form.fill(form).withError("email", Messages("team.member.error.email.last.admin")), form.email))
              }
              case _ => Future.successful(Redirect(routes.ApplicationController.manageTeamMembers(appId)))
            }
          }

          def handleInvalidForm(formWithErrors: Form[RemoveTeamMemberConfirmationForm]) =
            Future.successful(BadRequest(remove_team_member(app.application, formWithErrors, formWithErrors("email").value.getOrElse(""))))

          RemoveTeamMemberConfirmationForm.form.bindFromRequest.fold(handleInvalidForm, handleValidForm)
        }
  }
}
