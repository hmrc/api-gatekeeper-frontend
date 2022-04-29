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

package utils

import java.io.StringWriter

object CsvHelper {
  case class ColumnDefinition[T](name: String, getValue : T => String)

  def toCsvString[T](csvColumnDefinitions: Seq[ColumnDefinition[T]], data: Seq[T]) : String = {
    import org.apache.commons.csv.{CSVFormat, CSVPrinter}

    val headers: Seq[String]  = csvColumnDefinitions.map(_.name)

    val format = CSVFormat.RFC4180.builder
      .setHeader(headers: _*)
      .setRecordSeparator(System.lineSeparator())
      .build()

    val output = new StringWriter()
    val printer = new CSVPrinter(output, format)

    def getCsvRowValues(dataItem: T) : Seq[String] = {
      csvColumnDefinitions.map(_.getValue(dataItem))
    }

    data.foreach(row => printer.printRecord(getCsvRowValues(row): _*))

    output.getBuffer.toString
  }
}
