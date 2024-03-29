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

package uk.gov.hmrc.gatekeeper.connectors

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.{BeforeAndAfterEach, Suite}

trait WiremockSugarIt extends BeforeAndAfterEach {
  this: Suite =>
  val stubPort    = sys.env.getOrElse("WIREMOCK", "11111").toInt
  val stubHost    = "localhost"
  val wireMockUrl = s"http://$stubHost:$stubPort"

  private val wireMockConfiguration: WireMockConfiguration =
    wireMockConfig().port(stubPort)

  val wireMockServer = new WireMockServer(wireMockConfiguration)

  override def beforeEach(): Unit = {
    super.beforeEach()
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
  }

  override def afterEach(): Unit = {
    wireMockServer.stop()
    wireMockServer.resetMappings()
    super.afterEach()
  }

}
