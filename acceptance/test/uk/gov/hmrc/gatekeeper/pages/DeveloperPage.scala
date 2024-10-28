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

package uk.gov.hmrc.gatekeeper.pages

import org.openqa.selenium.{By, Keys}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{Environment, LaxEmailAddress}
import uk.gov.hmrc.gatekeeper.common.{Env, WebPage}
import uk.gov.hmrc.gatekeeper.pages.DeveloperPage.APIFilter.APIFilterList

object DeveloperPage extends WebPage {

  override val pageHeading: String = "Developers"

  override val url: String = s"http://localhost:${Env.port}/api-gatekeeper/developers"

  def selectByDeveloperEmail(email: LaxEmailAddress) = {
    // If we use click we sometimes get a selenium error where it can't click on the element.
    // However, if we open using the keyboard, we don't get these problems.
    val element = findElement(By.linkText(email.text))
    element.sendKeys(Keys.ENTER)
  }

  def writeInSearchBox(text: String) = {
    writeInTextBox(text, "textFilter")
  }

  def searchByPartialEmail(partialEmail: String): Unit = {
    writeInSearchBox(partialEmail)
    clickSubmit()
  }

  def selectBySubscription(api: APIFilterList) = {
    getSelectBox(By.id("apiVersionFilter")).selectByValue(api.name)
  }

  def selectByEnvironment(environment: Environment) = {
    getSelectBox(By.id("environmentFilter")).selectByValue(environment.toString)
  }

  def selectByDeveloperStatus(status: String) = {
    getSelectBox(By.id("developerStatusFilter")).selectByValue(status)
  }

  def getDeveloperFirstNameByIndex(index: Int): String = {
    getText(By.id(s"dev-fn-$index"))
  }

  def getDeveloperSurnameByIndex(index: Int): String = {
    getText(By.id(s"dev-sn-$index"))
  }

  def getDeveloperEmailByIndex(index: Int): String = {
    getText(By.id(s"dev-email-$index"))
  }

  def getDeveloperStatusByIndex(index: Int): String = {
    getText(By.id(s"dev-status-$index"))
  }

  def developerRowExists(rowIndex: Int): Boolean = {
    findElements(By.id(s"dev-fn-$rowIndex")).nonEmpty
  }

  object APIFilter {

    sealed abstract class APIFilterList(val name: String) {}

    case object EMPLOYERSPAYE extends APIFilterList("employers-paye__1.0")

  }
}
