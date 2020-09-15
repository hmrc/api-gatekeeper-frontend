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
import model.EmailPreferencesChoice.{SPECIFIC_API, TAX_REGIME, TOPIC}
import model.{APIDefinition, APIVersion, EmailPreferencesChoice, User, TopicOptionChoice, APICategory}
import model.TopicOptionChoice._
import model.APICategory._
import org.jsoup.nodes.Document
import utils.ViewHelpers.{elementExistsByAttr, elementExistsByIdWithAttr, elementExistsByText, elementExistsContainsText, getElementBySelector, getSelectedOptionValue}

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
       for(version: APIVersion <- api.versions) {
         val versionOption = getElementBySelector(document, s"option[value=${api.context}__${version.version}]")
         versionOption.isDefined mustBe true
       }
     }
     validateButtonText(document, "filter", "Filter")
  }

  def validateEmailApiSubscriptionsPage(document: Document, apis: Seq[APIDefinition], selectedApiName : String): Unit = {
    elementExistsByText(document, "h1", "Email all users subscribed to an API") mustBe true
    getSelectedOptionValue(document).fold(fail("There should be a selected option"))(selectedValue => selectedValue mustBe selectedApiName)
    for(api: APIDefinition <- apis){
      for(version: APIVersion <- api.versions) {
        val versionOption = getElementBySelector(document, s"option[value=${api.context}__${version.version}]")
        versionOption.isDefined mustBe true
      }
    }
    validateButtonText(document, "filter", "Filter Again")
  }
}

trait EmailPreferencesChoiceViewHelper extends EmailUsersHelper with UserTableHelper { // Is UserTableHelper required here?
  def validateEmailPreferencesChoicePage(document: Document): Unit = {
    validatePageHeader(document, "Who do you want to email?")

    verifyEmailPreferencesChoiceOptions(SPECIFIC_API, document)
    verifyEmailPreferencesChoiceOptions(TAX_REGIME, document)
    verifyEmailPreferencesChoiceOptions(TOPIC, document)
  }
}

trait EmailPreferencesTopicViewHelper extends EmailUsersHelper with UserTableHelper {
  def validateEmailPreferencesTopicPage(document: Document) = {
    elementExistsByText(document, "h1", "Email users interested in a topic") mustBe true
    checkElementsExistById(document, Seq(BUSINESS_AND_POLICY.toString, TECHNICAL.toString, RELEASE_SCHEDULES.toString, EVENT_INVITES.toString))

    validateButtonText(document, "filter", "Filter")

    elementExistsByAttr(document, "a", "data-clip-text") mustBe false
    noInputChecked(document)
    verifyTableHeader(document, tableIsVisible = false)
  }

  def validateEmailPreferencesTopicResultsPage(document: Document, selectedTopic: TopicOptionChoice, users: Seq[User]) = {
    elementExistsByText(document, "h1", "Email users interested in a topic") mustBe true
    checkElementsExistById(document, Seq(BUSINESS_AND_POLICY.toString, TECHNICAL.toString, RELEASE_SCHEDULES.toString, EVENT_INVITES.toString))
    isElementChecked(document, selectedTopic.toString)
    validateButtonText(document, "filter", "Filter Again")
    elementExistsContainsText(document, "div", s"${users.size} results") mustBe true
    elementExistsByAttr(document, "a", "data-clip-text") mustBe users.nonEmpty
    if(users.nonEmpty){
      verifyTableHeader(document)
    }
    users.foreach(verifyUserRow(document, _))
  }
}

trait EmailPreferencesAPICategoryViewHelper extends EmailUsersHelper with UserTableHelper {
  private def validateCategoryDropDown(document: Document, categories: List[APICategory]) = {
    for(category <- categories){
      withClue(s"Category: option `${category.category}` not in select list: ") {
        elementExistsByText(document, "option", category.name) mustBe true
      }
    }
  }

  private def validateStaticPageElements(document: Document, categories: List[APICategory]) = {
      validatePageHeader(document, "Email users interested in a tax regime")
      validateCategoryDropDown(document, categories)
      checkElementsExistById(document, Seq(BUSINESS_AND_POLICY.toString, TECHNICAL.toString, RELEASE_SCHEDULES.toString, EVENT_INVITES.toString))
  }

  def validateEmailPreferencesAPICategoryPage(document: Document, categories: List[APICategory]) = {
    validateStaticPageElements(document, categories)
    validateCopyToClipboardLink(document, isVisible = false)

    getSelectedOptionValue(document) mustBe None

    verifyTableHeader(document, tableIsVisible = false)
  }

  def validateEmailPreferencesAPICategoryPageWithCategoryFilter(document: Document, categories: List[APICategory], selectedCategory: APICategory) = {
    validateStaticPageElements(document, categories)
    validateCopyToClipboardLink(document, isVisible = false)

    getSelectedOptionValue(document) mustBe Some(selectedCategory.category)
    noInputChecked(document)

    verifyTableHeader(document, false)
  }

  def validateEmailPreferencsAPICategoryResultsPage(document: Document, categories: List[APICategory], selectedCategory: APICategory, selectedTopic: TopicOptionChoice, users: Seq[User]) = {
    validateStaticPageElements(document, categories)
    elementExistsContainsText(document, "div", s"${users.size} results") mustBe true
    validateCopyToClipboardLink(document, isVisible = users.nonEmpty)
    getSelectedOptionValue(document) mustBe Some(selectedCategory.category)

    isElementChecked(document, selectedTopic.toString)

    verifyTableHeader(document, tableIsVisible = users.nonEmpty)

    users.foreach(verifyUserRow(document, _))
  }
}

trait EmailPreferencesSpecificAPIViewHelper extends EmailUsersHelper with UserTableHelper {
  private def validateStaticPageElements(document: Document, filterButtonText: String, selectedTopic: Option[TopicOptionChoice]) {
    validatePageHeader(document, "Email users interested in a specific API")
    validateFormDestination(document, "api-filters", "/api-gatekeeper/emails/email-preferences/select-api")
    validateFormDestination(document, "topic-filter", "/api-gatekeeper/emails/email-preferences/by-specific-api")
    validateButtonText(document, "filter", filterButtonText)
    validateTopicGrid(document, selectedTopic)
  }

  def validateEmailPreferencesSpecificAPIPage(document: Document, selectedApis: Seq[APIDefinition]) = {
    validateStaticPageElements(document, "Filter", None)
    validateHiddenSelectedApiValues(document, selectedApis, 2)
    verifyTableHeader(document, tableIsVisible = false)
  }

  def validateEmailPreferencesSpecificAPIWithOnlyTopicFilter(document: Document, selectedTopic: TopicOptionChoice) = {
    validateStaticPageElements(document, "Filter Again", Some(selectedTopic))
    verifyTableHeader(document, tableIsVisible = false)
  }

  def validateEmailPreferencesSpecificAPIResults(document: Document, selectedTopic: TopicOptionChoice, selectedAPIs: Seq[APIDefinition], users: Seq[User], emailsString: String) = {
    validateStaticPageElements(document, "Filter Again", Some(selectedTopic))
    validateSelectedSpecificApiItems(document, selectedAPIs)
    validateHiddenSelectedApiValues(document, selectedAPIs, 2)
    verifyTableHeader(document)
    users.foreach(verifyUserRow(document, _))
    
    validateCopyToClipboardValue(document, emailsString)
  }
}

trait EmailPreferencesSelectAPIViewHelper extends EmailUsersHelper {
  private def validateStaticPageElements(document: Document, dropDownAPIs: Seq[APIDefinition]){
    validatePageHeader(document, "Email users interested in a specific API")
    validateNonSelectedApiDropDown(document, dropDownAPIs, "Select an API")

    validateFormDestination(document, "apiSelectionForm", "/api-gatekeeper/emails/email-preferences/by-specific-api")
    validateButtonText(document, "submit", "Select API")
  }

  def validateSelectAPIPageWithNonePreviouslySelected(document: Document, dropDownAPIs: Seq[APIDefinition]) = {
    validateStaticPageElements(document, dropDownAPIs)
  }

  def validateSelectAPIPageWithPreviouslySelectedAPIs(document: Document, dropDownAPIs: Seq[APIDefinition], selectedAPIs: Seq[APIDefinition]) = {
    validateStaticPageElements(document, dropDownAPIs)
    validateHiddenSelectedApiValues(document, selectedAPIs)
  }
}
