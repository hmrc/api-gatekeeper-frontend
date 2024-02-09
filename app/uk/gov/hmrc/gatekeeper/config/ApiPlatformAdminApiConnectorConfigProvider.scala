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

import com.google.inject.{Inject, Provider, Singleton}

import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import uk.gov.hmrc.gatekeeper.connectors.ApiPlatformAdminApiConnector

@Singleton
class ApiPlatformAdminApiConnectorConfigProvider @Inject() (config: ServicesConfig) extends Provider[ApiPlatformAdminApiConnector.Config] {

  override def get(): ApiPlatformAdminApiConnector.Config =
    ApiPlatformAdminApiConnector.Config(
      serviceBaseUrl = config.baseUrl("api-platform-admin-api"),
      authToken = config.getString("internal-auth.token")
    )
}