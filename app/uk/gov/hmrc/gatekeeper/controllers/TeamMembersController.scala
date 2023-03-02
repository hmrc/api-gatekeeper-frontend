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

import cats.data.NonEmptyList

import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.CommandFailures
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInRequest
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.gatekeeper.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.gatekeeper.controllers.actions.ActionBuilders
import uk.gov.hmrc.gatekeeper.models.Forms._
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.services.{ApmService, ApplicationService, DeveloperService}
import uk.gov.hmrc.gatekeeper.utils.ErrorHelper
import uk.gov.hmrc.gatekeeper.views.html.applications._
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

trait WithRestrictedApp {
  self: TeamMembersController =>

  def withRestrictedApp(appId: ApplicationId)(f: ApplicationWithHistory => Future[Result])(implicit request: LoggedInRequest[_], ec: ExecutionContext, hc: HeaderCarrier) = {
    withApp(appId) { app =>
      app.application.access match {
        case _: Standard                   => f(app)
        case _ if request.role.isSuperUser => f(app)
        case _                             => successful(Forbidden(forbiddenView()))
      }
    }
  }
}

@Singleton
class TeamMembersController @Inject() (
    developerService: DeveloperService,
    mcc: MessagesControllerComponents,
    manageTeamMembersView: ManageTeamMembersView,
    addTeamMemberView: AddTeamMemberView,
    removeTeamMemberView: RemoveTeamMemberView,
    val applicationService: ApplicationService,
    val apmService: ApmService,
    val errorTemplate: ErrorTemplate,
    val forbiddenView: ForbiddenView,
    val errorHandler: ErrorHandler,
    strideAuthorisationService: StrideAuthorisationService
  )(implicit val appConfig: AppConfig,
    override val ec: ExecutionContext
  ) extends GatekeeperBaseController(strideAuthorisationService, mcc)
    with ErrorHelper
    with ActionBuilders
    with WithRestrictedApp {

  def manageTeamMembers(appId: ApplicationId): Action[AnyContent] = anyStrideUserAction { implicit request =>
    withRestrictedApp(appId) { app =>
      successful(Ok(manageTeamMembersView(app.application)))
    }
  }

  def addTeamMember(appId: ApplicationId): Action[AnyContent] = anyStrideUserAction { implicit request =>
    withRestrictedApp(appId) { app =>
      successful(Ok(addTeamMemberView(app.application, AddTeamMemberForm.form)))
    }
  }

  // def worked(successResult: Result): PartialFunction[Either[List[CommandFailure],T], Unit] = {
  //   case Right(()) => successResult
  // }

  // val otherwiseThrow: PartialFunction[Either[List[CommandFailure],_], Result] = {
  //   case _ => throw new RuntimeException("Bang")
  // }

  // def failOnCollaborAlreadyExists(failureResult: Result): PartialFunction[Either[List[CommandFailure],_], Result] = {
  //   case Left(List(CommandFailures.CollaboratorAlreadyExistsOnApp)) => failureResult
  // }
  // e => worked(successResult) orElse failOnCollaborAlreadyExists(failureResult) orElse otherwiseThrow }

  def addTeamMemberAction(appId: ApplicationId): Action[AnyContent] = anyStrideUserAction(implicit request =>
    withRestrictedApp(appId) { app =>
      def handleValidForm(form: AddTeamMemberForm) = {
        val emailAddress       = LaxEmailAddress(form.email)
        lazy val successResult = Redirect(routes.TeamMembersController.manageTeamMembers(appId))
        lazy val failureResult = BadRequest(addTeamMemberView(
          app.application,
          AddTeamMemberForm.form.fill(form).withError("email", messagesApi.preferred(request)("team.member.error.email.already.exists"))
        ))

        for {
          user        <- developerService.fetchOrCreateUser(emailAddress)
          role         = CollaboratorRole.from(form.role).getOrElse(CollaboratorRole.DEVELOPER)
          collaborator = if (role == CollaboratorRole.DEVELOPER) Collaborators.Developer(user.userId, emailAddress) else Collaborators.Administrator(user.userId, emailAddress)
          result      <- applicationService.addTeamMember(app.application, collaborator, loggedIn.userFullName.get)
                           .map {
                             case Right(())                                                               => successResult
                             case Left(NonEmptyList(CommandFailures.CollaboratorAlreadyExistsOnApp, Nil)) => failureResult
                             case _                                                                       => InternalServerError("Action failed")
                           }
        } yield result
      }

      def handleInvalidForm(formWithErrors: Form[AddTeamMemberForm]) =
        successful(BadRequest(addTeamMemberView(app.application, formWithErrors)))

      AddTeamMemberForm.form.bindFromRequest.fold(handleInvalidForm, handleValidForm)
    }
  )

  def removeTeamMember(appId: ApplicationId): Action[AnyContent] = anyStrideUserAction { implicit request =>
    withRestrictedApp(appId) { app =>
      def handleValidForm(form: RemoveTeamMemberForm) =
        successful(Ok(removeTeamMemberView(app.application, RemoveTeamMemberConfirmationForm.form, form.email)))

      def handleInvalidForm(formWithErrors: Form[RemoveTeamMemberForm]) = {
        val email = formWithErrors("email").value.getOrElse("")
        successful(BadRequest(removeTeamMemberView(app.application, RemoveTeamMemberConfirmationForm.form.fillAndValidate(RemoveTeamMemberConfirmationForm(email)), email)))
      }

      RemoveTeamMemberForm.form.bindFromRequest.fold(handleInvalidForm, handleValidForm)
    }
  }

  def removeTeamMemberAction(appId: ApplicationId): Action[AnyContent] = anyStrideUserAction { implicit request =>
    withRestrictedApp(appId) { app =>
      def handleValidForm(form: RemoveTeamMemberConfirmationForm): Future[Result] = {
        val emailAddress       = LaxEmailAddress(form.email)
        lazy val successResult = Redirect(routes.TeamMembersController.manageTeamMembers(appId))
        lazy val failureResult = BadRequest(removeTeamMemberView(
          app.application,
          RemoveTeamMemberConfirmationForm.form.fill(form).withError("email", messagesApi.preferred(request)("team.member.error.email.last.admin")),
          form.email
        ))

        form.confirm match {
          case Some("Yes") => applicationService.removeTeamMember(app.application, emailAddress, loggedIn.userFullName.get).map {
              case Right(())                                                      => successResult
              case Left(NonEmptyList(CommandFailures.CannotRemoveLastAdmin, Nil)) => failureResult
              case _                                                              => throw new RuntimeException("Bang")
            }
          case _           => successful(successResult)
        }
      }

      def handleInvalidForm(formWithErrors: Form[RemoveTeamMemberConfirmationForm]) =
        successful(BadRequest(removeTeamMemberView(app.application, formWithErrors, formWithErrors("email").value.getOrElse(""))))

      RemoveTeamMemberConfirmationForm.form.bindFromRequest.fold(handleInvalidForm, handleValidForm)
    }
  }
}
