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

package uk.gov.hmrc.gatekeeper.views.review

import java.time.LocalDateTime

import mocks.config.AppConfigMock
import org.jsoup.Jsoup

import play.api.test.FakeRequest

import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInUser
import uk.gov.hmrc.gatekeeper.controllers.HandleUpliftForm
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.utils.ViewHelpers._
import uk.gov.hmrc.gatekeeper.views.CommonViewSpec
import uk.gov.hmrc.gatekeeper.views.html.review.ReviewView

class ReviewViewSpec extends CommonViewSpec {

  trait Setup extends AppConfigMock {
    implicit val request = FakeRequest().withCSRFToken

    val reviewView = app.injector.instanceOf[ReviewView]

    val applicationReviewDetails =
      ApplicationReviewDetails(
        ApplicationId.random,
        "Test Application",
        "Test Application",
        None,
        SubmissionDetails("Test Name", "test@example.com", LocalDateTime.now()),
        None,
        None,
        None,
        None,
        None
      )
  }

  "review view" should {
    "show review information with pass and fail options" in new Setup {
      val result = reviewView.render(HandleUpliftForm.form, applicationReviewDetails, request, LoggedInUser("Bobby Example"), messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "label", "Pass") shouldBe true
      elementExistsByText(document, "label", "Fail") shouldBe true
      elementExistsByText(document, "label", "Failure reason") shouldBe true
      elementExistsByText(document, "p", "Tell the submitter why the application failed the check. This text will appear in the email to them.") shouldBe true
    }
  }
}
