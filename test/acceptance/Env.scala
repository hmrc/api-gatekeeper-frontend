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

import java.util.logging.{Level, Logger}

import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.{HasCapabilities, WebDriver}
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxProfile}

import scala.util.Try

trait Env {

  val webDriverConfig = System.getProperty("test.driver", "HtmlUnitDriver").toLowerCase
  val driver = if (webDriverConfig == "firefox") {
    val driver: WebDriver with HasCapabilities = {
      val profile = new FirefoxProfile
      profile.setAcceptUntrustedCertificates(true)
      new FirefoxDriver(profile)
    }
    driver
  } else if (webDriverConfig == "chrome"){
    val driver: WebDriver = {
      new ChromeDriver()
    }
    driver
  } else {
    val driver: WebDriver = {
      val capabilities = DesiredCapabilities.htmlUnit()
      capabilities.setJavascriptEnabled(true)
      Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF)
      Logger.getLogger("com.gargoylesoftware.htmlunit.javascript.StrictErrorReporter").setLevel(Level.OFF)
      new HtmlUnitDriver(capabilities)
    }
    driver
  }

  sys addShutdownHook {
    Try(driver.quit())
  }
}

object Env extends Env

