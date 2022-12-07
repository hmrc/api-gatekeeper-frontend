/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.views.emails

import org.jsoup.nodes.Document
import uk.gov.hmrc.gatekeeper.models.RegisteredUser
import uk.gov.hmrc.gatekeeper.utils.ViewHelpers._
import org.scalatest.matchers.should.Matchers

trait UserTableHelper {
  self: Matchers =>

  def verifyUserRow(document: Document, user: RegisteredUser): Unit = {
    elementExistsByText(document, "td", user.email) shouldBe true
    elementExistsByText(document, "td", user.firstName) shouldBe true
    elementExistsByText(document, "td", user.lastName) shouldBe true
  }

  def verifyTableHeader(document: Document, tableIsVisible: Boolean = true): Unit = {
    elementExistsByText(document, "th", "Email") shouldBe tableIsVisible
    elementExistsByText(document, "th", "First name") shouldBe tableIsVisible
    elementExistsByText(document, "th", "Last name") shouldBe tableIsVisible
  }
}
