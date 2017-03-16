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

package acceptance

import acceptance.pages.SignInPage
import com.github.tomakehurst.wiremock.client.WireMock._
import org.openqa.selenium.WebDriver

trait SignInSugar extends NavigationSugar {
  val gatekeeperId: String = "joe.test"

  def signInGatekeeper()(implicit webDriver: WebDriver) = {

//    val authBody =
//      s"""
//        |{
//        | "access_token": {
//        |     "authToken":"Bearer fggjmiJzyVZrR6/e39TimjqHyla3x8kmlTd",
//        |     "expiry":1459365831061
//        |     },
//        |     "expires_in":14400,
//        |     "roles":[{"scope":"api","name":"gatekeeper"}],
//        |     "authority_uri":"/auth/oid/$gatekeeperId",
//        |     "token_type":"Bearer"
//        |}
//      """.stripMargin
//
//    stubFor(post(urlEqualTo("/auth/authenticate/user"))
//      .willReturn(aResponse().withBody(authBody).withStatus(200)))
//
//    stubFor(get(urlEqualTo("/auth/authenticate/user/authorise?scope=api&role=gatekeeper"))
//      .willReturn(aResponse().withStatus(200)))

    //      val body =
    //        """
    //          |{
    //          | "access_token": {
    //          |     "authToken":"Bearer fggjmiJzyVZrR6/e39TimjqHyla3x8kmlTd",
    //          |     "expiry":1459365831061
    //          |     },
    //          |     "expires_in":14400,
    //          |     "roles":[{"scope":"something","name":"gatekeeper"}],
    //          |     "authority_uri":"/auth/oid/joe.test",
    //          |     "token_type":"Bearer"
    //          |}
    //        """.stripMargin

    goOn(SignInPage)

    SignInPage.signIn(gatekeeperId, "password123")
  }

}
