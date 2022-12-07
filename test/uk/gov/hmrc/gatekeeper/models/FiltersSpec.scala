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

package uk.gov.hmrc.gatekeeper.models

import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec

class FiltersSpec extends AsyncHmrcSpec {

  "ApiFilter" when {
    "given an api id pattern" should {
      "create a valid Value for a simple service name and dotted decimal version" in {
        val context = "a-test-service"
        val version = "1.0"

        ApiFilter(Some(s"${context}__$version")) shouldBe Value(context, version)
      }

      "create a valid Value for a simple service name and extended version" in {
        val context = "a-test-service"
        val version = "0.0.1-pre-alpha"

        ApiFilter(Some(s"${context}__$version")) shouldBe Value(context, version)
      }

      "create a valid Value for a complex service name and extended version" in {
        val context = "a-test-service__with__double__underscores"
        val version = "0.0.1-pre-alpha"

        ApiFilter(Some(s"${context}__$version")) shouldBe Value(context, version)
      }
    }
  }
}
