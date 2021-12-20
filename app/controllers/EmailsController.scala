/*
 * Copyright 2021 HM Revenue & Customs
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
import model.Forms._
import javax.inject.{Inject, Singleton}
import model.DeveloperStatusFilter.VerifiedStatus
import model.EmailOptionChoice.{EMAIL_ALL_USERS, _}
import model.EmailPreferencesChoice.{SPECIFIC_API, TAX_REGIME, TOPIC}
import model.TopicOptionChoice.TopicOptionChoice
import model.{APICategory, ApiDefinition, AnyEnvironment, ApiContextVersion, Developers2Filter, DropDownValue, EmailOptionChoice, GatekeeperRole, SendEmailChoice, SendEmailPreferencesChoice, TopicOptionChoice}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{ApiDefinitionService, ApplicationService, DeveloperService, ApmService}
import uk.gov.hmrc.http.NotFoundException
import utils.{ErrorHelper, UserFunctionsWrapper}
import views.html.{ErrorTemplate, ForbiddenView}
import views.html.emails.{EmailAllUsersView, EmailApiSubscriptionsView, EmailInformationView, EmailLandingView, EmailPreferencesAPICategoryView, EmailPreferencesChoiceView, EmailPreferencesSelectApiView, EmailPreferencesSpecificApiView, EmailPreferencesTopicView}

import uk.gov.hmrc.modules.stride.controllers.GatekeeperBaseController
import uk.gov.hmrc.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.modules.stride.controllers.actions.ForbiddenHandler
import uk.gov.hmrc.modules.stride.connectors.AuthConnector

import scala.concurrent.{ExecutionContext, Future}
import model.{User,RegisteredUser}

@Singleton
class EmailsController @Inject()(
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
  strideAuthConfig: StrideAuthConfig,
  authConnector: AuthConnector,
  forbiddenHandler: ForbiddenHandler
)(implicit val appConfig: AppConfig, override val ec: ExecutionContext)
  extends GatekeeperBaseController(strideAuthConfig, authConnector, forbiddenHandler, mcc)
    with ErrorHelper
    with UserFunctionsWrapper {

  def landing(): Action[AnyContent] = anyStrideUserAction { implicit request =>
    Future.successful(Ok(emailLandingView()))
  }

  def chooseEmailOption(): Action[AnyContent] = anyStrideUserAction { implicit request =>
    def handleValidForm(form: SendEmailChoice): Future[Result] = {
      form.sendEmailChoice match {
        case EMAIL_PREFERENCES => Future.successful(Redirect(routes.EmailsController.emailPreferencesChoice()))
        case API_SUBSCRIPTION => Future.successful(Redirect(routes.EmailsController.showEmailInformation(emailChoice = "api-subscription")))
        case EMAIL_ALL_USERS => Future.successful(Redirect(routes.EmailsController.showEmailInformation(emailChoice = "all-users")))
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
        case SPECIFIC_API => Future.successful(Redirect(routes.EmailsController.selectSpecficApi(None)))
        case TAX_REGIME => Future.successful(Redirect(routes.EmailsController.emailPreferencesAPICategory(None, None)))
        case TOPIC => Future.successful(Redirect(routes.EmailsController.emailPreferencesTopic(None)))
      }
    }

    def handleInvalidForm(formWithErrors: Form[SendEmailPreferencesChoice]) =
      Future.successful(BadRequest(emailPreferencesChoiceView()))

    SendEmailPrefencesChoiceForm.form.bindFromRequest.fold(handleInvalidForm, handleValidForm)
  }

  def selectSpecficApi(selectedAPIs: Option[List[String]]): Action[AnyContent] = anyStrideUserAction { implicit request =>
    for {
      apis <- apiDefinitionService.fetchAllDistinctApisIgnoreVersions()
      selectedApis <- Future.successful(filterSelectedApis(selectedAPIs, apis))
    } yield Ok(emailPreferencesSelectApiView(apis.sortBy(_.name), selectedApis.sortBy(_.name)))
  }

  private def filterSelectedApis(maybeSelectedAPIs: Option[List[String]], apiList: List[ApiDefinition]) =
    maybeSelectedAPIs.fold(List.empty[ApiDefinition])(selectedAPIs => apiList.filter(api => selectedAPIs.contains(api.serviceName)))

  def emailPreferencesSpecificApis(selectedAPIs: List[String],
                                   selectedTopicStr: Option[String] = None): Action[AnyContent] = anyStrideUserAction { implicit request =>
    val selectedTopic = selectedTopicStr.map(TopicOptionChoice.withName)
    if (selectedAPIs.forall(_.isEmpty)) {
      Future.successful(Redirect(routes.EmailsController.selectSpecficApi(None)))
    } else {
      for {
        apis <- apiDefinitionService.fetchAllDistinctApisIgnoreVersions()
        filteredApis = filterSelectedApis(Some(selectedAPIs), apis).sortBy(_.name)
        apiNames = filteredApis.map(_.serviceName)
        categories = filteredApis.flatMap(_.categories.getOrElse(List.empty))
        users <- selectedTopic.fold(Future.successful(List.empty[RegisteredUser]))(topic => {
          developerService.fetchDevelopersBySpecificAPIEmailPreferences(topic, categories, apiNames).map(_.filter(_.verified))
        })
      } yield Ok(emailPreferencesSpecificApiView(users, usersToEmailCopyText(users), filteredApis, selectedTopic))
    }
  }

  def emailPreferencesTopic(selectedTopic: Option[String] = None): Action[AnyContent] = anyStrideUserAction { implicit request => 
    //withName could throw an exception here
    val maybeTopic = selectedTopic.map(TopicOptionChoice.withName)
    maybeTopic.map(developerService.fetchDevelopersByEmailPreferences(_)).getOrElse(Future.successful(List.empty))
      .map(users => {
        val filteredUsers = users.filter(_.verified)
        Ok(emailPreferencesTopicView(filteredUsers, usersToEmailCopyText(filteredUsers), maybeTopic))
      })
  }

  def emailPreferencesAPICategory(selectedTopic: Option[String] = None, selectedCategory: Option[String] = None): Action[AnyContent] = anyStrideUserAction { implicit request =>
    val topicAndCategory: Option[(TopicOptionChoice, String)] =
      for {
        topic <- selectedTopic.map(TopicOptionChoice.withName)
        category <- selectedCategory.filter(!_.isEmpty)
      } yield (topic, category)

    for {
      categories <- apiDefinitionService.apiCategories
      users <- topicAndCategory.map(tup =>
        developerService.fetchDevelopersByAPICategoryEmailPreferences(tup._1, APICategory(tup._2)))
        .getOrElse(Future.successful(List.empty)).map(_.filter(_.verified))

    } yield Ok(emailPreferencesAPICategoryView(users, usersToEmailCopyText(users), topicAndCategory.map(_._1), categories, selectedCategory.getOrElse("")))
  }


  def showEmailInformation(emailChoice: String): Action[AnyContent] = anyStrideUserAction { implicit request =>
    emailChoice match {
      case "all-users" => Future.successful(Ok(emailInformationView(EmailOptionChoice.EMAIL_ALL_USERS)))
      case "api-subscription" => Future.successful(Ok(emailInformationView(EmailOptionChoice.API_SUBSCRIPTION)))
      case _ => Future.failed(new NotFoundException("Page Not Found"))
    }
  }

  def emailAllUsersPage(): Action[AnyContent] = anyStrideUserAction { implicit request =>
    developerService.fetchUsers
      .map((users: List[RegisteredUser]) => {
        val filteredUsers = users.filter(_.verified)
        Ok(emailsAllUsersView(filteredUsers, usersToEmailCopyText(filteredUsers)))
      })
  }

  def emailApiSubscribersPage(maybeApiVersionFilter: Option[String] = None): Action[AnyContent] = anyStrideUserAction { implicit request =>
    val queryParams = getQueryParametersAsKeyValues(request)
    val apiDropDowns: Future[List[DropDownValue]] = for {
      apiVersions <- apiDefinitionService.fetchAllApiDefinitions()
      apiDropDowns <- Future.successful(getApiVersionsDropDownValues(apiVersions))
    } yield apiDropDowns

    val filter = Developers2Filter(None, ApiContextVersion(mapEmptyStringToNone(maybeApiVersionFilter)), AnyEnvironment, VerifiedStatus)
    val fetchedUsers = mapEmptyStringToNone(maybeApiVersionFilter).fold(Future.successful(List.empty[User]))(_ => developerService.searchDevelopers(filter))
    for {
      registeredUsers <- fetchedUsers.map(users => users.collect {
        case r : RegisteredUser => r
      })
      verifiedUsers = registeredUsers.filter(_.verified)
      apis <- apiDropDowns
    } yield Ok(emailApiSubscriptionsView(apis, verifiedUsers, usersToEmailCopyText(verifiedUsers), queryParams))
  }
}
