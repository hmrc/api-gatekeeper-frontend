/*
 * Copyright 2021 HM Revenue & Customs
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

package utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.{BeforeAndAfterEach, Suite}
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import org.scalatest.BeforeAndAfterAll

trait WireMockSugar extends BeforeAndAfterEach with BeforeAndAfterAll {
  this: Suite =>
  val stubPort = sys.env.getOrElse("WIREMOCK", "22222").toInt
  val stubHost = "localhost"
  val wireMockUrl = s"http://$stubHost:$stubPort"

  private val wireMockConfiguration: WireMockConfiguration =
    wireMockConfig().port(stubPort)

  val wireMockServer = new WireMockServer(wireMockConfiguration)

  override def beforeAll() = {
    super.beforeAll()
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
  }

  override protected def afterAll() {
    wireMockServer.stop()
    super.afterAll()
  }
  
  override def afterEach() {
    wireMockServer.resetMappings()
    super.afterEach()
  }

  implicit class withJsonRequestBodySyntax(bldr: MappingBuilder) {
    import com.github.tomakehurst.wiremock.client.WireMock._
    import play.api.libs.json._
    
    def withJsonRequestBody[T](t:T)(implicit writes: Writes[T]): MappingBuilder = {
      bldr.withRequestBody(equalTo(Json.toJson(t).toString))
    }
  }

  
  implicit class withJsonBodySyntax(bldr: ResponseDefinitionBuilder) {
    import play.api.libs.json._
    
    def withJsonBody[T](t:T)(implicit writes: Writes[T]): ResponseDefinitionBuilder = {
      bldr.withBody(Json.toJson(t).toString)
    }
  }
}

