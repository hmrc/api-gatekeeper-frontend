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
import uk.gov.hmrc.gatekeeper.models.MfaType.{AUTHENTICATOR_APP, SMS}
import uk.gov.hmrc.gatekeeper.models.{AuthenticatorAppMfaDetailSummary, MfaDetail, MfaId, MfaType, SmsMfaDetail}

import java.util.UUID

class MfaDetailHelperSpec extends AnyWordSpec with Matchers {

  "MfaDetailHelper" when {
    def generateDetail(verified: Boolean, mfaType: MfaType): MfaDetail = {
      mfaType match {
        case AUTHENTICATOR_APP => AuthenticatorAppMfaDetailSummary(MfaId(UUID.randomUUID()), "name", LocalDateTime.now(), verified = verified)
        case SMS => SmsMfaDetail(name = "****6789", createdOn = LocalDateTime.now(), verified = verified, mobileNumber = "0123456789")
      }
    }

    "isMfaVerified(" should {
      "return false when MfaDetails are empty List" in {
        val mfaDetails = List.empty

        MfaDetailHelper.isMfaVerified(mfaDetails) shouldBe false
      }

      "return false when MfaDetails contains auth app mfa detail where verified is false" in {
        val mfaDetails = List(generateDetail(false, AUTHENTICATOR_APP))

        MfaDetailHelper.isMfaVerified(mfaDetails) shouldBe false
      }

      "return true when MfaDetails contains auth app mfa detail where verified is true" in {
        val mfaDetails = List(generateDetail(true, AUTHENTICATOR_APP))

        MfaDetailHelper.isMfaVerified(mfaDetails) shouldBe true
      }

      "return false when MfaDetails contains sms mfa detail where verified is false" in {
        val mfaDetails = List(generateDetail(false, SMS))

        MfaDetailHelper.isMfaVerified(mfaDetails) shouldBe false
      }

      "return true when MfaDetails contains sms mfa detail where verified is true" in {
        val mfaDetails = List(generateDetail(true, SMS))

        MfaDetailHelper.isMfaVerified(mfaDetails) shouldBe true
      }

      "return true when MfaDetails contains verified auth app mfa detail and unverified sms mfa detail" in {
        val mfaDetails = List(generateDetail(true, AUTHENTICATOR_APP), generateDetail(false, SMS))

        MfaDetailHelper.isMfaVerified(mfaDetails) shouldBe true
      }

      "return true when MfaDetails contains verified sms mfa detail and unverified auth app mfa detail" in {
        val mfaDetails = List(generateDetail(false, AUTHENTICATOR_APP), generateDetail(true, SMS))

        MfaDetailHelper.isMfaVerified(mfaDetails) shouldBe true
      }

      "return true when MfaDetails contains verified both sms and auth app mfa details" in {
        val mfaDetails = List(generateDetail(true, AUTHENTICATOR_APP), generateDetail(true, SMS))

        MfaDetailHelper.isMfaVerified(mfaDetails) shouldBe true
      }

      "return true when MfaDetails contains unverified sms mfa detail and unverified auth app mfa detail" in {
        val mfaDetails = List(generateDetail(false, AUTHENTICATOR_APP), generateDetail(false, SMS))

        MfaDetailHelper.isMfaVerified(mfaDetails) shouldBe false
      }
    }
  }
}
