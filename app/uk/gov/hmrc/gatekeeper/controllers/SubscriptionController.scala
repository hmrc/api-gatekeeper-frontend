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

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.controllers.actions.ActionBuilders
import uk.gov.hmrc.gatekeeper.config.{AppConfig, ErrorHandler}
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.gatekeeper.views.html.applications.ManageSubscriptionsView
import uk.gov.hmrc.gatekeeper.services.ApmService
import uk.gov.hmrc.gatekeeper.services.ApplicationService
import uk.gov.hmrc.gatekeeper.views.html.ErrorTemplate
import uk.gov.hmrc.gatekeeper.views.html.ForbiddenView
import com.google.inject.{Singleton, Inject}
import uk.gov.hmrc.gatekeeper.models.view.SubscriptionViewModel
import uk.gov.hmrc.gatekeeper.models.subscriptions.ApiData
import uk.gov.hmrc.gatekeeper.utils.SortingHelper

import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._

@Singleton
class SubscriptionController @Inject()(
  manageSubscriptionsView: ManageSubscriptionsView,
  mcc: MessagesControllerComponents,
  val forbiddenView: ForbiddenView,
  val errorTemplate: ErrorTemplate,
  val applicationService: ApplicationService,
  val apmService: ApmService,
  val errorHandler: ErrorHandler,
  strideAuthorisationService: StrideAuthorisationService
)(implicit val appConfig: AppConfig, override val ec: ExecutionContext)
  extends GatekeeperBaseController(strideAuthorisationService, mcc) with ActionBuilders {
    
  def manageSubscription(appId: ApplicationId): Action[AnyContent] = atLeastSuperUserAction { implicit request =>
    def convertToVersionSubscription(apiData: ApiData, apiVersions: List[ApiVersion]): List[VersionSubscriptionWithoutFields] = {
      apiData.versions.map {
        case (version, data) => 
          VersionSubscriptionWithoutFields(
            ApiVersionDefinition(version, ApiVersionSource.UNKNOWN, data.status, Some(data.access)),
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
        subscriptionsViewModel = SubscriptionViewModel(appWithSubsData.application.id, appWithSubsData.application.name, sortedSubscriptions)
      } yield Ok(manageSubscriptionsView(subscriptionsViewModel))
    }
  }

  def subscribeToApi(appId: ApplicationId, apiContext: ApiContext, version: ApiVersion): Action[AnyContent] = atLeastSuperUserAction { implicit request =>
    withApp(appId) { app =>
      applicationService.subscribeToApi(app.application, ApiIdentifier(apiContext, version)).map(_ => Redirect(routes.SubscriptionController.manageSubscription(appId)))
    }
  }

  def unsubscribeFromApi(appId: ApplicationId, apiContext: ApiContext, version: ApiVersion): Action[AnyContent] = atLeastSuperUserAction { implicit request =>
    withApp(appId) { app =>
      applicationService.unsubscribeFromApi(app.application, apiContext, version).map(_ => Redirect(routes.SubscriptionController.manageSubscription(appId)))
    }
  }
}
