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

import uk.gov.hmrc.apiplatform.modules.common.utils.HmrcSpec

class SortingHelperSpec extends HmrcSpec {

  "SortingHelper" when {
    "calling descendingVersionWithoutFields" should {
      "put version 1.0 and 2.0 in order regardless of parameter order" in {
        val list1 = List("1.0", "2.0")
        val list2 = List("2.0", "1.0")
        list1.sortWith(SortingHelper.versionSorter) shouldBe list2.sortWith(SortingHelper.versionSorter)
      }
      "put version 1.0 and 2.0 in descending order" in {
        val list = List("1.0", "2.0")
        list.sortWith(SortingHelper.versionSorter) shouldBe List("2.0", "1.0")
      }
      "put version 1.0 and 1.1 in descending order" in {
        val list = List("1.0", "1.1")
        list.sortWith(SortingHelper.versionSorter) shouldBe List("1.1", "1.0")
      }
      "put versions in descending order" in {
        val list = List("1.0", "2.0", "1.1", "2.1")
        list.sortWith(SortingHelper.versionSorter) shouldBe List("2.1", "2.0", "1.1", "1.0")
      }
      "cope with non numerics mixed in" in {
        val list = List("P1.1", "2.x", "1.0", "2.0")
        list.sortWith(SortingHelper.versionSorter) shouldBe List("2.x", "2.0", "P1.1", "1.0")
      }
    }
  }
}
