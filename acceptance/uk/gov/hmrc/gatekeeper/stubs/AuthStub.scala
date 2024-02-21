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

package uk.gov.hmrc.gatekeeper.stubs

import com.github.tomakehurst.wiremock.client.WireMock._

import play.api.http.Status._

import uk.gov.hmrc.apiplatform.modules.common.utils.WireMockExtensions


trait AuthStub extends WireMockExtensions {



    def stubAuthenticate(gateKeeperId: String) ={ 

                  val authBody =
        s"""
           |{
           | "access_token": {
           |     "authToken":"Bearer fggjmiJzyVZrR6/e39TimjqHyla3x8kmlTd",
           |     "expiry":1459365831061
           |     },
           |     "expires_in":14400,
           |     "roles":[{"scope":"api","name":"gatekeeper"}],
           |     "authority_uri":"/auth/oid/$gateKeeperId",
           |     "token_type":"Bearer"
           |}
      """.stripMargin

        stubFor(post(urlEqualTo("/auth/authenticate/user"))
            .willReturn(aResponse().withBody(authBody).withStatus(OK)))
    }

    def stubAuthenticateAuthorise() = {
        stubFor(get(urlEqualTo("/auth/authenticate/user/authorise?scope=api&role=gatekeeper"))
            .willReturn(aResponse().withStatus(OK)))
    }      

    def stubAuthorise() ={
         stubFor(post(urlPathEqualTo("/auth/authorise"))
        .willReturn(aResponse()
        .withHeader("WWW-Authenticate", "MDTP detail=\"InsufficientEnrolments\"")
        .withStatus(UNAUTHORIZED)))      
    }


}
