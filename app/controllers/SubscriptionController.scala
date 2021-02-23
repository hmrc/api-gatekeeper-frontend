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

import model.ApplicationId
import play.api.mvc.Action
import play.api.mvc.AnyContent
import model.GatekeeperRole
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import play.api.mvc.MessagesControllerComponents
import model.ApiContext
import model.ApiVersion
import utils.ActionBuilders
import utils.GatekeeperAuthWrapper
import config.AppConfig
import scala.concurrent.ExecutionContext
import views.html.applications.ManageSubscriptionsView
import services.ApmService
import services.ApplicationService
import views.html.ErrorTemplate
import views.html.ForbiddenView
import connectors.AuthConnector
import com.google.inject.{Singleton, Inject}
import model.view.SubscriptionViewModel
import model.ApiIdentifier
import model.subscriptions.ApiData
import utils.SortingHelper
import model.SubscriptionWithoutFields
import model.VersionSubscriptionWithoutFields
import model.ApiVersionDefinition

@Singleton
class SubscriptionController @Inject()(
  manageSubscriptionsView: ManageSubscriptionsView,
  mcc: MessagesControllerComponents,
  val forbiddenView: ForbiddenView,
  val authConnector: AuthConnector,
  val errorTemplate: ErrorTemplate,
  val applicationService: ApplicationService,
  val apmService: ApmService)
  (implicit val appConfig: AppConfig, implicit val ec: ExecutionContext)
  extends FrontendController(mcc)
  with GatekeeperAuthWrapper
  with ActionBuilders {
    
  def manageSubscription(appId: ApplicationId): Action[AnyContent] = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit request =>
      def convertToVersionSubscription(apiData: ApiData, apiVersions: List[ApiVersion]): List[VersionSubscriptionWithoutFields] = {
        apiData.versions.map {
          case (version, data) => 
            VersionSubscriptionWithoutFields(
              ApiVersionDefinition(version, data.status, Some(data.access)),
              apiVersions.contains(version)
            )
        }.toList.sortWith(SortingHelper.descendingVersionWithoutFields)
      }

      def filterSubscriptionsByContext(subscriptions: Set[ApiIdentifier], context: ApiContext) : List[ApiVersion] = {
        subscriptions.filter(id => id.context == context).map(id => id.version).toList
      }

      def convertToSubscriptions(subscriptions: Set[ApiIdentifier], allPossibleSubs: Map[ApiContext, ApiData]): List[SubscriptionWithoutFields] = {
        allPossibleSubs.map {
          case (context, data) => SubscriptionWithoutFields(data.name, data.serviceName, context, convertToVersionSubscription(data, filterSubscriptionsByContext(subscriptions, context)))
        }.toList
      }

      withAppAndSubsData(appId) { appWithSubsData =>
        for {
          allPossibleSubs <- apmService.fetchAllPossibleSubscriptions(appId)
          subscriptions = convertToSubscriptions(appWithSubsData.subscriptions, allPossibleSubs)
          sortedSubscriptions = subscriptions.sortWith(_.name.toLowerCase < _.name.toLowerCase)
          subscriptionsViewModel = SubscriptionViewModel(appWithSubsData.application.id, appWithSubsData.application.name, sortedSubscriptions, isAtLeastSuperUser)
        } yield Ok(manageSubscriptionsView(subscriptionsViewModel))
      }
  }

  def subscribeToApi(appId: ApplicationId, apiContext: ApiContext, version: ApiVersion): Action[AnyContent] = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit request =>
        withApp(appId) { app =>
          applicationService.subscribeToApi(app.application, apiContext, version).map(_ => Redirect(routes.SubscriptionController.manageSubscription(appId)))
        }
  }

  def unsubscribeFromApi(appId: ApplicationId, apiContext: ApiContext, version: ApiVersion): Action[AnyContent] = requiresAtLeast(GatekeeperRole.SUPERUSER) {
    implicit request =>
        withApp(appId) { app =>
          applicationService.unsubscribeFromApi(app.application, apiContext, version).map(_ => Redirect(routes.SubscriptionController.manageSubscription(appId)))
        }
  }
}
