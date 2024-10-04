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

case class BaseApplicationPage(applicationName: String, applicationId: String) extends WebPage {
  override val pageHeading: String = applicationName

  override val url: String = s"http://localhost:${Env.port}/api-gatekeeper/applications/$applicationId" // a97541e8-f93d-4d0a-ab0b-862e63204b7d

  def clickDeleteApplicationButton() = {
    click(By.id("delete-application"))
  }

  def clickBlockApplication() = {
    click(By.id("block-application"))
  }

  def getDataPrivacyUrl(): String = {
    getText(By.id("privacy-url"))
  }

  def getDataTermsUrl(): String = {
    getText(By.id("terms-url"))
  }

  def isNotFound(): Boolean = {
    getText(By.tagName("h1")) == "This page can’t be found"
  }
}

object ApplicationPage extends BaseApplicationPage("My new app", "a97541e8-f93d-4d0a-ab0b-862e63204b7d")
