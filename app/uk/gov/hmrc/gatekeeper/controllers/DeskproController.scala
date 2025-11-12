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
import scala.concurrent.ExecutionContext

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.UpstreamErrorResponse

import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.GatekeeperAuthorisationActions
import uk.gov.hmrc.apiplatform.modules.gkauth.services._
import uk.gov.hmrc.gatekeeper.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.gatekeeper.controllers.actions.ActionBuilders
import uk.gov.hmrc.gatekeeper.services._
import uk.gov.hmrc.gatekeeper.utils.ErrorHelper
import uk.gov.hmrc.gatekeeper.views.html.ErrorTemplate

@Singleton
class DeskproController @Inject() (
    strideAuthorisationService: StrideAuthorisationService,
    val applicationService: ApplicationService,
    val applicationQueryService: ApplicationQueryService,
    mcc: MessagesControllerComponents,
    override val errorTemplate: ErrorTemplate,
    val apmService: ApmService,
    val errorHandler: ErrorHandler,
    val ldapAuthorisationService: LdapAuthorisationService,
    val apiPlatformDeskproService: ApiPlatformDeskproService,
    val developerService: DeveloperService
  )(implicit val appConfig: AppConfig,
    override val ec: ExecutionContext
  ) extends GatekeeperBaseController(strideAuthorisationService, mcc)
    with GatekeeperAuthorisationActions
    with ErrorHelper
    with ActionBuilders
    with ApplicationLogger {

  def developerPage(ticketId: Int): Action[AnyContent] = anyStrideUserAction { implicit request =>
    {
      for {
        ticket <- apiPlatformDeskproService.getDeskproTicket(ticketId)
        user   <- developerService.fetchUser(ticket.personEmail)
      } yield Redirect(routes.DeveloperController.developerPage(user.userId))
    } recover {
      case e: UpstreamErrorResponse if e.statusCode == NOT_FOUND =>
        logger.error(s"Failed to find Deskpro ticket: $ticketId", e)
        notFound(s"Failed to find Deskpro ticket $ticketId")
      case e: IllegalArgumentException                           =>
        logger.error(s"Failed to find developer for Deskpro ticket: $ticketId", e)
        notFound(s"Failed to find developer for Deskpro ticket $ticketId")
      case _                                                     => technicalDifficulties
    }
  }

}
