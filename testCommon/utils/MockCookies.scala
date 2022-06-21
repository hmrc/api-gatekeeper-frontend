/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.Application
import play.api.libs.ws.WSCookie
import play.api.mvc.{Cookie, Session, SessionCookieBaker}
import org.openqa.selenium.{Cookie => SeleniumCookie}
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCrypto


object MockCookies {
  def makeCookieValue(application: Application) = makeSessionCookie(application).value


  private val sessionCookieName = "mdtp"

    val mockSession = Session(Map(
      SessionKeys.lastRequestTimestamp -> System.currentTimeMillis().toString,
      SessionKeys.authToken -> "mock-bearer-token",
      SessionKeys.sessionId -> "mock-sessionid"
    ))

    def makeSessionCookie( app: Application): Cookie = {
      val cookieCrypto = app.injector.instanceOf[SessionCookieCrypto]
      val cookieBaker = app.injector.instanceOf[SessionCookieBaker]
      val sessionCookie = cookieBaker.encodeAsCookie(mockSession)
      val encryptedValue = cookieCrypto.crypto.encrypt(PlainText(sessionCookie.value))
      sessionCookie.copy(value = encryptedValue.value)
    }


 def makeWsCookie(application: Application) = {
   val cookie = makeSessionCookie(application)
   new WSCookie() {
      override def name: String = sessionCookieName
      override def value: String = cookie.value
      override def domain: Option[String] = cookie.domain
      override def path: Option[String] = Some(cookie.path)
      override def maxAge: Option[Long] = cookie.maxAge.map(_.toLong)
      override def secure: Boolean = cookie.secure
      override def httpOnly: Boolean = cookie.httpOnly
    }
 }

  def makeSeleniumCookie(application: Application)={
    val playCookie = makeSessionCookie(application)
    //dr.add_cookie({'domain': '192.168.2.211',
    // 'httpOnly': True, 'name': 'JSESSIONID', 'path': '/smartcommty', 'sameSite': 'Lax', 'secure': False, 'value': '5574c24a-dbc4-4a7d-9607-cc24f5653ebf'})
    new SeleniumCookie.Builder(sessionCookieName, playCookie.value).path("/").domain("http://127.0.0.1").isHttpOnly(true).isSecure(false).build()
  }

}
