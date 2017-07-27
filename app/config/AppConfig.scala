/*
 * Copyright 2017 HM Revenue & Customs
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

import config.AppConfig.getConfInt
import play.api.Configuration
import play.api.Play.{configuration, current}
import uk.gov.hmrc.play.config.ServicesConfig

trait AppConfig {

  protected val configuration: Configuration

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  lazy val assetsPrefix = loadConfig("assets.url") + loadConfig("assets.version")
  lazy val analyticsToken = loadConfig("google-analytics.token")
  lazy val analyticsHost = loadConfig("google-analytics.host")
  lazy val devHubBaseUrl = loadConfig("devHubBaseUrl")
  lazy val isExternalTestEnvironment = configuration.getBoolean("isExternalTestEnvironment").getOrElse(false)
  lazy val title = if (isExternalTestEnvironment) "HMRC API Gatekeeper - Developer Sandbox" else "HMRC API Gatekeeper"
  lazy val superUsers: Seq[String] = configuration.getStringSeq("superUsers").getOrElse(Seq.empty)
}

object AppConfig extends AppConfig with ServicesConfig {
  override val configuration = play.api.Play.configuration
}
