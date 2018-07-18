/*
 * Copyright 2018 HM Revenue & Customs
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

package unit.view.applications

import config.AppConfig
import model.AccessType
import org.jsoup.Jsoup
import org.scalatestplus.play.OneServerPerSuite
import play.api.i18n.Messages.Implicits._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.test.UnitSpec
import unit.utils.ViewHelpers._
import views.html



class CreatePrivOrROPCAppSuccessViewSpec extends UnitSpec with OneServerPerSuite {

  "CreatePrivOrROPCAppSuccess page" should {

    implicit val userFullName = Option("firstname lastname")

    "render" in {

      val appId = "245dfgs-2dfgd578-968sdg5-23f456-dgf324"
      val appName = "This is my app name"
      val env = "Production"
      val accessType = Some(AccessType.PRIVILEGED)
      val totpSecret = "DSKL595KJDHK540K09421"
      val clientId = "ask249850sokfjslkfalki4u954p2qejwwmeds"

      val page: () => HtmlFormat.Appendable =
        () => html.applications.create_application_success(appId, appName, env, accessType, totpSecret, clientId)(Some(""), applicationMessages, AppConfig)

      page().contentType should include("text/html")

      val document = Jsoup.parse(page().body)

      elementExistsByText(document, "h1", appName) shouldBe true
      elementExistsByText(document, "h1", "Application added") shouldBe true
      document.body().toString.contains("This is your only chance to copy and save this application's TOTP secret.") shouldBe true
      elementExistsByText(document, "tr", s"Application ID $appId") shouldBe true
      elementExistsByText(document, "tr", s"Application name $appName") shouldBe true
      elementExistsByText(document, "tr", s"Environment $env") shouldBe true
      elementExistsByText(document, "tr", "Access type Privileged") shouldBe true
      elementExistsByText(document, "tr", s"TOTP secret $totpSecret") shouldBe true
      elementExistsByText(document, "tr", s"Client ID $clientId") shouldBe true

    }
  }

}
