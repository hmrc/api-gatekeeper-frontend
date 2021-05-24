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

package pages

import common.{NavigationSugar, WebPage}
import org.openqa.selenium.Keys.ENTER
import pages.ApplicationsPage.APIFilter.APIFilterList

object ApplicationsPage extends WebPage with NavigationSugar {

  override val url: String = s"http://localhost:$port/api-gatekeeper/applications"

  override def isCurrentPage: Boolean = {
    currentUrl == url
  }

  def previousLink = find(linkText("Previous")).get

  def isForbidden() = {
    find(cssSelector("h1")).fold(false)(_.text == "You do not have permission to access Gatekeeper")
  }

  def nextLink = find(linkText("Next")).get

  def selectBySubscription(api: APIFilterList) = {
    singleSel("filter").value = api.name
  }

  def applicationsNavLink = find(linkText("Applications")).get


  def selectApplications() = {
    click on applicationsNavLink
  }

  def selectNoofRows(noOfRows: String) = {
    singleSel("pageSize").value = noOfRows
  }

  def showPreviousEntries() = {
    click on previousLink
  }

  def showNextEntries() = {
    click on nextLink
  }

  def selectByApplicationName(name: String) = {
    // If we use click we sometimes get a selenium error where it can't click on the element.
    // However, if we open using the keyboard, we don't get these problems.
    val element = find(linkText(name)).get
    element.underlying.sendKeys(ENTER)
  }

  def selectDeveloperByEmail(email: String) = {
    click on find(linkText(email)).get
  }

  def developersNavLink = find(linkText("Developers")).get

  def selectDevelopers() = {
    click on developersNavLink
    on(Developer2Page)
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
