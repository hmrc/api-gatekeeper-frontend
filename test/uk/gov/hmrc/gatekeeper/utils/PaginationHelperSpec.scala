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

package uk.gov.hmrc.gatekeeper.utils

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

class PaginationHelperSpec extends AnyWordSpec with Matchers {
  "PaginationHelper" should {
    "get max page size" in {
      PaginationHelper.maxPage(totalResults = 0, pageSize = 10) shouldBe 0
      PaginationHelper.maxPage(totalResults = 1, pageSize = 10) shouldBe 1
      PaginationHelper.maxPage(totalResults = 9, pageSize = 10) shouldBe 1
      PaginationHelper.maxPage(totalResults = 10, pageSize = 10) shouldBe 1
      PaginationHelper.maxPage(totalResults = 11, pageSize = 10) shouldBe 2
      PaginationHelper.maxPage(totalResults = 21, pageSize = 10) shouldBe 3
    }

    "handle page size of 0" in {
      PaginationHelper.maxPage(totalResults = 0, pageSize = 0) shouldBe 0
    }
  }
}
