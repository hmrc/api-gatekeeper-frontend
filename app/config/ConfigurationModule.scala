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

package config

import com.google.inject.name.Names
import connectors._
import play.api.{Configuration, Environment}
import play.api.inject.Module
import services.SubscriptionFieldsService.SubscriptionFieldsConnector

class ConfigurationModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration) = {

    val developerConnectorBinding = if (configuration.getOptional[Boolean]("isExternalTestEnvironment").getOrElse(false)) {
      bind[DeveloperConnector].to[DummyDeveloperConnector]
    } else {
      bind[DeveloperConnector].to[HttpDeveloperConnector]
    }

    Seq(
      developerConnectorBinding,

      bind(classOf[SubscriptionFieldsConnector])
        .qualifiedWith(Names.named("SANDBOX"))
        .to(classOf[SandboxSubscriptionFieldsConnector]),
      bind(classOf[SubscriptionFieldsConnector])
        .qualifiedWith(Names.named("PRODUCTION"))
        .to(classOf[ProductionSubscriptionFieldsConnector]),
      bind(classOf[ApmConnector.Config])
      .toProvider(classOf[LiveApmConnectorConfigProvider]),
      bind(classOf[ApiCataloguePublishConnector.Config])
      .toProvider(classOf[ApiCataloguePublishConnectorConfigProvider])
    )
  }
}
