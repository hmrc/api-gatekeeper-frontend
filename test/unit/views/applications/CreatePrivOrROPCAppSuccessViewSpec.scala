/*
 * Copyright 2019 HM Revenue & Customs
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

package unit.views.applications

import config.AppConfig
import model.{AccessType, TotpSecrets}
import org.jsoup.Jsoup
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.i18n.Messages.Implicits._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.test.UnitSpec
import unit.utils.ViewHelpers._
import utils.LoggedInUser
import views.html

class CreatePrivOrROPCAppSuccessViewSpec extends UnitSpec with OneServerPerSuite with MockitoSugar {

  private val mockAppConfig = mock[AppConfig]

  "CreatePrivOrROPCAppSuccess page" when {
    implicit val userFullName = "firstname lastname"

    val appId = "245dfgs-2dfgd578-968sdg5-23f456-dgf324"
    val appName = "This is my app name"
    val env = "Production"
    val clientId = "ask249850sokfjslkfalki4u954p2qejwwmeds"
    val totpSecret = "DSKL595KJDHK540K09421"
    val clientSecret = "ASDFGHJK-9087EWTRGFHJ;KJHGDFJTH"

    "a privileged application is created" should {
      "render" in {

        val accessType = Some(AccessType.PRIVILEGED)
        val totp = Some(TotpSecrets(totpSecret, ""))

        implicit val loggedInUser = LoggedInUser(Some(""))

        val page: () => HtmlFormat.Appendable =
          () => html.applications.create_application_success(appId, appName, env, accessType, totp, clientId, clientSecret)(loggedInUser, applicationMessages, mockAppConfig)

        page().contentType should include("text/html")

        val document = Jsoup.parse(page().body)

        elementExistsByText(document, "h1", appName) shouldBe true
        elementExistsByText(document, "h1", "Application added") shouldBe true
        document.body().toString.contains("This is your only chance to copy and save this application's TOTP and client secrets.") shouldBe true
        document.body().toString.contains("This is your only chance to copy and save this application's client secret.") shouldBe false
        elementExistsByText(document, "tr", s"Application ID $appId") shouldBe true
        elementExistsByText(document, "tr", s"Application name $appName") shouldBe true
        elementExistsByText(document, "tr", s"Environment $env") shouldBe true
        elementExistsByText(document, "tr", "Access type Privileged") shouldBe true
        elementExistsByText(document, "tr", s"TOTP secret $totpSecret") shouldBe true
        elementExistsByText(document, "tr", s"Client secret $clientSecret") shouldBe true
        elementExistsByText(document, "tr", s"Client ID $clientId") shouldBe true

      }
    }

    "an ROPC application is created" should {
      "render" in {

        val accessType = Some(AccessType.ROPC)
        val totp = None

        val page: () => HtmlFormat.Appendable =
          () => html.applications.create_application_success(appId, appName, env, accessType, None, clientId, clientSecret)(LoggedInUser(Some("")), applicationMessages, mockAppConfig)

        page().contentType should include("text/html")

        val document = Jsoup.parse(page().body)

        elementExistsByText(document, "h1", appName) shouldBe true
        elementExistsByText(document, "h1", "Application added") shouldBe true
        document.body().toString.contains("This is your only chance to copy and save this application's TOTP and client secrets.") shouldBe false
        document.body().toString.contains("This is your only chance to copy and save this application's client secret.") shouldBe true
        elementExistsByText(document, "tr", s"Application ID $appId") shouldBe true
        elementExistsByText(document, "tr", s"Application name $appName") shouldBe true
        elementExistsByText(document, "tr", s"Environment $env") shouldBe true
        elementExistsByText(document, "tr", "Access type ROPC") shouldBe true
        elementExistsByText(document, "tr", s"TOTP secret $totpSecret") shouldBe false
        elementExistsByText(document, "tr", s"Client secret $clientSecret") shouldBe true
        elementExistsByText(document, "tr", s"Client ID $clientId") shouldBe true

      }
    }
  }

}
