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

package views.emails

import model.EmailOptionChoice.{API_SUBSCRIPTION, EMAIL_ALL_USERS, EMAIL_PREFERENCES}
import model.{APIDefinition, User}
import org.jsoup.nodes.Document
import utils.ViewHelpers.{elementExistsByAttr, elementExistsByIdWithAttr, elementExistsByText, elementExistsContainsText}


trait EmailLandingViewHelper extends EmailUsersHelper {

  def validateLandingPage(document: Document): Unit = {
    validatePageHeader(document, "Send emails to users based on")

    verifyEmailOptions(EMAIL_PREFERENCES, document, isDisabled = false)
    verifyEmailOptions(API_SUBSCRIPTION, document, isDisabled = false)
    verifyEmailOptions(EMAIL_ALL_USERS, document, isDisabled = false)
    elementExistsByIdWithAttr(document, EMAIL_PREFERENCES.toString, "checked") mustBe true

    validateButtonText(document, "submit", "Continue")
  }

}

trait EmailInformationViewHelper extends EmailUsersHelper {

  def validateApiSubcriptionInformationPage(document: Document): Unit = {
      elementExistsContainsText(document, "title", "Check you can send your email") mustBe true
      elementExistsByText(document, "h1", "Check you can send your email") mustBe true
      elementExistsContainsText(document, "div", "You can only email all users based on their API subscription if your message is about:") mustBe true
      elementExistsByText(document, "li", "important notices and service updates") mustBe true
      elementExistsByText(document, "li", "changes to any application they have") mustBe true
      elementExistsByText(document, "li", "making their application accessible") mustBe true
  }

  def validateAllUsersInformationPage(document: Document): Unit = {
      elementExistsContainsText(document, "title", "Check you can send your email") mustBe true
      elementExistsByText(document, "h2", "There is an error on the page") mustBe false
      elementExistsByText(document, "h1", "Check you can email all users") mustBe true
      elementExistsContainsText(document, "div", "You can only email all users if your message is about:") mustBe true
      elementExistsByText(document, "li", "important notices and service updates") mustBe true
      elementExistsByText(document, "li", "changes to any application they have") mustBe true
      elementExistsByText(document, "li", "making their application accessible") mustBe true
  }

}

trait EmailAllUsersViewHelper extends EmailUsersHelper with UserTableHelper {

  def validateEmailAllUsersPage(document: Document): Unit = {
    elementExistsByText(document, "h1", "Email all users") mustBe true

  }

  def validateResultsTable(document: Document, users: Seq[User]) = {
    elementExistsContainsText(document, "div", s"${users.size} results") mustBe true
    elementExistsByAttr(document, "a", "data-clip-text") mustBe users.nonEmpty
    verifyTableHeader(document, tableIsVisible = users.nonEmpty)
    users.foreach(user => verifyUserRow(document, user))
  }
}

trait EmailApiSubscriptionsViewHelper  extends EmailUsersHelper with UserTableHelper {
  def validateEmailApiSubscriptionsPage(document: Document, apis: Seq[APIDefinition]): Unit = {
     elementExistsByText(document, "h1", "Email all users subscribed to an API") mustBe true

     for(api: APIDefinition <- apis){
      elementExistsByText(document, "option", api.name) mustBe true
     }
     validateButtonText(document, "submit", "Filter")
  }
}
