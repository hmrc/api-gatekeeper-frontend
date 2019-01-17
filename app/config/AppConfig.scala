/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.{Configuration, Environment, Play}
import uk.gov.hmrc.play.config.ServicesConfig

class AppConfig @Inject()(override val runModeConfiguration: Configuration, environment: Environment ) extends ServicesConfig {

  override protected def mode: Mode = environment.mode

  private def loadStringConfig(key: String) = {
    Play.current.configuration.getString(key)
      .getOrElse(throw new Exception(s"Missing configuration key: $key"))
  }

  lazy val assetsPrefix = loadStringConfig("assets.url") + loadStringConfig("assets.version")
  lazy val devHubBaseUrl = loadStringConfig("devHubBaseUrl")
  lazy val apiScopeBaseUrl = baseUrl("api-scope")
  lazy val applicationBaseUrl = s"${baseUrl("third-party-application")}"
  lazy val authBaseUrl = s"${baseUrl("auth")}/auth/authenticate/user"
  lazy val developerBaseUrl = s"${baseUrl("third-party-developer")}"
  lazy val subscriptionFieldsBaseUrl = s"${baseUrl("api-subscription-fields")}"
  lazy val serviceBaseUrl = baseUrl("api-definition")
  def isExternalTestEnvironment = Play.current.configuration.getBoolean("isExternalTestEnvironment").getOrElse(false)
  def title = if (isExternalTestEnvironment) "HMRC API Gatekeeper - Developer Sandbox" else "HMRC API Gatekeeper"
  def superUsers: Seq[String] = {
    Play.current.configuration.getStringSeq(s"$env.superUsers")
      .orElse(Play.current.configuration.getStringSeq("superUsers"))
      .getOrElse(Seq.empty)
  }

}
