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

package uk.gov.hmrc.gatekeeper.common

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.openqa.selenium.By
import org.scalatest.concurrent.Eventually
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, TestData}
import org.scalatestplus.play.guice.GuiceOneServerPerTest

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.RunningServer
import play.api.{Application, Mode}
import uk.gov.hmrc.selenium.webdriver.{Browser, Driver, ScreenshotOnFailure}

trait BaseSpec extends AnyFeatureSpec
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with Matchers
    with GuiceOneServerPerTest
    with Eventually
    with Browser
    with ScreenshotOnFailure {

  val stubPort = 6003
  val stubHost = "localhost"

  override protected def newServerForTest(app: Application, testData: TestData): RunningServer = MyTestServerFactory.start(app)

  val wireMockServer = new WireMockServer(wireMockConfig()
    .port(stubPort))

  override def newAppForTest(testData: TestData): Application = {
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.host"                               -> stubHost,
        "microservice.services.auth.port"                               -> stubPort,
        "microservice.services.stride-auth-frontend.host"               -> stubHost,
        "microservice.services.stride-auth-frontend.port"               -> stubPort,
        "microservice.services.third-party-application-sandbox.host"    -> stubHost,
        "microservice.services.third-party-application-sandbox.port"    -> stubPort,
        "microservice.services.third-party-application-production.host" -> stubHost,
        "microservice.services.third-party-application-production.port" -> stubPort,
        "microservice.services.api-definition-sandbox.host"             -> stubHost,
        "microservice.services.api-definition-sandbox.port"             -> stubPort,
        "microservice.services.api-definition-production.host"          -> stubHost,
        "microservice.services.api-definition-production.port"          -> stubPort,
        "microservice.services.api-platform-microservice.host"          -> stubHost,
        "microservice.services.api-platform-microservice.port"          -> stubPort,
        "microservice.services.api-platform-deskpro.host"               -> stubHost,
        "microservice.services.api-platform-deskpro.port"               -> stubPort,
        "microservice.services.third-party-developer.host"              -> stubHost,
        "microservice.services.third-party-developer.port"              -> stubPort,
        "microservice.services.api-platform-xml-services.host"          -> stubHost,
        "microservice.services.api-platform-xml-services.port"          -> stubPort,
        "microservice.services.api-platform-events-subordinate.host"    -> stubHost,
        "microservice.services.api-platform-events-subordinate.port"    -> stubPort,
        "microservice.services.api-platform-events-principal.host"      -> stubHost,
        "microservice.services.api-platform-events-principal.port"      -> stubPort
      )
      .in(Mode.Prod)
      .build()
  }

  override def beforeAll() = {
    super.beforeAll()
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
  }

  override def afterAll() = {
    wireMockServer.stop()
    super.afterAll()
  }

  override def beforeEach() = {
    super.beforeEach()
    startBrowser()
    Driver.instance.manage().deleteAllCookies()
    WireMock.reset()
  }

  override def afterEach(): Unit = {
    quitBrowser()
    super.afterEach()
  }

  def on(page: WebPage): Unit =
    eventually {
      withClue(s"Currently in page: ${Driver.instance.getCurrentUrl()}, when expecting heading '${page.pageHeading}' but found '${page.heading}' - ") {
        assert(page.isCurrentPage(), s"Page was not loaded: ${page.url}")
      }
    }

  protected def verifyCountOfElementsByAttribute(attributeName: String, expected: Int) = {
    Driver.instance.findElements(By.cssSelector(s"[$attributeName]")).size() shouldBe expected
  }

  protected def verifyText(attributeName: String, expected: String, index: Int = 0) = {
    Driver.instance.findElements(By.cssSelector(s"[$attributeName]")).get(index).getText should include(expected)
  }

  def onTechDifficultiesFor(page: WebPage) = {
    val element = eventually {
      Driver.instance.findElement(By.tagName("body"))
    }

    assert(element.getText().contains("Sorry, we’re experiencing technical difficulties"), s"Page loaded WITHOUT tech difficulties: ${page.url}")
  }
}
