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

import play.api.Play.{configuration, current}
import uk.gov.hmrc.play.config.ServicesConfig

trait AppConfig {
  val assetsPrefix: String
  val analyticsToken: String
  val analyticsHost: String
  val nameDisplayLimit: Int
  val devHubBaseUrl: String
}

object FrontendAppConfig extends AppConfig with ServicesConfig {

  override lazy val nameDisplayLimit = getConfInt("name.display.limit", 20)

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  override lazy val assetsPrefix = loadConfig("assets.url") + loadConfig("assets.version")
  override lazy val analyticsToken = loadConfig("google-analytics.token")
  override lazy val analyticsHost = loadConfig("google-analytics.host")
  override lazy val devHubBaseUrl = loadConfig("devHubBaseUrl")
}
