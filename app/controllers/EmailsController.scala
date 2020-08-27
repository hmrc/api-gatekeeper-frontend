/*
 * Copyright 2020 HM Revenue & Customs
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
import connectors.AuthConnector
import javax.inject.{Inject, Singleton}
import model.DeveloperStatusFilter.VerifiedStatus
import model.EmailOptionChoice.{EMAIL_ALL_USERS, _}
import model.EmailPreferencesChoice.{SPECIFIC_API, TAX_REGIME, TOPIC}
import model.TopicOptionChoice.TopicOptionChoice
import model.{AnyEnvironment, ApiContextVersion, Developers2Filter, DropDownValue, EmailOptionChoice, GatekeeperRole, SendEmailChoice, SendEmailPreferencesChoice, TopicOptionChoice, User}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{ApiDefinitionService, ApplicationService, DeveloperService}
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{ActionBuilders, ErrorHelper, GatekeeperAuthWrapper, UserFunctionsWrapper}
import views.html.{ErrorTemplate, ForbiddenView}
import views.html.emails.{EmailAllUsersView, EmailApiSubscriptionsView, EmailInformationView, EmailPreferencesChoiceView, EmailPreferencesTopicView, SendEmailChoiceView}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailsController  @Inject()(developerService: DeveloperService,
                                  apiDefinitionService: ApiDefinitionService,
                                  sendEmailChoiceView: SendEmailChoiceView,
                                  emailInformationView: EmailInformationView,
                                  emailsAllUsersView: EmailAllUsersView,
                                  emailApiSubscriptionsView: EmailApiSubscriptionsView,
                                  emailPreferencesChoiceView: EmailPreferencesChoiceView,
                                  emailPreferencesTopicView: EmailPreferencesTopicView,
                                  val applicationService: ApplicationService,
                                  val forbiddenView: ForbiddenView,
                                  override val authConnector: AuthConnector,
                                  mcc: MessagesControllerComponents,
                                  override val errorTemplate: ErrorTemplate
                                 )(implicit val appConfig: AppConfig, val ec: ExecutionContext)
  extends FrontendController(mcc) with ErrorHelper with GatekeeperAuthWrapper with UserFunctionsWrapper with ActionBuilders with I18nSupport {

  def landing(): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      Future.successful(Ok(sendEmailChoiceView()))
  }

  def chooseEmailOption(): Action[AnyContent] = {
    requiresAtLeast(GatekeeperRole.USER) {
      implicit request => {
        def handleValidForm(form: SendEmailChoice): Future[Result] = {
            form.sendEmailChoice match {
              case EMAIL_PREFERENCES => Future.successful(Redirect(routes.EmailsController.emailPreferencesChoice()))
              case API_SUBSCRIPTION => Future.successful(Redirect(routes.EmailsController.showEmailInformation(emailChoice = "api-subscription")))
              case EMAIL_ALL_USERS => Future.successful(Redirect(routes.EmailsController.showEmailInformation(emailChoice = "all-users")))
            }
        }
        def handleInvalidForm(formWithErrors: Form[SendEmailChoice]) =
          Future.successful(BadRequest(sendEmailChoiceView()))
          SendEmailChoiceForm.form.bindFromRequest.fold(handleInvalidForm, handleValidForm)

      }
    }

  }

  def emailPreferencesChoice(): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      Future.successful(Ok(emailPreferencesChoiceView()))
  }

  def chooseEmailPreferences(): Action[AnyContent] = {
    requiresAtLeast(GatekeeperRole.USER) {
      implicit request => {
        def handleValidForm(form: SendEmailPreferencesChoice): Future[Result] = {
            form.sendEmailPreferences match {
              case SPECIFIC_API => Future.successful(Ok("1"))
              case TAX_REGIME =>  Future.successful(Ok("2"))
              case TOPIC =>  Future.successful(Redirect(routes.EmailsController.emailPreferencesTopic(None)))
            }
        }
        def handleInvalidForm(formWithErrors: Form[SendEmailPreferencesChoice]) =
          Future.successful(BadRequest(emailPreferencesChoiceView()))
          SendEmailPrefencesChoiceForm.form.bindFromRequest.fold(handleInvalidForm, handleValidForm)

      }
    }

  }

  def emailPreferencesTopic(maybeTopicFilter: Option[String] = None): Action[AnyContent] = {
    requiresAtLeast(GatekeeperRole.USER) {
      implicit request => {
        //withName could throw an exception here
        val maybeTopic = maybeTopicFilter.map(TopicOptionChoice.withName(_))
        Future.successful(Ok(emailPreferencesTopicView(Seq.empty, "", maybeTopic)))
      }
    }
  }

  def showEmailInformation(emailChoice: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      emailChoice match {
        case "all-users" => Future.successful(Ok(emailInformationView(EmailOptionChoice.EMAIL_ALL_USERS)))
        case "api-subscription" => Future.successful(Ok(emailInformationView(EmailOptionChoice.API_SUBSCRIPTION)))
        case _ => Future.failed(new NotFoundException("Page Not Found"))
      }
  }

  def emailAllUsersPage(): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      developerService.fetchUsers
        .map((users :Seq[User]) => {val filteredUsers = users.filter((u:User) => u.verified.contains(true))
          Ok(emailsAllUsersView(filteredUsers, usersToEmailCopyText(filteredUsers)))
        })
  }

  def emailApiSubscribersPage(maybeApiVersionFilter: Option[String] = None): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      val queryParams = getQueryParametersAsKeyValues(request)
      val apiDropDowns: Future[Seq[DropDownValue]] =  for {
        apiVersions  <- apiDefinitionService.fetchAllApiDefinitions()
        apiDropDowns <- Future.successful(getApiVersionsDropDownValues(apiVersions))
      } yield apiDropDowns

      val filter = Developers2Filter(None, ApiContextVersion(mapEmptyStringToNone(maybeApiVersionFilter)), AnyEnvironment, VerifiedStatus)
      val fetchedUsers = mapEmptyStringToNone(maybeApiVersionFilter).fold(Future.successful(Seq.empty[User]))(_=>developerService.searchDevelopers(filter))
          for{
            userList <- fetchedUsers
            apis     <- apiDropDowns
          } yield Ok(emailApiSubscriptionsView(apis, userList, usersToEmailCopyText(userList), queryParams))
  }

}
