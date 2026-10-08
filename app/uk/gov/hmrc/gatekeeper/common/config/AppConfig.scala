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

package uk.gov.hmrc.gatekeeper.common.config

import com.google.inject.ImplementedBy

@ImplementedBy(classOf[AppConfigImpl])
trait AppConfig {
  def title: String

  def appName: String

  def devHubBaseUrl: String

  def apiScopeSandboxBaseUrl: String
  def apiScopeSandboxUseProxy: Boolean
  def apiScopeSandboxBearerToken: String
  def apiScopeSandboxApiKey: String
  def apiScopeProductionBaseUrl: String

  def applicationSandboxBaseUrl: String
  def applicationSandboxUseProxy: Boolean
  def applicationSandboxBearerToken: String
  def applicationSandboxApiKey: String
  def applicationProductionBaseUrl: String

  def authBaseUrl: String
  def strideLoginUrl: String
  def developerBaseUrl: String

  def subscriptionFieldsSandboxBaseUrl: String
  def subscriptionFieldsSandboxUseProxy: Boolean
  def subscriptionFieldsSandboxBearerToken: String
  def subscriptionFieldsSandboxApiKey: String
  def subscriptionFieldsProductionBaseUrl: String

  def apiPublisherSandboxBaseUrl: String
  def apiPublisherSandboxUseProxy: Boolean
  def apiPublisherSandboxBearerToken: String
  def apiPublisherSandboxApiKey: String
  def apiPublisherProductionBaseUrl: String

  def gatekeeperXmlServicesBaseUrl: String

  def gatekeeperApprovalsEnabled: Boolean
  def gatekeeperApprovalsBaseUrl: String
  def gatekeeperApisBaseUrl: String
  def gatekeeperApisUrl: String
  def gatekeeperOrganisationBaseUrl: String
  def gatekeeperOrganisationUrl: String

  def apiGatekeeperEmailUrl: String
  def apiGatekeeperEmailUsersUrl: String
}
