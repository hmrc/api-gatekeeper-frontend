/*
 * Copyright 2021 HM Revenue & Customs
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

package views.emails

import org.jsoup.nodes.Document
import model.User
import org.scalatest.MustMatchers
import utils.ViewHelpers._

trait UserTableHelper extends MustMatchers  {
  
    def verifyUserRow(document: Document, user: User): Unit ={
      elementExistsByText(document, "td", user.email) mustBe true
      elementExistsByText(document, "td", user.firstName) mustBe true
      elementExistsByText(document, "td", user.lastName) mustBe true
    }

    def verifyTableHeader(document: Document, tableIsVisible: Boolean = true): Unit ={
      elementExistsByText(document, "th", "Email") mustBe tableIsVisible
      elementExistsByText(document, "th", "First name") mustBe tableIsVisible
      elementExistsByText(document, "th", "Last name") mustBe tableIsVisible
    }
}
