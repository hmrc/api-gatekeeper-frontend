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

package utils

import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements

import scala.collection.JavaConverters._

object ViewHelpers {

  def elementExistsByText(doc: Document, elementType: String, elementText: String): Boolean = {
    doc.select(elementType).asScala.exists(node => node.text.trim == elementText)
  }

  def elementExistsContainsText(doc: Document, elementType: String, elementText: String): Boolean = {
    doc.select(elementType).asScala.exists(node => node.text.trim.contains(elementText))
  }

  def getSelectedOptionValue(doc: Document): Option[String] = {
    getElementBySelector(doc, "select option[selected]").map(_.attr("value"))
  }

  def getElementBySelector(doc: Document, selector: String): Option[Element] = {
    doc.select(s"$selector").asScala.headOption
  }

  def elementExistsById(doc: Document, id: String): Boolean = doc.select(s"#$id").asScala.nonEmpty

  def elementExistsByIdWithAttr(doc: Document, id: String, attr: String): Boolean =
    doc.select(s"#$id").asScala.filter(_.hasAttr(attr)).nonEmpty

  def elementExistsByAttr(doc: Document, elementType: String, attr: String): Boolean = {
    doc.select(s"$elementType[$attr]").asScala.nonEmpty
  }

  def elementIdentifiedByAttrContainsText(doc: Document, elementType: String, attr: String, text: String): Boolean = {
    doc.select(s"$elementType[$attr]").asScala.exists(element => element.text.equals(text))
  }

  def elementIdentifiedByIdContainsText(doc: Document, elementType: String, id: String, text: String): Boolean = {
    doc.select(s"#$id").asScala.exists(element => element.text.equals(text))
  }
}
