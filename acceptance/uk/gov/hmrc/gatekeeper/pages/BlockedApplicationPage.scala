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

object BlockedApplicationPage extends WebPage {

  override val pageHeading: String = "My new app"
  
  override val url: String = s"http://localhost:${Env.port}/api-gatekeeper/applications/fa38d130-7c8e-47d8-abc0-0374c7f73217"


  // def deleteApplicationButton = find(id("delete-application")).get

  def selectDeleteApplication() = {
     click(By.id("delete-application"))
  }

  // def blockApplicationButton = find(id("block-application")).get

  def selectBlockApplication() = {
    click(By.id("block-application"))
  }

  // def unblockApplicationButton = find(id("unblock-application")).get

  def selectUnblockApplication() = {
    click(By.id("unblock-application"))
  }
}
