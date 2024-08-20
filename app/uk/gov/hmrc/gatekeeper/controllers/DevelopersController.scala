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

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.mvc.MessagesControllerComponents

import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.GatekeeperAuthorisationActions
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInRequest
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationService, StrideAuthorisationService}
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.services.{ApiDefinitionService, DeveloperService}
import uk.gov.hmrc.gatekeeper.utils.CsvHelper.ColumnDefinition
import uk.gov.hmrc.gatekeeper.utils.{CsvHelper, ErrorHelper, UserFunctionsWrapper}
import uk.gov.hmrc.gatekeeper.views.html.developers.DevelopersView
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

@Singleton
class DevelopersController @Inject() (
    val forbiddenView: ForbiddenView,
    developerService: DeveloperService,
    val apiDefinitionService: ApiDefinitionService,
    mcc: MessagesControllerComponents,
    developersView: DevelopersView,
    override val errorTemplate: ErrorTemplate,
    strideAuthorisationService: StrideAuthorisationService,
    val ldapAuthorisationService: LdapAuthorisationService
  )(implicit val appConfig: AppConfig,
    override val ec: ExecutionContext
  ) extends GatekeeperBaseController(strideAuthorisationService, mcc)
    with GatekeeperAuthorisationActions
    with ErrorHelper
    with UserFunctionsWrapper
    with ApplicationLogger {

  def blankDevelopersPage() = anyAuthenticatedUserAction { implicit request =>
    combineUsersIntoPage(Future.successful(List.empty), DevelopersSearchForm(None, None, None, None))
  }

  def developersCsv() = atLeastSuperUserAction { implicit request =>
    {

      val csvColumnDefinitions = Seq[ColumnDefinition[RegisteredUser]](
        ColumnDefinition("First Name", (app => app.firstName)),
        ColumnDefinition("Last Name", (app => app.lastName)),
        ColumnDefinition("Email", (app => app.email.text))
      )
      developerService.fetchUsers
        .map(users => CsvHelper.toCsvString(csvColumnDefinitions, users.filter(_.verified)))
        .map(Ok(_).withHeaders(CONTENT_DISPOSITION -> s"attachment; filename=developers-${Instant.now()}.csv").as("text/csv"))
    }

  }

  def developersPage() = anyAuthenticatedUserAction { implicit request =>
    DevelopersSearchForm.form.bindFromRequest().fold(
      formWithErrors => {
        logger.warn("Errors found trying to bind request for developers search")
        // binding failure, you retrieve the form containing errors:
        Future.successful(BadRequest(developersView(Seq.empty, "", Seq.empty, DevelopersSearchForm.form.fill(DevelopersSearchForm(None, None, None, None)))))
      },
      searchParams => {
        val allFoundUsers = searchParams match {
          case DevelopersSearchForm(None, None, None, None)                                                                      =>
            logger.info("Not performing a query for empty parameters")
            Future.successful(List.empty)
          case DevelopersSearchForm(maybeEmailFilter, maybeApiVersionFilter, maybeEnvironmentFilter, maybeDeveloperStatusFilter) =>
            logger.info("Searching developers")
            val filters =
              DevelopersSearchFilter(
                mapEmptyStringToNone(maybeEmailFilter),
                ApiContextVersion(mapEmptyStringToNone(maybeApiVersionFilter)),
                ApiSubscriptionInEnvironmentFilter(maybeEnvironmentFilter),
                DeveloperStatusFilter(maybeDeveloperStatusFilter)
              )
            developerService.searchDevelopers(filters)
        }
        combineUsersIntoPage(allFoundUsers, searchParams)
          .map(_.withHeaders("Cache-Control" -> "no-cache"))
      }
    )
  }

  def combineUsersIntoPage(allFoundUsers: Future[List[AbstractUser]], searchParams: DevelopersSearchForm)(implicit request: LoggedInRequest[_]) = {
    for {
      users          <- allFoundUsers
      registeredUsers = users.collect {
                          case r: RegisteredUser => r
                        }
      verifiedUsers   = registeredUsers.filter(_.verified)
      apiVersions    <- apiDefinitionService.fetchAllApiDefinitions()
      form            = DevelopersSearchForm.form.fill(searchParams)
    } yield Ok(developersView(users, usersToEmailCopyText(verifiedUsers), getApiVersionsDropDownValues(apiVersions), form))
  }

}
