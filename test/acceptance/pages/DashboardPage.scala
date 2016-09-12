/*
 * Copyright 2016 HM Revenue & Customs
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

object DashboardPage extends WebPage {

  override val url: String = "http://localhost:9000/api-gatekeeper/dashboard"
  override def isCurrentPage: Boolean = {
    currentUrl == url
  }

  def isUnauthorised() = {
    find(cssSelector("h2")).fold(false)(_.text == "Only Authorised users can access the requested page")
  }

  def developerListLink = find(linkText("Developer List")).get

<<<<<<< HEAD
  def selectDeveloperList = {
=======
  def selectDeveloperList() = {
>>>>>>> b8fa63e9de284ac7a84cc2458be375293a9c763b
    click on developerListLink
  }


}
