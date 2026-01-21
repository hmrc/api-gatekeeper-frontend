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

package uk.gov.hmrc.gatekeeper.pages

import org.openqa.selenium.By

import uk.gov.hmrc.gatekeeper.common.{Env, WebPage}
import uk.gov.hmrc.gatekeeper.testdata.CommonTestData
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

object ManageTeamMembersPage extends WebPage with UrlEncoding with CommonTestData {

  override val pageHeading: String = "Manage Team Members"

  override val url: String = s"http://localhost:${Env.port}/api-gatekeeper/applications/$applicationId/team-members"

  def whyCantRemoveAdminShowing = {
    getText(By.className("govuk-details__summary-text")) == "Why can't I remove the verified admin from this application?"
  }
}
