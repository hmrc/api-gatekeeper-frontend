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

import org.openqa.selenium.{By, Keys, WebElement}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.gatekeeper.common.{Env, WebPage}
import uk.gov.hmrc.gatekeeper.pages.ApplicationsPage.APIFilter.APIFilterList


object ApplicationsPage extends WebPage {
  override val pageHeading: String = "Applications"

  override val url: String = s"http://localhost:${Env.port}/api-gatekeeper/applications"

  def isForbidden(): Boolean = {
    getText(By.tagName("h1")) == "You do not have permission to access Gatekeeper"
  }

  def selectBySubscription(api: APIFilterList) = {
    getSelectBox(By.id("filter")).selectByValue(api.name)
  }

  def clickApplicationsNavLink() = {
    click(By.linkText("Applications"))
  }

  def selectNoofRows(noOfRows: String) = {
    getSelectBox(By.id("pageSize")).selectByValue(noOfRows)
  }

  def showPreviousEntries() = {
    click(By.linkText("Previous"))
  }

  def showNextEntries() = {
    click(By.linkText("Next"))
  }

  def clickApplicationNameLink(name: String) = {
    // If we use click we sometimes get a selenium error where it can't click on the element.
    // However, if we open using the keyboard, we don't get these problems.
    val element: WebElement = findElement(By.linkText(name))
    element.sendKeys(Keys.ENTER)
  }

  def selectDeveloperByEmail(email: LaxEmailAddress) = {
    click(By.linkText(email.text))
  }

  def selectDevelopers() = {
    click(By.linkText("Developers"))
  }

  def clickOnReview() = {
    click(By.id("review"))
  }
  
  def getApplicationName(): String = {
    getText(By.className("hmrc-header__service-name"))
  }

  object APIFilter {

    sealed abstract class APIFilterList(val name: String) {}

    case object ALLUSERS extends APIFilterList("ALL")

    case object ONEORMORESUBSCRIPTION extends APIFilterList("ANYSUB")

    case object NOSUBSCRIPTION extends APIFilterList("NOSUB")

    case object NOAPPLICATIONS extends APIFilterList("NOAPP")

    case object ONEORMOREAPPLICATIONS extends APIFilterList("ANYAPP")

    case object EMPLOYERSPAYE extends APIFilterList("Employers PAYE")

  }

}
