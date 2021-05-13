/*
 * Copyright 2021 HM Revenue & Customs
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
import connectors.AuthConnector
import javax.inject.{Inject, Singleton}
import model._
import play.api.i18n.I18nSupport
import play.api.mvc.MessagesControllerComponents
import services.{ApiDefinitionService, DeveloperService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{ErrorHelper, GatekeeperAuthWrapper, UserFunctionsWrapper}
import views.html.{ErrorTemplate, ForbiddenView}
import views.html.developers.Developers2View
import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc._
import play.api.Logger

@Singleton
class Developers2Controller @Inject()(val authConnector: AuthConnector,
                                      val forbiddenView: ForbiddenView,
                                      developerService: DeveloperService,
                                      val apiDefinitionService: ApiDefinitionService,
                                      mcc: MessagesControllerComponents,
                                      developersView: Developers2View,
                                      override val errorTemplate: ErrorTemplate
                                     )(implicit val appConfig: AppConfig, val ec: ExecutionContext)
    extends FrontendController(mcc) 
    with ErrorHelper 
    with GatekeeperAuthWrapper 
    with UserFunctionsWrapper 
    with I18nSupport {

  def blankDevelopersPage() = requiresAtLeast(GatekeeperRole.USER) { 
    implicit request =>
      combineUsersIntoPage(Future.successful(List.empty), DevelopersSearchForm(None, None, None, None))
  }

  def developersPage() = requiresAtLeast(GatekeeperRole.USER) { 
      implicit request =>
      DevelopersSearchForm.form.bindFromRequest.fold(
        formWithErrors => {
            Logger.warn("Errors found trying to bind request for developers search")
            // binding failure, you retrieve the form containing errors:
            Future.successful(BadRequest(developersView(Seq.empty, "", Seq.empty, DevelopersSearchForm.form.fill(DevelopersSearchForm(None,None,None,None)))))
        },
        searchParams => {
          val allFoundUsers = searchParams match {
            case DevelopersSearchForm(None, None, None, None) => 
              Logger.info("Not performing a query for empty parameters")
              Future.successful(List.empty)
            case DevelopersSearchForm(maybeEmailFilter, maybeApiVersionFilter, maybeEnvironmentFilter, maybeDeveloperStatusFilter) =>
              Logger.info("Searching developers")
              val filters = 
              Developers2Filter(
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
