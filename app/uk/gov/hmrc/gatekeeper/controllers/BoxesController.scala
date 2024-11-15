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

import scala.concurrent.ExecutionContext

import com.google.inject.{Inject, Singleton}

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, Environment}
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.GatekeeperAuthorisationActions
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationService, StrideAuthorisationService}
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models.pushpullnotifications.{Box, SubscriptionType}
import uk.gov.hmrc.gatekeeper.services.ApmService
import uk.gov.hmrc.gatekeeper.utils.CsvHelper
import uk.gov.hmrc.gatekeeper.utils.CsvHelper.ColumnDefinition
import uk.gov.hmrc.gatekeeper.views.html.ppns.BoxesView

@Singleton
class BoxesController @Inject() (
    mcc: MessagesControllerComponents,
    val apmService: ApmService,
    strideAuthorisationService: StrideAuthorisationService,
    val ldapAuthorisationService: LdapAuthorisationService,
    boxesView: BoxesView
  )(implicit val appConfig: AppConfig,
    override val ec: ExecutionContext
  ) extends GatekeeperBaseController(strideAuthorisationService, mcc)
    with GatekeeperAuthorisationActions {

  def getAll(): Action[AnyContent] = anyAuthenticatedUserAction { implicit request =>
    apmService.fetchAllBoxes().map(boxes => {
      val columnDefinitions: Seq[ColumnDefinition[Box]] = Seq(
        ColumnDefinition("environment", (box => box.environment.toString())),
        ColumnDefinition("applicationId", (box => box.applicationId.fold("")(_.value.toString()))),
        ColumnDefinition("clientId", (box => box.boxCreator.clientId.value)),
        ColumnDefinition("name", (box => box.boxName)),
        ColumnDefinition("boxId", (box => box.boxId.value)),
        ColumnDefinition("subscriptionType", (box => (box.subscriber.fold("")(s => s.subscriptionType.toString())))),
        ColumnDefinition("callbackUrl", (box => box.subscriber.fold("")(s => s.callBackUrl)))
      )

      Ok(CsvHelper.toCsvString(columnDefinitions, boxes))
    })
  }

  def page(): Action[AnyContent] = anyAuthenticatedUserAction { implicit request =>
    def getMetrics(boxes: List[Box], env: Environment) = {
      val envBoxes             = boxes.filter(_.environment == env)
      val pushBoxes: List[Box] = envBoxes.filter(_.subscriber.exists(_.subscriptionType == SubscriptionType.API_PUSH_SUBSCRIBER))
      val pullBoxes: List[Box] = envBoxes.filterNot(_.subscriber.exists(_.subscriptionType == SubscriptionType.API_PUSH_SUBSCRIBER))
      (envBoxes.size, pushBoxes.size, pullBoxes.size)
    }

    apmService.fetchAllBoxes().map(boxes => {
      val sandboxMetrics: (Int, Int, Int)             = getMetrics(boxes, Environment.SANDBOX)
      val productionMetrics: (Int, Int, Int)          = getMetrics(boxes, Environment.PRODUCTION)
      val appBoxMap: List[(ApplicationId, List[Box])] = boxes.filter(_.applicationId.isDefined).groupBy(_.applicationId.get).toList.sortBy(_._2.size).reverse

      Ok(boxesView(sandboxMetrics, productionMetrics, appBoxMap))
    })
  }
}
