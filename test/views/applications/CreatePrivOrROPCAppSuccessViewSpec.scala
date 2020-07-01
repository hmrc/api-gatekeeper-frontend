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

package views.applications

import model.{AccessType, LoggedInUser, TotpSecrets}
import org.jsoup.Jsoup
import play.twirl.api.HtmlFormat
import utils.ViewHelpers._
import views.{CommonViewSetup, CommonViewSpec}
import views.html.applications.CreateApplicationSuccessView

class CreatePrivOrROPCAppSuccessViewSpec extends CommonViewSpec {

  trait Setup extends CommonViewSetup {
    val createApplicationSuccessView = app.injector.instanceOf[CreateApplicationSuccessView]
  }

  "CreatePrivOrROPCAppSuccess page" when {
    implicit val userFullName = "firstname lastname"

    val appId = "245dfgs-2dfgd578-968sdg5-23f456-dgf324"
    val appName = "This is my app name"
    val env = "Production"
    val clientId = "ask249850sokfjslkfalki4u954p2qejwwmeds"
    val totpSecret = "DSKL595KJDHK540K09421"

    "a privileged application is created" must {
      "render" in new Setup {

        val accessType = Some(AccessType.PRIVILEGED)
        val totp = Some(TotpSecrets(totpSecret))

        implicit val loggedInUser = LoggedInUser(Some(""))

        val page: () => HtmlFormat.Appendable =
          () => createApplicationSuccessView(appId, appName, env, accessType, totp, clientId)(loggedInUser, messagesProvider)

        page().contentType must include("text/html")

        val document = Jsoup.parse(page().body)

        elementExistsByText(document, "h1", appName) mustBe true
        elementExistsByText(document, "h1", "Application added") mustBe true
        document.body().toString.contains("This is your only chance to copy and save this application's TOTP.") mustBe true
        elementExistsByText(document, "tr", s"Application ID $appId") mustBe true
        elementExistsByText(document, "tr", s"Application name $appName") mustBe true
        elementExistsByText(document, "tr", s"Environment $env") mustBe true
        elementExistsByText(document, "tr", "Access type Privileged") mustBe true
        elementExistsByText(document, "tr", s"TOTP secret $totpSecret") mustBe true
        elementExistsByText(document, "tr", s"Client ID $clientId") mustBe true

      }
    }

    "an ROPC application is created" must {
      "render" in new Setup {

        val accessType = Some(AccessType.ROPC)
        val totp = None

        val page: () => HtmlFormat.Appendable =
          () => createApplicationSuccessView(appId, appName, env, accessType, None, clientId)(LoggedInUser(Some("")), messagesProvider)

        page().contentType must include("text/html")

        val document = Jsoup.parse(page().body)

        elementExistsByText(document, "h1", appName) mustBe true
        elementExistsByText(document, "h1", "Application added") mustBe true
        document.body().toString.contains("This is your only chance to copy and save this application's TOTP.") mustBe true
        elementExistsByText(document, "tr", s"Application ID $appId") mustBe true
        elementExistsByText(document, "tr", s"Application name $appName") mustBe true
        elementExistsByText(document, "tr", s"Environment $env") mustBe true
        elementExistsByText(document, "tr", "Access type ROPC") mustBe true
        elementExistsByText(document, "tr", s"TOTP secret $totpSecret") mustBe false
        elementExistsByText(document, "tr", s"Client ID $clientId") mustBe true

      }
    }
  }
}
