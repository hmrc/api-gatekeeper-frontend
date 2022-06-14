/*
 * Copyright 2020 HM Revenue & Customs
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

package common

import net.lightbody.bmp.BrowserMobProxyServer
import net.lightbody.bmp.client.ClientUtil
import net.lightbody.bmp.util.{HttpMessageContents, HttpMessageInfo}
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse

import java.net.URL
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.remote.{CapabilityType, DesiredCapabilities, RemoteWebDriver}
import org.openqa.selenium.{Dimension, WebDriver}

import scala.util.{Properties, Try}
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.firefox.FirefoxDriver
import utils.MockCookies


trait Env {

  val driver: WebDriver = createWebDriver
  lazy val port = 6001
  lazy val proxyPort = 6003
  lazy val windowSize = new Dimension(1024, 800)

def createProxy()= {
  val proxy: BrowserMobProxyServer = new BrowserMobProxyServer()
  proxy.addHeader("Authorization", "Bearer 123")
  proxy.start(proxyPort)
  println("*** CREATE PROXY")

  // get the Selenium proxy object
  val seleniumProxy = ClientUtil.createSeleniumProxy(proxy)
    proxy.addRequestFilter(new net.lightbody.bmp.filters.RequestFilter() {
       override def filterRequest(request: HttpRequest , contents: HttpMessageContents, messageInfo: HttpMessageInfo): HttpResponse  = {
         println("*** FILTER REQUEST")
       request.headers().add("Authorization", "Bearer 123")

        // in the request filter, you can return an HttpResponse object to "short-circuit" the request
        return null
      }
    });

  seleniumProxy
}


  lazy val createWebDriver: WebDriver = {

    Properties.propOrElse("test_driver", "chrome") match {
      case "chrome" => createChromeDriver()
      case "firefox" => createFirefoxDriver()
      case "remote-chrome" => createRemoteChromeDriver()
      case "remote-firefox" => createRemoteFirefoxDriver()
      case other => throw new IllegalArgumentException(s"target browser $other not recognised")
    }
  }

  def createRemoteChromeDriver() = {
    println("*** CREATE REMOTE CHROME DRIVER")
    val options = new ChromeOptions()
    .setProxy(createProxy())
    val driver = new RemoteWebDriver(new URL(s"http://localhost:4444/wd/hub"), options)
    driver.manage().window().setSize(windowSize)
    driver
  }

  def createRemoteFirefoxDriver() = {
    println("*** CREATE REMOTE FIREFOX DRIVER")
    val options = new FirefoxOptions().setProxy(createProxy())
    new RemoteWebDriver(new URL(s"http://localhost:4444/wd/hub"), options)
  }

  def createChromeDriver(): WebDriver = {
    println("*** CREATE  CHROME DRIVER")
    val proxy = createProxy()
    val options = new ChromeOptions()
    options.addArguments(s"--proxy-server=${proxy.getHttpProxy}")
    options.addArguments("--headless")
//    options.addArguments("--proxy-server='direct://'")
//    options.addArguments("--proxy-bypass-list=*")

    val driver = new ChromeDriver(options)
    driver.manage().deleteAllCookies()
    driver.manage().window().setSize(windowSize)
    driver
  }

  def createFirefoxDriver(): WebDriver = {
    println("*** CREATE FIREFOX DRIVER")
    val options = new FirefoxOptions()
    .setAcceptInsecureCerts(true)

      .setProxy(createProxy())
    new FirefoxDriver(options)
  }

  def shutdown = Try(driver.quit())

  sys addShutdownHook {
    shutdown
  }
}

object Env extends Env

class AfterHook {
  Env.shutdown
}
