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
import javax.inject.Inject
import model.SubscriptionFields.SubscriptionFieldsWrapper
import model._
import model.view.SubscriptionVersion
import org.joda.time.DateTime
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.ApplicationService
import utils.{ActionBuilders, GatekeeperAuthWrapper}
import views.html.applications.subscriptionConfiguration.list_subscription_configuration

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionConfigurationController @Inject()(val applicationService: ApplicationService,
                                                    override val authConnector: AuthConnector
                                                   )(implicit override val appConfig: AppConfig, val ec: ExecutionContext)
  extends BaseController with GatekeeperAuthWrapper with ActionBuilders {

  implicit val dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)

  def listConfigurations(appId: String): Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) {
    implicit request =>
      implicit hc =>
        withAppAndFieldDefinitions(appId) {
          app => {
            Future.successful(Ok(list_subscription_configuration(app.application,  SubscriptionVersion(app.subscriptionsWithFieldDefinitions))))
          }
        }
  }
}


