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

package model

import org.scalatest.{Matchers, WordSpec}

class DeveloperStatusFilterSpec extends WordSpec with Matchers {
  "DeveloperStatusFilter" should {

    "parses verified" in {
      DeveloperStatusFilter(Some("VERIFIED")) shouldBe DeveloperStatusFilter.VerifiedStatus
    }

    "parses unverified" in {
      DeveloperStatusFilter(Some("UNVERIFIED")) shouldBe DeveloperStatusFilter.UnverifiedStatus
    }

    "parses all" in {
      DeveloperStatusFilter(Some("ALL")) shouldBe DeveloperStatusFilter.AllStatus
    }

    "parses None" in {
      DeveloperStatusFilter(None) shouldBe DeveloperStatusFilter.AllStatus
    }

    "throw an exception if developer status is invalid" in {

      val invalidStatusString = "Invalid status"

      val thrown = intercept[Exception] {
        DeveloperStatusFilter(Some(invalidStatusString))
      }
      assert(thrown.getMessage === "Invalid developer status filter: " + invalidStatusString)
    }
  }
}