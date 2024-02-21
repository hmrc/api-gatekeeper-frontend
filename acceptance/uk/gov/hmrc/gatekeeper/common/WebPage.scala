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

import java.time.Duration
import scala.jdk.CollectionConverters._

import org.openqa.selenium.support.ui.{ExpectedConditions, FluentWait, Select, Wait}
import org.openqa.selenium.{By, WebDriver, WebElement}

import uk.gov.hmrc.selenium.component.PageObject
import uk.gov.hmrc.selenium.webdriver.Driver

trait WebPage extends PageObject {

  def url: String

  def pageHeading: String

  // These two should be moved someplace sensible and renamed
  def heading = getText(By.tagName("h1"))
  def bodyText = getText(By.tagName("body"))

  def goTo(): Unit = {
    get(url)
     waitForElementToBePresent(By.tagName("body"))
  }

  def isCurrentPage(): Boolean = {
    this.heading == this.pageHeading
  }

  def clickSubmit(): Unit = {
    click(By.id("submit"))
  }

  protected def writeInTextBox(input: String, id: String) = {
    sendKeys(By.cssSelector(s"input[id='$id']"), input)
  }

  protected def getSelectBoxSelectedItemValue(id: By): String = {
    getSelectBox(id)
      .getFirstSelectedOption
      .getAttribute("value")
  }

  protected def getSelectBox(id: By): Select = {
    new Select(findElements(id).head)
  }

  protected def findElements(location: By): List[WebElement] = {
    Driver.instance.findElements(location).asScala.toList
  }

  protected def findElement(location: By): WebElement = {
    Driver.instance.findElement(location)
  }

  private def waitForElementToBePresent(locator: By): WebElement = {
    fluentWait.until(ExpectedConditions.presenceOfElementLocated(locator))
  }

  private def fluentWait: Wait[WebDriver] = new FluentWait[WebDriver](Driver.instance)
    .withTimeout(Duration.ofSeconds(3))
    .pollingEvery(Duration.ofSeconds(1))
}
