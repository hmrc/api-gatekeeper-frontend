/*
 * Copyright 2025 HM Revenue & Customs
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
import scala.concurrent.{ExecutionContext, Future}

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation._
import play.api.mvc.MessagesControllerComponents

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{LoginRedirectUri, PostLogoutRedirectUri}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.GatekeeperAuthorisationActions
import uk.gov.hmrc.apiplatform.modules.gkauth.services._
import uk.gov.hmrc.gatekeeper.config.ErrorHandler
import uk.gov.hmrc.gatekeeper.controllers.RedirectUrisController.{LoginRedirectUriForm, PostLogoutRedirectUriForm}
import uk.gov.hmrc.gatekeeper.controllers.actions.ActionBuilders
import uk.gov.hmrc.gatekeeper.services._
import uk.gov.hmrc.gatekeeper.utils.ErrorHelper
import uk.gov.hmrc.gatekeeper.views.html._
import uk.gov.hmrc.gatekeeper.views.html.applications._

@Singleton
class RedirectUrisController @Inject() (
    strideAuthorisationService: StrideAuthorisationService,
    val applicationService: ApplicationService,
    mcc: MessagesControllerComponents,
    manageLoginRedirectUriView: ManageLoginRedirectUriView,
    managePostLogoutRedirectUriView: ManagePostLogoutRedirectUriView,
    override val errorTemplate: ErrorTemplate,
    val apmService: ApmService,
    val errorHandler: ErrorHandler,
    val ldapAuthorisationService: LdapAuthorisationService
  )(implicit override val ec: ExecutionContext
  ) extends GatekeeperBaseController(strideAuthorisationService, mcc)
    with GatekeeperAuthorisationActions
    with ErrorHelper
    with ActionBuilders
    with ApplicationLogger {

  def manageLoginRedirectUriPage(appId: ApplicationId) = atLeastSuperUserAction { implicit request =>
    withApp(appId) { app =>
      app.application.access match {
        case Access.Standard(redirects, _, _, _, _, _, _) =>
          Future.successful(Ok(manageLoginRedirectUriView(app.application, LoginRedirectUriForm.form.fill(LoginRedirectUriForm(redirects)))))
        case _                                            => Future.successful(NotFound)
      }

    }
  }

  def managePostLogoutRedirectUriPage(appId: ApplicationId) = atLeastSuperUserAction { implicit request =>
    withApp(appId) { app =>
      app.application.access match {
        case Access.Standard(_, redirects, _, _, _, _, _) =>
          Future.successful(Ok(managePostLogoutRedirectUriView(app.application, PostLogoutRedirectUriForm.form.fill(PostLogoutRedirectUriForm(redirects)))))
        case _                                            => Future.successful(NotFound)
      }

    }
  }

  def manageLoginRedirectUriAction(appId: ApplicationId) = atLeastSuperUserAction { implicit request =>
    withApp(appId) { app =>
      def handleValidForm(form: LoginRedirectUriForm) = {
        applicationService.manageLoginRedirectUris(app.application, form.redirectUris, loggedIn.userFullName.get).map { _ =>
          Redirect(routes.ApplicationController.applicationPage(appId))
        }
      }

      def handleFormError(form: Form[LoginRedirectUriForm]) = {
        Future.successful(BadRequest(manageLoginRedirectUriView(app.application, form)))
      }

      LoginRedirectUriForm.form.bindFromRequest().fold(handleFormError, handleValidForm)
    }
  }

  def managePostLogoutRedirectUriAction(appId: ApplicationId) = atLeastSuperUserAction { implicit request =>
    withApp(appId) { app =>
      def handleValidForm(form: LoginRedirectUriForm) = {
        applicationService.manageLoginRedirectUris(app.application, form.redirectUris, loggedIn.userFullName.get).map { _ =>
          Redirect(routes.ApplicationController.applicationPage(appId))
        }
      }

      def handleFormError(form: Form[LoginRedirectUriForm]) = {
        Future.successful(BadRequest(manageLoginRedirectUriView(app.application, form)))
      }

      LoginRedirectUriForm.form.bindFromRequest().fold(handleFormError, handleValidForm)
    }
  }
}

object RedirectUrisController {

  final case class LoginRedirectUriForm(redirectUris: List[LoginRedirectUri])
  final case class PostLogoutRedirectUriForm(redirectUris: List[PostLogoutRedirectUri])

  object LoginRedirectUriForm {

    def toForm(
        redirectUri1: Option[String],
        redirectUri2: Option[String],
        redirectUri3: Option[String],
        redirectUri4: Option[String],
        redirectUri5: Option[String]
      ): LoginRedirectUriForm = {
      val data = List(redirectUri1, redirectUri2, redirectUri3, redirectUri4, redirectUri5)
        .flatMap(_.map(LoginRedirectUri.apply))
        .collect { case Some(uri) => uri }
        .distinct
      LoginRedirectUriForm(data)
    }

    def fromForm(form: LoginRedirectUriForm): Option[(Option[String], Option[String], Option[String], Option[String], Option[String])] = {
      val data = form.redirectUris.map(_.toString())
      Some((data.headOption, data.lift(1), data.lift(2), data.lift(3), data.lift(4)))
    }

    def validateUri(uri: String): ValidationResult = {
      LoginRedirectUri(uri).fold[ValidationResult](Invalid(Seq(ValidationError("redirectUri.invalid", uri))))(_ => Valid)
    }

    val redirectUrisConstraint: Constraint[String] = Constraint({
      uri => validateUri(uri)
    })

    val form: Form[LoginRedirectUriForm] = Form(
      mapping(
        "redirectUri1" -> optional(text.verifying(redirectUrisConstraint)),
        "redirectUri2" -> optional(text.verifying(redirectUrisConstraint)),
        "redirectUri3" -> optional(text.verifying(redirectUrisConstraint)),
        "redirectUri4" -> optional(text.verifying(redirectUrisConstraint)),
        "redirectUri5" -> optional(text.verifying(redirectUrisConstraint))
      )(LoginRedirectUriForm.toForm)(LoginRedirectUriForm.fromForm)
    )
  }

  object PostLogoutRedirectUriForm {

    def toForm(
        redirectUri1: Option[String],
        redirectUri2: Option[String],
        redirectUri3: Option[String],
        redirectUri4: Option[String],
        redirectUri5: Option[String]
      ): PostLogoutRedirectUriForm = {
      val data = List(redirectUri1, redirectUri2, redirectUri3, redirectUri4, redirectUri5)
        .flatMap(_.map(PostLogoutRedirectUri.apply))
        .collect { case Some(uri) => uri }
        .distinct
      PostLogoutRedirectUriForm(data)
    }

    def fromForm(form: PostLogoutRedirectUriForm): Option[(Option[String], Option[String], Option[String], Option[String], Option[String])] = {
      val data = form.redirectUris.map(_.toString())
      Some((data.headOption, data.lift(1), data.lift(2), data.lift(3), data.lift(4)))
    }

    def validateUri(uri: String): ValidationResult = {
      PostLogoutRedirectUri(uri).fold[ValidationResult](Invalid(Seq(ValidationError("redirectUri.invalid", uri))))(_ => Valid)
    }

    val redirectUrisConstraint: Constraint[String] = Constraint({
      uri => validateUri(uri)
    })

    val form: Form[PostLogoutRedirectUriForm] = Form(
      mapping(
        "redirectUri1" -> optional(text.verifying(redirectUrisConstraint)),
        "redirectUri2" -> optional(text.verifying(redirectUrisConstraint)),
        "redirectUri3" -> optional(text.verifying(redirectUrisConstraint)),
        "redirectUri4" -> optional(text.verifying(redirectUrisConstraint)),
        "redirectUri5" -> optional(text.verifying(redirectUrisConstraint))
      )(PostLogoutRedirectUriForm.toForm)(PostLogoutRedirectUriForm.fromForm)
    )
  }
}
