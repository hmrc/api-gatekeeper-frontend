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

object DashboardPage extends WebPage {

  override val pageHeading: String = "DashboardPage"

  override val url: String = s"http://localhost:${Env.port}/api-gatekeeper/dashboard"

  def isUnauthorised: Boolean = {
    getText(By.cssSelector("h2")) == "Only Authorised users can access the requested page"
  }

  def selectDevelopers(): Unit = {
    click(By.linkText("Developers"))
  }

  def selectApplications(): Unit = {
    click(By.linkText("Applications"))
  }

}
