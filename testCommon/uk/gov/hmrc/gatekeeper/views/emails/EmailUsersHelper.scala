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

import org.jsoup.nodes.{Document, Element}

import uk.gov.hmrc.apiplatform.modules.common.utils.HmrcSpec
import uk.gov.hmrc.gatekeeper.models.ApiType.{REST_API, XML_API}
import uk.gov.hmrc.gatekeeper.models.EmailOptionChoice.{EmailOptionChoice, optionHint, optionLabel}
import uk.gov.hmrc.gatekeeper.models.EmailPreferencesChoice.EmailPreferencesChoice
import uk.gov.hmrc.gatekeeper.models.TopicOptionChoice.TopicOptionChoice
import uk.gov.hmrc.gatekeeper.models.{CombinedApi, RegisteredUser, _}
import uk.gov.hmrc.gatekeeper.utils.ViewHelpers._

trait EmailUsersHelper extends APIDefinitionHelper with CombinedApiHelper {
  self: HmrcSpec =>

  def validatePageHeader(document: Document, expectedTitle: String) = {
    val maybeTitleText = getElementBySelector(document, "#pageTitle")
    maybeTitleText.fold(fail("page title not present in page"))(_.text shouldBe expectedTitle)
  }

  def isElementChecked(document: Document, expectedValue: String, shouldBeChecked: Boolean = true): Unit = {
    val checkedRadio = getElementBySelector(document, "input[checked]")
    checkedRadio.isDefined shouldBe true
    checkedRadio.head.attr("value").equalsIgnoreCase(expectedValue) shouldBe shouldBeChecked
  }

  def noInputChecked(document: Document): Unit = {
    val checkedRadio = getElementBySelector(document, "input[checked]")
    checkedRadio.isDefined shouldBe false
  }

  def checkElementsExistById(document: Document, ids: Seq[String]): Unit = {
    ids.foreach(id => {
      withClue(s"$id element exists?:") {
        elementExistsById(document, id) shouldBe true
      }
      ()
    })
  }

  def validateCopyToClipboardLink(document: Document, users: Seq[RegisteredUser] = Seq.empty) = {
    withClue(s"Copy to clipboard link validation failed") {
      elementExistsById(document, "copy-users-to-clip") shouldBe users.nonEmpty
    }

    if (users.nonEmpty) {
      val expectedValue = users.map(_.email.text).sorted.mkString("; ")

      getElementBySelector(document, "#emails-to-copy")
        .fold(fail("Copy to clipboard button not found"))(element => element.text.contains(expectedValue) shouldBe true)
    }
  }

  def handleXmlAppendValue(api: CombinedApi) = {
    api.apiType match {
      case XML_API  => api.displayName + " - XML API"
      case REST_API => api.displayName
    }
  }

  def validateNonSelectedApiDropDown(document: Document, apis: Seq[CombinedApi], defaultOption: String) = {

    val combinedTuples = Seq(("", defaultOption)) ++ apis.flatMap(x => Seq((x.serviceName, handleXmlAppendValue(x))))
    validateNonSelectedDropDown(document, "#selectedAPIs", combinedTuples, defaultOption)

  }

  def validateNonSelectedDropDown(document: Document, jsoupSelector: String, dropdownItems: Seq[(String, String)], defaultOption: String) = {

    withClue(s"Expected Default Select Option: `$defaultOption` is not rendered") {
      elementExistsByText(document, "option", defaultOption) shouldBe true
    }
    elementExistsById(document, "selectedAPIs")
    val selectElement: Option[Element] = getElementBySelector(document, jsoupSelector)
    withClue(s"Expected select with id: `$jsoupSelector` is not present") {

      selectElement.isDefined shouldBe true
    }

    val dropdownsWithIndex: Seq[((String, String), Int)] = dropdownItems.zipWithIndex

    for ((dropdownItem, index) <- dropdownsWithIndex) {
      val optionItem: Element = selectElement.head.child(index)
      optionItem.text shouldBe dropdownItem._2
      optionItem.attr("value") shouldBe dropdownItem._1
    }
  }

  def validateFormDestination(document: Document, formId: String, expectedDestination: String) = {
    val formSelector = s"#$formId"
    val maybeForm    = getElementBySelector(document, formSelector)

    withClue(s"Form with id $formId was not found") {
      maybeForm.isDefined shouldBe true
    }
    withClue(s"Form destination url was not as expected") {
      maybeForm.get.attr("action") shouldBe expectedDestination
    }
  }

  def validateButtonText(document: Document, buttonId: String, expectedButtonText: String): Unit = {
    val maybeButtonElement = getElementBySelector(document, s"#$buttonId")
    withClue(s"button with id `$buttonId` was not found") {
      maybeButtonElement.isDefined shouldBe true
    }
    maybeButtonElement.head.tag().getName match {
      case "input"  => maybeButtonElement.head.attr("value") shouldBe expectedButtonText
      case "button" => maybeButtonElement.head.text shouldBe expectedButtonText
    }
  }

  def validateHiddenSelectedApiValues(document: Document, selectedAPIs: Seq[CombinedApi], numberOfSets: Int = 1): Unit = {
    val elements: List[Element] = getElementsBySelector(document, "input[name=selectedAPIs][type=hidden]")
    elements.size shouldBe selectedAPIs.size * numberOfSets
    elements.map(_.attr("value")).toSet should contain allElementsOf selectedAPIs.map(_.serviceName)
  }

  def validateHiddenSelectedTaxRegimeValues(document: Document, selectedCategories: Seq[APICategoryDetails], numberOfSets: Int = 1): Unit = {
    val elements: List[Element] = getElementsBySelector(document, "input[name=selectedCategories][type=hidden]")
    elements.size shouldBe selectedCategories.size * numberOfSets
    elements.map(_.attr("value")).toSet should contain allElementsOf selectedCategories.map(_.category)
  }

  def validateTopicGrid(document: Document, selectedTopic: Option[TopicOptionChoice]): Unit = {
    TopicOptionChoice.values.foreach(topic => validateTopicEntry(document, topic))
    validateSelectedTopic(document, selectedTopic)
  }

  private def validateTopicEntry(document: Document, topic: TopicOptionChoice): Unit = {
    val maybeRadioButton = getElementBySelector(document, s"input#$topic")
    maybeRadioButton.fold(fail(s"Topic $topic radio button is missing"))(radioButton => {
      radioButton.attr("value") shouldBe topic.toString
    })
  }

  private def validateSelectedTopic(document: Document, selectedTopic: Option[TopicOptionChoice]) = {
    val selectedInput = getElementBySelector(document, "input[checked]")
    selectedTopic.fold(selectedInput shouldBe None)(topic => {
      withClue(s"selected topic was not as expected..") {
        selectedInput.fold(fail("elements is missing"))(_.attr("value") shouldBe topic.toString)
      }
    })
  }

  def validateSelectedSpecificApiItems(document: Document, apis: Seq[CombinedApi], hiddenInputSizeInto: Int = 1): Unit = {
    val hiddenApiInputs = getElementsBySelector(document, "form#apiFilters input[type=hidden]")

    hiddenApiInputs.size shouldBe apis.size * hiddenInputSizeInto
    hiddenApiInputs.map(_.attr("value")) should contain allElementsOf apis.map(_.serviceName)
  }

  def verifyEmailOptions(option: EmailOptionChoice, document: Document, isDisabled: Boolean = false): Unit = {
    elementExistsById(document, option.toString) shouldBe true
    elementExistsContainsText(document, "label", optionLabel(option)) shouldBe true
    elementExistsContainsText(document, "div", optionHint(option)) shouldBe true
    elementExistsByIdWithAttr(document, option.toString, "disabled") shouldBe isDisabled
  }

  def verifyEmailPreferencesChoiceOptions(option: EmailPreferencesChoice, document: Document, isDisabled: Boolean = false): Unit = {
    elementExistsById(document, option.toString) shouldBe true
    elementExistsContainsText(document, "label", EmailPreferencesChoice.optionLabel(option)) shouldBe true
    elementExistsContainsText(document, "div", EmailPreferencesChoice.optionHint(option)) shouldBe true
    elementExistsByIdWithAttr(document, option.toString, "disabled") shouldBe isDisabled
  }

  def verifyEmailNonPrimaryLinks(document: Document): Unit = {
    elementIdentifiedByIdContainsText(document, "a", "email-all-users", "email all users with a Developer Hub account") shouldBe true
    elementIdentifiedByIdContainsText(document, "a", "email-mandatory-info", "email users mandatory information about APIs they subscribe to") shouldBe true
  }
}
