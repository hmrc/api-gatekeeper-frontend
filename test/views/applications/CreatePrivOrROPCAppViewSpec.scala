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

import config.AppConfig
import model.Forms._
import org.jsoup.Jsoup
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.test.UnitSpec
import utils.FakeRequestCSRFSupport._
import model.LoggedInUser
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.MessagesProvider
import utils.ViewHelpers._
import views.html.applications.create_application

class CreatePrivOrROPCAppViewSpec extends UnitSpec with MockitoSugar with GuiceOneAppPerSuite {
  trait Setup {

    val createApplicationView = app.injector.instanceOf[create_application]
    implicit val messagesProvider = app.injector.instanceOf[MessagesProvider]
    implicit val request = FakeRequest().withCSRFToken

    implicit val userFullName = LoggedInUser(Some("firstName lastName"))
    val page: () => HtmlFormat.Appendable = () => createApplicationView(createPrivOrROPCAppForm)
  }

  "CreatePrivOrROPCApp page" should {
    "with no fields filled" should {
      "have the correct content type" in new Setup {
        page().contentType should include("text/html")
      }

      "render the correct heading" in new Setup {
        val document = Jsoup.parse(page().body)
        elementExistsByText(document, "h1", "Add privileged or ROPC application") shouldBe true
      }

      "render unchecked accesstype buttons" in new Setup {
        val document = Jsoup.parse(page().body)
        document.getElementById("accessTypePrivileged").hasAttr("checked") shouldBe false
        document.getElementById("accessTypeROPC").hasAttr("checked") shouldBe false
      }

      "render an empty application name" in new Setup {
        val document = Jsoup.parse(page().body)
        elementExistsByText(document, "label", "Application name") shouldBe true
        document.getElementById("applicationName").attr("value") shouldBe ""
      }

      "render an empty description" in new Setup {
        val document = Jsoup.parse(page().body)
        elementExistsByText(document, "label", "Application description") shouldBe true
        document.getElementById("applicationDescription").text shouldBe ""
      }

      "render an empty email address" in new Setup {
        val document = Jsoup.parse(page().body)
        elementExistsByText(document, "label", "Administrator email address") shouldBe true
        document.getElementById("adminEmail").attr("value") shouldBe ""
      }

      "renders with no errors" in new Setup {
        val document = Jsoup.parse(page().body)
        elementExistsByAttr(document, "class", "form-field--error") shouldBe false
      }
    }

    "render errors for fields when given errors in form" in  new Setup {

      override val page: () => HtmlFormat.Appendable = () => createApplicationView(createPrivOrROPCAppForm
        .withError("accessType", "This is an error about access type")
        .withError("applicationName", "This is an error about application name")
        .withError("applicationDescription", "This is an error about application description")
        .withError("adminEmail", "This is an error about admin email")
      )

      val document = Jsoup.parse(page().body)
      document.body.toString.contains("This is an error about access type") shouldBe true
      document.body.toString.contains("This is an error about application name") shouldBe true
      document.body.toString.contains("This is an error about application description") shouldBe true
      document.body.toString.contains("This is an error about admin email") shouldBe true
    }
  }
}
