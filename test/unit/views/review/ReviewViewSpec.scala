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

package unit.views.review

import java.util.UUID

import config.AppConfig
import controllers.HandleUpliftForm
import model._
import org.jsoup.Jsoup
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import uk.gov.hmrc.time.DateTimeUtils
import unit.utils.ViewHelpers._
import utils.CSRFTokenHelper._

class ReviewViewSpec extends PlaySpec with OneServerPerSuite {
  "review view" must {
    implicit val request = FakeRequest().withCSRFToken

    val applicationReviewDetails =
      ApplicationReviewDetails(
        UUID.randomUUID().toString,
        "Test Application",
        "Test Application",
        None,
        SubmissionDetails("Test Name", "test@example.com", DateTimeUtils.now),
        None,
        None,
        None,
        None,
        None
      )

    "show review information with pass and fail options" in {

      val result = views.html.review.review.render(HandleUpliftForm.form, applicationReviewDetails,request, None, applicationMessages, AppConfig)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByText(document, "label", "Pass") mustBe true
      elementExistsByText(document, "label", "Fail") mustBe true
      elementExistsByText(document, "label", "Failure reason") mustBe true
      elementExistsByText(document, "p", "Tell the submitter why the application failed the check. This text will appear in the email to them.") mustBe true
    }
  }
}
