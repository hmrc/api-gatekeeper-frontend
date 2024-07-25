/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatform.modules.deskpro.models

import play.api.libs.json.{Json, OFormat}

case class DeskproPerson(id: Int, name: String)

object DeskproPerson {
  implicit val format: OFormat[DeskproPerson] = Json.format[DeskproPerson]
}

case class DeskproPeopleResponse(data: List[DeskproPerson])

object DeskproPeopleResponse {
  implicit val format: OFormat[DeskproPeopleResponse] = Json.format[DeskproPeopleResponse]
}

case class DeskproCreatePersonRequest(name: String, primary_email: String)

object DeskproCreatePersonRequest {
  implicit val format: OFormat[DeskproCreatePersonRequest] = Json.format[DeskproCreatePersonRequest]
}
