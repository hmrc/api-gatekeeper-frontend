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

import scala.concurrent.{ExecutionContext, Future}

import com.google.inject.{Inject, Singleton}

import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.gkauth.utils.GatekeeperAuthorisationHelper
import uk.gov.hmrc.gatekeeper.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.gatekeeper.controllers.actions.ActionBuilders
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.view.SubscriptionViewModel
import uk.gov.hmrc.gatekeeper.services.{ApmService, ApplicationService, SubscriptionsService}
import uk.gov.hmrc.gatekeeper.views.html.applications.ManageSubscriptionsView
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

object SubscriptionController {

  case class SubsForm(subscribed: Boolean)

  object SubsForm {
    import play.api.data.Forms._
    import play.api.data._

    lazy val form = Form[SubsForm](
      mapping(
        "subscribed" -> boolean
      )(SubsForm.apply)(SubsForm.unapply)
    )
  }
}

@Singleton
class SubscriptionController @Inject() (
    manageSubscriptionsView: ManageSubscriptionsView,
    mcc: MessagesControllerComponents,
    val forbiddenView: ForbiddenView,
    val errorTemplate: ErrorTemplate,
    val applicationService: ApplicationService,
    subscriptionService: SubscriptionsService,
    val apmService: ApmService,
    val errorHandler: ErrorHandler,
    strideAuthorisationService: StrideAuthorisationService
  )(implicit val appConfig: AppConfig,
    override val ec: ExecutionContext
  ) extends GatekeeperBaseController(strideAuthorisationService, mcc) with ActionBuilders with GatekeeperAuthorisationHelper {

  import SubscriptionController._

  def manageSubscription(appId: ApplicationId): Action[AnyContent] = atLeastSuperUserAction { implicit request =>
    implicit val versionOrdering: Ordering[VersionSubscriptionWithoutFields] =
      Ordering.by[VersionSubscriptionWithoutFields, ApiVersionNbr](_.version.versionNbr).reverse

    def convertToVersionSubscription(apiDefinition: ApiDefinition, apiVersions: List[ApiVersionNbr]): List[VersionSubscriptionWithoutFields] = {
      apiDefinition.versions.map {
        case (version, data) =>
          VersionSubscriptionWithoutFields(data, apiVersions.contains(version))
      }.toList.sorted
    }

    def filterSubscriptionsByContext(subscriptions: Set[ApiIdentifier], context: ApiContext): List[ApiVersionNbr] = {
      subscriptions.filter(id => id.context == context).map(id => id.versionNbr).toList
    }

    def convertToSubscriptions(subscriptions: Set[ApiIdentifier], allPossibleSubs: List[ApiDefinition]): List[SubscriptionWithoutFields] = {
      allPossibleSubs.map(defn =>
        SubscriptionWithoutFields(defn.name, defn.serviceName, defn.context, convertToVersionSubscription(defn, filterSubscriptionsByContext(subscriptions, defn.context)))
      )
    }

    withAppAndSubsData(appId) { appWithSubsData =>
      for {
        allPossibleSubs       <- apmService.fetchAllPossibleSubscriptions(appId)
        subscriptions          = convertToSubscriptions(appWithSubsData.subscriptions, allPossibleSubs)
        sortedSubscriptions    = subscriptions.sortWith(_.name.toLowerCase < _.name.toLowerCase)
        subscriptionsViewModel = SubscriptionViewModel(appWithSubsData.id, appWithSubsData.name, sortedSubscriptions)
      } yield Ok(manageSubscriptionsView(subscriptionsViewModel))
    }
  }

  def updateSubscription(appId: ApplicationId, apiContext: ApiContext, versionNbr: ApiVersionNbr): Action[AnyContent] = atLeastSuperUserAction { implicit request =>
    def handleFormError(form: Form[SubsForm]): Future[Result] = {
      Future.successful(Redirect(routes.SubscriptionController.manageSubscription(appId)))
    }

    def handleValidForm(app: ApplicationWithHistory)(form: SubsForm): Future[Result] = {
      if (form.subscribed) {
        subscriptionService.subscribeToApi(app.application, ApiIdentifier(apiContext, versionNbr), gatekeeperUser.get).map(_ =>
          Redirect(routes.SubscriptionController.manageSubscription(appId))
        )
      } else {
        subscriptionService.unsubscribeFromApi(app.application, ApiIdentifier(apiContext, versionNbr), gatekeeperUser.get).map(_ =>
          Redirect(routes.SubscriptionController.manageSubscription(appId))
        )
      }
    }

    withApp(appId) { app =>
      SubsForm.form.bindFromRequest().fold(handleFormError, handleValidForm(app))
    }
  }
}
