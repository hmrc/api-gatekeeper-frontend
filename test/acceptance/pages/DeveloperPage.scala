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

object DeveloperPage extends WebPage {

  val apiFilterList = Map("Any" -> "One", "None" -> "two", "Individual PAYE" -> "ipaye")

  override val url: String = "http://localhost:9000/api-gatekeeper/developers"
  override def isCurrentPage: Boolean = {
    currentUrl == url
  }

  def emailDeveloperLink = find(linkText("Click here")).get

  def APIDropdown = find(name("filter_options")).get

  def emailDevelopers() = {
    click on emailDeveloperLink
  }

  def selectAPIDropdown() = {
    click on APIDropdown
  }

  def selectSelfAssessmentAPI() = {
    singleSel("filter_options").value = "selfass"
    singleSel("filter_options").value = apiFilterList.get("Any").toString
  }



}
