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
import acceptance.pages.DeveloperPage.APIFilter.APIFilterList

object DeveloperPage extends WebPage {

  override val url: String = "http://localhost:9000/api-gatekeeper/developers"
  override def isCurrentPage: Boolean = {
    currentUrl == url
  }

  def emailDeveloperLink = find(linkText("Click here")).get

  def APIDropdown = find(name("filter_options")).get

  def rowsDropdowwn = find(name("perpage")).get.toString()

  def emailDevelopers() = {
    click on emailDeveloperLink
  }

  def selectAPI (api: APIFilterList) = {
    singleSel("filter_options").value = api.name
  }

  def selectNoofRows(noOfRows: String) = {
    click on rowsDropdowwn
    singleSel(rowsDropdowwn).value = noOfRows
  }

  object APIFilter  {

    sealed abstract class APIFilterList(val name: String) {}

    case object ANY extends APIFilterList("one")

    case object NONE extends APIFilterList("two")

    case object INDIVIDUALPAYE extends APIFilterList("ipaye")

    case object INDIVIDUALEMPLOYMENT extends APIFilterList("iemp")

    case object INDIVIDUALINCOME extends APIFilterList("iincome")

    case object INDIVIDUALTAX extends APIFilterList("itax")

    case object MARRIAGEALLOWANCE extends APIFilterList("marriageallowance")

    case object NATIONALINSURANCE extends APIFilterList("ni")

    case object PAYECHARGES extends APIFilterList("payech")

    case object PAYECREDITS extends APIFilterList("payecr")

    case object PAYEINTEREST extends APIFilterList("payei")

    case object PAYEPAYMENTS extends APIFilterList("payep")

    case object SELFASSESSMENT extends APIFilterList("selfass")

  }



}
