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

package views.review

import java.util.UUID

import controllers.HandleUpliftForm
import mocks.config.AppConfigMock
import model.{LoggedInUser, _}
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import uk.gov.hmrc.time.DateTimeUtils
import utils.FakeRequestCSRFSupport._
import utils.ViewHelpers._
import views.CommonViewSpec
import views.html.review.ReviewView

class ReviewViewSpec extends CommonViewSpec {

  trait Setup extends AppConfigMock {
    implicit val request = FakeRequest().withCSRFToken

    val reviewView = app.injector.instanceOf[ReviewView]

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
  }

  "review view" must {
    "show review information with pass and fail options" in new Setup {
      val result = reviewView.render(HandleUpliftForm.form, applicationReviewDetails,request, LoggedInUser(None), messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByText(document, "label", "Pass") mustBe true
      elementExistsByText(document, "label", "Fail") mustBe true
      elementExistsByText(document, "label", "Failure reason") mustBe true
      elementExistsByText(document, "p", "Tell the submitter why the application failed the check. This text will appear in the email to them.") mustBe true
    }
  }
}
