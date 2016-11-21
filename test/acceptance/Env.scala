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

import java.util.logging.Level

import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.WebDriver

import scala.util.Try

trait Env {

  val driver: WebDriver = {
    val capabilities = DesiredCapabilities.htmlUnit()
    capabilities.setJavascriptEnabled(true)
    java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF)
    new HtmlUnitDriver(capabilities)
  }

  sys addShutdownHook {
    Try(driver.quit())
  }
}

object Env extends Env
