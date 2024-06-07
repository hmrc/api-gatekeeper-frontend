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

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneOffset}
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.StateHelper._
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.submissions.domain.models.{ImportantSubmissionData, TermsOfUseAcceptance}
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.apiplatform.modules.events.connectors.{DisplayEvent, EnvironmentAwareApiPlatformEventsConnector}
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.GatekeeperAuthorisationActions
import uk.gov.hmrc.apiplatform.modules.gkauth.services._
import uk.gov.hmrc.gatekeeper.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.gatekeeper.controllers.actions.ActionBuilders
import uk.gov.hmrc.gatekeeper.models.Forms._
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields.Fields.Alias
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.view.{ApplicationViewModel, ResponsibleIndividualHistoryItem}
import uk.gov.hmrc.gatekeeper.services.{ApiDefinitionService, ApmService, ApplicationService, DeveloperService, TermsOfUseService}
import uk.gov.hmrc.gatekeeper.utils.CsvHelper._
import uk.gov.hmrc.gatekeeper.utils.{ErrorHelper, MfaDetailHelper}
import uk.gov.hmrc.gatekeeper.views.html.applications._
import uk.gov.hmrc.gatekeeper.views.html.approvedApplication.ApprovedView
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

@Singleton
class ApplicationController @Inject() (
    strideAuthorisationService: StrideAuthorisationService,
    val applicationService: ApplicationService,
    val forbiddenView: ForbiddenView,
    apiDefinitionService: ApiDefinitionService,
    developerService: DeveloperService,
    termsOfUseService: TermsOfUseService,
    mcc: MessagesControllerComponents,
    applicationsView: ApplicationsView,
    applicationView: ApplicationView,
    manageAccessOverridesView: ManageAccessOverridesView,
    manageScopesView: ManageScopesView,
    ipAllowlistView: IpAllowlistView,
    manageRedirectUriView: ManageRedirectUriView,
    manageIpAllowlistView: ManageIpAllowlistView,
    manageRateLimitView: ManageRateLimitView,
    deleteApplicationView: DeleteApplicationView,
    deleteApplicationSuccessView: DeleteApplicationSuccessView,
    override val errorTemplate: ErrorTemplate,
    blockApplicationView: BlockApplicationView,
    blockApplicationSuccessView: BlockApplicationSuccessView,
    unblockApplicationView: UnblockApplicationView,
    unblockApplicationSuccessView: UnblockApplicationSuccessView,
    approvedView: ApprovedView,
    createApplicationView: CreateApplicationView,
    createApplicationSuccessView: CreateApplicationSuccessView,
    manageGrantLengthView: ManageGrantLengthView,
    manageGrantLengthSuccessView: ManageGrantLengthSuccessView,
    manageAutoDeleteEnabledView: ManageAutoDeleteEnabledView,
    manageAutoDeleteDisabledView: ManageAutoDeleteDisabledView,
    autoDeleteSuccessView: AutoDeleteSuccessView,
    eventsConnector: EnvironmentAwareApiPlatformEventsConnector,
    val apmService: ApmService,
    val errorHandler: ErrorHandler,
    val ldapAuthorisationService: LdapAuthorisationService
  )(implicit val appConfig: AppConfig,
    override val ec: ExecutionContext
  ) extends GatekeeperBaseController(strideAuthorisationService, mcc)
    with GatekeeperAuthorisationActions
    with ErrorHelper
    with ActionBuilders
    with ApplicationLogger {

  implicit val dateTimeOrdering: Ordering[LocalDateTime] = Ordering.fromLessThan(_ isBefore _)

  def applicationsPage(environment: Option[String] = None): Action[AnyContent] = anyAuthenticatedUserAction { implicit request =>
    val env                                                   = Environment.apply(environment.getOrElse("SANDBOX"))
    val defaults                                              = Map("page" -> "1", "pageSize" -> "100", "sort" -> "NAME_ASC", "includeDeleted" -> "false")
    val params                                                = defaults ++ request.queryString.map { case (k, v) => k -> v.mkString }
    val buildAppUrlFn: (ApplicationId, Environment) => String = (appId, deployedTo) =>
      if (appConfig.gatekeeperApprovalsEnabled && deployedTo == Environment.PRODUCTION) {
        s"${appConfig.gatekeeperApprovalsBaseUrl}/api-gatekeeper-approvals/applications/$appId"
      } else {
        routes.ApplicationController.applicationPage(appId).url
      }

    for {
      paginatedApplicationResponse <- applicationService.searchApplications(env, params)
      apis                         <- apmService.fetchNonOpenApis(env.get)
    } yield Ok(applicationsView(paginatedApplicationResponse, groupApisByStatus(apis), request.role.isSuperUser, params, buildAppUrlFn))
  }

  def applicationsPageCsv(environment: Option[String] = None): Action[AnyContent] = anyAuthenticatedUserAction { implicit request =>
    val env      = Environment.apply(environment.getOrElse("SANDBOX"))
    val defaults = Map("page" -> "1", "pageSize" -> "100", "sort" -> "NAME_ASC")
    val params   = defaults ++ request.queryString.map { case (k, v) => k -> v.mkString }

    applicationService.searchApplications(env, params)
      .map(applicationResponse => Ok(toCsvContent(applicationResponse)))
  }

  private def toCsvContent(paginatedApplicationResponse: PaginatedApplicationResponse): String = {
    val csvColumnDefinitions = Seq[ColumnDefinition[GKApplicationResponse]](
      ColumnDefinition("Name", (app => app.name)),
      ColumnDefinition("App ID", (app => app.id.toString())),
      ColumnDefinition("Client ID", (app => app.clientId.value)),
      ColumnDefinition("Gateway ID", (app => app.gatewayId)),
      ColumnDefinition("Environment", (app => app.deployedTo.toString)),
      ColumnDefinition("Status", (app => app.state.name.displayText)),
      ColumnDefinition("Rate limit tier", (app => app.rateLimitTier.toString())),
      ColumnDefinition("Access type", (app => app.access.accessType.toString())),
      ColumnDefinition("Blocked", (app => app.blocked.toString())),
      ColumnDefinition("Has IP Allow List", (app => app.ipAllowlist.allowlist.nonEmpty.toString())),
      ColumnDefinition("Submitted/Created on", (app => app.createdOn.toString())),
      ColumnDefinition("Last API call", (app => app.lastAccess.fold("")(_.toString))),
      ColumnDefinition("Auto delete", (app => app.moreApplication.allowAutoDelete.toString()))
    )

    val pagingRow = s"page: ${paginatedApplicationResponse.page} of ${paginatedApplicationResponse.maxPage} from ${paginatedApplicationResponse.matching} results"

    toCsvString(csvColumnDefinitions, paginatedApplicationResponse.applications)

    val csvRows = toCsvString(csvColumnDefinitions, paginatedApplicationResponse.applications)

    Seq(pagingRow, csvRows).mkString(System.lineSeparator())
  }

  private def toCsvContent(response: List[ApplicationWithSubscriptionsResponse], env: Option[Environment]): String = {

    val identifiers: Seq[ApiIdentifier]                                         = response.map(_.apiIdentifiers).reduceOption((a, b) => a ++ b).getOrElse(Set()).toSeq.sorted
    val apiColumns: Seq[ColumnDefinition[ApplicationWithSubscriptionsResponse]] =
      identifiers.map(apiIdentity =>
        ColumnDefinition(
          apiIdentity.asText("."),
          (app: ApplicationWithSubscriptionsResponse) => app.apiIdentifiers.contains(apiIdentity).toString
        )
      )

    val csvColumnDefinitions = Seq[ColumnDefinition[ApplicationWithSubscriptionsResponse]](
      ColumnDefinition("Name", app => app.name),
      ColumnDefinition("App ID", app => app.id.value.toString),
      ColumnDefinition("Environment", _ => env.getOrElse(Environment.SANDBOX).toString),
      ColumnDefinition("Last API call", app => app.lastAccess.fold("")(_.toString))
    ) ++ apiColumns

    toCsvString(csvColumnDefinitions, response)
  }

  def applicationWithSubscriptionsCsv(environment: Option[String] = None): Action[AnyContent] = anyAuthenticatedUserAction { implicit request =>
    val env: Option[Environment] = Environment.apply(environment.getOrElse("SANDBOX"))

    applicationService.fetchApplicationsWithSubscriptions(env)
      .map(applicationResponse => Ok(toCsvContent(applicationResponse, env)))
  }

  def applicationPage(appId: ApplicationId): Action[AnyContent] = anyAuthenticatedUserAction { implicit request =>
    withAppAndSubscriptionsAndStateHistory(appId) { applicationWithSubscriptionsAndStateHistory =>
      val app                                                                 = applicationWithSubscriptionsAndStateHistory.applicationWithSubscriptionData.application
      val subscriptions: Set[ApiIdentifier]                                   = applicationWithSubscriptionsAndStateHistory.applicationWithSubscriptionData.subscriptions
      val subscriptionFieldValues: Map[ApiContext, Map[ApiVersionNbr, Alias]] = applicationWithSubscriptionsAndStateHistory.applicationWithSubscriptionData.subscriptionFieldValues
      val stateHistory                                                        = applicationWithSubscriptionsAndStateHistory.stateHistory
      val gatekeeperApprovalsUrl                                              = s"${appConfig.gatekeeperApprovalsBaseUrl}/api-gatekeeper-approvals/applications/${appId}"
      val termsOfUseInvitationUrl                                             = s"${appConfig.gatekeeperApprovalsBaseUrl}/api-gatekeeper-approvals/applications/${appId}/send-new-terms-of-use"

      def isSubscribed(defn: ApiDefinition): Boolean = {
        subscriptions.exists(id => id.context == defn.context)
      }

      def filterOutVersions(defn: ApiDefinition): ApiDefinition = {
        val apiContext       = defn.context
        val filteredVersions = defn.versions.filter(versions => subscriptions.contains(ApiIdentifier(apiContext, versions._1)))

        defn.copy(versions = filteredVersions)
      }

      def filterForFields(defn: ApiDefinition): ApiDefinition = {
        def hasFields(apiContext: ApiContext, apiVersion: ApiVersionNbr): Boolean = {
          subscriptionFieldValues.get(apiContext) match {
            case Some(versions) => versions.get(apiVersion).isDefined
            case None           => false
          }
        }

        val apiContext       = defn.context
        val filteredVersions = defn.versions.filter(v => hasFields(apiContext, v._1))

        defn.copy(versions = filteredVersions)
      }

      def asListOfList(data: ApiDefinition): List[(String, List[(ApiVersionNbr, ApiStatus)])] = {

        if (data.versions.isEmpty) {
          List.empty
        } else {
          List((data.name, data.versions.toList.sortBy(v => v._1).map(v => (v._1, v._2.status))))
        }
      }

      def getResponsibleIndividualHistory(access: Access): List[ResponsibleIndividualHistoryItem] = {
        access match {
          case Access.Standard(_, _, _, _, _, Some(ImportantSubmissionData(_, _, _, _, _, termsOfUseAcceptances))) => {
            buildResponsibleIndividualHistoryItems(termsOfUseAcceptances).reverse
          }
          case _                                                                                                   => List.empty
        }
      }

      def checkEligibleForTermsOfUseInvite(app: GKApplicationResponse, hasSubmissions: Boolean, hasTermsOfUseInvite: Boolean): Boolean = {
        app.access match {
          case std: Access.Standard
              if (app.state.name == State.PRODUCTION &&
                app.deployedTo == Environment.PRODUCTION &&
                !hasSubmissions &&
                !hasTermsOfUseInvite) => true
          case _ => false
        }
      }

      for {
        collaborators                       <- developerService.fetchDevelopersByEmails(app.collaborators.map(colab => colab.emailAddress))
        allPossibleSubs                     <- apmService.fetchAllPossibleSubscriptions(appId)
        subscribedContexts                   = allPossibleSubs.filter(isSubscribed)
        subscribedVersions                   = subscribedContexts.map(filterOutVersions)
        subscribedWithFields                 = subscribedVersions.map(filterForFields)
        doesApplicationHaveSubmissions      <- applicationService.doesApplicationHaveSubmissions(appId)
        doesApplicationHaveTermsOfUseInvite <- applicationService.doesApplicationHaveTermsOfUseInvitation(appId)

        seqOfSubscriptions              = subscribedVersions.flatMap(asListOfList).sortWith(_._1 < _._1)   // TODO
        subscriptionsThatHaveFieldDefns = subscribedWithFields.flatMap(asListOfList).sortWith(_._1 < _._1) // TODO
        responsibleIndividualHistory    = getResponsibleIndividualHistory(app.access)
        maybeTermsOfUseAcceptance       = termsOfUseService.getAgreementDetails(app)
        isEligibleForTermsOfUseInvite   = checkEligibleForTermsOfUseInvite(app, doesApplicationHaveSubmissions, doesApplicationHaveTermsOfUseInvite)
      } yield Ok(applicationView(ApplicationViewModel(
        collaborators,
        app,
        seqOfSubscriptions,
        subscriptionsThatHaveFieldDefns,
        stateHistory,
        doesApplicationHaveSubmissions,
        gatekeeperApprovalsUrl,
        responsibleIndividualHistory,
        maybeTermsOfUseAcceptance,
        isEligibleForTermsOfUseInvite,
        termsOfUseInvitationUrl
      )))
    }
  }

  private def buildResponsibleIndividualHistoryItems(termsOfUseAcceptances: List[TermsOfUseAcceptance]): List[ResponsibleIndividualHistoryItem] = {
    def formatInstant(instant: Instant) = instant.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
    termsOfUseAcceptances match {
      case Nil                       => List.empty
      case first :: Nil              =>
        List(ResponsibleIndividualHistoryItem(first.responsibleIndividual.fullName.value, first.responsibleIndividual.emailAddress.text, formatInstant(first.dateTime), "Present"))
      case first :: second :: others => List(ResponsibleIndividualHistoryItem(
          first.responsibleIndividual.fullName.value,
          first.responsibleIndividual.emailAddress.text,
          formatInstant(first.dateTime),
          formatInstant(second.dateTime)
        )) ++
          buildResponsibleIndividualHistoryItems(second :: others)
    }
  }

  def resendVerification(appId: ApplicationId): Action[AnyContent] = anyStrideUserAction { implicit request =>
    withApp(appId) { app =>
      val result = Redirect(routes.ApplicationController.applicationPage(appId))
      applicationService.resendVerification(app.application, loggedIn.userFullName.get) map {
        case ApplicationUpdateSuccessResult =>
          result.flashing("success" -> "Verification email has been sent")
        case _                              =>
          result.flashing("failed" -> "Verification email has not been sent")
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
        case OverrideFlag.SuppressIvForAgents(_)        => FormFields.suppressIvForAgentsScopes
        case OverrideFlag.SuppressIvForOrganisations(_) => FormFields.suppressIvForOrganisationsScopes
        case OverrideFlag.SuppressIvForIndividuals(_)   => FormFields.suppressIvForIndividualsScopes
        case OverrideFlag.GrantWithoutConsent(_)        => FormFields.grantWithoutConsentScopes
        case OverrideFlag.OriginOverride(_)             => FormFields.originOverrideValue
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
          case UpdateOverridesSuccessResult                     => Redirect(routes.ApplicationController.applicationPage(appId))
        }
      }

      def handleFormError(form: Form[Set[OverrideFlag]]) = {
        Future.successful(BadRequest(manageAccessOverridesView(app.application, form, request.role.isSuperUser)))
      }

      accessOverridesForm.bindFromRequest().fold(handleFormError, handleValidForm)
    }
  }

  def manageScopes(appId: ApplicationId): Action[AnyContent] = atLeastSuperUserAction { implicit request =>
    withApp(appId) { app =>
      def showManageScopesView(scopes: Set[String]) =
        Future.successful(Ok(manageScopesView(app.application, scopesForm.fill(scopes))))

      app.application.access match {
        case Access.Privileged(_, scopes) => showManageScopesView(scopes)
        case Access.Ropc(scopes)          => showManageScopesView(scopes)
        case _                            => Future.failed(new RuntimeException("Invalid access type on application"))
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

      scopesForm.bindFromRequest().fold(handleFormError, handleValidForm)
    }
  }

  def viewIpAllowlistPage(appId: ApplicationId) = anyStrideUserAction { implicit request =>
    withApp(appId) { app =>
      Future.successful(Ok(ipAllowlistView(app.application)))
    }
  }

  def manageIpAllowlistPage(appId: ApplicationId) = atLeastSuperUserAction { implicit request =>
    withApp(appId) { app =>
      Future.successful(Ok(manageIpAllowlistView(
        app.application,
        IpAllowlistForm.form.fill(IpAllowlistForm(app.application.ipAllowlist.required, app.application.ipAllowlist.allowlist))
      )))
    }
  }

  def manageIpAllowlistAction(appId: ApplicationId) = atLeastSuperUserAction { implicit request =>
    withApp(appId) { app =>
      def handleValidForm(form: IpAllowlistForm) = {
        if (form.required && form.allowlist.isEmpty) {
          val formWithErrors = IpAllowlistForm.form.fill(form).withError("allowlistedIps", messagesApi.preferred(request)("ipAllowlist.invalid.required"))
          Future.successful(BadRequest(manageIpAllowlistView(app.application, formWithErrors)))
        } else {
          applicationService.manageIpAllowlist(app.application, form.required, form.allowlist, loggedIn.userFullName.get).map { _ =>
            Redirect(routes.ApplicationController.applicationPage(appId))
          }
        }
      }

      def handleFormError(form: Form[IpAllowlistForm]) = {
        Future.successful(BadRequest(manageIpAllowlistView(app.application, form)))
      }

      IpAllowlistForm.form.bindFromRequest().fold(handleFormError, handleValidForm)
    }
  }

  def manageRedirectUriPage(appId: ApplicationId) = atLeastSuperUserAction { implicit request =>
    withApp(appId) { app =>
      app.application.access match {
        case Access.Standard(redirects, _, _, _, _, _) => Future.successful(Ok(manageRedirectUriView(app.application, RedirectUriForm.form.fill(RedirectUriForm(redirects)))))
        case _                                         => Future.successful(NotFound)
      }

    }
  }

  def manageRedirectUriAction(appId: ApplicationId) = atLeastSuperUserAction { implicit request =>
    withApp(appId) { app =>
      def handleValidForm(form: RedirectUriForm) = {
        applicationService.manageRedirectUris(app.application, form.redirectUris, loggedIn.userFullName.get).map { _ =>
          Redirect(routes.ApplicationController.applicationPage(appId))
        }
      }

      def handleFormError(form: Form[RedirectUriForm]) = {
        Future.successful(BadRequest(manageRedirectUriView(app.application, form)))
      }

      RedirectUriForm.form.bindFromRequest().fold(handleFormError, handleValidForm)
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
        RateLimitTier.apply(form.tier) match {
          case Some(tier) => applicationService.updateRateLimitTier(app.application, tier, loggedIn.userFullName.get).map { _ =>
              Redirect(routes.ApplicationController.applicationPage(appId))
            }
          case None       => Future.successful(BadRequest(manageRateLimitView(app.application, UpdateRateLimitForm.form.fill(form))))
        }
      }

      def handleFormError(form: Form[UpdateRateLimitForm]) = {
        Future.successful(BadRequest(manageRateLimitView(app.application, form)))
      }

      UpdateRateLimitForm.form.bindFromRequest().fold(handleFormError, handleValidForm)
    }
  }

  private def getGrantLengths(): List[GrantLength] = {
    if (appConfig.showNoRefreshTokenGrantLength) {
      GrantLength.values.toList
    } else {
      GrantLength.values.filterNot((g: GrantLength) => g == GrantLength.FOUR_HOURS).toList
    }
  }

  def manageGrantLength(appId: ApplicationId) = adminOnlyAction { implicit request =>
    withApp(appId) { app =>
      val form = UpdateGrantLengthForm.form.fill(UpdateGrantLengthForm(Some(app.application.grantLength.period.getDays())))
      Future.successful(Ok(manageGrantLengthView(app.application, form, getGrantLengths())))
    }
  }

  def updateGrantLength(appId: ApplicationId) = adminOnlyAction { implicit request =>
    withApp(appId) { app =>
      def handleValidForm(form: UpdateGrantLengthForm) = {
        val grantLength = GrantLength.apply(form.grantLength.get).get
        applicationService.updateGrantLength(app.application, grantLength, loggedIn.userFullName.get) map { _ =>
          Ok(manageGrantLengthSuccessView(app.application, grantLength))
        }
      }

      def handleFormError(form: Form[UpdateGrantLengthForm]) = {
        Future.successful(BadRequest(manageGrantLengthView(app.application, form, getGrantLengths())))
      }

      UpdateGrantLengthForm.form.bindFromRequest().fold(handleFormError, handleValidForm)
    }
  }

  def manageAutoDelete(appId: ApplicationId) = atLeastSuperUserAction { implicit request =>
    withApp(appId) { app =>
      def getView(event: Option[DisplayEvent]) = {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
        val reasonNotFound    = "Reason not found"
        event match {
          case None        => NotFound(errorHandler.standardErrorTemplate("Something unexpected happened", reasonNotFound, reasonNotFound))
          case Some(event) => Ok(manageAutoDeleteDisabledView(
              app.application,
              event.metaData.mkString,
              event.eventDateTime.atZone(ZoneOffset.UTC).format(dateTimeFormatter),
              AutoDeletePreviouslyDisabledForm.form
            ))
        }
      }

      def handleAutoDeleteDisabled(application: GKApplicationResponse) = {
        for {
          event <- eventsConnector.query(application.id, application.deployedTo, Some("APP_LIFECYCLE"), None)
                     .map(events => events.find(e => e.eventType == "Application auto delete blocked"))
          view   = getView(event)
        } yield view
      }

      if (app.application.moreApplication.allowAutoDelete) {
        Future.successful(Ok(manageAutoDeleteEnabledView(app.application, AutoDeletePreviouslyEnabledForm.form)))
      } else {
        handleAutoDeleteDisabled(app.application)
      }

    }
  }

  def updateAutoDeletePreviouslyEnabled(appId: ApplicationId): Action[AnyContent] = atLeastSuperUserAction { implicit request =>
    withApp(appId) { app =>
      def handleUpdateAutoDelete(allowAutoDelete: Boolean, reason: String) = {
        applicationService.updateAutoDelete(appId, allowAutoDelete, loggedIn.userFullName.get, reason) map { _ =>
          Ok(autoDeleteSuccessView(app.application, allowAutoDelete))
        }
      }

      def handleValidForm(form: AutoDeletePreviouslyEnabledForm): Future[Result] = {
        form.confirm match {
          case "yes" => handleUpdateAutoDelete(allowAutoDelete = true, "No reasons given")
          case "no"  => handleUpdateAutoDelete(allowAutoDelete = false, form.reason)
          case _     => successful(Redirect(routes.ApplicationController.applicationPage(appId).url))
        }
      }

      def handleInvalidForm(form: Form[AutoDeletePreviouslyEnabledForm]): Future[Result] = {
        successful(BadRequest(manageAutoDeleteEnabledView(app.application, form)))
      }

      AutoDeletePreviouslyEnabledForm.form.bindFromRequest().fold(handleInvalidForm, handleValidForm)
    }
  }

  def updateAutoDeletePreviouslyDisabled(appId: ApplicationId): Action[AnyContent] = atLeastSuperUserAction { implicit request =>
    withApp(appId) { app =>
      def handleUpdateAutoDelete(allowAutoDelete: Boolean, reason: String) = {
        applicationService.updateAutoDelete(appId, allowAutoDelete, loggedIn.userFullName.get, reason) map { _ =>
          Ok(autoDeleteSuccessView(app.application, allowAutoDelete))
        }
      }

      def handleValidForm(form: AutoDeletePreviouslyDisabledForm) = {
        form.confirm match {
          case "yes" => handleUpdateAutoDelete(allowAutoDelete = true, "No reasons given")
          case "no"  => Future.successful(Ok(autoDeleteSuccessView(app.application, allowAutoDelete = false)))
          case _     => successful(Redirect(routes.ApplicationController.applicationPage(appId).url))
        }
      }

      def handleInvalidForm(form: Form[AutoDeletePreviouslyDisabledForm]): Future[Result] = {
        successful(BadRequest(manageAutoDeleteDisabledView(app.application, form.data("reason"), form.data("reasonDate"), form)))
      }

      AutoDeletePreviouslyDisabledForm.form.bindFromRequest().fold(handleInvalidForm, handleValidForm)
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
        if (app.application.name.trim() == form.applicationNameConfirmation) {
          applicationService.deleteApplication(app.application, loggedIn.userFullName.get, LaxEmailAddress(form.collaboratorEmail.get)).map {
            case ApplicationUpdateSuccessResult => Ok(deleteApplicationSuccessView(app))
            case ApplicationUpdateFailureResult => technicalDifficulties
          }
        } else {
          val formWithErrors = deleteApplicationForm.fill(form).withError(FormFields.applicationNameConfirmation, messagesApi.preferred(request)("application.confirmation.error"))

          Future.successful(BadRequest(deleteApplicationView(app, request.role.isSuperUser, formWithErrors)))
        }
      }

      def handleFormError(form: Form[DeleteApplicationForm]) = {
        Future.successful(BadRequest(deleteApplicationView(app, request.role.isSuperUser, form)))
      }

      deleteApplicationForm.bindFromRequest().fold(handleFormError, handleValidForm)
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
        if (app.application.name.trim() == form.applicationNameConfirmation) {
          applicationService.blockApplication(app.application, loggedIn.userFullName.get).map {
            case ApplicationBlockSuccessResult => Ok(blockApplicationSuccessView(app))
            case ApplicationBlockFailureResult => technicalDifficulties
          }
        } else {
          val formWithErrors = blockApplicationForm.fill(form).withError(FormFields.applicationNameConfirmation, messagesApi.preferred(request)("application.confirmation.error"))

          Future.successful(BadRequest(blockApplicationView(app, formWithErrors)))
        }
      }

      def handleFormError(form: Form[BlockApplicationForm]) = {
        Future.successful(BadRequest(blockApplicationView(app, form)))
      }

      blockApplicationForm.bindFromRequest().fold(handleFormError, handleValidForm)
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
        if (app.application.name.trim() == form.applicationNameConfirmation) {
          applicationService.unblockApplication(app.application, loggedIn.userFullName.get).map {
            case ApplicationUnblockSuccessResult => Ok(unblockApplicationSuccessView(app))
            case ApplicationUnblockFailureResult => technicalDifficulties
          }
        } else {
          val formWithErrors = unblockApplicationForm.fill(form).withError(FormFields.applicationNameConfirmation, messagesApi.preferred(request)("application.confirmation.error"))

          Future.successful(BadRequest(unblockApplicationView(app, formWithErrors)))
        }
      }

      def handleFormError(form: Form[UnblockApplicationForm]) = {
        Future.successful(BadRequest(unblockApplicationView(app, form)))
      }

      unblockApplicationForm.bindFromRequest().fold(handleFormError, handleValidForm)
    }
  }

  private def groupApisByStatus(apis: List[ApiDefinition]): Map[String, List[VersionSummary]] = {
    val versions = for {
      api     <- apis
      version <- api.versionsAsList
    } yield VersionSummary(api.name, version.status, ApiIdentifier(api.context, version.versionNbr))

    versions.groupBy(_.status.displayText)
  }

  def handleUpdateRateLimitTier(appId: ApplicationId): Action[AnyContent] = anyStrideUserAction { implicit request =>
    withApp(appId) { app =>
      val result = Redirect(routes.ApplicationController.applicationPage(appId))

      if (request.role.isSuperUser) {
        RateLimitTier.apply(UpdateRateLimitForm.form.bindFromRequest().get.tier) match {
          case Some(tier) => applicationService.updateRateLimitTier(app.application, tier, loggedIn.userFullName.get) map {
              case ApplicationUpdateSuccessResult =>
                result.flashing("success" -> s"Rate limit tier has been changed to ${tier.displayText}")
              case _                              =>
                result.flashing("failed" -> "Rate limit tier was not changed successfully") // Don't expect this as an error is thrown
            }
          case None       => Future.successful(result.flashing("failed" -> "Rate limit tier was not changed successfully"))
        }
      } else {
        Future.successful(result)
      }
    }
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

    type FieldName                = String
    type ErrorCode                = String
    type ValidationResult[A]      = ValidatedNec[(FieldName, ErrorCode), A]
    type FieldValidationResult[A] = ValidatedNec[ErrorCode, A]

    implicit class WithFieldSyntax[T](v: FieldValidationResult[T]) {
      def withField(fn: FieldName): ValidationResult[T] = v.leftMap(_.map(err => (fn, err)))
    }

    def validateApplicationName(environment: Environment, applicationName: String, apps: Seq[GKApplicationResponse]): FieldValidationResult[String] = {
      val isValid = environment match {
        case Environment.PRODUCTION => !apps.exists(app => (app.deployedTo == Environment.PRODUCTION) && (app.name == applicationName))
        case _                      => true
      }
      if (isValid) applicationName.valid else "application.name.already.exists".invalidNec
    }

    def validateUserSuitability(user: Option[User]): FieldValidationResult[RegisteredUser] = user match {
      case None                                                                          => "admin.email.is.not.registered".invalidNec
      case Some(UnregisteredUser(_, _))                                                  => "admin.email.is.not.registered".invalidNec
      case Some(user: RegisteredUser) if !user.verified                                  => "admin.email.is.not.verified".invalidNec
      case Some(user: RegisteredUser) if !MfaDetailHelper.isMfaVerified(user.mfaDetails) => "admin.email.is.not.mfa.enabled".invalidNec
      case Some(user: RegisteredUser)                                                    => user.validNec
    }

    def handleValidForm(form: CreatePrivOrROPCAppForm): Future[Result] = {
      def createApp(user: User, accessType: AccessType) = {
        val collaborators = List(Collaborators.Administrator(user.userId, LaxEmailAddress(form.adminEmail)))

        applicationService.createPrivOrROPCApp(form.environment, form.applicationName, form.applicationDescription, collaborators, AppAccess(accessType, List.empty))
          .map {
            case CreatePrivOrROPCAppFailureResult                                                 => InternalServerError("Unexpected problems creating application")
            case CreatePrivOrROPCAppSuccessResult(appId, appName, appEnv, clientId, totp, access) =>
              Ok(createApplicationSuccessView(appId, appName, appEnv, Some(access.accessType), totp, clientId))
          }
      }

      def validateValues(apps: Seq[GKApplicationResponse], user: Option[User]): ValidationResult[(String, RegisteredUser)] =
        (
          validateApplicationName(form.environment, form.applicationName, apps).withField("applicationName"),
          validateUserSuitability(user).withField("adminEmail")
        )
          .mapN((n, u) => (n, u))

      def handleValues(apps: Seq[GKApplicationResponse], user: Option[User], accessType: AccessType): Future[Result] =
        validateValues(apps, user)
          .fold[Future[Result]](
            errs => successful(viewWithFormErrors(errs)),
            goodData => createApp(goodData._2, accessType)
          )

      def formWithErrors(errs: NonEmptyChain[(FieldName, ErrorCode)]): Form[CreatePrivOrROPCAppForm] =
        errs.foldLeft(createPrivOrROPCAppForm.fill(form))((f, e) => f.withError(e._1, e._2))

      def viewWithFormErrors(errs: NonEmptyChain[(FieldName, ErrorCode)]): Result =
        BadRequest(createApplicationView(formWithErrors(errs)))

      val accessType =
        form.accessType.flatMap(AccessType.apply).getOrElse(throw new RuntimeException(s"Access Type ${form.accessType} not recognized when attempting to create Priv or ROPC app"))

      val fApps = applicationService.fetchApplications
      val fUser = developerService.seekUser(LaxEmailAddress(form.adminEmail))

      for {
        apps   <- fApps
        user   <- fUser
        result <- handleValues(apps, user, accessType)
      } yield result
    }

    createPrivOrROPCAppForm.bindFromRequest().fold(handleInvalidForm, handleValidForm)
  }
}
