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

package acceptance.specs

import acceptance.BaseSpec
import acceptance.pages.{DashboardPage, SignInPage}
import com.github.tomakehurst.wiremock.client.WireMock._

class SignInSpec extends BaseSpec {

  feature("Gatekeeper Sign in") {

    info("In order to manage uplift application requests")
    info("As a gatekeeper")
    info("I would like to sign in")

    scenario("Sign in with valid credentials") {
      val body =
        """
          |{
          | "access_token": {
          |     "authToken":"Bearer fggjmiJzyVZrR6/e39TimjqHyla3x8kmlTd",
          |     "expiry":1459365831061
          |     },
          |     "expires_in":14400,
          |     "roles":[{"scope":"api","name":"gatekeeper"}],
          |     "authority_uri":"/auth/oid/joe.test",
          |     "token_type":"Bearer"
          |}
        """.stripMargin
      stubFor(post(urlEqualTo("/auth/authenticate/user"))
        .willReturn(aResponse().withBody(body).withStatus(200)))

      stubFor(get(urlEqualTo("/auth/authenticate/user/authorise?scope=api&role=gatekeeper"))
        .willReturn(aResponse().withStatus(200)))

      stubFor(get(urlEqualTo("/gatekeeper/applications"))
        .willReturn(aResponse().withBody("[]").withStatus(200)))

      goOn(SignInPage)

      SignInPage.signIn("joe.test", "password")

      on(DashboardPage)
    }


    scenario("Sign in with invalid credentials"){
        stubFor(post(urlEqualTo("/auth/authenticate/user"))
          .willReturn(aResponse().withStatus(401)))

        goOn(SignInPage)

        SignInPage.signIn("joe.test", "password")

        on(SignInPage)
        SignInPage.isError shouldBe true
    }

    scenario("Sign in with unauthorised credentials")  {
      val body =
        """
          |{
          | "access_token": {
          |     "authToken":"Bearer fggjmiJzyVZrR6/e39TimjqHyla3x8kmlTd",
          |     "expiry":1459365831061
          |     },
          |     "expires_in":14400,
          |     "roles":[{"scope":"something","name":"gatekeeper"}],
          |     "authority_uri":"/auth/oid/joe.test",
          |     "token_type":"Bearer"
          |}
        """.stripMargin
      stubFor(post(urlEqualTo("/auth/authenticate/user"))
        .willReturn(aResponse().withBody(body).withStatus(200)))

      stubFor(get(urlEqualTo("/auth/authenticate/user/authorise?scope=api&role=gatekeeper"))
        .willReturn(aResponse().withStatus(401)))

      goOn(SignInPage)

      SignInPage.signIn("joe.test", "password")
      on(DashboardPage)
      DashboardPage.isUnauthorised shouldBe true
    }
  }
}
