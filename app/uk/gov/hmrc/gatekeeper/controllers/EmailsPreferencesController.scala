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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}

import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models.APIAccessType.{PRIVATE, PUBLIC}
import uk.gov.hmrc.gatekeeper.models.CombinedApiCategory.toAPICategory
import uk.gov.hmrc.gatekeeper.models.EmailPreferencesChoice._
import uk.gov.hmrc.gatekeeper.models.Forms._
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.services.{ApiDefinitionService, ApmService, ApplicationService, DeveloperService}
import uk.gov.hmrc.gatekeeper.utils.{ErrorHelper, UserFunctionsWrapper}
import uk.gov.hmrc.gatekeeper.views.html.emails._
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

@Singleton
class EmailsPreferencesController @Inject() (
    developerService: DeveloperService,
    apiDefinitionService: ApiDefinitionService,
    emailsAllUsersNewView: EmailAllUsersNewView,
    emailInformationNewView: EmailInformationNewView,
    emailPreferencesChoiceNewView: EmailPreferencesChoiceNewView,
    emailPreferencesSpecificApiNewView: EmailPreferencesSpecificApiNewView,
    emailPreferencesSelectApiNewView: EmailPreferencesSelectApiNewView,
    emailPreferencesSelectTopicView: EmailPreferencesSelectTopicView,
    emailPreferencesSelectedTopicView: EmailPreferencesSelectedTopicView,
    emailPreferencesSelectTaxRegimeView: EmailPreferencesSelectTaxRegimeView,
    emailPreferencesSelectedTaxRegimeView: EmailPreferencesSelectedTaxRegimeView,
    emailPreferencesSelectedUserTaxRegimeView: EmailPreferencesSelectedUserTaxRegimeView,
    emailPreferencesSelectUserTopicView: EmailPreferencesSelectUserTopicView,
    emailPreferencesSelectedUserTopicView: EmailPreferencesSelectedUserTopicView,
    emailPreferencesSelectSubscribedApiView: EmailPreferencesSelectSubscribedApiView,
    emailPreferencesSubscribedApiView: EmailPreferencesSubscribedApiView,
    emailPreferencesSelectedSubscribedApiView: EmailPreferencesSelectedSubscribedApiView,
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

  def emailPreferencesChoice(): Action[AnyContent] = anyStrideUserAction { implicit request =>
    Future.successful(Ok(emailPreferencesChoiceNewView()))
  }

  def chooseEmailPreferences(): Action[AnyContent] = anyStrideUserAction { implicit request =>
    def handleValidForm(form: SendEmailPreferencesChoice): Future[Result] = {
      form.sendEmailPreferences match {
        case SPECIFIC_API => Future.successful(Redirect(routes.EmailsPreferencesController.selectSpecificApi(None, None)))
        case TAX_REGIME   => Future.successful(Redirect(routes.EmailsPreferencesController.selectTaxRegime(None)))
        case TOPIC        => Future.successful(Redirect(routes.EmailsPreferencesController.selectUserTopicPage(None)))
      }
    }

    def handleInvalidForm(formWithErrors: Form[SendEmailPreferencesChoice]) =
      Future.successful(BadRequest(emailPreferencesChoiceNewView()))

    SendEmailPrefencesChoiceForm.form.bindFromRequest().fold(handleInvalidForm, handleValidForm)
  }

  def selectSpecificApi(selectedAPIs: Option[List[String]], selectedTopic: Option[String] = None): Action[AnyContent] = anyStrideUserAction { implicit request =>
    for {
      apis         <- apmService.fetchAllCombinedApis()
      selectedApis <- Future.successful(filterSelectedApis(selectedAPIs, apis))
    } yield Ok(emailPreferencesSelectApiNewView(apis.sortBy(_.displayName), selectedApis.sortBy(_.displayName), selectedTopic))
  }

  def selectSubscribedApiPage(selectedAPIs: Option[List[String]]): Action[AnyContent] = anyStrideUserAction { implicit request =>
    for {
      apis         <- apmService.fetchAllCombinedApis()
      selectedApis <- Future.successful(filterSelectedApis(selectedAPIs, apis))
    } yield Ok(emailPreferencesSelectSubscribedApiView(apis.sortBy(_.displayName), selectedApis.sortBy(_.displayName)))
  }

  def selectTopicPage(selectedAPIs: Option[List[String]], selectedTopic: Option[String]): Action[AnyContent] = anyStrideUserAction { implicit request =>
    Future.successful(Ok(emailPreferencesSelectTopicView(selectedAPIs.get, selectedTopic.map(TopicOptionChoice.withName))))
  }

  def addAnotherApiOption(selectOption: String, selectedAPIs: Option[List[String]], selectedTopic: Option[String]): Action[AnyContent] = anyStrideUserAction { _ =>
    selectOption.toUpperCase match {
      case "YES" => Future.successful(Redirect(routes.EmailsPreferencesController.selectSpecificApi(selectedAPIs, selectedTopic)))
      case _     => Future.successful(Redirect(routes.EmailsPreferencesController.selectTopicPage(selectedAPIs, selectedTopic)))
    }
  }

  def addAnotherSubscribedApiOption(selectOption: String, selectedAPIs: Option[List[String]], offset: Int, limit: Int): Action[AnyContent] =
    anyStrideUserAction { _ =>
      selectOption.toUpperCase match {
        case "YES" => Future.successful(Redirect(routes.EmailsPreferencesController.selectSubscribedApiPage(selectedAPIs)))
        case _     => Future.successful(Redirect(routes.EmailsPreferencesController.selectedSubscribedApi(selectedAPIs.getOrElse(List.empty), offset, limit)))
      }
    }

  def addAnotherTaxRegimeOption(selectOption: String, selectedCategories: Option[List[String]], offset: Int, limit: Int): Action[AnyContent] =
    anyStrideUserAction { _ =>
      selectOption.toUpperCase match {
        case "YES" => Future.successful(Redirect(routes.EmailsPreferencesController.selectTaxRegime(selectedCategories)))
        case _     => Future.successful(Redirect(routes.EmailsPreferencesController.selectedUserTaxRegime(selectedCategories, offset, limit)))
      }
    }

  def selectedUserTaxRegime(selectedCategories: Option[List[String]], offset: Int, limit: Int): Action[AnyContent] = {
    anyStrideUserAction { implicit request =>
      for {
        categories                <- apiDefinitionService.apiCategories()
        selectedApiCategories      = filterSelectedApiCategories(selectedCategories, categories)
        userPaginatedResponse     <- developerService.fetchDevelopersBySpecificTaxRegimesEmailPreferencesPaginated(selectedApiCategories, offset, limit)
        totalCount                 = userPaginatedResponse.totalCount
        users                      = userPaginatedResponse.users.filter(_.verified)
        selectedApiCategoryDetails = filterSelectedCategories(selectedCategories, categories)
        usersAsJson                = Json.toJson(users)
      } yield Ok(emailPreferencesSelectedUserTaxRegimeView(
        users,
        usersAsJson,
        usersToEmailCopyText(users),
        selectedApiCategoryDetails,
        offset,
        limit,
        totalCount
      ))
    }
  }

  def selectUserTopicPage(selectedTopic: Option[String]): Action[AnyContent] = anyStrideUserAction { implicit request =>
    Future.successful(Ok(emailPreferencesSelectUserTopicView(selectedTopic.map(TopicOptionChoice.withName))))
  }

  def selectedUserTopic(selectedTopic: Option[String], offset: Int, limit: Int): Action[AnyContent] =
    anyStrideUserAction { implicit request =>
      val maybeTopic = selectedTopic.map(TopicOptionChoice.withName)
      for {
        userPaginatedResult <- developerService.fetchDevelopersByEmailPreferencesPaginated(maybeTopic, offset = offset, limit = limit)
        totalCount           = userPaginatedResult.totalCount
        filteredUsers        = userPaginatedResult.users.filter(_.verified)
        filteredUsersAsJson  = Json.toJson(filteredUsers)
      } yield Ok(emailPreferencesSelectedUserTopicView(filteredUsers, filteredUsersAsJson, usersToEmailCopyText(filteredUsers), maybeTopic, offset, limit, totalCount))
    }

  private def filterSelectedApiCategories(maybeSelectedCategories: Option[List[String]], categories: List[APICategoryDetails]) =
    maybeSelectedCategories.fold(List.empty[APICategory])(selectedCategories =>
      categories.filter(category => selectedCategories.contains(category.category)).map(cat => cat.toAPICategory)
    )

  private def filterSelectedCategories(maybeSelectedCategories: Option[List[String]], categories: List[APICategoryDetails]) =
    maybeSelectedCategories.fold(List.empty[APICategoryDetails])(selectedCategories =>
      categories.filter(category => selectedCategories.contains(category.category))
    )

  private def filterSelectedApis(maybeSelectedAPIs: Option[List[String]], apiList: List[CombinedApi]) =
    maybeSelectedAPIs.fold(List.empty[CombinedApi])(selectedAPIs => apiList.filter(api => selectedAPIs.contains(api.serviceName)))

  private def handleGettingApiUsers(
      apis: List[CombinedApi],
      selectedTopic: Option[TopicOptionChoice.Value],
      apiAccessType: APIAccessType
    )(implicit hc: HeaderCarrier
    ): Future[List[RegisteredUser]] = {
    // APSR-1418 - the accesstype inside combined api is option as a temporary measure until APM version which conatins the change to
    // return this is deployed out to all environments
    val filteredApis = apis.filter(_.accessType.getOrElse(APIAccessType.PUBLIC) == apiAccessType)
    val categories   = filteredApis.flatMap(_.categories.map(toAPICategory))
    val apiNames     = filteredApis.map(_.serviceName)
    selectedTopic.fold(Future.successful(List.empty[RegisteredUser]))(topic => {
      (apiAccessType, filteredApis) match {
        case (_, Nil)     => successful(List.empty[RegisteredUser])
        case (PUBLIC, _)  =>
          developerService.fetchDevelopersBySpecificAPIEmailPreferences(topic, categories, apiNames, privateApiMatch = false).map(_.filter(_.verified))
        case (PRIVATE, _) =>
          developerService.fetchDevelopersBySpecificAPIEmailPreferences(topic, categories, apiNames, privateApiMatch = true).map(_.filter(_.verified))
      }
    })
  }

  def specificApis(selectedAPIs: List[String], selectedTopicStr: Option[String] = None): Action[AnyContent] = anyStrideUserAction { implicit request =>
    val selectedTopic: Option[TopicOptionChoice.Value] = selectedTopicStr.map(TopicOptionChoice.withName)
    if (selectedAPIs.forall(_.isEmpty)) {
      Future.successful(Redirect(routes.EmailsPreferencesController.selectSpecificApi(None, selectedTopicStr)))
    } else {
      for {
        apis         <- apmService.fetchAllCombinedApis()
        filteredApis  = filterSelectedApis(Some(selectedAPIs), apis).sortBy(_.displayName)
        publicUsers  <- handleGettingApiUsers(filteredApis, selectedTopic, PUBLIC)
        privateUsers <- handleGettingApiUsers(filteredApis, selectedTopic, PRIVATE)
        combinedUsers = publicUsers ++ privateUsers
        usersAsJson   = Json.toJson(combinedUsers)
      } yield Ok(emailPreferencesSpecificApiNewView(combinedUsers, usersAsJson, usersToEmailCopyText(combinedUsers), filteredApis, selectedTopic))
    }
  }

  def subscribedApis(selectedAPIs: List[String]): Action[AnyContent] = anyStrideUserAction { implicit request =>
    if (selectedAPIs.forall(_.isEmpty)) {
      Future.successful(Redirect(routes.EmailsPreferencesController.selectSubscribedApiPage(None)))
    } else {
      for {
        apis        <- apmService.fetchAllCombinedApis()
        filteredApis = filterSelectedApis(Some(selectedAPIs), apis).sortBy(_.displayName)
      } yield Ok(emailPreferencesSubscribedApiView(filteredApis))
    }
  }

  def selectedApiTopic(
      selectedTopic: Option[String] = None,
      selectedCategory: Option[String] = None,
      selectedAPIs: List[String] = List.empty,
      offset: Int,
      limit: Int
    ): Action[AnyContent] =
    anyStrideUserAction { implicit request =>
      val topicAndCategory: Option[(TopicOptionChoice.Value, String)] =
        for {
          topic    <- selectedTopic.map(TopicOptionChoice.withName)
          category <- selectedCategory.filter(_.nonEmpty).orElse(Some(""))
        } yield (topic, category)
      for {
        apis                <- apmService.fetchAllCombinedApis()
        filteredApis         = filterSelectedApis(Some(selectedAPIs), apis).sortBy(_.displayName)
        categories          <- apiDefinitionService.apiCategories()
        userPaginatedResult <- topicAndCategory.map(tup =>
                                 developerService.fetchDevelopersByEmailPreferencesPaginated(Some(tup._1), Some(selectedAPIs), None, privateApiMatch = false, offset, limit)
                               )
                                 .getOrElse(Future.successful(UserPaginatedResponse(0, List.empty)))
        totalCount           = userPaginatedResult.totalCount
        users                = userPaginatedResult.users.filter(_.verified)
        usersAsJson          = Json.toJson(users)
        selectedCategories   = categories.filter(category => category.category == topicAndCategory.map(_._2).getOrElse(""))
        selectedCategoryName = if (selectedCategories.nonEmpty) selectedCategories.head.name else ""
      } yield Ok(emailPreferencesSelectedTopicView(
        users,
        usersAsJson,
        usersToEmailCopyText(users),
        topicAndCategory.map(_._1),
        categories,
        selectedCategory.getOrElse(""),
        selectedCategoryName,
        filteredApis,
        offset,
        limit,
        totalCount
      ))
    }

  def selectedSubscribedApi(selectedAPIs: List[String] = List.empty, offset: Int, limit: Int): Action[AnyContent] =
    anyStrideUserAction { implicit request =>
      for {
        apis                <- apmService.fetchAllCombinedApis()
        filteredApis         = filterSelectedApis(Some(selectedAPIs), apis).sortBy(_.displayName)
        userPaginatedResult <- developerService.fetchDevelopersBySpecificApisEmailPreferences(selectedAPIs, offset, limit)
        totalCount           = userPaginatedResult.totalCount
        users                = userPaginatedResult.users.filter(_.verified)
        usersAsJson          = Json.toJson(users)
      } yield Ok(emailPreferencesSelectedSubscribedApiView(users, usersAsJson, usersToEmailCopyText(users), filteredApis, offset, limit, totalCount))
    }

  def selectTaxRegime(previouslySelectedCategories: Option[List[String]] = None): Action[AnyContent] = anyStrideUserAction { implicit request =>
    for {
      categories        <- apiDefinitionService.apiCategories()
      selectedCategories = categories.filter(c => previouslySelectedCategories.exists(categories => categories.contains(c.category)))
    } yield Ok(emailPreferencesSelectTaxRegimeView(categories, selectedCategories))
  }

  def selectedTaxRegime(selectedCategories: List[String], selectedTopicStr: Option[String] = None): Action[AnyContent] = anyStrideUserAction { implicit request =>
    val selectedTopic: Option[TopicOptionChoice.Value] = selectedTopicStr.map(TopicOptionChoice.withName)
    for {
      categories        <- apiDefinitionService.apiCategories()
      filteredCategories = filterSelectedCategories(Some(selectedCategories), categories).sortBy(_.name)
    } yield Ok(emailPreferencesSelectedTaxRegimeView(filteredCategories, selectedTopic))
  }

  def showEmailInformation(emailChoice: String): Action[AnyContent] = anyStrideUserAction { implicit request =>
    emailChoice match {
      case "all-users"        => Future.successful(Ok(emailInformationNewView(EmailOptionChoice.EMAIL_ALL_USERS)))
      case "api-subscription" => Future.successful(Ok(emailInformationNewView(EmailOptionChoice.API_SUBSCRIPTION)))
      case _                  => Future.failed(new NotFoundException("Page Not Found"))
    }
  }

  def emailAllUsersPage(offset: Int, limit: Int): Action[AnyContent] = anyStrideUserAction { implicit request =>
    for {
      result       <- developerService.fetchUsersPaginated(offset, limit)
      filteredUsers = result.users.filter(_.verified)
      usersAsJson   = Json.toJson(filteredUsers)
      totalCount    = result.totalCount
    } yield Ok(emailsAllUsersNewView(filteredUsers, usersAsJson, usersToEmailCopyText(filteredUsers), offset, limit, totalCount))
  }
}
