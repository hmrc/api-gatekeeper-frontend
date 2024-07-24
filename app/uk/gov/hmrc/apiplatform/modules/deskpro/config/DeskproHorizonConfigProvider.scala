/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatform.modules.deskpro.config

import javax.inject.{Inject, Provider, Singleton}
import play.api.Configuration

case class DeskproHorizonConfig(
  deskproHorizonUrl: String,
  deskproHorizonApiKey: String
)

@Singleton
class DeskproHorizonConfigProvider @Inject() (configuration: Configuration) extends Provider[DeskproHorizonConfig] {
  override def get(): DeskproHorizonConfig = {
    val deskproHorizonUrl: String          = configuration.get[String]("deskpro-horizon.uri")
    val deskproHorizonApiKey: String       = configuration.getOptional[String]("deskpro-horizon.api-key").map(key => s"key $key").getOrElse("")

    DeskproHorizonConfig(deskproHorizonUrl, deskproHorizonApiKey)
  }
}
