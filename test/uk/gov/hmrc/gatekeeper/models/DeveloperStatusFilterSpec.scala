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

package uk.gov.hmrc.gatekeeper.models

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.apiplatform.modules.common.utils._

class DeveloperStatusFilterSpec extends HmrcSpec {

  def aUser(name: String, verified: Option[Boolean]): AbstractUser = {
    verified.fold[AbstractUser](UnregisteredUser(s"$name@example.com".toLaxEmail, UserId.random))(v =>
      RegisteredUser(s"$name@example.com".toLaxEmail, UserId.random, "Fred", "Example", v)
    )
  }

  "DeveloperStatusFilter parsing" should {

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
  "DeveloperStatusFilter isMatch" should {

    val verifiedUser     = aUser("user1", verified = Some(true))
    val unverifiedUser   = aUser("user2", verified = Some(false))
    val noneVerifiedUser = aUser("user3", verified = None)

    "match verified" in {
      DeveloperStatusFilter.VerifiedStatus.isMatch(verifiedUser) shouldBe true
      DeveloperStatusFilter.VerifiedStatus.isMatch(unverifiedUser) shouldBe false
      DeveloperStatusFilter.VerifiedStatus.isMatch(noneVerifiedUser) shouldBe true

    }

    "match unverified" in {
      DeveloperStatusFilter.UnverifiedStatus.isMatch(unverifiedUser) shouldBe true
      DeveloperStatusFilter.UnverifiedStatus.isMatch(verifiedUser) shouldBe false
      DeveloperStatusFilter.UnverifiedStatus.isMatch(noneVerifiedUser) shouldBe false
    }

    "match all" in {
      DeveloperStatusFilter.AllStatus.isMatch(unverifiedUser) shouldBe true
      DeveloperStatusFilter.AllStatus.isMatch(verifiedUser) shouldBe true
      DeveloperStatusFilter.AllStatus.isMatch(noneVerifiedUser) shouldBe true
    }
  }
}
