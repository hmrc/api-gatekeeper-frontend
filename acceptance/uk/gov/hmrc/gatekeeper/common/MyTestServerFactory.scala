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

package uk.gov.hmrc.gatekeeper.common

import play.api.test.DefaultTestServerFactory
import play.api.{Application, Mode}
import play.core.server.ServerConfig

object MyTestServerFactory extends MyTestServer

class MyTestServer extends DefaultTestServerFactory {
  override protected def serverConfig(app: Application): ServerConfig = {
    val sc = ServerConfig(port = Some(6001), sslPort = Some(6002), mode = Mode.Test, rootDir = app.path)
    sc.copy(configuration = sc.configuration withFallback overrideServerConfiguration(app))
  }
}
