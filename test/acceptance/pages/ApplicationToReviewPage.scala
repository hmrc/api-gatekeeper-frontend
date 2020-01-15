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

package acceptance.pages

import acceptance.WebPage

object ApplicationToReviewPage extends WebPage {

  override val url: String = s"http://localhost:$port/api-gatekeeper/applications/df0c32b6-bbb7-46eb-ba50-e6e5459162ff"

  override def isCurrentPage: Boolean = {
    currentUrl == url
  }

  def deleteApplicationButton() = find(id("delete-application")).get

  def selectDeleteApplication() = {
    click on deleteApplicationButton
  }
}
