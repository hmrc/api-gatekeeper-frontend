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

package config

import javax.inject.Inject
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig

class AppConfig @Inject()(override val runModeConfiguration: Configuration, environment: Environment ) extends ServicesConfig {

  override protected def mode: Mode = environment.mode

  private def loadStringConfig(key: String) = {
    runModeConfiguration.getString(key)
      .getOrElse(throw new Exception(s"Missing configuration key: $key"))
  }

  lazy val appName = loadStringConfig("appName")
  lazy val assetsPrefix = loadStringConfig("assets.url") + loadStringConfig("assets.version")

  lazy val devHubBaseUrl = loadStringConfig("devHubBaseUrl")
  lazy val retryCount = runModeConfiguration.getInt("retryCount").getOrElse(0)
  lazy val retryDelayMilliseconds = runModeConfiguration.getInt("retryDelayMilliseconds").getOrElse(500)

  lazy val apiScopeSandboxBaseUrl = serviceUrl("api-scope")("api-scope-sandbox")
  lazy val apiScopeSandboxUseProxy = useProxy("api-scope-sandbox")
  lazy val apiScopeSandboxBearerToken = bearerToken("api-scope-sandbox")
  lazy val apiScopeSandboxApiKey = apiKey("api-scope-sandbox")
  lazy val apiScopeProductionBaseUrl = serviceUrl("api-scope")("api-scope-production")
  lazy val apiScopeProductionUseProxy = useProxy("api-scope-production")
  lazy val apiScopeProductionBearerToken = bearerToken("api-scope-production")
  lazy val apiScopeProductionApiKey = apiKey("api-scope-production")

  lazy val applicationSandboxBaseUrl = serviceUrl("third-party-application")("third-party-application-sandbox")
  lazy val applicationSandboxUseProxy = useProxy("third-party-application-sandbox")
  lazy val applicationSandboxBearerToken = bearerToken("third-party-application-sandbox")
  lazy val applicationSandboxApiKey = apiKey("third-party-application-sandbox")
  lazy val applicationProductionBaseUrl = serviceUrl("third-party-application")("third-party-application-production")
  lazy val applicationProductionUseProxy = useProxy("third-party-application-production")
  lazy val applicationProductionBearerToken = bearerToken("third-party-application-production")
  lazy val applicationProductionApiKey = apiKey("third-party-application-production")

  lazy val authBaseUrl = baseUrl("auth")
  lazy val strideLoginUrl = s"${baseUrl("stride-auth-frontend")}/stride/sign-in"
  lazy val developerBaseUrl = baseUrl("third-party-developer")

  lazy val subscriptionFieldsSandboxBaseUrl = serviceUrl("api-subscription-fields")("api-subscription-fields-sandbox")
  lazy val subscriptionFieldsSandboxUseProxy = useProxy("api-subscription-fields-sandbox")
  lazy val subscriptionFieldsSandboxBearerToken = bearerToken("api-subscription-fields-sandbox")
  lazy val subscriptionFieldsSandboxApiKey = apiKey("api-subscription-fields-sandbox")
  lazy val subscriptionFieldsProductionBaseUrl = serviceUrl("api-subscription-fields")("api-subscription-fields-production")
  lazy val subscriptionFieldsProductionUseProxy = useProxy("api-subscription-fields-production")
  lazy val subscriptionFieldsProductionBearerToken = bearerToken("api-subscription-fields-production")
  lazy val subscriptionFieldsProductionApiKey = apiKey("api-subscription-fields-production")

  lazy val apiPublisherSandboxBaseUrl = serviceUrl("api-publisher")("api-publisher-sandbox")
  lazy val apiPublisherSandboxUseProxy = useProxy("api-publisher-sandbox")
  lazy val apiPublisherSandboxBearerToken = bearerToken("api-publisher-sandbox")
  lazy val apiPublisherSandboxApiKey = apiKey("api-publisher-sandbox")
  lazy val apiPublisherProductionBaseUrl = serviceUrl("api-publisher")("api-publisher-production")
  lazy val apiPublisherProductionUseProxy = useProxy("api-publisher-production")
  lazy val apiPublisherProductionBearerToken = bearerToken("api-publisher-production")
  lazy val apiPublisherProductionApiKey = apiKey("api-publisher-production")

  lazy val apiDefinitionSandboxBaseUrl = serviceUrl("api-definition")("api-definition-sandbox")
  lazy val apiDefinitionSandboxUseProxy = useProxy("api-definition-sandbox")
  lazy val apiDefinitionSandboxBearerToken = bearerToken("api-definition-sandbox")
  lazy val apiDefinitionSandboxApiKey = apiKey("api-definition-sandbox")
  lazy val apiDefinitionProductionBaseUrl = serviceUrl("api-definition")("api-definition-production")
  lazy val apiDefinitionProductionUseProxy = useProxy("api-definition-production")
  lazy val apiDefinitionProductionBearerToken = bearerToken("api-definition-production")
  lazy val apiDefinitionProductionApiKey = apiKey("api-definition-production")

  lazy val gatekeeperSuccessUrl = loadStringConfig("api-gatekeeper-frontend-success-url")

  lazy val superUserRole = loadStringConfig("roles.super-user")
  lazy val userRole = loadStringConfig("roles.user")
  lazy val adminRole = loadStringConfig("roles.admin")

  def isExternalTestEnvironment = runModeConfiguration.getBoolean("isExternalTestEnvironment").getOrElse(false)
  def title = if (isExternalTestEnvironment) "HMRC API Gatekeeper - Developer Sandbox" else "HMRC API Gatekeeper"
  def superUsers: Seq[String] = {
    runModeConfiguration.getStringSeq(s"$env.superUsers")
      .orElse(runModeConfiguration.getStringSeq("superUsers"))
      .getOrElse(Seq.empty)
  }

  private def serviceUrl(key: String)(serviceName: String): String = {
    if (useProxy(serviceName)) s"${baseUrl(serviceName)}/${getConfString(s"$serviceName.context", key)}"
    else baseUrl(serviceName)
  }

  private def useProxy(serviceName: String) = getConfBool(s"$serviceName.use-proxy", false)

  private def bearerToken(serviceName: String) = getConfString(s"$serviceName.bearer-token", "")

  private def apiKey(serviceName: String) = getConfString(s"$serviceName.api-key", "")


}
