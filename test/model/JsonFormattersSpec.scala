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

package model

import model.{APIStatus, APIStatusJson}
import play.api.libs.json.{JsError, JsObject, JsString, JsSuccess}
import uk.gov.hmrc.play.test.UnitSpec

class JsonFormattersSpec extends UnitSpec {

  trait Setup {
    val underTest = APIStatusJson.apiStatusReads(APIStatus)
  }

  "reads" should {
    "map API status of PROTOTYPED to BETA" in new Setup {

      val result = underTest.reads(JsString("PROTOTYPED"))

      result shouldBe JsSuccess(APIStatus.BETA)
    }

    "map API status of PUBLISHED to STABLE" in new Setup {

      val result = underTest.reads(JsString("PUBLISHED"))

      result shouldBe JsSuccess(APIStatus.STABLE)
    }

    "map API status of ALPHA to ALPHA" in new Setup {

      val result = underTest.reads(JsString("ALPHA"))

      result shouldBe JsSuccess(APIStatus.ALPHA)
    }

    "map API status of DEPRECATED to DEPRECATED" in new Setup {

      val result = underTest.reads(JsString("DEPRECATED"))

      result shouldBe JsSuccess(APIStatus.DEPRECATED)
    }

    "map API status of RETIRED to RETIRED" in new Setup {

      val result = underTest.reads(JsString("RETIRED"))

      result shouldBe JsSuccess(APIStatus.RETIRED)
    }

    "error when the status is unrecognised" in new Setup {

      val result = underTest.reads(JsString("NOT_A_STATUS"))

      result shouldBe JsError(s"Enumeration expected of type: APIStatus, but it does not contain 'NOT_A_STATUS'")
    }

    "error when not given a JsString" in new Setup {

      val result = underTest.reads(JsObject(Map("I'm an object" -> JsString("value"))))

      result shouldBe JsError("String value expected")
    }
  }
}
