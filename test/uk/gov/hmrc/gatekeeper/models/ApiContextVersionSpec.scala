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

import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec

class ApiContextVersionSpec extends AsyncHmrcSpec {

  "ApiContextVersionFilter" when {
    "given an api context and version pattern" in {

      val apiContextVersion = ApiContextVersion(Some("my-api-context__1.0"))

      apiContextVersion.nonEmpty shouldBe true
      apiContextVersion.get.context.value shouldBe "my-api-context"
      apiContextVersion.get.versionNbr.value shouldBe "1.0"
    }

    "given an invalid api context and version should error" in {
      val thrown = intercept[Exception] {
        ApiContextVersion(Some("notValid"))
      }

      thrown.getMessage shouldBe "Invalid API context or version"
    }
  }
}
