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

import org.jsoup.nodes.{Document, Element}
import org.scalatest.MustMatchers
import utils.ViewHelpers._
import model.APIDefinition

trait EmailUsersHelper extends MustMatchers{
    def validatePageHeader(document: Document, expectedTitle: String)= {
      val maybeTitleText = getElementBySelector(document, "#pageTitle")
      maybeTitleText.fold(fail("page title not present in page"))(_.text mustBe expectedTitle)
    }


    def isElementChecked(document: Document, expectedValue: String, shouldBeChecked: Boolean = true): Unit ={
      val checkedRadio = getElementBySelector(document, "input[checked]")
      checkedRadio.isDefined mustBe true
      checkedRadio.head.attr("value").equalsIgnoreCase(expectedValue) mustBe shouldBeChecked
    }

    def noInputChecked(document: Document): Unit ={
      val checkedRadio = getElementBySelector(document, "input[checked]")
      checkedRadio.isDefined mustBe false
    }


    def checkElementsExistById(document: Document, ids: Seq[String]): Unit ={
      ids.foreach(id => {
        withClue(s"$id element exists?:") {
          elementExistsById(document, id) mustBe true
        }
        ()
      })
    }

    def validateCopyToClipboardLink(document: Document, isVisible: Boolean = true){
       withClue(s"Copy to cliboard link validation failed") {
         elementExistsByAttr(document, "a", "data-clip-text") mustBe isVisible
       }

    }

     def validateNonSelectedApiDropDown(document: Document, apis: Seq[APIDefinition], defaultOption: String){
      val combinedTuples =  Seq(("" ,defaultOption)) ++ apis.map(x=> Seq((x.serviceName, x.name))).flatten
      validateNonSelectedDropDown(document, "#selectedAPIs", combinedTuples, defaultOption)
     
    }
    def validateNonSelectedDropDown(document: Document, jsoupSelector: String, dropdownItems: Seq[(String, String)], defaultOption: String){
     
       withClue(s"Expected Default Select Option: `$defaultOption` is not rendered") {
          elementExistsByText(document, "option", defaultOption) mustBe true
       }
     elementExistsById(document, "selectedAPIs")
       val selectElement: Option[Element]  = getElementBySelector(document, jsoupSelector)
        withClue(s"Expected select with id: `$jsoupSelector` is not present") {
          
          selectElement.isDefined mustBe true
        }
    
       val dropdownsWithIndex: Seq[((String,String), Int)] = dropdownItems.zipWithIndex

        for((dropdownItem, index) <- dropdownsWithIndex){
          val optionItem: Element = selectElement.head.child(index)
          optionItem.text mustBe dropdownItem._2
          optionItem.attr("value") mustBe dropdownItem._1 

        }
     
    }
}
