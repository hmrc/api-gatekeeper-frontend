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

@Singleton
class Developers2Controller @Inject()(val authConnector: AuthConnector,
                                      val forbiddenView: ForbiddenView,
                                      developerService: DeveloperService,
                                      val apiDefinitionService: ApiDefinitionService,
                                      mcc: MessagesControllerComponents,
                                      developersView: Developers2View,
                                      override val errorTemplate: ErrorTemplate
                                     )(implicit val appConfig: AppConfig, val ec: ExecutionContext)
  extends FrontendController(mcc) with ErrorHelper with GatekeeperAuthWrapper with UserFunctionsWrapper with I18nSupport {

  def developersPage(maybeEmailFilter: Option[String] = None,
                     maybeApiVersionFilter: Option[String] = None,
                     maybeEnvironmentFilter: Option[String] = None,
                     maybeDeveloperStatusFilter: Option[String] = None) = {

    requiresAtLeast(GatekeeperRole.USER) {

      implicit request => {

          val queryParameters = getQueryParametersAsKeyValues(request)

          val filteredUsers = (maybeEmailFilter, maybeApiVersionFilter, maybeEnvironmentFilter, maybeDeveloperStatusFilter) match {
            case (None, None, None, None) => Future.successful(Seq.empty)
            case _ => {
              val filter = Developers2Filter(
                mapEmptyStringToNone(maybeEmailFilter),
                ApiContextVersion(mapEmptyStringToNone(maybeApiVersionFilter)),
                ApiSubscriptionInEnvironmentFilter(maybeEnvironmentFilter),
                DeveloperStatusFilter(maybeDeveloperStatusFilter))

              developerService.searchDevelopers(filter)
            }
          }

          for {
            users <- filteredUsers
            apiVersions <- apiDefinitionService.fetchAllApiDefinitions()
          } yield Ok(developersView(users, usersToEmailCopyText(users), getApiVersionsDropDownValues(apiVersions), queryParameters))
        }
    }
  }

}

