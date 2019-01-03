/*
 * Copyright 2019 HM Revenue & Customs
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

package acceptance.pages

import acceptance.WebPage
import acceptance.pages.ApplicationsPage.APIFilter.APIFilterList

object ApplicationsPage extends WebPage {

  override val url: String = s"http://localhost:$port/api-gatekeeper/applications"
  override def isCurrentPage: Boolean = {
    currentUrl == url
  }

  def previousLink = find(linkText("Previous")).get

  def isUnauthorised() = {
    find(cssSelector("h2")).fold(false)(_.text == "Only Authorised users can access the requested page")
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
    click on find(linkText(name)).get
  }

  def selectDeveloperByEmail(email: String) = {
    click on find(linkText(email)).get
  }

  def developersNavLink = find(linkText("Developers")).get

  def selectDevelopers() = {
    click on developersNavLink
  }


  object APIFilter  {
    sealed abstract class APIFilterList(val name: String) {}

    case object ALLUSERS extends APIFilterList("ALL")
    case object ONEORMORESUBSCRIPTION extends APIFilterList("ANYSUB")
    case object NOSUBSCRIPTION extends APIFilterList("NOSUB")
    case object NOAPPLICATIONS extends APIFilterList("NOAPP")
    case object ONEORMOREAPPLICATIONS extends APIFilterList("ANYAPP")
    case object EMPLOYERSPAYE extends APIFilterList("Employers PAYE")
  }
}
