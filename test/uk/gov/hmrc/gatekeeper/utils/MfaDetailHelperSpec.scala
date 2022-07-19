/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.utils
import java.time.LocalDateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.gatekeeper.models.{AuthenticatorAppMfaDetailSummary, MfaId}

import java.util.UUID

class MfaDetailHelperSpec extends AnyWordSpec with Matchers {

  "MfaDetailHelper" when {
    def generateDetail(verified: Boolean): AuthenticatorAppMfaDetailSummary ={
      AuthenticatorAppMfaDetailSummary(MfaId(UUID.randomUUID()), "name", LocalDateTime.now(), verified = verified)
    }
    "isAuthAppMfaVerified" should {
      "return false when MfaDetails are empty List" in {
        val mfaDetails = List.empty

        MfaDetailHelper.isAuthAppMfaVerified(mfaDetails) shouldBe false
      }

      "return false when MfaDetails contains detail where verified is false" in {
        val mfaDetails = List(generateDetail(false))

        MfaDetailHelper.isAuthAppMfaVerified(mfaDetails) shouldBe false
      }

      "return true when MfaDetails contains detail where verified is true" in {
        val mfaDetails = List(generateDetail(true))

        MfaDetailHelper.isAuthAppMfaVerified(mfaDetails) shouldBe true
      }

      "return true when MfaDetails contains details with same typ, one is true" in {
        val mfaDetails = List(generateDetail(false), generateDetail(false), generateDetail(true))

        MfaDetailHelper.isAuthAppMfaVerified(mfaDetails) shouldBe true
      }

      "return false when MfaDetails contains details with same type, all are false" in {
        val mfaDetails = List(generateDetail(false), generateDetail(false), generateDetail(false))

        MfaDetailHelper.isAuthAppMfaVerified(mfaDetails) shouldBe false
      }

      "return true when MfaDetails contains details with same type, all are true" in {
        val mfaDetails = List(generateDetail(true), generateDetail(true), generateDetail(true))

        MfaDetailHelper.isAuthAppMfaVerified(mfaDetails) shouldBe true
      }
    }
  }
}
