/*
 * Copyright 2022 HM Revenue & Customs
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

import uk.gov.hmrc.gatekeeper.config.AppConfig
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.gatekeeper.models._
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.gatekeeper.services.{ApiDefinitionService, DeveloperService}
import uk.gov.hmrc.gatekeeper.utils.{ErrorHelper, UserFunctionsWrapper}
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}
import uk.gov.hmrc.gatekeeper.views.html.developers.DevelopersView
import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc._
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger

import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInRequest
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService

@Singleton
class DevelopersController @Inject()(
  val forbiddenView: ForbiddenView,
  developerService: DeveloperService,
  val apiDefinitionService: ApiDefinitionService,
  mcc: MessagesControllerComponents,
  developersView: DevelopersView,
  override val errorTemplate: ErrorTemplate,
  strideAuthorisationService: StrideAuthorisationService
)(implicit val appConfig: AppConfig, override val ec: ExecutionContext)
  extends GatekeeperBaseController(strideAuthorisationService, mcc)
    with ErrorHelper
    with UserFunctionsWrapper
    with ApplicationLogger {

  def blankDevelopersPage() = anyStrideUserAction { implicit request =>
    combineUsersIntoPage(Future.successful(List.empty), DevelopersSearchForm(None, None, None, None))
  }

  def developersPage() = anyStrideUserAction { implicit request =>
    DevelopersSearchForm.form.bindFromRequest.fold(
      formWithErrors => {
          logger.warn("Errors found trying to bind request for developers search")
          // binding failure, you retrieve the form containing errors:
          Future.successful(BadRequest(developersView(Seq.empty, "", Seq.empty, DevelopersSearchForm.form.fill(DevelopersSearchForm(None,None,None,None)))))
      },
      searchParams => {
        val allFoundUsers = searchParams match {
          case DevelopersSearchForm(None, None, None, None) => 
            logger.info("Not performing a query for empty parameters")
            Future.successful(List.empty)
          case DevelopersSearchForm(maybeEmailFilter, maybeApiVersionFilter, maybeEnvironmentFilter, maybeDeveloperStatusFilter) =>
            logger.info("Searching developers")
            val filters = 
            DevelopersSeachFilter(
              mapEmptyStringToNone(maybeEmailFilter),
                  ApiContextVersion(mapEmptyStringToNone(maybeApiVersionFilter)),
                  ApiSubscriptionInEnvironmentFilter(maybeEnvironmentFilter),
                  DeveloperStatusFilter(maybeDeveloperStatusFilter)
                )
                developerService.searchDevelopers(filters)
        }
        combineUsersIntoPage(allFoundUsers, searchParams)
      }
    )
  }

  def combineUsersIntoPage(allFoundUsers: Future[List[User]], searchParams: DevelopersSearchForm)(implicit request: LoggedInRequest[_]) = {
    for {
      users <- allFoundUsers
      registeredUsers = users.collect {
                          case r : RegisteredUser => r
                        }
      verifiedUsers = registeredUsers.filter(_.verified)
      apiVersions <- apiDefinitionService.fetchAllApiDefinitions()
      form = DevelopersSearchForm.form.fill(searchParams)
    } yield Ok(developersView(users, usersToEmailCopyText(verifiedUsers), getApiVersionsDropDownValues(apiVersions), form))
  }
}
