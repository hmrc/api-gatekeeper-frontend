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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.GatekeeperAuthorisationActions
import uk.gov.hmrc.apiplatform.modules.gkauth.services._
import uk.gov.hmrc.gatekeeper.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.gatekeeper.controllers.actions.ActionBuilders
import uk.gov.hmrc.gatekeeper.services._
import uk.gov.hmrc.gatekeeper.utils.ErrorHelper
import uk.gov.hmrc.gatekeeper.views.html.applications._
import uk.gov.hmrc.gatekeeper.views.html.ErrorTemplate

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class OrganisationController @Inject()(
    strideAuthorisationService: StrideAuthorisationService,
    val applicationService: ApplicationService,
    mcc: MessagesControllerComponents,
    organisationView: OrganisationView,
    override val errorTemplate: ErrorTemplate,
    val apmService: ApmService,
    val errorHandler: ErrorHandler,
    val ldapAuthorisationService: LdapAuthorisationService
  )(implicit val appConfig: AppConfig,
    override val ec: ExecutionContext
  ) extends GatekeeperBaseController(strideAuthorisationService, mcc)
    with GatekeeperAuthorisationActions
    with ErrorHelper
    with ActionBuilders
    with ApplicationLogger {

  implicit val dateTimeOrdering: Ordering[LocalDateTime] = Ordering.fromLessThan(_ isBefore _)

  //todo what is gonna be the ordId?
  def organisationPage(orgId: ApplicationId): Action[AnyContent] = anyAuthenticatedUserAction { implicit request =>
    val defaults                                              = Map("page" -> "1", "pageSize" -> "100", "sort" -> "NAME_ASC", "includeDeleted" -> "false")
    val params                                                = defaults ++ request.queryString.map { case (k, v) => k -> v.mkString }
    val buildAppUrlFn: (ApplicationId, Environment) => String = (appId, deployedTo) =>
      if (appConfig.gatekeeperApprovalsEnabled && deployedTo == Environment.PRODUCTION) {
        s"${appConfig.gatekeeperApprovalsBaseUrl}/api-gatekeeper-approvals/applications/$appId"
      } else {
        routes.ApplicationController.applicationPage(appId).url
      }

    for {
      paginatedApplicationResponse <- applicationService.searchApplications(None, params)
    } yield Ok(organisationView(paginatedApplicationResponse, request.role.isSuperUser, params, buildAppUrlFn))
  }
}
