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



import org.openqa.selenium.support.ui.{ExpectedCondition, ExpectedConditions, WebDriverWait}
import org.openqa.selenium.{By, WebDriver, WebElement}
import org.scalatest.{Assertions, Matchers}
import org.scalatest.concurrent.Eventually
import org.scalatest.selenium.WebBrowser
import org.scalatest.selenium.WebBrowser.{go => goo}
import org.scalatest.time.{Millis, Seconds, Span}
import component.matchers.CustomMatchers
import uk.gov.hmrc.WebLink

trait NavigationSugar extends WebBrowser with Eventually with Assertions with Matchers {

  implicit override val patienceConfig = PatienceConfig(timeout = scaled(Span(3, Seconds)), interval = scaled(Span(100, Millis)))

  def goOn(page: WebPage)(implicit webDriver: WebDriver) = {
    go(page)
    on(page)
  }

  def go(page: WebLink)(implicit webDriver: WebDriver) = {
    goo to page
  }

  def on(page: WebPage)(implicit webDriver: WebDriver) = {
    eventually {
      webDriver.findElement(By.tagName("body"))
    }
    withClue(s"Currently in page: $currentUrl " + find(tagName("h1")).map(_.text).fold(" - ")(h1 => s", with title '$h1' - ")) {
      assert(page.isCurrentPage, s"Page was not loaded: ${page.url}")
    }
  }

  def loadPage()(implicit webDriver: WebDriver) = {
    val wait = new WebDriverWait(webDriver, 30)
    wait.until(
      new ExpectedCondition[WebElement] {
        override def apply(d: WebDriver) = d.findElement(By.tagName("body"))
      }
    )
  }

  def anotherTabIsOpened()(implicit webDriver: WebDriver) = {
    webDriver.getWindowHandles.size() shouldBe 2
  }

  def clickOnLink(attributeName: String)(implicit webDriver: WebDriver) = {
    webDriver.findElement(By.cssSelector(s"[$attributeName]")).click()
  }

  def clickOnSubmit()(implicit webDriver: WebDriver) = {
    webDriver.findElement(By.id("submit")).click()
  }

  def clickOnReview(attributeName: String)(implicit webDriver: WebDriver) = {
    webDriver.findElement(By.id("review")).click()
  }

  def clickOnElement(id: String)(implicit webDriver: WebDriver) = {
    webDriver.findElement(By.id(id)).click()
  }

  def verifyLinkPresent(attributeName: String, expectedUrl: String)(implicit webDriver: WebDriver) = {
    webDriver.findElement(By.cssSelector(s"[$attributeName]")).getAttribute("href") contains expectedUrl
  }

  def verifyText(attributeName: String, expected: String, index: Int = 0)(implicit webDriver: WebDriver) = {
    webDriver.findElements(By.cssSelector(s"[$attributeName]")).get(index).getText shouldBe expected
  }

  def verifyElementIsVisible(attributeId: String)(implicit webDriver: WebDriver) = {
    webDriver.findElement(By.id(attributeId)).isDisplayed
  }
}
