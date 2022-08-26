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

package uk.gov.hmrc.gatekeeper.pages

import uk.gov.hmrc.gatekeeper.common.WebPage
import uk.gov.hmrc.gatekeeper.testdata.CommonTestData
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

object DeveloperDetailsPage extends WebPage with UrlEncoding with CommonTestData {

  override val url: String = s"http://localhost:$port/api-gatekeeper/developer"

  override def isCurrentPage: Boolean = {
    currentUrl.startsWith(url)
  }

  def firstName() = {
    find(cssSelector("#first-name")).get.text
  }

  def lastName() = {
    find(cssSelector("#last-name")).get.text
  }

  def status() = {
    find(cssSelector("#status")).get.text
  }

  def mfaHeading() = {
    find(cssSelector("#mfa-heading")).get.text
  }

  def removeMfaLink = {
    find(cssSelector("#remove-2SV"))
  }

  def authAppMfaType = {
    find(cssSelector("#mfa-type-0"))
  }

  def authAppMfaName = {
    find(cssSelector("#mfa-name-0"))
  }

  def selectByApplicationName(name: String) = {
    click on find(linkText(name)).get
  }

  def removeMfa() = {
    click on removeMfaLink.get
  }

}
