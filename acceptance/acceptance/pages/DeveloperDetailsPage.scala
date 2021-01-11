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

package acceptance.pages

import acceptance.WebPage
import acceptance.testdata.CommonTestData
import acceptance.specs.MockDataSugar

object DeveloperDetailsPage extends WebPage with utils.UrlEncoding with CommonTestData {

  override val url: String = s"http://localhost:$port/api-gatekeeper/developer?email=${encode({MockDataSugar.developer8})}"

  override def isCurrentPage: Boolean = {
    currentUrl == url
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

  def mfaEnabled() = {
    find(cssSelector("#mfaEnabled")).get.text
  }

  def removeMfaButton = {
    find(cssSelector("#remove-2SV"))
  }

  def selectByApplicationName(name: String) = {
    click on find(linkText(name)).get
  }

  def removeMfa() = {
    click on removeMfaButton.get
  }

}
