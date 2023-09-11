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

package uk.gov.hmrc.apiplatform.modules.apis.domain.models

import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.play.json.Union

sealed trait ApiAccess {
  lazy val displayText: String = ApiAccess.displayText(this)
  lazy val accessType: ApiAccessType = ApiAccess.accessType(this)
}

object ApiAccess {
  case object PUBLIC                                                                           extends ApiAccess
  case class Private(whitelistedApplicationIds: List[String], isTrial: Option[Boolean] = None) extends ApiAccess

  def displayText(apiAccess: ApiAccess): String = apiAccess match {
    case PUBLIC => "Public"
    case Private(_,_) => "Private"
  }

  private implicit val formatPublicApiAccess = Json.format[PUBLIC.type]

  private implicit val readsPrivateApiAccess: Reads[Private] = (
    ((JsPath \ "whitelistedApplicationIds").read[List[String]] or Reads.pure(List.empty[String])) and
      (JsPath \ "isTrial").readNullable[Boolean]
  )(Private.apply _)

  private implicit val writesPrivateApiAccess: OWrites[Private] = Json.writes[Private]

  implicit val formatApiAccess: Format[ApiAccess] = Union.from[ApiAccess]("type")
    .and[PUBLIC.type]("PUBLIC")
    .and[Private]("PRIVATE")
    .format

  def accessType(apiAccess: ApiAccess) = apiAccess match {
    case PUBLIC => ApiAccessType.PUBLIC
    case Private(_,_) => ApiAccessType.PRIVATE
  }
}
