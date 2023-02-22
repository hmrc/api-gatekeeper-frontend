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

package uk.gov.hmrc.apiplatform.modules.events.config

import com.google.inject.{Inject, Provider, Singleton}

import play.api.inject.Module
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import uk.gov.hmrc.apiplatform.modules.common.config.EBbridgeConfigHelper
import uk.gov.hmrc.apiplatform.modules.events.connectors._

class EventsConfigurationModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[SubordinateApiPlatformEventsConnector.Config].toProvider[SubordinateApiPlatformEventsConnectorProvider],
      bind[PrincipalApiPlatformEventsConnector.Config].toProvider[PrincipalApiPlatformEventsConnectorProvider]
    )
  }
}

@Singleton
class SubordinateApiPlatformEventsConnectorProvider @Inject() (config: Configuration) extends ServicesConfig(config) with EBbridgeConfigHelper with Provider[SubordinateApiPlatformEventsConnector.Config] {

  override def get(): SubordinateApiPlatformEventsConnector.Config = {
    val name = "api-platform-events-subordinate"
    SubordinateApiPlatformEventsConnector.Config(serviceUrl("api-platform-events")(name), useProxy("api-platform-events-subordinate"), bearerToken(name), apiKey(name))
  }
}

@Singleton
class PrincipalApiPlatformEventsConnectorProvider @Inject() (config: Configuration) extends ServicesConfig(config) with EBbridgeConfigHelper with Provider[PrincipalApiPlatformEventsConnector.Config] {

  override def get(): PrincipalApiPlatformEventsConnector.Config = {

    val serviceBaseUrl = serviceUrl("api-platform-events")("api-platform-events-principal")

    PrincipalApiPlatformEventsConnector.Config(serviceBaseUrl)
  }

}
