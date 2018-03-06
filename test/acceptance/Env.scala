/*
 * Copyright 2018 HM Revenue & Customs
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
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions}
import org.openqa.selenium.{HasCapabilities, WebDriver}

import scala.util.Try

trait Env {

  val webDriverConfig = System.getProperty("test.driver", "firefox").toLowerCase

  val driver = if (webDriverConfig == "firefox") {
    val driver: WebDriver with HasCapabilities = {
      val options = new FirefoxOptions
      options.setAcceptInsecureCerts(true)
      new FirefoxDriver(options)
    }
    driver
  } else {
    val driver: WebDriver = {
      val driver = new ChromeDriver()
      driver.manage().deleteAllCookies()
      driver.manage().window().fullscreen()
      driver
    }
    driver
  }

  sys addShutdownHook {
    Try(driver.quit())
  }
}

object Env extends Env
