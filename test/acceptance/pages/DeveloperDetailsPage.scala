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

package acceptance.pages

import acceptance.WebPage
import acceptance.pages.ApplicationsPage.{find, linkText}

object DeveloperDetailsPage extends WebPage {

  override val url: String = "http://localhost:9000/api-gatekeeper/developer/Dixie.Upton@mail.com"

  override def isCurrentPage: Boolean = {
    currentUrl == url
  }

  def firstName = {
    find(cssSelector("#first-name")).get.text
  }

  def lastName = {
    find(cssSelector("#last-name")).get.text
  }

  def status = {
    find(cssSelector("#status")).get.text
  }

  def selectByApplicationName(name: String) = {
    click on find(linkText(name)).get
  }

}
