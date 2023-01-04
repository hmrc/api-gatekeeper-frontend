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

package uk.gov.hmrc.gatekeeper.config

import com.google.inject.name.Names
import uk.gov.hmrc.gatekeeper.connectors._
import play.api.{Configuration, Environment}
import play.api.inject.Module
import uk.gov.hmrc.gatekeeper.services.SubscriptionFieldsService.SubscriptionFieldsConnector

import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.ForbiddenHandler
import uk.gov.hmrc.gatekeeper.controllers.HandleForbiddenWithView

class ConfigurationModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration) = {

    val developerConnectorBinding = if (configuration.getOptional[Boolean]("isExternalTestEnvironment").getOrElse(false)) {
      bind[DeveloperConnector].to[DummyDeveloperConnector]
    } else {
      bind[DeveloperConnector].to[HttpDeveloperConnector]
    }

    Seq(
      developerConnectorBinding,
      bind[SubscriptionFieldsConnector]
        .qualifiedWith(Names.named("SANDBOX"))
        .to[SandboxSubscriptionFieldsConnector],
      bind[SubscriptionFieldsConnector]
        .qualifiedWith(Names.named("PRODUCTION"))
        .to[ProductionSubscriptionFieldsConnector],
      bind[ApmConnector.Config].toProvider[LiveApmConnectorConfigProvider],
      bind[ApiCataloguePublishConnector.Config].toProvider[ApiCataloguePublishConnectorConfigProvider],
      bind[XmlServicesConnector.Config].toProvider[XmlServicesConnectorConfigProvider],
      bind[ForbiddenHandler].to(classOf[HandleForbiddenWithView])
    )
  }
}
