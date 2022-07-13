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

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.gatekeeper.utils.CsvHelper._

class CsvHelperSpec extends AnyWordSpec with Matchers{
  "CsvHelper" should {
    "turn data to a CSV" in {
      val data = Seq("Hello", "World!")

      val columnDefinitions = Seq[ColumnDefinition[String]](
        ColumnDefinition("Value", (text=>text)),
        ColumnDefinition("Length", (text=>text.length().toString()))
      )
      val csvText = toCsvString(columnDefinitions, data)

      csvText shouldBe """Value,Length
Hello,5
World!,6
"""
    }
  }
  
  "CsvHelper" should {
    "handle line break characters in data" in {
      val data = Seq("\n")

      val columnDefinitions = Seq[ColumnDefinition[String]](
        ColumnDefinition("Value", (text=>text)),
        ColumnDefinition("Length", (text=>text.length().toString()))
      )
      val csvText = toCsvString(columnDefinitions, data)

      csvText shouldBe "Value,Length\n\"\n\",1\n" 
    }
  }
}
