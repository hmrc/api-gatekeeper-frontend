/*
 * Copyright 2021 HM Revenue & Customs
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
import views.CommonViewSpec
import views.html.applications.CreateApplicationSuccessView
import model.ApplicationId
import model.ClientId

class CreatePrivOrROPCAppSuccessViewSpec extends CommonViewSpec {

  trait Setup {
    val createApplicationSuccessView = app.injector.instanceOf[CreateApplicationSuccessView]
  }

  "CreatePrivOrROPCAppSuccess page" when {

    val appId = ApplicationId("245dfgs-2dfgd578-968sdg5-23f456-dgf324")
    val appName = "This is my app name"
    val env = "Production"
    val clientId = ClientId.random
    val totpSecret = "DSKL595KJDHK540K09421"

    "a privileged application is created" should {
      "render" in new Setup {

        val accessType = Some(AccessType.PRIVILEGED)
        val totp = Some(TotpSecrets(totpSecret))

        implicit val loggedInUser = LoggedInUser(Some(""))

        val page: () => HtmlFormat.Appendable =
          () => createApplicationSuccessView(appId, appName, env, accessType, totp, clientId)(loggedInUser, messagesProvider)

        page().contentType should include("text/html")

        val document = Jsoup.parse(page().body)

        elementExistsByText(document, "h1", appName) shouldBe true
        elementExistsByText(document, "h1", "Application added") shouldBe true
        document.body().toString.contains("This is your only chance to copy and save this application's TOTP.") shouldBe true
        elementExistsByText(document, "tr", s"Application ID ${appId.value}") shouldBe true
        elementExistsByText(document, "tr", s"Application name $appName") shouldBe true
        elementExistsByText(document, "tr", s"Environment $env") shouldBe true
        elementExistsByText(document, "tr", "Access type Privileged") shouldBe true
        elementExistsByText(document, "tr", s"TOTP secret $totpSecret") shouldBe true
        elementExistsByText(document, "tr", s"Client ID ${clientId.value}") shouldBe true

      }
    }

    "an ROPC application is created" should {
      "render" in new Setup {

        val accessType = Some(AccessType.ROPC)

        val page: () => HtmlFormat.Appendable =
          () => createApplicationSuccessView(appId, appName, env, accessType, None, clientId)(LoggedInUser(Some("")), messagesProvider)

        page().contentType should include("text/html")

        val document = Jsoup.parse(page().body)

        elementExistsByText(document, "h1", appName) shouldBe true
        elementExistsByText(document, "h1", "Application added") shouldBe true
        document.body().toString.contains("This is your only chance to copy and save this application's TOTP.") shouldBe true
        elementExistsByText(document, "tr", s"Application ID ${appId.value}") shouldBe true
        elementExistsByText(document, "tr", s"Application name $appName") shouldBe true
        elementExistsByText(document, "tr", s"Environment $env") shouldBe true
        elementExistsByText(document, "tr", "Access type ROPC") shouldBe true
        elementExistsByText(document, "tr", s"TOTP secret $totpSecret") shouldBe false
        elementExistsByText(document, "tr", s"Client ID ${clientId.value}") shouldBe true

      }
    }
  }
}
