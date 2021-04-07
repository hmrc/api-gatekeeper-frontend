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

object RemoveMfaPage extends WebPage {

  override val url: String = s"http://localhost:$port/api-gatekeeper/developer/mfa/remove"

  override def isCurrentPage: Boolean = {
    currentUrl.startsWith(url)
  }

  def removeMfaButton: RemoveMfaPage.Element = {
    find(cssSelector("#submit")).get
  }

  def removeMfa(): Unit = {
    click on removeMfaButton
  }
}
