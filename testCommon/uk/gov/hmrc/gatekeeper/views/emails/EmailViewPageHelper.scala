/*
 * Copyright 2023 HM Revenue & Customs
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

import uk.gov.hmrc.apiplatform.modules.common.utils.HmrcSpec
import uk.gov.hmrc.gatekeeper.models.EmailOptionChoice.{API_SUBSCRIPTION, EMAIL_ALL_USERS, EMAIL_PREFERENCES}
import uk.gov.hmrc.gatekeeper.models.EmailPreferencesChoice.{SPECIFIC_API, TAX_REGIME, TOPIC}
import uk.gov.hmrc.gatekeeper.models.TopicOptionChoice._
import uk.gov.hmrc.gatekeeper.models.{APICategoryDetails, ApiDefinition, ApiVersionDefinition, CombinedApi, RegisteredUser}
import uk.gov.hmrc.gatekeeper.utils.ViewHelpers._

trait EmailsPagesHelper extends EmailLandingViewHelper
    with EmailInformationViewHelper
    with EmailAllUsersViewHelper
    with EmailAPISubscriptionsViewHelper
    with EmailPreferencesChoiceViewHelper
    with EmailPreferencesTopicViewHelper
    with EmailPreferencesAPICategoryViewHelper
    with EmailPreferencesSpecificAPIViewHelper
    with EmailPreferencesSelectAPIViewHelper with APIDefinitionHelper {
  self: HmrcSpec =>
}

trait EmailLandingViewHelper extends EmailUsersHelper {
  self: HmrcSpec =>

  def validateLandingPage(document: Document): Unit = {
    validatePageHeader(document, "Send emails to users based on")

    verifyEmailOptions(EMAIL_PREFERENCES, document, isDisabled = false)
    verifyEmailOptions(API_SUBSCRIPTION, document, isDisabled = false)
    verifyEmailOptions(EMAIL_ALL_USERS, document, isDisabled = false)
    elementExistsByIdWithAttr(document, EMAIL_PREFERENCES.toString, "checked") shouldBe true

    validateButtonText(document, "submit", "Continue")
  }
}

trait EmailInformationViewHelper extends EmailUsersHelper {
  self: HmrcSpec =>

  def validateApiSubcriptionInformationPage(document: Document): Unit = {
    elementExistsContainsText(document, "title", "Check you can send your email") shouldBe true
    elementExistsByText(document, "h1", "Check you can send your email") shouldBe true
    elementExistsContainsText(document, "div", "You can only email users based on their API subscription if your message is about:") shouldBe true
    elementExistsByText(document, "li", "important notices and service updates") shouldBe true
    elementExistsByText(document, "li", "changes to any application they have") shouldBe true
    elementExistsByText(document, "li", "making their application accessible") shouldBe true
  }

  def validateAllUsersInformationPage(document: Document): Unit = {
    elementExistsContainsText(document, "title", "Check you can send your email") shouldBe true
    elementExistsByText(document, "h2", "There is an error on the page") shouldBe false
    elementExistsByText(document, "h1", "Check you can email all users") shouldBe true
    elementExistsContainsText(document, "div", "You can only email all users if your message is about:") shouldBe true
    elementExistsByText(document, "li", "important notices and service updates") shouldBe true
    elementExistsByText(document, "li", "changes to any application they have") shouldBe true
    elementExistsByText(document, "li", "making their application accessible") shouldBe true
  }

}

trait EmailAllUsersViewHelper extends EmailUsersHelper with UserTableHelper {
  self: HmrcSpec =>

  def validateEmailAllUsersPage(document: Document, users: Seq[RegisteredUser]): Unit = {
    elementExistsByText(document, "h1", "Email all users") shouldBe true
    elementExistsContainsText(document, "div", s"${users.size} results") shouldBe true
    validateCopyToClipboardLink(document, users)
    verifyTableHeader(document, tableIsVisible = users.nonEmpty)
    users.foreach(user => verifyUserRow(document, user))
  }
}

trait EmailAPISubscriptionsViewHelper extends EmailUsersHelper with UserTableHelper {
  self: HmrcSpec =>

  def validateEmailAPISubscriptionsPage(document: Document, apis: Seq[ApiDefinition]): Unit = {
    elementExistsByText(document, "h1", "Email all users subscribed to an API") shouldBe true

    for (api: ApiDefinition <- apis) {
      for (version: ApiVersionDefinition <- api.versions) {
        val versionOption = getElementBySelector(document, s"option[value=${api.context.value}__${version.version.value}]")
        withClue(s"dropdown option not rendered for ${api.serviceName} version ${version.version}") {
          versionOption.isDefined shouldBe true
        }
      }
    }
    validateButtonText(document, "filter", "Filter")
    validateCopyToClipboardLink(document, Seq.empty)
    verifyTableHeader(document, tableIsVisible = false)
  }

  def validateEmailAPISubscriptionsPage(document: Document, apis: Seq[ApiDefinition], selectedApiName: String, users: Seq[RegisteredUser]): Unit = {
    elementExistsByText(document, "h1", "Email all users subscribed to an API") shouldBe true

    getSelectedOptionValue(document).fold(fail("There should be a selected option"))(selectedValue => selectedValue shouldBe selectedApiName)
    validateButtonText(document, "filter", "Filter Again")

    for (api: ApiDefinition <- apis) {
      for (version: ApiVersionDefinition <- api.versions) {
        val versionOption = getElementBySelector(document, s"option[value=${api.context.value}__${version.version.value}]")
        versionOption.isDefined shouldBe true
      }
    }

    validateCopyToClipboardLink(document, users)
    verifyTableHeader(document, tableIsVisible = users.nonEmpty)
    users.foreach(verifyUserRow(document, _))
  }
}

trait EmailPreferencesChoiceViewHelper extends EmailUsersHelper with UserTableHelper { // Is UserTableHelper required here?
  self: HmrcSpec =>

  def validateEmailPreferencesChoicePage(document: Document): Unit = {
    validatePageHeader(document, "Who do you want to email?")

    verifyEmailPreferencesChoiceOptions(SPECIFIC_API, document)
    verifyEmailPreferencesChoiceOptions(TAX_REGIME, document)
    verifyEmailPreferencesChoiceOptions(TOPIC, document)
  }

  def validateEmailPreferencesChoiceNewPage(document: Document): Unit = {
    validateEmailPreferencesChoicePage(document)
    verifyEmailNonPrimaryLinks(document)
  }
}

trait EmailPreferencesTopicViewHelper extends EmailUsersHelper with UserTableHelper {
  self: HmrcSpec =>

  def validateEmailPreferencesTopicPage(document: Document) = {
    elementExistsByText(document, "h1", "Email users interested in a topic") shouldBe true
    checkElementsExistById(document, Seq(BUSINESS_AND_POLICY.toString, TECHNICAL.toString, RELEASE_SCHEDULES.toString, EVENT_INVITES.toString))

    validateButtonText(document, "filter", "Filter")

    validateCopyToClipboardLink(document)
    noInputChecked(document)
    verifyTableHeader(document, tableIsVisible = false)
  }

  def validateEmailPreferencesSelectTopicPage(document: Document) = {
    elementExistsByText(document, "h1", "Select the topic of the email") shouldBe true
    checkElementsExistById(document, Seq(BUSINESS_AND_POLICY.toString, TECHNICAL.toString, RELEASE_SCHEDULES.toString, EVENT_INVITES.toString))
  }

  def validateEmailPreferencesTopicResultsPage(document: Document, selectedTopic: TopicOptionChoice, users: Seq[RegisteredUser]) = {
    elementExistsByText(document, "h1", "Email users interested in a topic") shouldBe true
    checkElementsExistById(document, Seq(BUSINESS_AND_POLICY.toString, TECHNICAL.toString, RELEASE_SCHEDULES.toString, EVENT_INVITES.toString))
    isElementChecked(document, selectedTopic.toString)
    validateButtonText(document, "filter", "Filter Again")
    elementExistsContainsText(document, "div", s"${users.size} results") shouldBe true
    validateCopyToClipboardLink(document, users)

    if (users.nonEmpty) verifyTableHeader(document)
    users.foreach(verifyUserRow(document, _))
  }
}

trait EmailPreferencesAPICategoryViewHelper extends EmailUsersHelper with UserTableHelper {
  self: HmrcSpec =>

  private def validateCategoryDropDown(document: Document, categories: List[APICategoryDetails]) = {
    for (category <- categories) {
      withClue(s"Category: option `${category.category}` not in select list: ") {
        elementExistsByText(document, "option", category.name) shouldBe true
      }
    }
  }

  private def validateStaticPageElements(document: Document, categories: List[APICategoryDetails]) = {
    validatePageHeader(document, "Email users interested in a tax regime")
    validateCategoryDropDown(document, categories)
    checkElementsExistById(document, Seq(BUSINESS_AND_POLICY.toString, TECHNICAL.toString, RELEASE_SCHEDULES.toString, EVENT_INVITES.toString))
  }

  private def validateStaticPageElementsInTaxRegime(document: Document, categories: List[APICategoryDetails], expectedDestination: String) = {
    validatePageHeader(document, "Email users interested in a tax regime")
    validateCategoryDropDown(document, categories)
    validateFormDestination(document, "taxRegimeForm", expectedDestination)
  }

  def validateEmailPreferencesAPICategoryPage(document: Document, categories: List[APICategoryDetails]) = {
    validateStaticPageElements(document, categories)
    validateCopyToClipboardLink(document, Seq.empty)

    getSelectedOptionValue(document) shouldBe None

    verifyTableHeader(document, tableIsVisible = false)
  }

  def validateEmailPreferencesSpecificCategoryPage(document: Document, selectedCategories: List[APICategoryDetails]) = {
    val sizeOfSelectedCategories = selectedCategories.size
    val headerTitle              = if (sizeOfSelectedCategories < 2) "tax regime" else "tax regimes"
    validatePageHeader(document, s"You have selected $sizeOfSelectedCategories $headerTitle")
    validateHiddenSelectedTaxRegimeValues(document, selectedCategories, 2)
  }

  def validateEmailPreferencesSelectedApiTopicPage(document: Document, users: Seq[RegisteredUser]) = {
    validatePageHeader(document, "Email users interested in a specific API")

    withClue(s"Copy to clipboard link validation failed") {
      elementExistsById(document, "copy-users-to-clip") shouldBe users.nonEmpty
      elementExistsById(document, "compose-email") shouldBe users.nonEmpty
    }

    verifyTableHeader(document)
  }

  def validateEmailPreferencesSelectedTaxRegimePage(document: Document, users: Seq[RegisteredUser]) = {
    validatePageHeader(document, "Email users interested in a tax regime")

    withClue(s"Copy to clipboard link validation failed") {
      elementExistsById(document, "copyUsersToClip") shouldBe users.nonEmpty
      elementExistsById(document, "composeEmail") shouldBe users.nonEmpty
    }

    verifyTableHeader(document)
  }
  def validateEmailPreferencesAPICategoryPageWithCategoryFilter(document: Document, categories: List[APICategoryDetails], selectedCategory: APICategoryDetails) = {
    validateStaticPageElements(document, categories)
    validateCopyToClipboardLink(document, Seq.empty)

    getSelectedOptionValue(document) shouldBe Some(selectedCategory.category)
    noInputChecked(document)

    verifyTableHeader(document, tableIsVisible = false)
  }

  def validateEmailPreferencesAPICategoryResultsPage(
      document: Document,
      categories: List[APICategoryDetails],
      mayBeSelectedCategory: Option[APICategoryDetails],
      selectedTopic: TopicOptionChoice,
      users: Seq[RegisteredUser]
    ) = {
    validateStaticPageElements(document, categories)

    mayBeSelectedCategory.map { selectedCategory =>
      elementExistsContainsText(document, "div", s"${users.size} results") shouldBe true
      validateCopyToClipboardLink(document, users)
      getSelectedOptionValue(document) shouldBe Some(selectedCategory.category)
      verifyTableHeader(document, tableIsVisible = users.nonEmpty)
      users.foreach(verifyUserRow(document, _))
    }

    isElementChecked(document, selectedTopic.toString)
  }

  def validateEmailPreferencesSelectTaxRegimeResultsPage(
      document: Document,
      categories: List[APICategoryDetails],
      expectedDestination: String
    ) = {
    validateStaticPageElementsInTaxRegime(document, categories, expectedDestination)
    validateButtonText(document, "continue", "Continue")
  }

  def validateSelectTaxRegimePageWithPreviouslySelectedTaxRegimes(
      document: Document,
      categories: List[APICategoryDetails],
      selectedCategories: List[APICategoryDetails],
      expectedDestination: String
    ) = {
    validateStaticPageElementsInTaxRegime(document, categories, expectedDestination)
    validateHiddenSelectedTaxRegimeValues(document, selectedCategories)
  }
}

trait EmailPreferencesSpecificAPIViewHelper extends EmailUsersHelper with UserTableHelper {
  self: HmrcSpec =>

  private def validateStaticPageElementsNew(document: Document, filterButtonText: String, selectedTopic: Option[TopicOptionChoice]) {
    validateFormDestination(document, "apiFilters", "/api-gatekeeper/emails/email-preferences/select-api-new")
  }

  def validateEmailPreferencesSpecificApiPageNew(document: Document, selectedApis: Seq[CombinedApi]) = {
    val sizeOfSelectedApis = selectedApis.size
    val headerTitle        = if (sizeOfSelectedApis < 2) "API" else "APIs"
    validatePageHeader(document, s"You have selected $sizeOfSelectedApis $headerTitle")
    validateStaticPageElementsNew(document, "Filter", None)
    validateHiddenSelectedApiValues(document, selectedApis, 2)
    verifyTableHeader(document, tableIsVisible = false)
  }

  private def validateStaticPageElements(document: Document, filterButtonText: String, selectedTopic: Option[TopicOptionChoice]) {
    validatePageHeader(document, "Email users interested in a specific API")
    validateFormDestination(document, "apiFilters", "/api-gatekeeper/emails/email-preferences/select-api")
    validateFormDestination(document, "topicFilter", "/api-gatekeeper/emails/email-preferences/by-specific-api")
    validateButtonText(document, "filter", filterButtonText)
    validateTopicGrid(document, selectedTopic)
  }

  def validateEmailPreferencesSpecificApiPage(document: Document, selectedApis: Seq[CombinedApi]) = {
    validateStaticPageElements(document, "Filter", None)
    validateHiddenSelectedApiValues(document, selectedApis, 2)
    verifyTableHeader(document, tableIsVisible = false)
  }

  def validateEmailPreferencesSpecificAPIWithOnlyTopicFilter(document: Document, selectedTopic: TopicOptionChoice) = {
    validateStaticPageElements(document, "Filter Again", Some(selectedTopic))
    verifyTableHeader(document, tableIsVisible = false)
  }

  def validateEmailPreferencesSpecificAPIResults(
      document: Document,
      selectedTopic: TopicOptionChoice,
      selectedAPIs: Seq[CombinedApi],
      users: Seq[RegisteredUser],
      emailsString: String
    ) = {
    validateStaticPageElements(document, "Filter Again", Some(selectedTopic))
    validateSelectedSpecificApiItems(document, selectedAPIs)
    validateHiddenSelectedApiValues(document, selectedAPIs, 2)
    verifyTableHeader(document, users.nonEmpty)
    users.foreach(verifyUserRow(document, _))

    validateCopyToClipboardLink(document, users)
  }

  def validateEmailPreferencesSpecificAPIResults(
      document: Document,
      selectedTopic: TopicOptionChoice,
      selectedAPIs: Seq[CombinedApi]
    ) = {
    validateStaticPageElements(document, "Filter Again", Some(selectedTopic))
    validateSelectedSpecificApiItems(document, selectedAPIs)
  }
}

trait EmailPreferencesSelectAPIViewHelper extends EmailUsersHelper {
  self: HmrcSpec =>

  private def validateStaticPageElements(document: Document, dropDownAPIs: Seq[CombinedApi], expectedDestination: String) {
    validatePageHeader(document, "Email users interested in a specific API")
    validateNonSelectedApiDropDown(document, dropDownAPIs, "Select an API")

    validateFormDestination(document, "apiSelectionForm", expectedDestination)
  }

  private def validateStaticPageElementsCurrent(document: Document, dropDownAPIs: Seq[CombinedApi], expectedDestination: String) {
    validateStaticPageElements(document, dropDownAPIs, expectedDestination)
    validateButtonText(document, "submit", "Select API")
  }

  private def validateStaticPageElementsNew(document: Document, dropDownAPIs: Seq[CombinedApi], expectedDestination: String) {
    validateStaticPageElements(document, dropDownAPIs, expectedDestination)
    validateButtonText(document, "submit", "Continue")
  }

  def validateSelectAPIPageWithNonePreviouslySelected(document: Document, dropDownAPIs: Seq[CombinedApi], expectedDestination: String) = {
    validateStaticPageElementsCurrent(document, dropDownAPIs, expectedDestination)
  }

  def validateSelectAPIPageWithNonePreviouslySelectedNew(document: Document, dropDownAPIs: Seq[CombinedApi], expectedDestination: String) = {
    validateStaticPageElementsNew(document, dropDownAPIs, expectedDestination)
  }

  def validateSelectAPIPageWithPreviouslySelectedAPIs(document: Document, dropDownAPIs: Seq[CombinedApi], selectedAPIs: Seq[CombinedApi], expectedDestination: String) = {
    validateStaticPageElements(document, dropDownAPIs, expectedDestination)
    validateHiddenSelectedApiValues(document, selectedAPIs)
  }
}
