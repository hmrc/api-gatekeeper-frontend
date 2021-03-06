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

package pages

import common.WebPage

object DeleteApplicationPage extends WebPage {

  override val url: String = s"http://localhost:$port/api-gatekeeper/applications/a97541e8-f93d-4d0a-ab0b-862e63204b7d/delete"

  override def isCurrentPage: Boolean = currentUrl == url

  def radioButton = find(name("collaboratorEmail")).get

  def selectRadioButton() = {
    click on radioButton
  }

  def textBox = textField("applicationNameConfirmation")

  def writeInTextBox(input: String) = {
    textBox.value = input
  }

  def deleteApplicationButton = find(id("delete-application")).get

  def selectDeleteButton() = {
    click on deleteApplicationButton
  }

  def completeForm(input: String) = {
    selectRadioButton()
    writeInTextBox(input)
  }
}
