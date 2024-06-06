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

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.GrantLength
import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec

class GrantLengthDisplayerSpec extends AsyncHmrcSpec {
  "display" should {
    "return text for 4 hour grant length when Grant length is 4 hours" in {
      GrantLengthDisplayer.display(GrantLength.FOUR_HOURS) shouldBe "4 hours + no refresh token"
    }

    "return text for 18 month grant length when Grant length is 18 months" in {
      GrantLengthDisplayer.display(GrantLength.EIGHTEEN_MONTHS) shouldBe "18 months"
    }
  }

}
