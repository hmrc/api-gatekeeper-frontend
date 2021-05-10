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

import common.WebPage
import pages.DeveloperPage.APIFilter.APIFilterList
import pages.DeveloperPage.StatusFilter.StatusFilterList
import org.openqa.selenium.Keys.ENTER

object DeveloperPage extends WebPage {

  override val url: String = s"http://localhost:$port/api-gatekeeper/developers"
  override def isCurrentPage: Boolean = {
    currentUrl == url
  }

  def developerEmail(email:String) = find(linkText(email)).getOrElse(throw new IllegalArgumentException(s"Developer email link not found for ${email}"))

  def previousLink = find(linkText("Previous")).get

  def nextLink = find(linkText("Next")).get

  def selectByDeveloperEmail(email: String) = {
    // If we use click we sometimes get a selenium error where it can't click on the element.
    // However, if we open using the keyboard, we don't get these problems.
    val element = developerEmail(email)
    element.underlying.sendKeys(ENTER)
  }

  def selectBySubscription(api: APIFilterList) = {
    singleSel("filter").value = api.name
  }

  def selectByStatus(status: StatusFilterList) = {
    singleSel("status").value = status.name
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

  def emailButton() {
     find(cssSelector("#content div p a:nth-child(1)")).get
  }

object APIFilter  {

    sealed abstract class APIFilterList(val name: String) {}

    case object ALLUSERS extends APIFilterList("ALL")

    case object ONEORMORESUBSCRIPTION extends APIFilterList("ANYSUB")

    case object NOSUBSCRIPTION extends APIFilterList("NOSUB")

    case object NOAPPLICATIONS extends APIFilterList("NOAPP")

    case object ONEORMOREAPPLICATIONS extends APIFilterList("ANYAPP")

    case object INDIVIDUALPAYE extends APIFilterList("individual-paye")

    case object INDIVIDUALBENEFITS extends APIFilterList("individual-benefits")

    case object EMPLOYERSPAYE extends APIFilterList("employers-paye__1.0")

    case object INDIVIDUALTAX extends APIFilterList("itax")

    case object MARRIAGEALLOWANCE extends APIFilterList("marriageallowance")

    case object NATIONALINSURANCE extends APIFilterList("ni")

    case object PAYECHARGES extends APIFilterList("payech")

    case object PAYECREDITS extends APIFilterList("paye-credits")

    case object PAYEINTEREST extends APIFilterList("payei")

    case object PAYEPAYMENTS extends APIFilterList("payep")

    case object SELFASSESSMENT extends APIFilterList("self-assessment")

  }

  object StatusFilter  {

    sealed abstract class StatusFilterList(val name: String) {}

    case object ALL extends StatusFilterList("ALL")

    case object VERIFIED extends StatusFilterList("VERIFIED")

    case object UNVERIFIED extends StatusFilterList("UNVERIFIED")

    case object NOTREGISTERED extends StatusFilterList("UNREGISTERED")

  }



}
