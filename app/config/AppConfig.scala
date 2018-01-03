/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.Play
import uk.gov.hmrc.play.config.ServicesConfig

trait AppConfig {
  val assetsPrefix: String
  val analyticsToken: String
  val analyticsHost: String
  val devHubBaseUrl: String
  def isExternalTestEnvironment: Boolean
  def title: String
  def superUsers: Seq[String]
}

object AppConfig extends AppConfig with ServicesConfig {

  private def loadStringConfig(key: String) = {
    Play.current.configuration.getString(key)
      .getOrElse(throw new Exception(s"Missing configuration key: $key"))
  }

  override lazy val assetsPrefix = loadStringConfig("assets.url") + loadStringConfig("assets.version")
  override lazy val analyticsToken = loadStringConfig("google-analytics.token")
  override lazy val analyticsHost = loadStringConfig("google-analytics.host")
  override lazy val devHubBaseUrl = loadStringConfig("devHubBaseUrl")
  override def isExternalTestEnvironment = Play.current.configuration.getBoolean("isExternalTestEnvironment").getOrElse(false)
  override def title = if (isExternalTestEnvironment) "HMRC API Gatekeeper - Developer Sandbox" else "HMRC API Gatekeeper"
  override def superUsers: Seq[String] = {
    Play.current.configuration.getStringSeq(s"$env.superUsers")
      .orElse(Play.current.configuration.getStringSeq("superUsers"))
      .getOrElse(Seq.empty)
  }
}
