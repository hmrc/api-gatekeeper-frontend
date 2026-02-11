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
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.gatekeeper.config.AppConfig
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

  def emailPreferencesChoice(): Action[AnyContent] = atLeastAdvancedUserAction { implicit request =>
    Future.successful(Ok(emailPreferencesChoiceNewView()))
  }

  def chooseEmailPreferences(): Action[AnyContent] = atLeastAdvancedUserAction { implicit request =>
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

  def selectSpecificApi(selectedAPIs: Option[List[String]], selectedTopic: Option[TopicOptionChoice] = None): Action[AnyContent] = atLeastAdvancedUserAction { implicit request =>
    for {
      apis         <- apmService.fetchAllCombinedApis()
      selectedApis <- Future.successful(filterSelectedApis(selectedAPIs, apis))
    } yield Ok(emailPreferencesSelectApiNewView(apis.sortBy(_.displayName), selectedApis.sortBy(_.displayName), selectedTopic))
  }

  def selectSubscribedApiPage(selectedAPIs: Option[List[String]]): Action[AnyContent] = atLeastAdvancedUserAction { implicit request =>
    for {
      apis         <- apmService.fetchAllCombinedApis()
      selectedApis <- Future.successful(filterSelectedApis(selectedAPIs, apis))
    } yield Ok(emailPreferencesSelectSubscribedApiView(apis.sortBy(_.displayName), selectedApis.sortBy(_.displayName)))
  }

  def selectTopicPage(selectedAPIs: Option[List[String]], selectedTopic: Option[TopicOptionChoice]): Action[AnyContent] = atLeastAdvancedUserAction { implicit request =>
    Future.successful(Ok(emailPreferencesSelectTopicView(selectedAPIs.get, selectedTopic)))
  }

  def addAnotherApiOption(selectOption: String, selectedAPIs: Option[List[String]], selectedTopic: Option[TopicOptionChoice]): Action[AnyContent] = atLeastAdvancedUserAction { _ =>
    selectOption.toUpperCase match {
      case "YES" => Future.successful(Redirect(routes.EmailsPreferencesController.selectSpecificApi(selectedAPIs, selectedTopic)))
      case _     => Future.successful(Redirect(routes.EmailsPreferencesController.selectTopicPage(selectedAPIs, selectedTopic)))
    }
  }

  def addAnotherSubscribedApiOption(selectOption: String, selectedAPIs: Option[List[String]], offset: Int, limit: Int): Action[AnyContent] =
    atLeastAdvancedUserAction { _ =>
      selectOption.toUpperCase match {
        case "YES" => Future.successful(Redirect(routes.EmailsPreferencesController.selectSubscribedApiPage(selectedAPIs)))
        case _     => Future.successful(Redirect(routes.EmailsPreferencesController.selectedSubscribedApi(selectedAPIs.getOrElse(List.empty), offset, limit)))
      }
    }

  def addAnotherTaxRegimeOption(selectOption: String, selectedCategories: Option[Set[ApiCategory]], offset: Int, limit: Int): Action[AnyContent] =
    atLeastAdvancedUserAction { _ =>
      selectOption.toUpperCase match {
        case "YES" => Future.successful(Redirect(routes.EmailsPreferencesController.selectTaxRegime(selectedCategories)))
        case _     => Future.successful(Redirect(routes.EmailsPreferencesController.selectedUserTaxRegime(selectedCategories, offset, limit)))
      }
    }

  def selectedUserTaxRegime(maybeSelectedCategories: Option[Set[ApiCategory]], offset: Int, limit: Int): Action[AnyContent] = {
    val selectedCategories = maybeSelectedCategories.getOrElse(Set.empty)
    atLeastAdvancedUserAction { implicit request =>
      for {
        userPaginatedResponse <- developerService.fetchDevelopersBySpecificTaxRegimesEmailPreferencesPaginated(selectedCategories, offset, limit)
        totalCount             = userPaginatedResponse.totalCount
        users                  = userPaginatedResponse.users.filter(_.verified)
      } yield Ok(emailPreferencesSelectedUserTaxRegimeView(
        users,
        usersToEmailCopyText(users),
        selectedCategories,
        offset,
        limit,
        totalCount
      ))
    }
  }

  def selectUserTopicPage(selectedTopic: Option[TopicOptionChoice]): Action[AnyContent] = atLeastAdvancedUserAction { implicit request =>
    Future.successful(Ok(emailPreferencesSelectUserTopicView(selectedTopic)))
  }

  def selectedUserTopic(maybeTopic: Option[TopicOptionChoice], offset: Int, limit: Int): Action[AnyContent] =
    atLeastAdvancedUserAction { implicit request =>
      for {
        userPaginatedResult <- developerService.fetchDevelopersByEmailPreferencesPaginated(maybeTopic, offset = offset, limit = limit)
        totalCount           = userPaginatedResult.totalCount
        filteredUsers        = userPaginatedResult.users.filter(_.verified)
      } yield Ok(emailPreferencesSelectedUserTopicView(filteredUsers, usersToEmailCopyText(filteredUsers), maybeTopic, offset, limit, totalCount))
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

  def specificApis(selectedAPIs: List[String], selectedTopic: Option[TopicOptionChoice] = None): Action[AnyContent] = atLeastAdvancedUserAction { implicit request =>
    if (selectedAPIs.forall(_.isEmpty)) {
      Future.successful(Redirect(routes.EmailsPreferencesController.selectSpecificApi(None, selectedTopic)))
    } else {
      for {
        apis         <- apmService.fetchAllCombinedApis()
        filteredApis  = filterSelectedApis(Some(selectedAPIs), apis).sortBy(_.displayName)
        publicUsers  <- handleGettingApiUsers(filteredApis, selectedTopic, ApiAccessType.PUBLIC)
        privateUsers <- handleGettingApiUsers(filteredApis, selectedTopic, ApiAccessType.PRIVATE)
        combinedUsers = publicUsers ++ privateUsers
      } yield Ok(emailPreferencesSpecificApiNewView(combinedUsers, usersToEmailCopyText(combinedUsers), filteredApis, selectedTopic))
    }
  }

  def subscribedApis(selectedAPIs: List[String]): Action[AnyContent] = atLeastAdvancedUserAction { implicit request =>
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
      selectedTopic: Option[TopicOptionChoice] = None,
      maybeSelectedCategory: Option[ApiCategory] = None,
      selectedAPIs: List[String] = List.empty,
      offset: Int,
      limit: Int
    ): Action[AnyContent] =
    atLeastAdvancedUserAction { implicit request =>
      for {
        apis                <- apmService.fetchAllCombinedApis()
        filteredApis         = filterSelectedApis(Some(selectedAPIs), apis).sortBy(_.displayName)
        userPaginatedResult <- selectedTopic.map(t =>
                                 developerService.fetchDevelopersByEmailPreferencesPaginated(Some(t), Some(selectedAPIs), None, privateApiMatch = false, offset, limit)
                               )
                                 .getOrElse(Future.successful(UserPaginatedResponse(0, List.empty)))
        totalCount           = userPaginatedResult.totalCount
        users                = userPaginatedResult.users.filter(_.verified)
      } yield Ok(emailPreferencesSelectedTopicView(
        users,
        usersToEmailCopyText(users),
        selectedTopic,
        maybeSelectedCategory,
        maybeSelectedCategory.map(_.displayText).getOrElse(""),
        filteredApis,
        offset,
        limit,
        totalCount
      ))
    }

  def selectedSubscribedApi(selectedAPIs: List[String] = List.empty, offset: Int, limit: Int): Action[AnyContent] =
    atLeastAdvancedUserAction { implicit request =>
      for {
        apis                <- apmService.fetchAllCombinedApis()
        filteredApis         = filterSelectedApis(Some(selectedAPIs), apis).sortBy(_.displayName)
        userPaginatedResult <- developerService.fetchDevelopersBySpecificApisEmailPreferences(selectedAPIs, offset, limit)
        totalCount           = userPaginatedResult.totalCount
        users                = userPaginatedResult.users.filter(_.verified)
      } yield Ok(emailPreferencesSelectedSubscribedApiView(users, usersToEmailCopyText(users), filteredApis, offset, limit, totalCount))
    }

  def selectTaxRegime(maybePreviouslySelectedCategories: Option[Set[ApiCategory]]): Action[AnyContent] = atLeastAdvancedUserAction { implicit request =>
    successful(Ok(emailPreferencesSelectTaxRegimeView(maybePreviouslySelectedCategories.getOrElse(Set.empty))))
  }

  def selectedTaxRegime(selectedCategories: Set[ApiCategory], selectedTopic: Option[TopicOptionChoice] = None): Action[AnyContent] = atLeastAdvancedUserAction { implicit request =>
    successful(Ok(emailPreferencesSelectedTaxRegimeView(selectedCategories, selectedTopic)))
  }

  def showEmailInformation(emailChoice: String): Action[AnyContent] = atLeastAdvancedUserAction { implicit request =>
    emailChoice match {
      case "all-users"        => Future.successful(Ok(emailInformationNewView(EmailOptionChoice.EMAIL_ALL_USERS)))
      case "api-subscription" => Future.successful(Ok(emailInformationNewView(EmailOptionChoice.API_SUBSCRIPTION)))
      case _                  => Future.failed(new NotFoundException("Page Not Found"))
    }
  }

  def emailAllUsersPage(offset: Int, limit: Int): Action[AnyContent] = atLeastAdvancedUserAction { implicit request =>
    for {
      result       <- developerService.fetchUsersPaginated(offset, limit)
      filteredUsers = result.users.filter(_.verified)
      totalCount    = result.totalCount
    } yield Ok(emailsAllUsersNewView(filteredUsers, usersToEmailCopyText(filteredUsers), offset, limit, totalCount))
  }
}
