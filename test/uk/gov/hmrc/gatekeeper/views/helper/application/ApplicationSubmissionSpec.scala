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

package uk.gov.hmrc.gatekeeper.views.helper.application

import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.gatekeeper.builder.ApplicationResponseBuilder
import uk.gov.hmrc.gatekeeper.models.State._

class ApplicationSubmissionSpec extends AsyncHmrcSpec with ApplicationResponseBuilder {
  "ApplicationsSubmission" when {
    "submittedBy" should {
      "is present" in {
        val app = anApplicationWithHistory(stateHistories = List(aStateHistory(PENDING_GATEKEEPER_APPROVAL)))
        ApplicationSubmission.getSubmittedBy(app.history) shouldBe Some("Unknown")
      }
      "is not present" in {
        val app = anApplicationWithHistory()
        ApplicationSubmission.getSubmittedBy(app.history) shouldBe None
      }
    }
  }
}
