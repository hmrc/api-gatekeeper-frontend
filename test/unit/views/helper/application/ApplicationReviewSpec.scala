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

package unit.views.helper.application


import org.joda.time.format.DateTimeFormat
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.DateTimeUtils
import unit.utils.ApplicationGenerator._
import model.State._
import views.helper.application.ApplicationReview

class ApplicationReviewSpec extends UnitSpec {
  "ApplicationsReview" when {
    "application is approved" should {
      val now = DateTimeUtils.now
      val dateFormatter = DateTimeFormat.forPattern("dd MMMM yyyy")
      val app = anApplicationWithHistory(stateHistories = Seq(aStateHistory(PENDING_REQUESTER_VERIFICATION, now)))
      val appResponse = anApplicationResponseWith(aCheckInformation())

      "approved by return Some" in {
        ApplicationReview.getApprovedBy(app) shouldBe Some("actor id")
      }
      "approved on return Some" in {
        ApplicationReview.getApprovedOn(app) shouldBe Some(dateFormatter.print(now))
      }
      "review contact name return Some" in {
        ApplicationReview.getReviewContactName(appResponse) shouldBe Some("contactFullName")
      }
      "review contact email return Some" in {
        ApplicationReview.getReviewContactEmail(appResponse) shouldBe Some("contactEmail")
      }
      "review contact telephone return Some" in {
        ApplicationReview.getReviewContactTelephone(appResponse) shouldBe Some("contactTelephone")
      }
    }
    "application is not approved" should {
      val appWithHistory = anApplicationWithHistory()
      val app = anApplicationResponse()

      "approved by return None" in {
        ApplicationReview.getApprovedBy(appWithHistory) shouldBe None
      }
      "approved on return None" in {
        ApplicationReview.getApprovedOn(appWithHistory) shouldBe None
      }
      "review contact name return None" in {
        ApplicationReview.getReviewContactName(app) shouldBe None
      }
      "review contact email return None" in {
        ApplicationReview.getReviewContactEmail(app) shouldBe None
      }
      "review contact telephone return None" in {
        ApplicationReview.getReviewContactTelephone(app) shouldBe None
      }
    }
  }
}