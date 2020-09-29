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

package views.helper.application

import model.State._
import uk.gov.hmrc.play.test.UnitSpec
import builder.ApplicationResponseBuilder

class ApplicationSubmissionSpec extends UnitSpec with ApplicationResponseBuilder {
  "ApplicationsSubmission" when {
    "submittedBy" should {
      "is present" in {
        val app = anApplicationWithHistory(stateHistories = Seq(aStateHistory(PENDING_GATEKEEPER_APPROVAL)))
        ApplicationSubmission.getSubmittedBy(app.history) shouldBe Some("actor id")
      }
      "is not present" in {
        val app = anApplicationWithHistory()
        ApplicationSubmission.getSubmittedBy(app.history) shouldBe None
      }
    }
  }
}
