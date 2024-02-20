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

object DeveloperDetailsPage extends WebPage with UrlEncoding with CommonTestData {

  override val pageHeading: String = "dixie.fakename@example.com"

  override val url: String = s"http://localhost:${Env.port}/api-gatekeeper/developer"

  def firstName(): String = {
    getText(By.cssSelector("#first-name"))
  }

  def lastName(): String = {
    getText(By.cssSelector("#last-name"))
  }

  def status(): String = {
   getText(By.cssSelector("#status"))
  }

  def mfaHeading(): String = {
    getText(By.id("mfa-heading"))
  }

  def removeMfaLinkText(): String = {
    getText(By.id("remove-2SV"))
  }

  def removeMfaLinkIsDisabled(): Boolean = {
    findElement(By.id("remove-2SV")).getAttribute("disabled") == "true"
  }

  def authAppMfaType(): String  = {
    getText(By.id("mfa-type-0"))
  }

  def authAppMfaName(): String = {
    getText(By.id("mfa-name-0"))
  }

  def smsMfaType(): String = {
    getText(By.id("mfa-type-1"))
  }

  def smsMfaName(): String = {
    getText(By.id("mfa-name-1"))
  }

  def selectByApplicationName(name: String): Unit = {
    click(By.linkText(name))
  }

  def removeMfa(): Unit = {
    click(By.id("remove-2SV"))
  }

}
