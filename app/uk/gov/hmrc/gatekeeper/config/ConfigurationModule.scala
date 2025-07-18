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

import play.api.inject.Module
import play.api.{Configuration, Environment}

import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.ForbiddenHandler
import uk.gov.hmrc.gatekeeper.connectors._
import uk.gov.hmrc.gatekeeper.controllers.HandleForbiddenWithView

class ConfigurationModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[ApmConnector.Config].toProvider[LiveApmConnectorConfigProvider],
      bind[ThirdPartyOrchestratorConnector.Config].toProvider[ThirdPartyOrchestratorConnectorConfigProvider],
      bind[ApiPlatformDeskproConnector.Config].toProvider[ApiPlatformDeskproConnectorConfigProvider],
      bind[ApiPlatformAdminApiConnector.Config].toProvider[ApiPlatformAdminApiConnectorConfigProvider],
      bind[ApiCataloguePublishConnector.Config].toProvider[ApiCataloguePublishConnectorConfigProvider],
      bind[XmlServicesConnector.Config].toProvider[XmlServicesConnectorConfigProvider],
      bind[ForbiddenHandler].to[HandleForbiddenWithView],
      bind[ApmConnectoCombinedApisModule].to[ApmConnector],
      bind[ApmConnectorApiDefinitionModule].to[ApmConnector],
      bind[ApmConnectorApplicationModule].to[ApmConnector],
      bind[ApmConnectorPpnsModule].to[ApmConnector],
      bind[ApmConnectorSubscriptionFieldsModule].to[ApmConnector]
    )
  }
}
