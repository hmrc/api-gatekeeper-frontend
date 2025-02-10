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
import play.api.mvc.MessagesControllerComponents

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.GatekeeperAuthorisationActions
import uk.gov.hmrc.apiplatform.modules.gkauth.services._
import uk.gov.hmrc.gatekeeper.config.ErrorHandler
import uk.gov.hmrc.gatekeeper.controllers.actions.ActionBuilders
import uk.gov.hmrc.gatekeeper.models.Forms.RedirectUriForm
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
          Future.successful(Ok(manageLoginRedirectUriView(app.application, RedirectUriForm.form.fill(RedirectUriForm(redirects)))))
        case _                                            => Future.successful(NotFound)
      }

    }
  }

  def manageLoginRedirectUriAction(appId: ApplicationId) = atLeastSuperUserAction { implicit request =>
    withApp(appId) { app =>
      def handleValidForm(form: RedirectUriForm) = {
        applicationService.manageLoginRedirectUris(app.application, form.redirectUris, loggedIn.userFullName.get).map { _ =>
          Redirect(routes.ApplicationController.applicationPage(appId))
        }
      }

      def handleFormError(form: Form[RedirectUriForm]) = {
        Future.successful(BadRequest(manageLoginRedirectUriView(app.application, form)))
      }

      RedirectUriForm.form.bindFromRequest().fold(handleFormError, handleValidForm)
    }
  }
}
