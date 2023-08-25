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

package uk.gov.hmrc.apiplatform.modules.events.connectors

import play.api.libs.json.Json

case class FilterValue(description: String, t: String)

object FilterValue {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json._

  implicit val format = ((__ \ "description").format[String] and
    (__ \ "type").format[String])(FilterValue.apply, unlift(FilterValue.unapply))
}

case class QueryableValues(eventTags: List[FilterValue], actorTypes: List[FilterValue])

object QueryableValues {
  implicit val format = Json.format[QueryableValues]
}
