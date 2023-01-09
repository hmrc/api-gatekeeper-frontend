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

import javax.inject.Inject

import com.google.inject.{ImplementedBy, Singleton}

import play.api.{ConfigLoader, Configuration}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import uk.gov.hmrc.apiplatform.modules.common.config.EBbridgeConfigHelper

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

  def apiDefinitionSandboxBaseUrl: String
  def apiDefinitionSandboxUseProxy: Boolean
  def apiDefinitionSandboxBearerToken: String
  def apiDefinitionSandboxApiKey: String
  def apiDefinitionProductionBaseUrl: String

  def superUserRole: String
  def userRole: String
  def adminRole: String

  def gatekeeperXmlServicesBaseUrl: String

  def gatekeeperApprovalsEnabled: Boolean
  def gatekeeperApprovalsBaseUrl: String

  def gatekeeperComposeEmailBaseUrl: String
  def gatekeeperComposeEmailUrl: String
  def gatekeeperComposeEmailUsersUrl: String
}

@Singleton
class AppConfigImpl @Inject() (config: Configuration) extends ServicesConfig(config) with AppConfig with EBbridgeConfigHelper {

  def title = "HMRC API Gatekeeper"

  def getConfigDefaulted[A](key: String, default: A)(implicit loader: ConfigLoader[A]) = config.getOptional[A](key)(loader).getOrElse(default)

  val appName = getString("appName")

  val devHubBaseUrl          = getString("devHubBaseUrl")
  val retryCount             = getConfigDefaulted("retryCount", 0)
  val retryDelayMilliseconds = getConfigDefaulted("retryDelayMilliseconds", 500)  // scalastyle:ignore

  val apiScopeSandboxBaseUrl        = serviceUrl("api-scope")("api-scope-sandbox")
  val apiScopeSandboxUseProxy       = useProxy("api-scope-sandbox")
  val apiScopeSandboxBearerToken    = bearerToken("api-scope-sandbox")
  val apiScopeSandboxApiKey         = apiKey("api-scope-sandbox")
  val apiScopeProductionBaseUrl     = serviceUrl("api-scope")("api-scope-production")
  val apiScopeProductionUseProxy    = useProxy("api-scope-production")
  val apiScopeProductionBearerToken = bearerToken("api-scope-production")
  val apiScopeProductionApiKey      = apiKey("api-scope-production")

  val applicationSandboxBaseUrl        = serviceUrl("third-party-application")("third-party-application-sandbox")
  val applicationSandboxUseProxy       = useProxy("third-party-application-sandbox")
  val applicationSandboxBearerToken    = bearerToken("third-party-application-sandbox")
  val applicationSandboxApiKey         = apiKey("third-party-application-sandbox")
  val applicationProductionBaseUrl     = serviceUrl("third-party-application")("third-party-application-production")
  val applicationProductionUseProxy    = useProxy("third-party-application-production")
  val applicationProductionBearerToken = bearerToken("third-party-application-production")
  val applicationProductionApiKey      = apiKey("third-party-application-production")

  val authBaseUrl      = baseUrl("auth")
  val strideLoginUrl   = s"${baseUrl("stride-auth-frontend")}/stride/sign-in"
  val developerBaseUrl = baseUrl("third-party-developer")

  val subscriptionFieldsSandboxBaseUrl        = serviceUrl("api-subscription-fields")("api-subscription-fields-sandbox")
  val subscriptionFieldsSandboxUseProxy       = useProxy("api-subscription-fields-sandbox")
  val subscriptionFieldsSandboxBearerToken    = bearerToken("api-subscription-fields-sandbox")
  val subscriptionFieldsSandboxApiKey         = apiKey("api-subscription-fields-sandbox")
  val subscriptionFieldsProductionBaseUrl     = serviceUrl("api-subscription-fields")("api-subscription-fields-production")
  val subscriptionFieldsProductionUseProxy    = useProxy("api-subscription-fields-production")
  val subscriptionFieldsProductionBearerToken = bearerToken("api-subscription-fields-production")
  val subscriptionFieldsProductionApiKey      = apiKey("api-subscription-fields-production")

  val apiPublisherSandboxBaseUrl        = serviceUrl("api-publisher")("api-publisher-sandbox")
  val apiPublisherSandboxUseProxy       = useProxy("api-publisher-sandbox")
  val apiPublisherSandboxBearerToken    = bearerToken("api-publisher-sandbox")
  val apiPublisherSandboxApiKey         = apiKey("api-publisher-sandbox")
  val apiPublisherProductionBaseUrl     = serviceUrl("api-publisher")("api-publisher-production")
  val apiPublisherProductionUseProxy    = useProxy("api-publisher-production")
  val apiPublisherProductionBearerToken = bearerToken("api-publisher-production")
  val apiPublisherProductionApiKey      = apiKey("api-publisher-production")

  val apiDefinitionSandboxBaseUrl        = serviceUrl("api-definition")("api-definition-sandbox")
  val apiDefinitionSandboxUseProxy       = useProxy("api-definition-sandbox")
  val apiDefinitionSandboxBearerToken    = bearerToken("api-definition-sandbox")
  val apiDefinitionSandboxApiKey         = apiKey("api-definition-sandbox")
  val apiDefinitionProductionBaseUrl     = serviceUrl("api-definition")("api-definition-production")
  val apiDefinitionProductionUseProxy    = useProxy("api-definition-production")
  val apiDefinitionProductionBearerToken = bearerToken("api-definition-production")
  val apiDefinitionProductionApiKey      = apiKey("api-definition-production")

  val superUserRole = getString("roles.super-user")
  val userRole      = getString("roles.user")
  val adminRole     = getString("roles.admin")

  val gatekeeperXmlServicesBaseUrl = baseUrl("api-gatekeeper-xml-services-frontend")

  val gatekeeperApprovalsEnabled = getBoolean("api-gatekeeper-approvals-frontend.enabled")
  val gatekeeperApprovalsBaseUrl = baseUrl("api-gatekeeper-approvals-frontend")

  val gatekeeperComposeEmailBaseUrl  = baseUrl("gatekeeper-compose-email-frontend")
  val gatekeeperComposeEmailUrl      = s"$gatekeeperComposeEmailBaseUrl/api-gatekeeper/compose-email/email"
  val gatekeeperComposeEmailUsersUrl = s"$gatekeeperComposeEmailBaseUrl/api-gatekeeper/compose-email/email/users"
}
