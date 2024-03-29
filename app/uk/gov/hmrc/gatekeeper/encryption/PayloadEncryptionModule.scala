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

package uk.gov.hmrc.gatekeeper.encryption

import com.google.inject.name.Names
import com.google.inject.{Inject, Provider, Singleton}

import play.api.inject.Module
import play.api.{Configuration, Environment}

class PayloadEncryptionModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[PayloadEncryption].qualifiedWith(Names.named("ThirdPartyDeveloper")).toProvider(classOf[ThirdPartyDeveloperPayloadEncryptionProvider])
    )
  }
}

@Singleton
class ThirdPartyDeveloperPayloadEncryptionProvider @Inject() (val config: Configuration)
    extends Provider[PayloadEncryption] {

  lazy val jsonEncryptionKey = config.get[String]("third-party-developer.json.encryption.key")
  lazy val payloadEncrypion  = new PayloadEncryption(jsonEncryptionKey)

  override def get() = payloadEncrypion
}
