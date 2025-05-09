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

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaborators, Collaborator, State}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models.EmailOptionChoice._
import uk.gov.hmrc.gatekeeper.models.EmailPreferencesChoice._
import uk.gov.hmrc.gatekeeper.models.Forms._
import uk.gov.hmrc.gatekeeper.models.{TopicOptionChoice, _}
import uk.gov.hmrc.gatekeeper.services.{ApiDefinitionService, ApmService, ApplicationService, DeveloperService}
import uk.gov.hmrc.gatekeeper.utils.{ErrorHelper, UserFunctionsWrapper}
import uk.gov.hmrc.gatekeeper.views.html.emails._
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

@Singleton
class EmailsController @Inject() (
    developerService: DeveloperService,
    apiDefinitionService: ApiDefinitionService,
    emailLandingView: EmailLandingView,
    emailInformationView: EmailInformationView,
    emailsAllUsersView: EmailAllUsersView,
    emailApiSubscriptionsView: EmailApiSubscriptionsView,
    emailPreferencesChoiceView: EmailPreferencesChoiceView,
    emailPreferencesTopicView: EmailPreferencesTopicView,
    emailPreferencesApiCategoryView: EmailPreferencesApiCategoryView,
    emailPreferencesSpecificApiView: EmailPreferencesSpecificApiView,
    emailPreferencesSelectApiView: EmailPreferencesSelectApiView,
    emailPreferencesSelectTopicView: EmailPreferencesSelectTopicView,
    val applicationService: ApplicationService,
    val forbiddenView: ForbiddenView,
    mcc: MessagesControllerComponents,
    override val errorTemplate: ErrorTemplate,
    val apmService: ApmService,
    strideAuthorisationService: StrideAuthorisationService
  )(implicit val appConfig: AppConfig,
    override val ec: ExecutionContext
  ) extends GatekeeperBaseController(strideAuthorisationService, mcc)
    with ErrorHelper
    with UserFunctionsWrapper
    with ApplicationLogger {

  def landing(): Action[AnyContent] = anyStrideUserAction { implicit request =>
    Future.successful(Ok(emailLandingView()))
  }

  def chooseEmailOption(): Action[AnyContent] = anyStrideUserAction { implicit request =>
    def handleValidForm(form: SendEmailChoice): Future[Result] = {
      form.sendEmailChoice match {
        case EMAIL_PREFERENCES => Future.successful(Redirect(routes.EmailsController.emailPreferencesChoice()))
        case API_SUBSCRIPTION  => Future.successful(Redirect(routes.EmailsController.showEmailInformation(emailChoice = "api-subscription")))
        case EMAIL_ALL_USERS   => Future.successful(Redirect(routes.EmailsController.showEmailInformation(emailChoice = "all-users")))
      }
    }

    def handleInvalidForm(formWithErrors: Form[SendEmailChoice]) =
      Future.successful(BadRequest(emailLandingView()))

    SendEmailChoiceForm.form.bindFromRequest().fold(handleInvalidForm, handleValidForm)
  }

  def emailPreferencesChoice(): Action[AnyContent] = anyStrideUserAction { implicit request =>
    Future.successful(Ok(emailPreferencesChoiceView()))
  }

  def chooseEmailPreferences(): Action[AnyContent] = anyStrideUserAction { implicit request =>
    def handleValidForm(form: SendEmailPreferencesChoice): Future[Result]   = {
      form.sendEmailPreferences match {
        case SPECIFIC_API => Future.successful(Redirect(routes.EmailsController.selectSpecificApi(None)))
        case TAX_REGIME   => Future.successful(Redirect(routes.EmailsController.emailPreferencesApiCategory(None, None)))
        case TOPIC        => Future.successful(Redirect(routes.EmailsController.emailPreferencesTopic(None)))
      }
    }
    def handleInvalidForm(formWithErrors: Form[SendEmailPreferencesChoice]) =
      Future.successful(BadRequest(emailPreferencesChoiceView()))

    SendEmailPrefencesChoiceForm.form.bindFromRequest().fold(handleInvalidForm, handleValidForm)
  }

  def selectSpecificApi(selectedAPIs: Option[List[String]]): Action[AnyContent] = anyStrideUserAction { implicit request =>
    for {
      apis         <- apmService.fetchAllCombinedApis()
      selectedApis <- Future.successful(filterSelectedApis(selectedAPIs, apis))
    } yield Ok(emailPreferencesSelectApiView(apis.sortBy(_.displayName), selectedApis.sortBy(_.displayName)))
  }

  private def filterSelectedApis(maybeSelectedAPIs: Option[List[String]], apiList: List[CombinedApi]) =
    maybeSelectedAPIs.fold(List.empty[CombinedApi])(selectedAPIs => apiList.filter(api => selectedAPIs.contains(api.serviceName)))

  private def handleGettingApiUsers(
      apis: List[CombinedApi],
      selectedTopic: Option[TopicOptionChoice],
      apiAccessType: ApiAccessType
    )(implicit hc: HeaderCarrier
    ): Future[List[RegisteredUser]] = {
    // APSR-1418 - the accesstype inside combined api is option as a temporary measure until APM version which conatins the change to
    // return this is deployed out to all environments
    val filteredApis = apis.filter(_.accessType.getOrElse(ApiAccessType.PUBLIC) == apiAccessType)
    val categories   = filteredApis.flatMap(_.categories).toSet
    val apiNames     = filteredApis.map(_.serviceName)
    selectedTopic.fold(Future.successful(List.empty[RegisteredUser]))(topic => {
      (apiAccessType, filteredApis) match {
        case (_, Nil)                   => successful(List.empty[RegisteredUser])
        case (ApiAccessType.PUBLIC, _)  =>
          developerService.fetchDevelopersBySpecificAPIEmailPreferences(topic, categories, apiNames, privateApiMatch = false).map(_.filter(_.verified))
        case (ApiAccessType.PRIVATE, _) =>
          developerService.fetchDevelopersBySpecificAPIEmailPreferences(topic, categories, apiNames, privateApiMatch = true).map(_.filter(_.verified))
      }
    })
  }

  def emailPreferencesSpecificApis(selectedAPIs: List[String], selectedTopic: Option[TopicOptionChoice] = None): Action[AnyContent] = anyStrideUserAction { implicit request =>
    if (selectedAPIs.forall(_.isEmpty)) {
      Future.successful(Redirect(routes.EmailsController.selectSpecificApi(None)))
    } else {
      for {
        apis         <- apmService.fetchAllCombinedApis()
        filteredApis  = filterSelectedApis(Some(selectedAPIs), apis).sortBy(_.displayName)
        publicUsers  <- handleGettingApiUsers(filteredApis, selectedTopic, ApiAccessType.PUBLIC)
        privateUsers <- handleGettingApiUsers(filteredApis, selectedTopic, ApiAccessType.PRIVATE)
        combinedUsers = (publicUsers ++ privateUsers).distinct
      } yield Ok(emailPreferencesSpecificApiView(combinedUsers, usersToEmailCopyText(combinedUsers), filteredApis, selectedTopic))
    }
  }

  def emailPreferencesTopic(maybeTopic: Option[TopicOptionChoice] = None): Action[AnyContent] = anyStrideUserAction { implicit request =>
    // withName could throw an exception here
    maybeTopic.map(developerService.fetchDevelopersByEmailPreferences(_)).getOrElse(Future.successful(List.empty))
      .map(users => {
        val filteredUsers = users.filter(_.verified)
        Ok(emailPreferencesTopicView(filteredUsers, usersToEmailCopyText(filteredUsers), maybeTopic))
      })
  }

  def emailPreferencesApiCategory(maybeSelectedTopic: Option[TopicOptionChoice] = None, maybeSelectedCategory: Option[ApiCategory] = None): Action[AnyContent] =
    anyStrideUserAction { implicit request =>
      val topicAndCategory: Option[(TopicOptionChoice, ApiCategory)] =
        for {
          topic    <- maybeSelectedTopic
          category <- maybeSelectedCategory
        } yield (topic, category)

      for {
        users               <- topicAndCategory.map(tup =>
                                 developerService.fetchDevelopersByAPICategoryEmailPreferences(tup._1, tup._2)
                               )
                                 .getOrElse(Future.successful(List.empty)).map(_.filter(_.verified))
        selectedCategory     = topicAndCategory.map(_._2)
        selectedCategoryName = selectedCategory.map(_.displayText).getOrElse("")
      } yield Ok(emailPreferencesApiCategoryView(
        users,
        usersToEmailCopyText(users),
        topicAndCategory.map(_._1),
        maybeSelectedCategory,
        selectedCategoryName
      ))
    }

  def showEmailInformation(emailChoice: String): Action[AnyContent] = anyStrideUserAction { implicit request =>
    emailChoice match {
      case "all-users"        => Future.successful(Ok(emailInformationView(EmailOptionChoice.EMAIL_ALL_USERS)))
      case "api-subscription" => Future.successful(Ok(emailInformationView(EmailOptionChoice.API_SUBSCRIPTION)))
      case _                  => Future.failed(new NotFoundException("Page Not Found"))
    }
  }

  def emailAllUsersPage(): Action[AnyContent] = anyStrideUserAction { implicit request =>
    developerService.fetchUsers()
      .map((users: List[RegisteredUser]) => {
        val filteredUsers = users.filter(_.verified)
        Ok(emailsAllUsersView(filteredUsers, usersToEmailCopyText(filteredUsers)))
      })
  }

  def emailApiSubscribersPage(maybeApiVersionFilter: Option[String] = None): Action[AnyContent] = anyStrideUserAction { implicit request =>
    val queryParams = getQueryParametersAsKeyValues(request)

    val apiDropDowns: Future[List[DropDownValue]] = for {
      apiVersions  <- apiDefinitionService.fetchAllApiDefinitions()
      apiDropDowns <- Future.successful(getApiVersionsDropDownValues(apiVersions))
    } yield apiDropDowns

    def getSubscribedRegisteredUsers(apiFilter: ApiFilter[String]): Future[List[RegisteredUser]] = {
      for {
        appsSubscribedToSelectedApiSandBox: List[ApplicationWithCollaborators]    <- applicationService.fetchApplications(apiFilter, SandboxEnvironment)
        appsSubscribedToSelectedApiProduction: List[ApplicationWithCollaborators] <- applicationService.fetchApplications(apiFilter, ProductionEnvironment)
        allApps: List[ApplicationWithCollaborators]                                = appsSubscribedToSelectedApiSandBox ++ appsSubscribedToSelectedApiProduction
        allAppsNotDeleted: List[ApplicationWithCollaborators]                      = allApps.filter(_.details.state.name != State.DELETED)
        allCollaborators: Set[Collaborator]                                        = allAppsNotDeleted.map(_.collaborators).flatten.toSet
        allCollaboratorEmails: Set[LaxEmailAddress]                                = allCollaborators.map(_.emailAddress)
        allRegisteredUsers: List[RegisteredUser]                                  <- developerService.fetchDevelopersByEmails(allCollaboratorEmails)
      } yield allRegisteredUsers
    }

    for {
      apis               <- apiDropDowns
      allRegisteredUsers <- maybeApiVersionFilter match {
                              case None              => successful(List.empty[RegisteredUser])
                              case api @ Some(value) => getSubscribedRegisteredUsers(ApiFilter(api))
                            }
    } yield Ok(emailApiSubscriptionsView(apis, allRegisteredUsers, usersToEmailCopyText(allRegisteredUsers), queryParams))
  }
}
