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

package uk.gov.hmrc.apiplatform.modules.events.config

import play.api.inject.Module
import play.api.Environment
import play.api.Configuration
import uk.gov.hmrc.apiplatform.modules.events.connectors._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import com.google.inject.{Inject, Provider, Singleton}

class EventsConfigurationModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[SubordinateApiPlatformEventsConnector.Config].toProvider[SubordinateApiPlatformEventsConnectorProvider],
      bind[PrincipalApiPlatformEventsConnector.Config].toProvider[PrincipalApiPlatformEventsConnectorProvider]
    )
  }
}

@Singleton
class SubordinateApiPlatformEventsConnectorProvider @Inject() (config: ServicesConfig) extends Provider[SubordinateApiPlatformEventsConnector.Config] {

  override def get(): SubordinateApiPlatformEventsConnector.Config = {

    val serviceBaseUrl     = config.baseUrl("api-platform-events-subordinate")
    val useProxy = config.getConfBool("api-platform-events-subordinate.useProxy", true)
    val bearerToken = config.getConfString("api-platform-events-subordinate.bearerToken", "")
    val apiKey = config.getConfString("api-platform-events-subordinate.apiKey", "")

    SubordinateApiPlatformEventsConnector.Config(serviceBaseUrl, useProxy, bearerToken, apiKey)
  }
}

@Singleton
class PrincipalApiPlatformEventsConnectorProvider @Inject()(config: ServicesConfig) extends Provider[PrincipalApiPlatformEventsConnector.Config] {

  override def get(): PrincipalApiPlatformEventsConnector.Config = {

    val serviceBaseUrl     = config.baseUrl("api-platform-events-principal")

    PrincipalApiPlatformEventsConnector.Config(serviceBaseUrl)
  }
  
}
