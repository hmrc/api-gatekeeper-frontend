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

package uk.gov.hmrc.gatekeeper.support

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status

trait AuthServiceStub {
  val authUrl                = "/auth/authorise"
  private val authUrlMatcher = urlEqualTo(authUrl)

  val headersWithBearer = Map()

  def primeAuthServiceNoClientId(body: String): Unit = {
    stubFor(post(authUrlMatcher)
      .withRequestBody(equalToJson(body))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(
            s"""{
               |}""".stripMargin
          )
      ))
  }

  def primeAuthServiceSuccess(): Unit = {
    stubFor(post(authUrlMatcher)
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(
            s"""{
               |  "authorisedEnrolments": [ { "key" : "user-role", "identifiers": [], "state": "123"} ],
               |  "optionalName": {"name": "bob", "lastName": "hope"}
               |}""".stripMargin
          )
      ))
  }

  def primeAuthServiceFail(): Unit = {
    stubFor(post(authUrlMatcher)
      .willReturn(
        aResponse()
          .withStatus(Status.UNAUTHORIZED)
          .withHeader("WWW-Authenticate", "MDTP detail=\"InsufficientEnrolments\"")
      ))
  }
}
