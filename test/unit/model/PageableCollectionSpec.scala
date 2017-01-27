/*
 * Copyright 2017 HM Revenue & Customs
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

package unit.model

import model.PageableCollection
import org.scalatest.Matchers
import uk.gov.hmrc.play.test.UnitSpec


class PageableCollectionSpec  extends UnitSpec with Matchers {
  "Pageable collection" should {
    "with empty sequence" should {
      "at page 1" in {
        val coll = PageableCollection(Nil, 1, 10)
        coll.start shouldBe 0
        coll.end shouldBe 0
        coll.projection shouldBe Nil
        coll.valid shouldBe true
        coll.hasPrevious shouldBe false
        coll.hasNext shouldBe false
      }

      "at page 2" in {
        val coll = PageableCollection(Nil, 2, 10)
        coll.start shouldBe 0
        coll.end shouldBe 0
        coll.projection shouldBe Nil
        coll.valid shouldBe false
        coll.hasPrevious shouldBe false
        coll.hasNext shouldBe false
      }

      "at page -1" in {
        val coll = PageableCollection(Nil, -1, 10)
        coll.start shouldBe 0
        coll.end shouldBe 0
        coll.projection shouldBe Nil
        coll.valid shouldBe false
        coll.hasPrevious shouldBe false
        coll.hasNext shouldBe false
      }
    }
    "with less than page size items" should {
      "at page 1" in {
        val coll = PageableCollection(1 to 6, 1, 10)
        coll.start shouldBe 0
        coll.end shouldBe 6
        coll.projection shouldBe List(1,2,3,4,5,6)
        coll.valid shouldBe true
        coll.hasPrevious shouldBe false
        coll.hasNext shouldBe false
      }

      "at page 2" in {
        val coll = PageableCollection(1 to 6, 2, 10)
        coll.start shouldBe 0
        coll.end shouldBe 6
        coll.projection shouldBe List(1,2,3,4,5,6)
        coll.valid shouldBe false
        coll.hasPrevious shouldBe false
        coll.hasNext shouldBe false
      }

      "at page -1" in {
        val coll = PageableCollection(1 to 6, -1, 10)
        coll.start shouldBe 0
        coll.end shouldBe 6
        coll.projection shouldBe List(1,2,3,4,5,6)
        coll.valid shouldBe false
        coll.hasPrevious shouldBe false
        coll.hasNext shouldBe false
      }
    }
    "with several pages worth of items" should {
      "at page 1" in {
        val coll = PageableCollection(1 to 39, 1, 10)
        coll.start shouldBe 0
        coll.end shouldBe 10
        coll.projection shouldBe List(1,2,3,4,5,6,7,8,9,10)
        coll.valid shouldBe true
        coll.hasPrevious shouldBe false
        coll.hasNext shouldBe true
      }

      "at page 2" in {
        val coll = PageableCollection(1 to 39, 2, 10)
        coll.start shouldBe 10
        coll.end shouldBe 20
        coll.projection shouldBe List(11,12,13,14,15,16,17,18,19,20)
        coll.valid shouldBe true
        coll.hasPrevious shouldBe true
        coll.hasNext shouldBe true
      }

      "at page -1" in {
        val coll = PageableCollection(1 to 39, -1, 10)
        coll.start shouldBe 0
        coll.end shouldBe 10
        coll.projection shouldBe Nil
        coll.valid shouldBe false
        coll.hasPrevious shouldBe false
        coll.hasNext shouldBe false
      }
    }
  }
}
