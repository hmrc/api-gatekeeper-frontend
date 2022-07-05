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

package uk.gov.hmrc.gatekeeper.controllers

import uk.gov.hmrc.gatekeeper.config.{AppConfig, ErrorHandler}
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.gatekeeper.models.Forms._
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields.Fields.Alias
import uk.gov.hmrc.gatekeeper.models.view.ApplicationViewModel
import uk.gov.hmrc.gatekeeper.models.UpliftAction.{APPROVE, REJECT}
import org.joda.time.DateTime
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.gatekeeper.services.{ApiDefinitionService, ApmService, ApplicationService, DeveloperService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.gatekeeper.utils.ErrorHelper
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}
import uk.gov.hmrc.gatekeeper.views.html.applications._
import uk.gov.hmrc.gatekeeper.views.html.approvedApplication.ApprovedView
import uk.gov.hmrc.gatekeeper.views.html.review.ReviewView
import uk.gov.hmrc.gatekeeper.controllers.actions.ActionBuilders
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.concurrent.Future.successful
import uk.gov.hmrc.gatekeeper.models.subscriptions.ApiData
import uk.gov.hmrc.gatekeeper.models.ApiStatus.ApiStatus
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.ApplicationLogger
import uk.gov.hmrc.gatekeeper.utils.CsvHelper._
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInRequest
import uk.gov.hmrc.apiplatform.modules.gkauth.services._
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.GatekeeperAuthorisationActions

@Singleton
class ApplicationController @Inject()(
  strideAuthorisationService: StrideAuthorisationService,
  val applicationService: ApplicationService,
  val forbiddenView: ForbiddenView,
  apiDefinitionService: ApiDefinitionService,
  developerService: DeveloperService,
  mcc: MessagesControllerComponents,
  applicationsView: ApplicationsView,
  applicationView: ApplicationView,
  manageAccessOverridesView: ManageAccessOverridesView,
  manageScopesView: ManageScopesView,
  ipAllowlistView: IpAllowlistView,
  manageIpAllowlistView: ManageIpAllowlistView,
  manageRateLimitView: ManageRateLimitView,
  deleteApplicationView: DeleteApplicationView,
  deleteApplicationSuccessView: DeleteApplicationSuccessView,
  override val errorTemplate: ErrorTemplate,
  blockApplicationView: BlockApplicationView,
  blockApplicationSuccessView: BlockApplicationSuccessView,
  unblockApplicationView: UnblockApplicationView,
  unblockApplicationSuccessView: UnblockApplicationSuccessView,
  reviewView: ReviewView,
  approvedView: ApprovedView,
  createApplicationView: CreateApplicationView,
  createApplicationSuccessView: CreateApplicationSuccessView,
  manageGrantLengthView: ManageGrantLengthView,
  manageGrantLengthSuccessView: ManageGrantLengthSuccessView,
  val apmService: ApmService,
  val errorHandler: ErrorHandler,
  val ldapAuthorisationService: LdapAuthorisationService

)(implicit val appConfig: AppConfig, override val ec: ExecutionContext)
  extends GatekeeperBaseController(strideAuthorisationService, mcc)
  with GatekeeperAuthorisationActions
    with ErrorHelper 
    with ActionBuilders 
    with ApplicationLogger {

  implicit val dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)

  def applicationsPage(environment: Option[String] = None): Action[AnyContent] = anyAuthenticatedUserAction { implicit request =>
    val env = Try(Environment.withName(environment.getOrElse("SANDBOX"))).toOption
    val defaults = Map("page" -> "1", "pageSize" -> "100", "sort" -> "NAME_ASC")
    val params = defaults ++ request.queryString.map { case (k, v) => k -> v.mkString }
    val buildAppUrlFn: (ApplicationId, String) => String = (appId, deployedTo) => 
      if(appConfig.gatekeeperApprovalsEnabled && deployedTo == "PRODUCTION")
        s"${appConfig.gatekeeperApprovalsBaseUrl}/api-gatekeeper-approvals/applications/${appId.value}"
      else
        routes.ApplicationController.applicationPage(appId).url

    for {
      paginatedApplicationResponse <- applicationService.searchApplications(env, params)
      apis <- apiDefinitionService.fetchAllApiDefinitions(env)
    } yield Ok(applicationsView(paginatedApplicationResponse, groupApisByStatus(apis), request.role.isSuperUser, params, buildAppUrlFn))
  }

  def applicationsPageCsv(environment: Option[String] = None): Action[AnyContent] = anyAuthenticatedUserAction { implicit request =>
    val env = Try(Environment.withName(environment.getOrElse("SANDBOX"))).toOption
    val defaults = Map("page" -> "1", "pageSize" -> "100", "sort" -> "NAME_ASC")
    val params = defaults ++ request.queryString.map { case (k, v) => k -> v.mkString }

    applicationService.searchApplications(env, params)
      .map(applicationResponse => Ok(toCsvContent(applicationResponse)))
  }

  private def toCsvContent(paginatedApplicationResponse: PaginatedApplicationResponse) : String = {    
    val csvColumnDefinitions = Seq[ColumnDefinition[ApplicationResponse]](
      ColumnDefinition("Name",                  (app => app.name)),
      ColumnDefinition("App ID",                (app => app.id.value)),
      ColumnDefinition("Client ID",             (app => app.clientId.value)),
      ColumnDefinition("Gateway ID",            (app => app.gatewayId)),
      ColumnDefinition("Environment",           (app => app.deployedTo)),
      ColumnDefinition("Status",                (app => State.displayedState(app.state.name))),
      ColumnDefinition("Rate limit tier",       (app => app.rateLimitTier.toString())),
      ColumnDefinition("Access type",           (app => app.access.accessType.toString())),
      ColumnDefinition("Blocked",               (app => app.blocked.toString())),
      ColumnDefinition("Has IP Allow List",     (app => app.ipAllowlist.allowlist.nonEmpty.toString())),
      ColumnDefinition("Submitted/Created on",  (app => app.createdOn.toString())),
      ColumnDefinition("Last API call",         (app => app.lastAccess.fold("")(_.toString)))
    )
    
    val pagingRow =  s"page: ${paginatedApplicationResponse.page} of ${paginatedApplicationResponse.maxPage} from ${paginatedApplicationResponse.matching} results" 
    
    toCsvString(csvColumnDefinitions, paginatedApplicationResponse.applications)
    
    val csvRows = toCsvString(csvColumnDefinitions, paginatedApplicationResponse.applications)
    
    Seq(pagingRow, csvRows).mkString(System.lineSeparator())
  }

  def applicationPage(appId: ApplicationId): Action[AnyContent] = anyStrideUserAction { implicit request =>
    withAppAndSubscriptionsAndStateHistory(appId) { applicationWithSubscriptionsAndStateHistory =>
      val app = applicationWithSubscriptionsAndStateHistory.applicationWithSubscriptionData.application
      val subscriptions: Set[ApiIdentifier] = applicationWithSubscriptionsAndStateHistory.applicationWithSubscriptionData.subscriptions
      val subscriptionFieldValues: Map[ApiContext, Map[ApiVersion, Alias]] = applicationWithSubscriptionsAndStateHistory.applicationWithSubscriptionData.subscriptionFieldValues
      val stateHistory = applicationWithSubscriptionsAndStateHistory.stateHistory
      val gatekeeperApprovalsUrl = s"${appConfig.gatekeeperApprovalsBaseUrl}/api-gatekeeper-approvals/applications/${appId.value}"

      def isSubscribed( t: (ApiContext, ApiData) ): Boolean = {
        subscriptions.exists(id => id.context == t._1)
      }

      def filterOutVersions( t: (ApiContext, ApiData) ): (ApiContext, ApiData) = {
        val apiContext = t._1
        val filteredVersions = t._2.versions.filter( versions => subscriptions.contains(ApiIdentifier(apiContext, versions._1)))
        val filteredApiData = t._2.copy(versions = filteredVersions)
        (apiContext, filteredApiData)
      }

      def filterForFields( t: (ApiContext, ApiData) ): (ApiContext, ApiData) = {
        def hasFields(apiContext: ApiContext, apiVersion: ApiVersion): Boolean = {
          subscriptionFieldValues.get(apiContext) match {
            case Some(versions) => versions.get(apiVersion).isDefined
            case None => false
          }
        }
        
        val apiContext = t._1
        val filteredVersions = t._2.versions.filter(v => hasFields(apiContext, v._1))
        val filteredApiData = t._2.copy(versions = filteredVersions)
        (apiContext, filteredApiData)
      }

      def asListOfList(data: ApiData): List[(String, List[(ApiVersion, ApiStatus)])] = {

        if(data.versions.isEmpty) {
          List.empty
        } else {
          List( (data.name, data.versions.toList.sortBy(v => v._1).map(v => (v._1, v._2.status))) )
        }
      }

      for {
        collaborators <- developerService.fetchDevelopersByEmails(app.collaborators.map(colab => colab.emailAddress))
        allPossibleSubs <- apmService.fetchAllPossibleSubscriptions(appId)
        subscribedContexts = allPossibleSubs.filter(isSubscribed)
        subscribedVersions = subscribedContexts.map(filterOutVersions)
        subscribedWithFields = subscribedVersions.map(filterForFields)
        doesApplicationHaveSubmissions <- applicationService.doesApplicationHaveSubmissions(appId)

        seqOfSubscriptions = subscribedVersions.values.toList.flatMap(asListOfList).sortWith(_._1 < _._1)
        subscriptionsThatHaveFieldDefns = subscribedWithFields.values.toList.flatMap(asListOfList).sortWith(_._1 < _._1)
      } yield Ok(applicationView(ApplicationViewModel(collaborators, app, seqOfSubscriptions, subscriptionsThatHaveFieldDefns, stateHistory, doesApplicationHaveSubmissions, gatekeeperApprovalsUrl)))
    }
  }

  def resendVerification(appId: ApplicationId): Action[AnyContent] = anyStrideUserAction { implicit request =>
    withApp(appId) { app =>
      for {
        _ <- applicationService.resendVerification(app.application, loggedIn.userFullName.get)
      } yield {
        Redirect(routes.ApplicationController.applicationPage(appId))
          .flashing("success" -> "Verification email has been sent")
      }
    }
  }

  def manageAccessOverrides(appId: ApplicationId): Action[AnyContent] = atLeastSuperUserAction { implicit request =>
    withStandardApp(appId) { (appWithHistory, access) =>
      Future.successful(Ok(manageAccessOverridesView(appWithHistory.application, accessOverridesForm.fill(access.overrides), request.role.isSuperUser)))
    }
  }

  def updateAccessOverrides(appId: ApplicationId) = atLeastSuperUserAction { implicit request =>
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

            overrideFlagErrors.foreach(err =>
              form = form.withError(
                formFieldForOverrideFlag(err),
                messagesApi.preferred(request)("invalid.scope")
              )
            )

            BadRequest(manageAccessOverridesView(app.application, form, request.role.isSuperUser))
          case UpdateOverridesSuccessResult => Redirect(routes.ApplicationController.applicationPage(appId))
        }
      }

      def handleFormError(form: Form[Set[OverrideFlag]]) = {
        Future.successful(BadRequest(manageAccessOverridesView(app.application, form, request.role.isSuperUser)))
      }

      accessOverridesForm.bindFromRequest.fold(handleFormError, handleValidForm)
    }
  }

  def manageScopes(appId: ApplicationId): Action[AnyContent] = atLeastSuperUserAction { implicit request =>
    withApp(appId) { app =>
      app.application.access match {
        case access: AccessWithRestrictedScopes => {
          val form = scopesForm.fill(access.scopes)
          Future.successful(Ok(manageScopesView(app.application, form)))
        }
        case _ => Future.failed(new RuntimeException("Invalid access type on application"))
      }
    }
  }

  def updateScopes(appId: ApplicationId) = atLeastSuperUserAction { implicit request =>
    withApp(appId) { app =>
      def handleValidForm(scopes: Set[String]) = {
        applicationService.updateScopes(app.application, scopes).map {
          case UpdateScopesInvalidScopesResult =>
            val form = scopesForm.fill(scopes).withError("scopes", messagesApi.preferred(request)("invalid.scope"))
            BadRequest(manageScopesView(app.application, form))

          case UpdateScopesSuccessResult => Redirect(routes.ApplicationController.applicationPage(appId))
        }
      }

      def handleFormError(form: Form[Set[String]]) = {
        Future.successful(BadRequest(manageScopesView(app.application, form)))
      }

      scopesForm.bindFromRequest.fold(handleFormError, handleValidForm)
    }
  }

  def viewIpAllowlistPage(appId: ApplicationId) = anyStrideUserAction { implicit request =>
    withApp(appId) { app =>
      Future.successful(Ok(ipAllowlistView(app.application)))
    }
  }

  def manageIpAllowlistPage(appId: ApplicationId) = atLeastSuperUserAction{ implicit request =>
    withApp(appId) { app =>
      Future.successful(Ok(manageIpAllowlistView(app.application,
        IpAllowlistForm.form.fill(IpAllowlistForm(app.application.ipAllowlist.required, app.application.ipAllowlist.allowlist)))))
    }
  }

  def manageIpAllowlistAction(appId: ApplicationId) = atLeastSuperUserAction { implicit request =>
    withApp(appId) { app =>
      def handleValidForm(form: IpAllowlistForm) = {
        if (form.required && form.allowlist.isEmpty) {
          val formWithErrors = IpAllowlistForm.form.fill(form).withError("allowlistedIps", messagesApi.preferred(request)("ipAllowlist.invalid.required"))
          Future.successful(BadRequest(manageIpAllowlistView(app.application, formWithErrors)))
        } else {
          applicationService.manageIpAllowlist(app.application, form.required, form.allowlist).map { _ =>
            Redirect(routes.ApplicationController.applicationPage(appId))
          }
        }
      }

      def handleFormError(form: Form[IpAllowlistForm]) = {
        Future.successful(BadRequest(manageIpAllowlistView(app.application, form)))
      }

      IpAllowlistForm.form.bindFromRequest.fold(handleFormError, handleValidForm)
    }
  }

  def manageRateLimitTier(appId: ApplicationId) = adminOnlyAction { implicit request =>
    withApp(appId) { app =>
      val form = UpdateRateLimitForm.form.fill(UpdateRateLimitForm(app.application.rateLimitTier.toString))
      Future.successful(Ok(manageRateLimitView(app.application, form)))
    }
  }

  def updateRateLimitTier(appId: ApplicationId) = adminOnlyAction { implicit request =>
    withApp(appId) { app =>
      def handleValidForm(form: UpdateRateLimitForm) = {
        applicationService.updateRateLimitTier(app.application, RateLimitTier.withName(form.tier)).map { _ =>
          Redirect(routes.ApplicationController.applicationPage(appId))
        }
      }

      def handleFormError(form: Form[UpdateRateLimitForm]) = {
        Future.successful(BadRequest(manageRateLimitView(app.application, form)))
      }

      UpdateRateLimitForm.form.bindFromRequest.fold(handleFormError, handleValidForm)
    }
  }

  def manageGrantLength(appId: ApplicationId) = adminOnlyAction { implicit request =>
    withApp(appId) { app =>
      val form = UpdateGrantLengthForm.form.fill(UpdateGrantLengthForm(Some(app.application.grantLength.getDays)))
      Future.successful(Ok(manageGrantLengthView(app.application, form)))
    }
  }

  def updateGrantLength(appId: ApplicationId) = adminOnlyAction { implicit request =>
    withApp(appId) { app =>
      def handleValidForm(form: UpdateGrantLengthForm) = {
        applicationService.updateGrantLength(app.application, GrantLength.from(form.grantLength.get)) map { _ =>
          Ok(manageGrantLengthSuccessView(app.application, GrantLength.displayedGrantLength(form.grantLength.get)))
        }
      }

      def handleFormError(form: Form[UpdateGrantLengthForm]) = {
        Future.successful(BadRequest(manageGrantLengthView(app.application, form)))
      }

      UpdateGrantLengthForm.form.bindFromRequest.fold(handleFormError, handleValidForm)
    }
  }

  def deleteApplicationPage(appId: ApplicationId) = atLeastSuperUserAction { implicit request =>
    withApp(appId) { app =>
      Future.successful(Ok(deleteApplicationView(app, request.role.isSuperUser, deleteApplicationForm.fill(DeleteApplicationForm("", Option(""))))))
    }
  }

  def deleteApplicationAction(appId: ApplicationId) = atLeastSuperUserAction { implicit request =>
    withApp(appId) { app =>
      def handleValidForm(form: DeleteApplicationForm) = {
        if (app.application.name == form.applicationNameConfirmation) {
          applicationService.deleteApplication(app.application, loggedIn.userFullName.get, form.collaboratorEmail.get).map {
            case ApplicationDeleteSuccessResult => Ok(deleteApplicationSuccessView(app))
            case ApplicationDeleteFailureResult => technicalDifficulties
          }
        }
        else {
          val formWithErrors = deleteApplicationForm.fill(form).withError(FormFields.applicationNameConfirmation, messagesApi.preferred(request)("application.confirmation.error"))

          Future.successful(BadRequest(deleteApplicationView(app, request.role.isSuperUser, formWithErrors)))
        }
      }

      def handleFormError(form: Form[DeleteApplicationForm]) = {
        Future.successful(BadRequest(deleteApplicationView(app, request.role.isSuperUser, form)))
      }

      deleteApplicationForm.bindFromRequest.fold(handleFormError, handleValidForm)
    }
  }

  def blockApplicationPage(appId: ApplicationId) = adminOnlyAction { implicit request =>
    withApp(appId) { app =>
      Future.successful(Ok(blockApplicationView(app, blockApplicationForm.fill(BlockApplicationForm("")))))
    }
  }

  def blockApplicationAction(appId: ApplicationId) = adminOnlyAction { implicit request =>
    withApp(appId) { app =>
      def handleValidForm(form: BlockApplicationForm) = {
        if (app.application.name == form.applicationNameConfirmation) {
          applicationService.blockApplication(app.application, loggedIn.userFullName.get).map {
            case ApplicationBlockSuccessResult => Ok(blockApplicationSuccessView(app))
            case ApplicationBlockFailureResult => technicalDifficulties
          }
        }
        else {
          messagesApi.preferred(request)("invalid.scope")
          val formWithErrors = blockApplicationForm.fill(form).withError(FormFields.applicationNameConfirmation, messagesApi.preferred(request)("application.confirmation.error"))

          Future.successful(BadRequest(blockApplicationView(app, formWithErrors)))
        }
      }

      def handleFormError(form: Form[BlockApplicationForm]) = {
        Future.successful(BadRequest(blockApplicationView(app, form)))
      }

      blockApplicationForm.bindFromRequest.fold(handleFormError, handleValidForm)
    }
  }

  def unblockApplicationPage(appId: ApplicationId) = adminOnlyAction { implicit request =>
    withApp(appId) { app =>
      Future.successful(Ok(unblockApplicationView(app, unblockApplicationForm.fill(UnblockApplicationForm("")))))
    }
  }

  def unblockApplicationAction(appId: ApplicationId) = adminOnlyAction { implicit request =>
    withApp(appId) { app =>
      def handleValidForm(form: UnblockApplicationForm) = {
        if (app.application.name == form.applicationNameConfirmation) {
          applicationService.unblockApplication(app.application, loggedIn.userFullName.get).map {
            case ApplicationUnblockSuccessResult => Ok(unblockApplicationSuccessView(app))
            case ApplicationUnblockFailureResult => technicalDifficulties
          }
        }
        else {
          val formWithErrors = unblockApplicationForm.fill(form).withError(FormFields.applicationNameConfirmation, messagesApi.preferred(request)("application.confirmation.error"))

          Future.successful(BadRequest(unblockApplicationView(app, formWithErrors)))
        }
      }

      def handleFormError(form: Form[UnblockApplicationForm]) = {
        Future.successful(BadRequest(unblockApplicationView(app, form)))
      }

      unblockApplicationForm.bindFromRequest.fold(handleFormError, handleValidForm)
    }
  }

  private def groupApisByStatus(apis: List[ApiDefinition]): Map[String, List[VersionSummary]] = {
    val versions = for {
      api <- apis
      version <- api.versions
    } yield VersionSummary(api.name, version.status, ApiIdentifier(api.context, version.version))

    versions.groupBy(v => ApiStatus.displayedStatus(v.status))
  }

  def reviewPage(appId: ApplicationId): Action[AnyContent] = anyStrideUserAction { implicit request =>
    withApp(appId) { app =>
      redirectIfIsSandboxApp(app) {
        fetchApplicationReviewDetails(appId) map (details => Ok(reviewView(HandleUpliftForm.form, details)))
      }
    }
  }

  private def fetchApplicationReviewDetails(appId: ApplicationId)(implicit hc: HeaderCarrier, request: LoggedInRequest[_]): Future[ApplicationReviewDetails] = {
    for {
      app <- applicationService.fetchApplication(appId)
      submission <- lastSubmission(app)
    } yield applicationReviewDetails(app.application, submission)
  }

  def approvedApplicationPage(appId: ApplicationId): Action[AnyContent] = anyStrideUserAction { implicit request =>
    withApp(appId) { app =>
      def lastApproval(app: ApplicationWithHistory): StateHistory = {
        app.history.filter(_.state.isPendingRequesterVerification)
          .sortWith(StateHistory.ascendingDateForAppId)
          .lastOption.getOrElse(throw new InconsistentDataState("pending requester verification state history item not found"))
      }

      def administrators(app: ApplicationWithHistory): Future[List[RegisteredUser]] = {
        val emails: Set[String] = app.application.admins.map(_.emailAddress)
        developerService.fetchDevelopersByEmails(emails)
      }

      def application(app: ApplicationResponse, approved: StateHistory, admins: List[RegisteredUser], submissionDetails: SubmissionDetails) = {
        val verified = app.isApproved
        val details = applicationReviewDetails(app, submissionDetails)(request)

        ApprovedApplication(details, admins, approved.actor.id, approved.changedAt, verified)
      }

      redirectIfIsSandboxApp(app) {

        for {
          submission <- lastSubmission(app)
          admins <- administrators(app)
          approval = lastApproval(app)
          approvedApp: ApprovedApplication = application(app.application, approval, admins, submission)
        } yield Ok(approvedView(approvedApp))
      }
    }
  }

  private def lastSubmission(app: ApplicationWithHistory)(implicit hc: HeaderCarrier): Future[SubmissionDetails] = {
    val submission: StateHistory = app.history.filter(_.state.isPendingGatekeeperApproval)
      .sortWith(StateHistory.ascendingDateForAppId)
      .lastOption.getOrElse(throw new InconsistentDataState("pending gatekeeper approval state history item not found"))

    developerService.fetchUser(submission.actor.id).map(s =>
      SubmissionDetails(s"${s.firstName} ${s.lastName}", s.email, submission.changedAt))
  }

  private def applicationReviewDetails(app: ApplicationResponse, submission: SubmissionDetails)(implicit request: LoggedInRequest[_]) = {
    val currentRateLimitTierToDisplay = if (request.role.isSuperUser) Some(app.rateLimitTier) else None

    val contactDetails = for {
      checkInformation <- app.checkInformation
      contactDetails <- checkInformation.contactDetails
    } yield contactDetails

    val reviewContactName = contactDetails.map(_.fullname)
    val reviewContactEmail = contactDetails.map(_.email)
    val reviewContactTelephone = contactDetails.map(_.telephoneNumber)
    val applicationDetails = app.checkInformation.flatMap(_.applicationDetails)

    ApplicationReviewDetails(
      app.id,
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

  def handleUplift(appId: ApplicationId): Action[AnyContent] = anyStrideUserAction { implicit request =>
    withApp(appId) { app =>
      redirectIfIsSandboxApp(app) {
        val requestForm = HandleUpliftForm.form.bindFromRequest

        def errors(errors: Form[HandleUpliftForm]) =
          fetchApplicationReviewDetails(appId) map (details => BadRequest(reviewView(errors, details)))

        def recovery: PartialFunction[Throwable, play.api.mvc.Result] = {
          case PreconditionFailedException => {
            logger.warn("Rejecting the uplift failed as the application might have already been rejected.", PreconditionFailedException)
            Redirect(routes.ApplicationController.applicationsPage(None))
          }
        }

        def addApplicationWithValidForm(validForm: HandleUpliftForm) = {
          UpliftAction.from(validForm.action) match {
            case Some(APPROVE) =>
              applicationService.approveUplift(app.application, loggedIn.userFullName.get) map (
                _ => Redirect(routes.ApplicationController.applicationPage(appId))) recover recovery
            case Some(REJECT) =>
              applicationService.rejectUplift(app.application, loggedIn.userFullName.get, validForm.reason.get) map (
                _ => Redirect(routes.ApplicationController.applicationPage(appId))) recover recovery

          }
        }
        requestForm.fold(errors, addApplicationWithValidForm)
      }
    }
  }

  def handleUpdateRateLimitTier(appId: ApplicationId): Action[AnyContent] = anyStrideUserAction { implicit request =>
      withApp(appId) { app =>
        val result = Redirect(routes.ApplicationController.applicationPage(appId))
        if (request.role.isSuperUser) {
          val newTier = RateLimitTier.withName(UpdateRateLimitForm.form.bindFromRequest().get.tier)
          applicationService.updateRateLimitTier(app.application, newTier) map {
            case ApplicationUpdateSuccessResult =>
              result.flashing("success" -> s"Rate limit tier has been changed to $newTier")
            case _ =>
              result.flashing("failed" -> "Rate limit tier was not changed successfully") // Don't expect this as an error is thrown
          }
        } else {
          Future.successful(result)
        }
      }
    }

  private def redirectIfIsSandboxApp(app: ApplicationWithHistory)(body: => Future[Result]) = {
    if (app.application.deployedTo == "SANDBOX") Future.successful(Redirect(routes.ApplicationController.applicationsPage(Some("SANDBOX")))) else body
  }

  def createPrivOrROPCApplicationPage(): Action[AnyContent] = atLeastSuperUserAction { implicit request =>
    Future.successful(Ok(createApplicationView(createPrivOrROPCAppForm.fill(CreatePrivOrROPCAppForm()))))
  }

  def createPrivOrROPCApplicationAction(): Action[AnyContent] = atLeastSuperUserAction { implicit request =>
    def handleInvalidForm(form: Form[CreatePrivOrROPCAppForm]) = {
      Future.successful(BadRequest(createApplicationView(form)))
    }

    import cats.data._
    import cats.implicits._
    import cats.data.Validated._
        
    type FieldName = String
    type ErrorCode = String
    type ValidationResult[A] = ValidatedNec[(FieldName,ErrorCode), A]
    type FieldValidationResult[A] = ValidatedNec[ErrorCode, A]

    implicit class WithFieldSyntax[T](v: FieldValidationResult[T]) {
      def withField(fn: FieldName): ValidationResult[T] = v.leftMap(_.map(err => ((fn, err))))
    }

    def validateApplicationName(environment: Environment.Environment, applicationName: String, apps: Seq[ApplicationResponse]): FieldValidationResult[String] = {
      val isValid = environment match {
        case Environment.PRODUCTION => !apps.exists(app => (app.deployedTo == Environment.PRODUCTION.toString) && (app.name == applicationName))
        case _                      => true
      }
      if(isValid) applicationName.valid else "application.name.already.exists".invalidNec
    }

    def validateUserSuitability(user: Option[User]): FieldValidationResult[RegisteredUser] = user match {
      case None                                            => "admin.email.is.not.registered".invalidNec
      case Some(UnregisteredUser(_,_))                     => "admin.email.is.not.registered".invalidNec
      case Some(user: RegisteredUser) if(!user.verified)   => "admin.email.is.not.verified".invalidNec
      case Some(user: RegisteredUser) if(!user.mfaEnabled) => "admin.email.is.not.mfa.enabled".invalidNec
      case Some(user: RegisteredUser)                      => user.validNec
    }

    def handleValidForm(form: CreatePrivOrROPCAppForm): Future[Result] = {
      def createApp(user: User, accessType: AccessType.AccessType) = {
        val collaborators = List(Collaborator(form.adminEmail, CollaboratorRole.ADMINISTRATOR, user.userId))

        applicationService.createPrivOrROPCApp(form.environment, form.applicationName, form.applicationDescription, collaborators, AppAccess(accessType, List.empty))
        .map {
          case CreatePrivOrROPCAppFailureResult => InternalServerError("Unexpected problems creating application")
          case CreatePrivOrROPCAppSuccessResult(appId, appName, appEnv, clientId, totp, access) => Ok(createApplicationSuccessView(appId, appName, appEnv, Some(access.accessType), totp, clientId))
        }
      }

      def validateValues(apps: Seq[ApplicationResponse], user: Option[User]): ValidationResult[(String, RegisteredUser)] = 
        (
          validateApplicationName(form.environment, form.applicationName, apps).withField("applicationName"),
          validateUserSuitability(user).withField("adminEmail")
        )
        .mapN( (n,u) => ((n,u)))
        
      def handleValues(apps: Seq[ApplicationResponse], user: Option[User], accessType: AccessType.AccessType): Future[Result] = 
        validateValues(apps, user)
        .fold[Future[Result]](
          errs => successful(viewWithFormErrors(errs)),
          goodData => createApp(goodData._2, accessType)
        )

      def formWithErrors(errs: NonEmptyChain[(FieldName, ErrorCode)]): Form[CreatePrivOrROPCAppForm] = 
        errs.foldLeft(createPrivOrROPCAppForm.fill(form))( (f,e) => f.withError(e._1, e._2))

      def viewWithFormErrors(errs: NonEmptyChain[(FieldName, ErrorCode)]): Result = 
        BadRequest(createApplicationView(formWithErrors(errs)))

      val accessType = form.accessType.flatMap(AccessType.from).getOrElse(throw new RuntimeException(s"Access Type ${form.accessType} not recognized when attempting to create Priv or ROPC app"))

      val fApps = applicationService.fetchApplications
      val fUser = developerService.seekUser(form.adminEmail)
      
      for {
        apps <- fApps
        user <- fUser
        result <- handleValues(apps, user, accessType)
      } yield result
    }

    createPrivOrROPCAppForm.bindFromRequest.fold(handleInvalidForm, handleValidForm)
  }
}