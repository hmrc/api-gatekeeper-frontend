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

import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models.APIAccessType.{PRIVATE, PUBLIC}
import uk.gov.hmrc.gatekeeper.models.Forms._
import uk.gov.hmrc.gatekeeper.models.DeveloperStatusFilter.VerifiedStatus
import uk.gov.hmrc.gatekeeper.models.EmailOptionChoice.{EMAIL_ALL_USERS, _}
import uk.gov.hmrc.gatekeeper.models.EmailPreferencesChoice.{SPECIFIC_API, TAX_REGIME, TOPIC}
import uk.gov.hmrc.gatekeeper.models.TopicOptionChoice.TopicOptionChoice
import uk.gov.hmrc.gatekeeper.models._
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.gatekeeper.services.{ApiDefinitionService, ApmService, ApplicationService, DeveloperService}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.gatekeeper.utils.{ErrorHelper, UserFunctionsWrapper}
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}
import uk.gov.hmrc.gatekeeper.views.html.emails.{
  EmailAllUsersView,
  EmailApiSubscriptionsView,
  EmailInformationView,
  EmailLandingView,
  EmailPreferencesAPICategoryView,
  EmailPreferencesChoiceView,
  EmailPreferencesSelectApiView,
  EmailPreferencesSpecificApiView,
  EmailPreferencesTopicView
}
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.Json
import uk.gov.hmrc.gatekeeper.models.CombinedApiCategory.toAPICategory

import scala.concurrent.Future.successful

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
    emailPreferencesAPICategoryView: EmailPreferencesAPICategoryView,
    emailPreferencesSpecificApiView: EmailPreferencesSpecificApiView,
    emailPreferencesSelectApiView: EmailPreferencesSelectApiView,
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
    with UserFunctionsWrapper {

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

    SendEmailChoiceForm.form.bindFromRequest.fold(handleInvalidForm, handleValidForm)
  }

  def emailPreferencesChoice(): Action[AnyContent] = anyStrideUserAction { implicit request =>
    Future.successful(Ok(emailPreferencesChoiceView()))
  }

  def chooseEmailPreferences(): Action[AnyContent] = anyStrideUserAction { implicit request =>
    def handleValidForm(form: SendEmailPreferencesChoice): Future[Result] = {
      form.sendEmailPreferences match {
        case SPECIFIC_API => Future.successful(Redirect(routes.EmailsController.selectSpecificApi(None)))
        case TAX_REGIME   => Future.successful(Redirect(routes.EmailsController.emailPreferencesAPICategory(None, None)))
        case TOPIC        => Future.successful(Redirect(routes.EmailsController.emailPreferencesTopic(None)))
      }
    }

    def handleInvalidForm(formWithErrors: Form[SendEmailPreferencesChoice]) =
      Future.successful(BadRequest(emailPreferencesChoiceView()))

    SendEmailPrefencesChoiceForm.form.bindFromRequest.fold(handleInvalidForm, handleValidForm)
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
      selectedTopic: Option[TopicOptionChoice.Value],
      apiAcessType: APIAccessType
    )(implicit hc: HeaderCarrier
    ): Future[List[RegisteredUser]] = {
    // APSR-1418 - the accesstype inside combined api is option as a temporary measure until APM version which conatins the change to
    // return this is deployed out to all environments
    val filteredApis = apis.filter(_.accessType.getOrElse(APIAccessType.PUBLIC) == apiAcessType)
    val categories   = filteredApis.flatMap(_.categories.map(toAPICategory))
    val apiNames     = filteredApis.map(_.serviceName)
    selectedTopic.fold(Future.successful(List.empty[RegisteredUser]))(topic => {
      (apiAcessType, filteredApis) match {
        case (_, Nil)     => successful(List.empty[RegisteredUser])
        case (PUBLIC, _)  =>
          developerService.fetchDevelopersBySpecificAPIEmailPreferences(topic, categories, apiNames, privateApiMatch = false).map(_.filter(_.verified))
        case (PRIVATE, _) =>
          developerService.fetchDevelopersBySpecificAPIEmailPreferences(topic, categories, apiNames, privateApiMatch = true).map(_.filter(_.verified))
      }

    })
  }

  def emailPreferencesSpecificApis(selectedAPIs: List[String], selectedTopicStr: Option[String] = None): Action[AnyContent] = anyStrideUserAction { implicit request =>
    val selectedTopic: Option[TopicOptionChoice.Value] = selectedTopicStr.map(TopicOptionChoice.withName)
    if (selectedAPIs.forall(_.isEmpty)) {
      Future.successful(Redirect(routes.EmailsController.selectSpecificApi(None)))
    } else {
      for {
        apis         <- apmService.fetchAllCombinedApis()
        filteredApis  = filterSelectedApis(Some(selectedAPIs), apis).sortBy(_.displayName)
        publicUsers  <- handleGettingApiUsers(filteredApis, selectedTopic, PUBLIC)
        privateUsers <- handleGettingApiUsers(filteredApis, selectedTopic, PRIVATE)
        combinedUsers = publicUsers ++ privateUsers
        usersAsJson   = Json.toJson(combinedUsers)
      } yield Ok(emailPreferencesSpecificApiView(combinedUsers, usersAsJson, usersToEmailCopyText(combinedUsers), filteredApis, selectedTopic))
    }
  }

  def emailPreferencesTopic(selectedTopic: Option[String] = None): Action[AnyContent] = anyStrideUserAction { implicit request =>
    // withName could throw an exception here
    val maybeTopic = selectedTopic.map(TopicOptionChoice.withName)
    maybeTopic.map(developerService.fetchDevelopersByEmailPreferences(_)).getOrElse(Future.successful(List.empty))
      .map(users => {
        val filteredUsers       = users.filter(_.verified)
        val filteredUsersAsJson = Json.toJson(filteredUsers)
        Ok(emailPreferencesTopicView(filteredUsers, filteredUsersAsJson, usersToEmailCopyText(filteredUsers), maybeTopic))
      })
  }

  def emailPreferencesAPICategory(selectedTopic: Option[String] = None, selectedCategory: Option[String] = None): Action[AnyContent] = anyStrideUserAction { implicit request =>
    val topicAndCategory: Option[(TopicOptionChoice, String)] =
      for {
        topic    <- selectedTopic.map(TopicOptionChoice.withName)
        category <- selectedCategory.filter(!_.isEmpty)
      } yield (topic, category)

    for {
      categories          <- apiDefinitionService.apiCategories
      users               <- topicAndCategory.map(tup =>
                               developerService.fetchDevelopersByAPICategoryEmailPreferences(tup._1, APICategory(tup._2))
                             )
                               .getOrElse(Future.successful(List.empty)).map(_.filter(_.verified))
      usersAsJson          = Json.toJson(users)
      selectedCategories   = categories.filter(category => category.category == topicAndCategory.map(_._2).getOrElse(""))
      selectedCategoryName = if (selectedCategories.nonEmpty) selectedCategories.head.name else ""
    } yield Ok(emailPreferencesAPICategoryView(
      users,
      usersAsJson,
      usersToEmailCopyText(users),
      topicAndCategory.map(_._1),
      categories,
      selectedCategory.getOrElse(""),
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
    developerService.fetchUsers
      .map((users: List[RegisteredUser]) => {
        val filteredUsers = users.filter(_.verified)
        val usersAsJson   = Json.toJson(filteredUsers)
        Ok(emailsAllUsersView(filteredUsers, usersAsJson, usersToEmailCopyText(filteredUsers)))
      })
  }

  def emailApiSubscribersPage(maybeApiVersionFilter: Option[String] = None): Action[AnyContent] = anyStrideUserAction { implicit request =>
    val queryParams                               = getQueryParametersAsKeyValues(request)
    val apiDropDowns: Future[List[DropDownValue]] = for {
      apiVersions  <- apiDefinitionService.fetchAllApiDefinitions()
      apiDropDowns <- Future.successful(getApiVersionsDropDownValues(apiVersions))
    } yield apiDropDowns

    val filter       = DevelopersSearchFilter(None, ApiContextVersion(mapEmptyStringToNone(maybeApiVersionFilter)), AnyEnvironment, VerifiedStatus)
    val fetchedUsers = mapEmptyStringToNone(maybeApiVersionFilter).fold(Future.successful(List.empty[User]))(_ => developerService.searchDevelopers(filter))
    for {
      registeredUsers <- fetchedUsers.map(users =>
                           users.collect {
                             case r: RegisteredUser => r
                           }
                         )
      verifiedUsers    = registeredUsers.filter(_.verified)
      usersAsJson      = Json.toJson(verifiedUsers)
      apis            <- apiDropDowns
    } yield Ok(emailApiSubscriptionsView(apis, verifiedUsers, usersAsJson, usersToEmailCopyText(verifiedUsers), queryParams))
  }
}
