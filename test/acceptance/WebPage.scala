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

import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}
import org.openqa.selenium.{By, WebDriver}
import org.scalatest._
import org.scalatest.selenium.{Page, WebBrowser}

case class Link(href: String, text: String)

trait WebLink extends Page with WebBrowser with Matchers {
  implicit val webDriver: WebDriver = Env.driver

  override def toString = this.getClass.getSimpleName
}

trait WebPage extends WebLink {

  def isCurrentPage: Boolean

  def heading = tagName("h1").element.text

  def bodyText = tagName("body").element.text

  def waitUntilElement(implicit webDriver: WebDriver, element: By) = {
    val wait = new WebDriverWait(webDriver, 30)
    wait.until(ExpectedConditions.presenceOfElementLocated(element))
  }
}
