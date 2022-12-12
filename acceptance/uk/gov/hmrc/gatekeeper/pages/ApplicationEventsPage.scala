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

package uk.gov.hmrc.gatekeeper.pages

import uk.gov.hmrc.gatekeeper.common.WebPage
import uk.gov.hmrc.apiplatform.modules.events.applications.domain.models.EventTag
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId

case class ApplicationEventsPage(applicationId: ApplicationId) extends WebPage {

  override val url: String = s"http://localhost:$port/api-gatekeeper/applications/${applicationId.value.toString}/events"

  override def isCurrentPage: Boolean = {
    currentUrl == url
  }

  def typeOfChangeDropdown = find(id("eventTag")).get

  def submitButton = find(id("filterResults")).get

  def selectTypeOfChange(tag: EventTag) = {
    singleSel("eventTag").value = tag.description
  }
}
