/*
 * Copyright 2016 HM Revenue & Customs
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

import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxProfile}
import org.openqa.selenium.{HasCapabilities, WebDriver}

import scala.util.Try

trait Env {

<<<<<<< HEAD
  val driver: WebDriver with HasCapabilities = {

 // val driver:WebDriver = new ChromeDriver()
  val profile = new FirefoxProfile
  profile.setAcceptUntrustedCertificates(true)
   new FirefoxDriver(profile)
  }
=======
 // val driver: WebDriver with HasCapabilities = {

  val driver:WebDriver = new ChromeDriver()
  //val profile = new FirefoxProfile
  //profile.setAcceptUntrustedCertificates(true)
  // new FirefoxDriver(profile)
  //}
>>>>>>> b8fa63e9de284ac7a84cc2458be375293a9c763b

  sys addShutdownHook {
    Try(driver.quit())
  }
}

object Env extends Env
