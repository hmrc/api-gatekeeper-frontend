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

import org.openqa.selenium.Keys.ENTER

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.gatekeeper.common.WebPage
import uk.gov.hmrc.gatekeeper.pages.DeveloperPage.APIFilter.APIFilterList

object DeveloperPage extends WebPage {
  override val url: String = s"http://localhost:$port/api-gatekeeper/developers"
  override def isCurrentPage: Boolean = {
    currentUrl == url
  }

  def developerEmail(email:LaxEmailAddress) = find(linkText(email.text)).get

  private def searchBox = textField("emailFilter")

  private def submitButton = find(id("submit")).get

  private def filterBySubscription = singleSel(id("apiVersionFilter"))

  private def filterByEnvironment = singleSel(id("environmentFilter"))

  private def filterByDeveloperStatus = singleSel(id("developerStatusFilter"))

  def selectByDeveloperEmail(email: LaxEmailAddress) = {
    // If we use click we sometimes get a selenium error where it can't click on the element.
    // However, if we open using the keyboard, we don't get these problems.
    val element = developerEmail(email)
    element.underlying.sendKeys(ENTER)
  }

  def writeInSearchBox(text: String) = {
    searchBox.value = text
  }

  def searchByPartialEmail(partialEmail: String) = {
    writeInSearchBox(partialEmail)
    click on submitButton
  }

  def selectBySubscription(api: APIFilterList) = {
    filterBySubscription.value = api.name
  }

  def selectByEnvironment(environment: String) = {
    filterByEnvironment.value = environment
  }

  def selectByDeveloperStatus(status: String) = {
    filterByDeveloperStatus.value = status
  }

  object APIFilter  {

    sealed abstract class APIFilterList(val name: String) {}

    case object EMPLOYERSPAYE extends APIFilterList("employers-paye__1.0")

  }
}
