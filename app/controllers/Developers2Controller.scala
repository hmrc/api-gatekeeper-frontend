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
import utils.{ErrorHelper, GatekeeperAuthWrapper}
import views.html.{ErrorTemplate, Forbidden}
import views.html.developers.developers2

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Developers2Controller @Inject()(val authConnector: AuthConnector,
                                      developerService: DeveloperService,
                                      val apiDefinitionService: ApiDefinitionService,
                                      mcc: MessagesControllerComponents,
                                      developersView: developers2,
                                      forbiddenView: Forbidden,
                                      override val errorTemplate: ErrorTemplate,
                                     )(implicit val appConfig: AppConfig, val ec: ExecutionContext)
  extends FrontendController(mcc) with ErrorHelper with GatekeeperAuthWrapper with I18nSupport {

  def developersPage(maybeEmailFilter: Option[String] = None,
                     maybeApiVersionFilter: Option[String] = None,
                     maybeEnvironmentFilter: Option[String] = None,
                     maybeDeveloperStatusFilter: Option[String] = None) = {

    requiresAtLeast(GatekeeperRole.USER, forbiddenView) {

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

  def mapEmptyStringToNone(filter: Option[String]): Option[String] = {
    filter match {
      case None | Some("")  => None
      case _ => filter
    }
  }

  private def getQueryParametersAsKeyValues(request: LoggedInRequest[_]) = {
    request.queryString.map { case (k, v) => k -> v.mkString }
  }

  def usersToEmailCopyText(users: Seq[User]): String = {
    users.map(_.email).mkString("; ")
  }

  def getApiVersionsDropDownValues(apiDefinitions: Seq[APIDefinition]) = {
    def toKeyValue(api: APIDefinition, version: APIVersion) = {
      val value = ApiContextVersion(api.context, version.version).toStringValue
      val displayedStatus = APIStatus.displayedStatus(version.status)
      val description = s"${api.name} (${version.version}) ($displayedStatus)"

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

