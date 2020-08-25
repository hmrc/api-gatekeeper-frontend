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
import controllers.Assets.Ok
import javax.inject.{Inject, Singleton}
import model.DeveloperStatusFilter.VerifiedStatus
import model.EmailOptionChoice.{EMAIL_ALL_USERS, _}
import model.{APIDefinition, APIStatus, APIVersion, AnyEnvironment, ApiContextVersion, Developers2Filter, DropDownValue, EmailOptionChoice, GatekeeperRole, SendEmailChoice, User}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{ApiDefinitionService, ApplicationService, DeveloperService}
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{ActionBuilders, ErrorHelper, GatekeeperAuthWrapper}
import views.html.{ErrorTemplate, ForbiddenView}
import views.html.emails.{EmailAllUsersView, EmailApiSubscriptionsView, EmailInformationView, SendEmailChoiceView}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailsController  @Inject()(developerService: DeveloperService,
                                  apiDefinitionService: ApiDefinitionService,
                                  sendEmailChoiceView: SendEmailChoiceView,
                                  emailInformationView: EmailInformationView,
                                  emailsAllUsersView: EmailAllUsersView,
                                 emailApiSubscriptionsView: EmailApiSubscriptionsView,
                                  val applicationService: ApplicationService,
                                  val forbiddenView: ForbiddenView,
                                  override val authConnector: AuthConnector,
                                  mcc: MessagesControllerComponents,
                                  override val errorTemplate: ErrorTemplate
                                 )(implicit val appConfig: AppConfig, val ec: ExecutionContext)
  extends FrontendController(mcc) with ErrorHelper with GatekeeperAuthWrapper with ActionBuilders with I18nSupport {

  def landing(): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      Future.successful(Ok(sendEmailChoiceView()))
  }

  def chooseEmailOption(): Action[AnyContent] = {
    requiresAtLeast(GatekeeperRole.USER) {
      implicit request => {
        def handleValidForm(form: SendEmailChoice): Future[Result] = {
            form.sendEmailChoice match {
              case EMAIL_PREFERENCES => Future.successful(Ok("1"))
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
      val queryParams = request.queryString.map { case (k, v) => k -> v.mkString }
     val apiDropDowns: Future[Seq[DropDownValue]] =  for {
        apiVersions  <- apiDefinitionService.fetchAllApiDefinitions()
        apiDropDowns <- Future.successful(getApiVersionsDropDownValues(apiVersions))
      } yield apiDropDowns

      val filter = Developers2Filter(None, ApiContextVersion(mapEmptyStringToNone(maybeApiVersionFilter)), AnyEnvironment, VerifiedStatus)
      val fetchedUsers = mapEmptyStringToNone(maybeApiVersionFilter).fold(Future.successful(Seq.empty[User]))(_=>developerService.searchDevelopers(filter))
          for{
            userList <- fetchedUsers
            apis     <- apiDropDowns
          } yield Ok(emailApiSubscriptionsView(apis, userList ,"", queryParams))
  }

 private  def mapEmptyStringToNone(filter: Option[String]): Option[String] = {
    filter match {
      case None | Some("")  => None
      case _ => filter
    }
  }

 private def usersToEmailCopyText(users: Seq[User]): String = {
    users.map(_.email).mkString("; ")
  }

  private def getApiVersionsDropDownValues(apiDefinitions: Seq[APIDefinition]) = {
    def toKeyValue(api: APIDefinition, version: APIVersion) = {
      val value: String = ApiContextVersion(api.context, version.version).toStringValue.trim
      val displayedStatus: String = APIStatus.displayedStatus(version.status).trim
      val description: String = s"${api.name} (${version.version}) ($displayedStatus)"

      DropDownValue(value, description)
    }

    (for {
      api <- apiDefinitions
      version <- api.versions
    } yield toKeyValue(api, version))
      .distinct
      .sortBy(keyValue => keyValue.description)
  }

}
