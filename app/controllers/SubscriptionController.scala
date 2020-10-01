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
import scala.concurrent.{ExecutionContext, Future}
import views.html.applications.ManageSubscriptionsView
import services.ApmService
import services.ApplicationService
import views.html.ErrorTemplate
import views.html.ForbiddenView
import connectors.AuthConnector
import com.google.inject.{Singleton, Inject}
import model.view.SubscriptionViewModel
import model.ApiIdentifier
import model.Subscription
import model.subscriptions.ApiData
import model.VersionSubscription
import model.ApiVersionDefinition
import model.ClientId
import model.SubscriptionFields.SubscriptionFieldsWrapper

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
      def convertToVersionSubscription(apiData: ApiData, apiVersions: Seq[ApiVersion], clientId: ClientId, apiContext: ApiContext): Seq[VersionSubscription] = {
        apiData.versions.map {
          case (version, data) => 
            VersionSubscription(
              ApiVersionDefinition(version, data.status, Some(data.access)),
              apiVersions.contains(version),
              SubscriptionFieldsWrapper(appId, clientId, apiContext, version, Seq.empty)
            )
        }.toSeq
      }

      def filterSubscriptionsByContext(subscriptions: Set[ApiIdentifier], context: ApiContext) : Seq[ApiVersion] = {
        subscriptions.filter(id => id.context == context).map(id => id.version).toSeq
      }

      def convertToSubscriptions(subscriptions: Set[ApiIdentifier], allPossibleSubs: Map[ApiContext, ApiData], clientId: ClientId): Seq[Subscription] = {
        allPossibleSubs.map {
          case (context, data) => Subscription(data.name, data.serviceName, context, convertToVersionSubscription(data, filterSubscriptionsByContext(subscriptions, context), clientId, context))
        }.toSeq
      }

      withAppAndSubsData(appId) { appWithSubsData =>
        for {
          allPossibleSubs <- apmService.fetchAllPossibleSubscriptions(appId)
          subscriptions = convertToSubscriptions(appWithSubsData.subscriptions, allPossibleSubs, appWithSubsData.application.clientId)
          subscriptionsViewModel = SubscriptionViewModel(appWithSubsData.application.id, appWithSubsData.application.name, subscriptions, isAtLeastSuperUser)
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
