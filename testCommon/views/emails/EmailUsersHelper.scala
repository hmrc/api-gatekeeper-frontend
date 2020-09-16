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

import model.EmailOptionChoice.{EmailOptionChoice, optionHint, optionLabel}
import model.EmailPreferencesChoice.EmailPreferencesChoice
import model.TopicOptionChoice.TopicOptionChoice
import model.{APIDefinition, EmailPreferencesChoice, TopicOptionChoice, User}
import org.jsoup.nodes.{Document, Element}
import org.scalatest.MustMatchers
import utils.ViewHelpers._

trait EmailUsersHelper extends MustMatchers with APIDefinitionHelper {

  def validatePageHeader(document: Document, expectedTitle: String) = {
    val maybeTitleText = getElementBySelector(document, "#pageTitle")
    maybeTitleText.fold(fail("page title not present in page"))(_.text mustBe expectedTitle)
  }


  def isElementChecked(document: Document, expectedValue: String, shouldBeChecked: Boolean = true): Unit = {
    val checkedRadio = getElementBySelector(document, "input[checked]")
    checkedRadio.isDefined mustBe true
    checkedRadio.head.attr("value").equalsIgnoreCase(expectedValue) mustBe shouldBeChecked
  }

  def noInputChecked(document: Document): Unit = {
    val checkedRadio = getElementBySelector(document, "input[checked]")
    checkedRadio.isDefined mustBe false
  }


  def checkElementsExistById(document: Document, ids: Seq[String]): Unit = {
    ids.foreach(id => {
      withClue(s"$id element exists?:") {
        elementExistsById(document, id) mustBe true
      }
      ()
    })
  }

  def validateCopyToClipboardLink(document: Document, users: Seq[User] = Seq.empty) = {
    withClue(s"Copy to clipboard link validation failed") {
      elementExistsById(document, "copy-users-to-clip") mustBe users.nonEmpty
    }

    if(users.nonEmpty) {
      val expectedValue = users.map(_.email).sorted.mkString("; ")
      getElementBySelector(document, "a#copy-users-to-clip")
        .fold(fail("Copy to Clipboard Link not found"))(link => link.attr("data-clip-text") mustBe expectedValue)
    }
  }


  def validateNonSelectedApiDropDown(document: Document, apis: Seq[APIDefinition], defaultOption: String) = {
    val combinedTuples = Seq(("", defaultOption)) ++ apis.flatMap(x => Seq((x.serviceName, x.name)))
    validateNonSelectedDropDown(document, "#selectedAPIs", combinedTuples, defaultOption)

  }

  def validateNonSelectedDropDown(document: Document, jsoupSelector: String, dropdownItems: Seq[(String, String)], defaultOption: String) = {

    withClue(s"Expected Default Select Option: `$defaultOption` is not rendered") {
      elementExistsByText(document, "option", defaultOption) mustBe true
    }
    elementExistsById(document, "selectedAPIs")
    val selectElement: Option[Element] = getElementBySelector(document, jsoupSelector)
    withClue(s"Expected select with id: `$jsoupSelector` is not present") {

      selectElement.isDefined mustBe true
    }

    val dropdownsWithIndex: Seq[((String, String), Int)] = dropdownItems.zipWithIndex

    for ((dropdownItem, index) <- dropdownsWithIndex) {
      val optionItem: Element = selectElement.head.child(index)
      optionItem.text mustBe dropdownItem._2
      optionItem.attr("value") mustBe dropdownItem._1
    }
  }

  def validateFormDestination(document: Document, formId: String, expectedDestination: String) = {
    val formSelector = s"#$formId"
    val maybeForm = getElementBySelector(document, formSelector)

    withClue(s"Form with id $formId was not found") {
      maybeForm.isDefined mustBe true
    }
    withClue(s"Form destination url was not as expected") {
      maybeForm.get.attr("action") mustBe expectedDestination
    }
  }

  def validateButtonText(document: Document, buttonId: String, expectedButtonText: String) = {
    val maybeButtonElement = getElementBySelector(document, s"#$buttonId")
    withClue(s"button with id `$buttonId` was not found") {
      maybeButtonElement.isDefined mustBe true
    }
    maybeButtonElement.head.tag().getName match {
      case "input" => maybeButtonElement.head.attr("value") mustBe expectedButtonText
      case "button" => maybeButtonElement.head.text mustBe expectedButtonText
    }
  }

  def validateHiddenSelectedApiValues(document: Document, selectedAPIs: Seq[APIDefinition], numberOfSets: Int = 1) = {
    val elements: List[Element] = getElementsBySelector(document, "input[name=selectedAPIs][type=hidden]")
    elements.size mustBe selectedAPIs.size * numberOfSets
    elements.map(_.attr("value")).toSet must contain allElementsOf selectedAPIs.map(_.serviceName)
  }


  def validateTopicGrid(document: Document, selectedTopic: Option[TopicOptionChoice]) {
    TopicOptionChoice.values.foreach(topic => validateTopicEntry(document, topic))
    validateSelectedTopic(document, selectedTopic)
  }

  private def validateTopicEntry(document: Document, topic: TopicOptionChoice) = {
    val maybeRadioButton = getElementBySelector(document, s"input#$topic")
    maybeRadioButton.fold(fail(s"Topic $topic radio button is missing"))(radioButton => {
      radioButton.attr("value") mustBe topic.toString
    })
  }

  private def validateSelectedTopic(document: Document, selectedTopic: Option[TopicOptionChoice]) = {
    val selectedInput = getElementBySelector(document, "input[checked]")
    selectedTopic.fold(selectedInput mustBe None)(topic => {
      withClue(s"selected topic was not as expected..") {
        selectedInput.fold(fail("elements is missing"))(_.attr("value") mustBe topic.toString)
      }
    })
  }

  def validateSelectedSpecificApiItems(document: Document, apis: Seq[APIDefinition]): Unit = {
    val hiddenApiInputs = getElementsBySelector(document, "form#api-filters input[type=hidden]")
    val hiddenTopicInputs = getElementsBySelector(document, "form#topic-filter input[type=hidden]")

    hiddenApiInputs.size mustBe apis.size
    hiddenApiInputs.map(_.attr("value")) must contain allElementsOf apis.map(_.serviceName)

    hiddenTopicInputs.size mustBe apis.size
    hiddenTopicInputs.map(_.attr("value")) must contain allElementsOf apis.map(_.serviceName)
  }

  def verifyEmailOptions(option: EmailOptionChoice, document: Document, isDisabled: Boolean = false): Unit = {
    elementExistsById(document, option.toString) mustBe true
    elementExistsContainsText(document, "label", optionLabel(option)) mustBe true
    elementExistsContainsText(document, "label", optionHint(option)) mustBe true
    elementExistsByIdWithAttr(document, option.toString, "disabled") mustBe isDisabled
  }

  def verifyEmailPreferencesChoiceOptions(option: EmailPreferencesChoice, document: Document, isDisabled: Boolean = false): Unit = {
    elementExistsById(document, option.toString) mustBe true
    elementExistsContainsText(document, "label", EmailPreferencesChoice.optionLabel(option)) mustBe true
    elementExistsContainsText(document, "label", EmailPreferencesChoice.optionHint(option)) mustBe true
    elementExistsByIdWithAttr(document, option.toString, "disabled") mustBe isDisabled
  }
}
