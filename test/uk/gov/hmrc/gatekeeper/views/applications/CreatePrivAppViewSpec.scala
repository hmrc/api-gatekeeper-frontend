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

package uk.gov.hmrc.gatekeeper.views.applications

import org.jsoup.Jsoup

import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat

import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInUser
import uk.gov.hmrc.gatekeeper.models.Forms._
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.utils.ViewHelpers._
import uk.gov.hmrc.gatekeeper.views.CommonViewSpec
import uk.gov.hmrc.gatekeeper.views.html.applications.CreateApplicationView

class CreatePrivAppViewSpec extends CommonViewSpec {

  trait Setup {
    val createApplicationView = app.injector.instanceOf[CreateApplicationView]

    implicit val userFullName: LoggedInUser = LoggedInUser(Some("firstName lastName"))
    val page: () => HtmlFormat.Appendable   = () => createApplicationView(createPrivAppForm)
  }

  "CreatePrivApp page" should {
    "with no fields filled" should {
      "have the correct content type" in new Setup {
        page().contentType should include("text/html")
      }

      "render the correct heading" in new Setup {
        val document = Jsoup.parse(page().body)
        elementExistsByText(document, "h1", "Add privileged application") shouldBe true
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

    "render errors for fields when given errors in form" in new Setup {

      override val page: () => HtmlFormat.Appendable = () =>
        createApplicationView(createPrivAppForm
          .withError("applicationName", "This is an error about application name")
          .withError("applicationDescription", "This is an error about application description")
          .withError("adminEmail", "This is an error about admin email"))

      val document = Jsoup.parse(page().body)
      document.body.toString.contains("This is an error about application name") shouldBe true
      document.body.toString.contains("This is an error about application description") shouldBe true
      document.body.toString.contains("This is an error about admin email") shouldBe true
    }
  }
}
