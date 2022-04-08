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

object CsvHelper {
  case class ColumnDefinition[T](name: String, getValue : T => String)

  def toCsvString[T](csvColumnDefinitions: Seq[ColumnDefinition[T]], data: Seq[T]) : String = {
  
    val csvSperator = ","

    def getCsvRowValues(dataItem: T) = {
      csvColumnDefinitions.map(_.getValue(dataItem)).mkString(csvSperator)
    }

    val headerRow = csvColumnDefinitions.map(columns => columns.name).mkString(csvSperator)
    val csvRows = data.map(getCsvRowValues)
    
    val headerAndApplicationRows = headerRow +: csvRows
    headerAndApplicationRows.mkString(System.lineSeparator())
  }
}
