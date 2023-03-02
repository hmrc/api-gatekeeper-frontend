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

package uk.gov.hmrc.gatekeeper.encryption

import uk.gov.hmrc.apiplatform.modules.common.utils.HmrcSpec
import play.api.libs.json.Json

class PayloadEncryptionSpec extends HmrcSpec {
  
  case class EncryptMe(word: String = "demo", number: Int = 5)
  object EncryptMe {
    implicit val formatter = Json.format[EncryptMe]
  }

  "PayloadEncryption" should {
    val encryption1 = new PayloadEncryption("czV2OHkvQj9FKEgrTWJQZVNoVm1ZcTN0Nnc5eiRDJkY=")
    val encryption2 = new PayloadEncryption("SXlkQ2d3bWlTRUl6dXV2N09pYjByNitxN25mMGxEYz0=")

    "encrypt and decrypt a json payload" in {
      val encrypted = encryption1.encrypt[EncryptMe](EncryptMe())
      val decrypt = encryption1.decrypt[EncryptMe](encrypted)

      decrypt shouldBe EncryptMe()
    }

    "decrypt should require the correct key" in {
      val encrypted1 = encryption1.encrypt[EncryptMe](EncryptMe())
      val encrypted2 = encryption2.encrypt[EncryptMe](EncryptMe())

      encrypted1 should not be encrypted2
      
      // thisShouldWork
      encryption2.decrypt[EncryptMe](encrypted2)

      intercept[SecurityException] {
        encryption2.decrypt[EncryptMe](encrypted1)
      }
    }
  }
}
