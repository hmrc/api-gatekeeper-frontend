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

object removeMfaPage extends WebPage {

  override val url: String = s"http://localhost:$port/api-gatekeeper/developer/mfa/remove?email=Dixie.fakename%40example.com"

  override def isCurrentPage: Boolean = {
    currentUrl == url
  }

  def removeMfaButton: removeMfaPage.Element = {
    find(cssSelector("#submit")).get
  }

  def removeMfa(): Unit = {
    click on removeMfaButton
  }
}
