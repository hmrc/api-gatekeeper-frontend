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
import uk.gov.hmrc.apiplatform.modules.events.connectors.ApiPlatformEventsConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import com.google.inject.{Singleton, Provider, Inject}

class EventsConfigurationModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[ApiPlatformEventsConnector.Config].toProvider[LiveApiPlatformEventsConnectorProvider]
    )
  }
}

@Singleton
class LiveApiPlatformEventsConnectorProvider @Inject() (config: ServicesConfig) extends Provider[ApiPlatformEventsConnector.Config] {
  override def get(): ApiPlatformEventsConnector.Config = {
    val url     = config.baseUrl("api-platform-events")
    val enabled = config.getConfBool("api-platform-events.enabled", true)
    ApiPlatformEventsConnector.Config(url, enabled)
  }
}